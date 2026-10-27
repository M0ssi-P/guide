package live

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage
import kotlin.concurrent.thread

/*
 * Live sharing over the local network, with no server in between:
 * the host announces itself with a UDP broadcast every two seconds and pushes its position over a
 * WebSocket a few times a second; listeners report back (language, mode, how far off they are) on the same socket.
 */

const val LIVE_WS_PORT = 47802
const val LIVE_DISCOVERY_PORT = 47801
private const val MAGIC = "THEGUIDE-LIVE"

private val gson = Gson()

/** What the host is showing and playing right now. */
data class LiveState(
    val title: String = "",
    val code: String = "",
    val location: String = "",
    val paragraph: Int? = null,
    /** How far into [paragraph] the host is, 0..1 by characters, for listeners whose text has no matching index. */
    val fraction: Float = 0f,
    /** The live Table sentence as its segment index [section, block, sentence], and how much of it is spoken. */
    val sentence: List<Int>? = null,
    val progress: Float = 0f,
    /** Seconds into the host's own recording; only meaningful to someone playing the same file. */
    val position: Double = 0.0,
    val playing: Boolean = false,
    /** Bumped by "Resync everyone": listeners jump straight to the host's place. */
    val resync: Int = 0,
    val hostName: String = "",
    val language: String = "",
)

data class ListenerReport(
    val id: String = "",
    val name: String = "",
    val language: String = "",
    val mode: String = "",
    val lag: Double = 0.0,
    val audio: Boolean = true,
)

data class ConnectedListener(val report: ListenerReport, val lastSeen: Long)

data class LiveHostInfo(
    val address: String, val port: Int, val name: String, val title: String, val code: String,
    val listeners: Int, val seenAt: Long, val joinCode: String = "", val startedAt: Long = 0,
)

object LiveHost {
    val running = mutableStateOf(false)
    val listeners = mutableStateMapOf<String, ConnectedListener>()
    /** Four digits shown to people joining; typing it picks this host. */
    val joinCode = mutableStateOf("")
    @Volatile var state = LiveState()
        private set
    private var server: WsServer? = null
    private var startedAt = 0L

    fun update(next: LiveState) { state = next.copy(resync = state.resync, hostName = state.hostName) }
    fun resyncEveryone() { state = state.copy(resync = state.resync + 1) }

    /** Listeners not heard from for 6 seconds are dropped. */
    fun active(): List<ConnectedListener> {
        val cutoff = System.currentTimeMillis() - 6000
        listeners.entries.removeAll { it.value.lastSeen < cutoff }
        return listeners.values.sortedBy { it.report.name }
    }

    fun start(hostName: String) {
        if (running.value) return
        state = state.copy(hostName = hostName)
        joinCode.value = (1000..9999).random().toString()
        startedAt = System.currentTimeMillis()
        server = WsServer(LIVE_WS_PORT) { client, text ->
            val o = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return@WsServer
            when (o.get("t")?.asString) {
                "hello" -> runCatching { gson.fromJson(o, ListenerReport::class.java) }.getOrNull()?.takeIf { it.id.isNotBlank() }
                    ?.let { listeners[it.id] = ConnectedListener(it, System.currentTimeMillis()) }
                "ping" -> client.send("""{"t":"pong","at":${o.get("at")?.asLong ?: 0}}""")
            }
        }.also { it.start() }
        running.value = true
        // push the position five times a second
        thread(isDaemon = true, name = "live-push") {
            while (running.value) {
                server?.broadcast(gson.toJsonTree(state).asJsonObject.apply { addProperty("t", "state") }.toString())
                Thread.sleep(200)
            }
        }
        thread(isDaemon = true, name = "live-announcer") {
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    while (running.value) {
                        val msg = listOf(MAGIC, "2", LIVE_WS_PORT, hostName, state.title, state.code, active().size, joinCode.value, startedAt)
                            .joinToString("|") { it.toString().replace("|", "/") }.toByteArray()
                        broadcastAddresses().forEach { addr -> runCatching { socket.send(DatagramPacket(msg, msg.size, addr, LIVE_DISCOVERY_PORT)) } }
                        Thread.sleep(2000)
                    }
                }
            }
        }
    }

    fun stop() {
        running.value = false
        server?.stop()
        server = null
        listeners.clear()
    }

    private fun broadcastAddresses(): List<InetAddress> =
        (runCatching {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses }.mapNotNull { it.broadcast }
        }.getOrNull() ?: emptyList()).ifEmpty { listOf(InetAddress.getByName("255.255.255.255")) }
}

object LiveDiscovery {
    private val _hosts = MutableStateFlow<List<LiveHostInfo>>(emptyList())
    /** Hosts heard on the network in the last 6 seconds (this machine's own host left out). */
    val hosts: StateFlow<List<LiveHostInfo>> = _hosts.asStateFlow()
    val chosen = mutableStateOf<LiveHostInfo?>(null)
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        thread(isDaemon = true, name = "live-discovery") {
            runCatching {
                DatagramSocket(null).use { socket ->
                    socket.reuseAddress = true
                    socket.soTimeout = 2000
                    socket.bind(InetSocketAddress(LIVE_DISCOVERY_PORT))
                    val buf = ByteArray(2048)
                    val seen = mutableMapOf<String, LiveHostInfo>()
                    val own = runCatching { NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }.map { it.hostAddress }.toSet() }.getOrDefault(emptySet())
                    while (true) {
                        val packet = DatagramPacket(buf, buf.size)
                        val got = runCatching { socket.receive(packet) }.isSuccess
                        val now = System.currentTimeMillis()
                        if (got) {
                            val parts = String(packet.data, 0, packet.length).split("|")
                            val address = packet.address.hostAddress
                            if (parts.size >= 8 && parts[0] == MAGIC && !(LiveHost.running.value && address in own)) {
                                seen[address] = LiveHostInfo(
                                    address, parts[2].toIntOrNull() ?: LIVE_WS_PORT, parts[3], parts[4], parts[5],
                                    parts[6].toIntOrNull() ?: 0, now, parts[7], parts.getOrNull(8)?.toLongOrNull() ?: 0
                                )
                            }
                        }
                        seen.entries.removeAll { now - it.value.seenAt > 6000 }
                        _hosts.value = seen.values.sortedBy { it.name }
                    }
                }
            }
            started = false
        }
    }
}

/**
 * The listener's socket to a host. [onState] gets every pushed state; [onPong] the round trip in ms;
 * [onClosed] fires when the link drops so the caller can retry.
 */
class LiveConnection(val host: LiveHostInfo, private val onState: (LiveState) -> Unit, private val onPong: (Long) -> Unit, private val onClosed: () -> Unit) {
    @Volatile private var ws: WebSocket? = null
    val open get() = ws != null

    fun connect() {
        val buf = StringBuilder()
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build().newWebSocketBuilder()
            .buildAsync(URI("ws://${host.address}:${host.port}/live"), object : WebSocket.Listener {
                override fun onOpen(webSocket: WebSocket) { ws = webSocket; webSocket.request(1) }
                override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                    buf.append(data)
                    if (last) {
                        val text = buf.toString(); buf.clear()
                        runCatching {
                            val o = JsonParser.parseString(text).asJsonObject
                            when (o.get("t")?.asString) {
                                "state" -> onState(gson.fromJson(o, LiveState::class.java))
                                "pong" -> onPong(System.currentTimeMillis() - (o.get("at")?.asLong ?: 0))
                            }
                        }
                    }
                    webSocket.request(1)
                    return null
                }
                override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? { ws = null; onClosed(); return null }
                override fun onError(webSocket: WebSocket, error: Throwable) { ws = null; onClosed() }
            })
            .whenComplete { _, err -> if (err != null) { ws = null; onClosed() } }
    }

    // the JDK socket allows one outstanding send at a time
    @Synchronized private fun send(text: String) { runCatching { ws?.sendText(text, true)?.join() } }
    fun report(r: ListenerReport) = send(gson.toJsonTree(r).asJsonObject.apply { addProperty("t", "hello") }.toString())
    fun ping() = send("""{"t":"ping","at":${System.currentTimeMillis()}}""")
    fun close() { runCatching { ws?.sendClose(WebSocket.NORMAL_CLOSURE, "bye") }; ws = null }
}
