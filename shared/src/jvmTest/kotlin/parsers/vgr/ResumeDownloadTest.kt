package parsers.vgr

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import okHttpClient
import okhttp3.OkHttpClient
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ResumeDownloadTest {
    private val body = ByteArray(200_000) { (it % 251).toByte() }
    private var etag = "\"v1\""
    private val ranges = mutableListOf<String?>()
    // serves [body] like the worker: whole, or from a "bytes=N-" range with 206
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/f") { ex ->
            val range = ex.requestHeaders.getFirst("Range").also { ranges += it }
            val from = range?.removePrefix("bytes=")?.removeSuffix("-")?.toInt() ?: 0
            ex.responseHeaders.add("ETag", etag)
            ex.sendResponseHeaders(if (range != null) 206 else 200, (body.size - from).toLong())
            ex.responseBody.use { it.write(body, from, body.size - from) }
        }
        start()
    }
    private val url = "http://127.0.0.1:${server.address.port}/f"
    private val dir = File.createTempFile("resume", "").apply { delete(); mkdirs() }
    private val target = File(dir, "x.db.gz")

    init { okHttpClient = OkHttpClient() }

    @AfterTest fun stop() { server.stop(0); dir.deleteRecursively() }

    /** What a cut-off download leaves: the first [n] bytes and the etag they came with. */
    private fun cutOffAt(n: Int, tag: String) {
        File(dir, "x.db.gz.part").writeBytes(body.copyOf(n))
        File(dir, "x.db.gz.part.etag").writeText(tag)
    }

    @Test fun downloadsWhole() = runBlocking {
        VgrMedia.download(url, target) {}
        assertContentEquals(body, target.readBytes())
        assertEquals(listOf<String?>(null), ranges)
        assertFalse(File(dir, "x.db.gz.part.etag").exists())
    }

    @Test fun resumesWhereItStopped() = runBlocking {
        cutOffAt(70_000, etag)
        VgrMedia.download(url, target) {}
        assertContentEquals(body, target.readBytes())
        assertEquals(listOf<String?>("bytes=70000-"), ranges)
    }

    @Test fun startsOverWhenTheFileChanged() = runBlocking {
        cutOffAt(70_000, "\"v0\"")
        VgrMedia.download(url, target) {}
        assertContentEquals(body, target.readBytes())
        assertEquals(listOf("bytes=70000-", null), ranges)
    }
}
