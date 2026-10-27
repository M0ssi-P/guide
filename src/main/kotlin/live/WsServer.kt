package live

import java.io.DataInputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Just enough of RFC 6455 for the live link: the upgrade handshake, text frames both ways, ping/pong and close.
 * The JDK has a WebSocket client but no server, and this avoids adding a dependency for it.
 */
class WsServer(private val port: Int, private val onText: (WsClient, String) -> Unit) {
    private val clients = CopyOnWriteArrayList<WsClient>()
    @Volatile private var socket: ServerSocket? = null

    fun start() {
        val ss = ServerSocket(port)
        socket = ss
        thread(isDaemon = true, name = "live-ws-accept") {
            while (!ss.isClosed) {
                val s = runCatching { ss.accept() }.getOrNull() ?: break
                thread(isDaemon = true, name = "live-ws-client") { serve(s) }
            }
        }
    }

    fun broadcast(text: String) = clients.forEach { if (!it.send(text)) clients.remove(it) }

    fun stop() {
        clients.forEach { it.close() }
        clients.clear()
        runCatching { socket?.close() }
    }

    private fun serve(s: Socket) = runCatching {
        s.tcpNoDelay = true
        val input = DataInputStream(s.getInputStream().buffered())
        val key = readHandshake(input) ?: return@runCatching s.close()
        val accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
        val out = s.getOutputStream()
        out.write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n".toByteArray())
        out.flush()
        val client = WsClient(s, out)
        clients += client
        try {
            while (true) {
                val b0 = input.readUnsignedByte()
                val b1 = input.readUnsignedByte()
                val op = b0 and 0x0F
                var len = (b1 and 0x7F).toLong()
                if (len == 126L) len = input.readUnsignedShort().toLong() else if (len == 127L) len = input.readLong()
                if (len > 1_000_000) break
                val mask = if (b1 and 0x80 != 0) ByteArray(4).also { input.readFully(it) } else null
                val data = ByteArray(len.toInt()).also { input.readFully(it) }
                if (mask != null) for (i in data.indices) data[i] = (data[i].toInt() xor mask[i % 4].toInt()).toByte()
                when (op) {
                    0x1 -> onText(client, String(data))
                    0x8 -> break
                    0x9 -> client.frame(0xA, data)
                }
            }
        } finally {
            clients.remove(client)
            client.close()
        }
    }

    private fun readHandshake(input: DataInputStream): String? {
        val lines = generateSequence { readLine(input) }.takeWhile { it.isNotEmpty() }.toList()
        return lines.firstOrNull { it.startsWith("Sec-WebSocket-Key:", true) }?.substringAfter(":")?.trim()
    }

    private fun readLine(input: DataInputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString().trimEnd('\r')
            sb.append(c.toChar())
        }
    }
}

class WsClient(private val socket: Socket, private val out: OutputStream) {
    /** False once the socket is gone. */
    fun send(text: String): Boolean = frame(0x1, text.toByteArray())

    @Synchronized
    fun frame(op: Int, data: ByteArray): Boolean = runCatching {
        out.write(0x80 or op)
        when {
            data.size < 126 -> out.write(data.size)
            data.size < 65536 -> { out.write(126); out.write(data.size shr 8); out.write(data.size and 0xFF) }
            else -> { out.write(127); for (i in 7 downTo 0) out.write(((data.size.toLong() shr (8 * i)) and 0xFF).toInt()) }
        }
        out.write(data)
        out.flush()
    }.isSuccess

    fun close() { runCatching { socket.close() } }
}
