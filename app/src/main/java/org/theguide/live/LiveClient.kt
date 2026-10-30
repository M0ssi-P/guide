package org.theguide.live

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import live.LIVE_DISCOVERY_PORT
import live.ListenerReport
import live.LiveHostInfo
import live.LiveState
import live.SyncAction
import live.decideSync
import live.parseAnnouncement
import live.sameRecording
import okHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.theguide.App
import org.theguide.audio.Audio
import org.theguide.data.Prefs
import org.theguide.data.Sentence
import org.theguide.data.Tables
import org.theguide.data.cumulativeShares
import org.theguide.data.isTimed
import org.theguide.data.locate
import org.theguide.data.locateTime
import org.theguide.data.sentences
import parsers.vgr.TimeMaps
import parsers.vgr.toRecording
import parsers.vgr.toTimeline
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Following a desktop host from the phone or TV, over the same wire as the desktop listener: hosts are heard on the
 * Wi-Fi, the host's state arrives over a WebSocket five times a second, and the listener's own recording is kept on the
 * host's sentence with the shared [decideSync]. Phones and TVs only join; they never host.
 */
object LiveClient {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val id = UUID.randomUUID().toString()

    var hosts by mutableStateOf<List<LiveHostInfo>>(emptyList()); private set
    var host by mutableStateOf<LiveHostInfo?>(null); private set
    var state by mutableStateOf<LiveState?>(null); private set
    var connected by mutableStateOf(false); private set
    var listening by mutableStateOf(false); private set
    /** ISO3 of the language followed. */
    var lang by mutableStateOf(Prefs.readingLang); private set
    var textOnly by mutableStateOf(false)
    var lag by mutableDoubleStateOf(0.0); private set
    /** The followed sermon's sentences in [lang], for the words on screen. */
    var items by mutableStateOf<List<Sentence>>(emptyList()); private set
    private var ws: WebSocket? = null
    private var receivedAt = 0L
    private var loop: Job? = null
    @Volatile private var discovering = false

    /** Listens for hosts' announcements; hosts not heard for 6 seconds drop off. */
    fun discover() {
        if (discovering) return
        discovering = true
        thread(isDaemon = true, name = "live-discovery") {
            val wifi = App.context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("guide-live").apply { setReferenceCounted(false); acquire() }
            runCatching {
                DatagramSocket(null).use { socket ->
                    socket.reuseAddress = true
                    socket.broadcast = true
                    socket.soTimeout = 2000
                    socket.bind(InetSocketAddress(LIVE_DISCOVERY_PORT))
                    val buf = ByteArray(2048)
                    val seen = mutableMapOf<String, LiveHostInfo>()
                    while (discovering) {
                        val packet = DatagramPacket(buf, buf.size)
                        val got = runCatching { socket.receive(packet) }.isSuccess
                        val now = System.currentTimeMillis()
                        if (got) parseAnnouncement(String(packet.data, 0, packet.length), packet.address.hostAddress ?: "", now)?.let { seen[it.address] = it }
                        seen.entries.removeAll { now - it.value.seenAt > 6000 }
                        val list = seen.values.sortedBy { it.name }
                        scope.launch { hosts = list }
                    }
                }
            }.onFailure { it.printStackTrace() }
            lock.release()
            discovering = false
        }
    }

    /** The host whose four-digit live ID this is. */
    fun byCode(code: String) = hosts.firstOrNull { it.joinCode == code }

    fun connect(h: LiveHostInfo) {
        if (host?.address == h.address && connected) return
        ws?.close(1000, "bye")
        host = h
        ws = okHttpClient.newWebSocket(Request.Builder().url("ws://${h.address}:${h.port}/live").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { scope.launch { connected = true } }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val o = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return
                if (o.get("t")?.asString == "state") {
                    val s = runCatching { gson.fromJson(o, LiveState::class.java) }.getOrNull() ?: return
                    scope.launch { state = s; receivedAt = System.currentTimeMillis(); connected = true }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { scope.launch { connected = false } }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { scope.launch { connected = false } }
        })
    }

    /** Starts listening in [iso3]: loads the host's sermon in that language and follows it. */
    fun join(iso3: String) {
        lang = iso3
        listening = true
        jumpNext = true
        loadedFor = null
        loop?.cancel()
        loop = scope.launch {
            var tick = 0
            while (isActive) {
                step()
                if (tick % 5 == 0) report()
                if (!connected && tick > 0 && tick % 10 == 0) host?.let { connect(it) }
                tick++
                delay(200)
            }
        }
        Audio.liveHost = {
            val s = state
            if (!listening || !connected || s == null || s.timeline <= 0) null
            else (s.timeline + if (s.playing) ((System.currentTimeMillis() - receivedAt) / 1000.0).coerceIn(0.0, 1.0) else 0.0) to s.playing
        }
    }

    /** Another language mid-service: the new voice picks up at the same sentence. */
    fun switchLanguage(iso3: String) { lang = iso3; loadedFor = null; jumpNext = true }

    fun resync() { jumpNext = true; host?.let { ws?.close(1000, "resync"); connected = false; connect(it) } }

    /** Leaves the host; with [keep] the sermon stays loaded to carry on alone. */
    fun leave(keep: Boolean) {
        listening = false
        loop?.cancel()
        Audio.liveHost = null
        Audio.liveDriven = false
        ws?.close(1000, "bye")
        ws = null
        connected = false
        host = null
        state = null
        if (!keep) Audio.pause() else Audio.liveRate(1.0)
    }

    private fun report() {
        val name = Prefs.liveName.ifBlank { "Listener" }
        val r = ListenerReport(id, name, Tables.localName(lang), if (textOnly) "Text only" else "Auto-center", lag, !textOnly)
        ws?.send(gson.toJsonTree(r).asJsonObject.apply { addProperty("t", "hello") }.toString())
        ws?.send("""{"t":"ping","at":${System.currentTimeMillis()}}""")
    }

    private var loadedFor: String? = null
    private var jumpNext = true
    private var holding = false
    private var rate = 1.0
    private var lastSeek = 0L
    private var lastHostPlaying: Boolean? = null
    private var lastHostPos = 0.0
    private var lastHostAt = 0L
    private var lastResync = -1

    /** The host's place in this language's sentences: by its timeline place, else its sentence index, else its paragraph. */
    fun locateHost(list: List<Sentence>, s: LiveState): Pair<Int, Float> =
        (if (s.timeline > 0 && isTimed(list)) locateTime(list, s.timeline, Double.MAX_VALUE) else null)
            ?: list.indexOfFirst { it.key != null && it.key == s.sentence }.takeIf { it >= 0 }?.let { it to s.progress }
            ?: locate(list, s.paragraph, s.fraction)

    /** One sync step: the desktop listener's, with ExoPlayer. */
    private suspend fun step() {
        val s = state ?: return
        // the host's sermon in our language
        if (loadedFor != "${s.code}/$lang" && s.code.isNotBlank()) {
            loadedFor = "${s.code}/$lang"
            val sermon = Tables.sermon(lang, s.code)
            if (sermon == null) { items = emptyList(); return }
            val full = Tables.full(lang, sermon)
            items = full.sentences()
            if (!textOnly) { Audio.play(lang, full, autoplay = s.playing); Audio.liveDriven = true; lastHostPlaying = null; jumpNext = true }
            return
        }
        if (textOnly) { if (Audio.playing) Audio.pause(); return }
        if (!connected) { Audio.liveRate(1.0); return }
        if (s.playing != lastHostPlaying) { if (s.playing) Audio.resume() else Audio.pause(); lastHostPlaying = s.playing; holding = false }
        val duration = Audio.duration
        if (duration <= 0) return
        val now = System.currentTimeMillis()
        val expected = if (s.playing) (now - lastHostAt) / 1000.0 else 0.0
        val hostJumped = lastHostAt > 0 && abs((s.position - lastHostPos) - expected) > 3.0
        lastHostPos = s.position; lastHostAt = now
        val resync = lastResync >= 0 && s.resync != lastResync
        lastResync = s.resync
        if (!s.playing) return
        if (now - lastSeek < 1200 && !resync) return
        val ourTime = Audio.position
        val inFlight = ((now - receivedAt) / 1000.0).coerceIn(0.0, 1.0)
        val src = Audio.track?.source
        val same = sameRecording(s.tableLanguage, s.code, Tables.tag(lang), Audio.track?.sermon?.dateCode)
        val action = if (same) {
            val target = s.position + inFlight
            val e = target - ourTime
            lag = abs(e)
            if (jumpNext || resync || hostJumped || abs(e) > 2.0) SyncAction.Seek(target, false) else SyncAction.Play(1.0 + (e * 0.1).coerceIn(-0.05, 0.05))
        } else {
            val map = TimeMaps.get(src)
            val tl = map.toTimeline(duration)
            val (i, p) = locateHost(items, s)
            val it = items.getOrNull(i) ?: return
            val (start, end) = if (it.start != null && isTimed(items)) it.start to (it.end ?: items.drop(i + 1).firstNotNullOfOrNull { n -> n.start } ?: (it.start + .1))
            else { val sh = cumulativeShares(items); ((sh.getOrNull(i - 1) ?: 0f) * tl).toDouble() to ((sh.getOrNull(i) ?: 1f) * tl).toDouble() }
            val tlTarget = if (s.timeline > 0 && isTimed(items)) s.timeline else start + p * (end - start)
            val rStart = map.toRecording(start); val rEnd = map.toRecording(end)
            val ratio = if (s.span > .05 && rEnd - rStart > .05) (rEnd - rStart) / s.span else 1.0
            val target = map.toRecording(tlTarget) + inFlight * ratio
            lag = abs(target - ourTime)
            val cur = if (isTimed(items)) items.getOrNull(locateTime(items, map.toTimeline(ourTime), tl).first)?.start?.let(map::toRecording) ?: 0.0 else rStart
            decideSync(ourTime, target, rEnd, (1 - s.progress) * s.span, ratio, cur, jumpNext || resync || hostJumped)
        }
        jumpNext = false
        when (action) {
            is SyncAction.Hold -> if (!holding) { holding = true; Audio.pause() }
            is SyncAction.Seek -> { if (holding) { holding = false; Audio.resume() }; Audio.seek(action.to.coerceIn(0.0, duration)); lastSeek = now }
            is SyncAction.Play -> { if (holding) { holding = false; Audio.resume() }; rate += (action.rate - rate) * .67; Audio.liveRate(rate) }
        }
    }
}
