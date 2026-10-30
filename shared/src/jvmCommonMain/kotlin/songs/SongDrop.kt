package songs

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.IProviderStats
import models.ISongDetails
import okHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

/*
 * Moving song books between The Guide on the same Wi-Fi, the way Live finds hosts: a device ready to receive
 * announces itself with a UDP broadcast every two seconds and takes one Guide songbook file over HTTP; senders list
 * the receivers they hear (or scan the receiver's code) and send to it.
 */

const val DROP_DISCOVERY_PORT = 47803
const val DROP_MAGIC = "THEGUIDE-DROP"

/** A device ready to take song books: where to send, its name, and the four digits it shows. */
data class DropReceiver(val address: String, val port: Int, val name: String, val code: String, val seenAt: Long = 0)

/** The receiver as a link (the TV's QR code, or pasted): theguide://drop?host=…&port=…&code=…&name=… */
fun DropReceiver.link() = "theguide://drop?host=$address&port=$port&code=$code&name=${java.net.URLEncoder.encode(name, "UTF-8")}"

fun parseDropLink(text: String): DropReceiver? {
    val t = text.trim()
    if (!t.startsWith("theguide://drop?")) return null
    val q = t.substringAfter('?').split('&').mapNotNull { p -> p.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to java.net.URLDecoder.decode(it[1], "UTF-8") } }.toMap()
    return DropReceiver(q["host"] ?: return null, q["port"]?.toIntOrNull() ?: return null, q["name"] ?: "The Guide", q["code"] ?: return null)
}

fun parseDropAnnouncement(text: String, address: String, now: Long): DropReceiver? {
    val p = text.split("|")
    if (p.size < 5 || p[0] != DROP_MAGIC) return null
    return DropReceiver(address, p[2].toIntOrNull() ?: return null, p[3], p[4], now)
}

/** One book as the Guide songbook file holds it. */
class Book(val stat: IProviderStats, val songs: List<ISongDetails>)

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

/** Books as a Guide songbook file: per book a folder with stat.json and songs.json (what the store's zips hold). */
fun guideZip(books: List<Book>): ByteArray = ByteArrayOutputStream().also { bytes ->
    ZipOutputStream(bytes).use { z ->
        books.forEach { b ->
            val dir = b.stat.saveName.ifBlank { b.stat.name }.replace(Regex("[^A-Za-z0-9_-]"), "_")
            z.putNextEntry(ZipEntry("$dir/stat.json")); z.write(json.encodeToString(b.stat).toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("$dir/songs.json")); z.write(json.encodeToString(b.songs).toByteArray()); z.closeEntry()
        }
    }
}.toByteArray()

/** The books in a Guide songbook file; a book's stat.json and songs files go together by folder. */
fun readGuideZip(bytes: ByteArray): List<Book> {
    val files = linkedMapOf<String, String>()
    ZipInputStream(bytes.inputStream()).use { z -> generateSequence { z.nextEntry }.filter { !it.isDirectory }.forEach { e -> files[e.name] = z.readBytes().decodeToString() } }
    return files.keys.groupBy { it.substringBeforeLast('/', "") }.mapNotNull { (_, names) ->
        val stat = names.firstOrNull { it.substringAfterLast('/').startsWith("stat") }?.let { json.decodeFromString<IProviderStats>(files.getValue(it)) } ?: return@mapNotNull null
        Book(stat, names.filterNot { it.substringAfterLast('/').startsWith("stat") }.flatMap { runCatching { json.decodeFromString<List<ISongDetails>>(files.getValue(it)) }.getOrDefault(emptyList()) })
    }
}

/** Sending: who is ready to receive, and the sending itself. */
object SongDropSender {
    @Volatile var receivers: List<DropReceiver> = emptyList(); private set
    // each listen() gets a new generation, so a stopped listener can't end the one that replaced it
    @Volatile private var generation = 0

    /** Hears receivers' announcements; [onChange] gets the list (receivers not heard for 6 seconds drop off). */
    fun listen(onChange: (List<DropReceiver>) -> Unit) {
        val mine = ++generation
        thread(isDaemon = true, name = "song-drop-listen") {
            runCatching {
                DatagramSocket(null).use { socket ->
                    socket.reuseAddress = true; socket.soTimeout = 1000
                    socket.bind(InetSocketAddress(DROP_DISCOVERY_PORT))
                    val buf = ByteArray(1024); val seen = mutableMapOf<String, DropReceiver>()
                    while (generation == mine) {
                        val packet = DatagramPacket(buf, buf.size)
                        val got = runCatching { socket.receive(packet) }.isSuccess
                        val now = System.currentTimeMillis()
                        if (got) parseDropAnnouncement(String(packet.data, 0, packet.length), packet.address.hostAddress ?: "", now)?.let { seen[it.address + ":" + it.port] = it }
                        seen.entries.removeAll { now - it.value.seenAt > 6000 }
                        if (generation == mine) { receivers = seen.values.sortedBy { it.name }; onChange(receivers) }
                    }
                }
            }
        }
    }

    fun stop() { generation++; receivers = emptyList() }

    /** Sends a Guide songbook file to [to]; true when it was taken. */
    fun send(to: DropReceiver, file: ByteArray): Boolean = runCatching {
        okHttpClient.newCall(Request.Builder().url("http://${to.address}:${to.port}/drop?code=${to.code}")
            .post(file.toRequestBody("application/zip".toMediaType())).build()).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}

/**
 * Receiving: while open, announces [name] on the Wi-Fi and takes song book files sent with its code. [onFile] gets
 * each file's bytes (on a background thread).
 */
class SongDropReceiver(private val name: String, private val onFile: (ByteArray) -> Unit) {
    val code = (1000..9999).random().toString()
    @Volatile private var running = false
    private var server: ServerSocket? = null
    var self: DropReceiver? = null; private set

    fun start(): DropReceiver {
        val ss = ServerSocket(0); server = ss; running = true
        val me = DropReceiver(localAddress() ?: "0.0.0.0", ss.localPort, name, code)
        self = me
        thread(isDaemon = true, name = "song-drop-accept") {
            while (running) {
                val s = runCatching { ss.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { runCatching { s.use { serve(it) } } }
            }
        }
        thread(isDaemon = true, name = "song-drop-announce") {
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val msg = listOf(DROP_MAGIC, "1", ss.localPort, name.replace("|", "/"), code).joinToString("|").toByteArray()
                    while (running) {
                        broadcastAddresses().forEach { a -> runCatching { socket.send(DatagramPacket(msg, msg.size, a, DROP_DISCOVERY_PORT)) } }
                        Thread.sleep(2000)
                    }
                }
            }
        }
        return me
    }

    fun stop() { running = false; runCatching { server?.close() } }

    // just enough HTTP for one POST: the request line, headers to Content-Length, the body
    private fun serve(s: java.net.Socket) {
        val input = DataInputStream(s.getInputStream().buffered())
        val request = line(input) ?: return
        var length = -1
        while (true) { val h = line(input) ?: return; if (h.isEmpty()) break; if (h.startsWith("Content-Length:", true)) length = h.substringAfter(':').trim().toIntOrNull() ?: -1 }
        val ok = request.startsWith("POST /drop?code=$code ") && length in 1..50_000_000
        if (ok) {
            val body = ByteArray(length).also { input.readFully(it) }
            onFile(body)
        }
        s.getOutputStream().write(("HTTP/1.1 ${if (ok) "200 OK" else "403 Forbidden"}\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").toByteArray())
        s.getOutputStream().flush()
    }

    private fun line(input: DataInputStream): String? {
        val sb = StringBuilder()
        while (true) { val c = input.read(); if (c < 0) return if (sb.isEmpty()) null else sb.toString(); if (c == '\n'.code) return sb.toString().trimEnd('\r'); sb.append(c.toChar()) }
    }

    private fun localAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }.flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }.getOrNull()

    private fun broadcastAddresses(): List<InetAddress> =
        (runCatching { NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }.flatMap { it.interfaceAddresses }.mapNotNull { it.broadcast } }.getOrNull() ?: emptyList())
            .ifEmpty { listOf(InetAddress.getByName("255.255.255.255")) }
}
