package parsers.vgr

import okHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Time maps per audio source (file path or URL), read once in the background. Null until read, or when there is none. */
object TimeMaps {
    private const val HEAD = 2_000_000
    private val headers = ConcurrentHashMap<String, Result<AudioHeader?>>()
    private val synopsisRegex = Regex(""""sermonId"\s*:\s*(\d+).*?"languageId"\s*:\s*(\d+)""")

    /** What VGR wrote into a recording's header: its time map, and which sermon and language it is (from `synopsis`). */
    class AudioHeader(val map: VgrTimeMap?, val sermonId: Int?, val languageId: Int?)

    /** The header of [source] (path or URL), read once in the background; null until read. */
    fun header(source: String?): AudioHeader? {
        source ?: return null
        headers[source]?.let { return it.getOrNull() }
        headers[source] = Result.success(null) // reading; also stops repeat requests
        thread(isDaemon = true, name = "vgr-time-map") {
            headers[source] = runCatching {
                val head = head(source)
                val syn = synopsisRegex.find(String(head, Charsets.ISO_8859_1))
                AudioHeader(VgrTimeMap.fromHeader(head), syn?.groupValues?.get(1)?.toInt(), syn?.groupValues?.get(2)?.toInt())
            }
        }
        return null
    }

    fun get(source: String?): VgrTimeMap? = header(source)?.map

    private fun head(source: String): ByteArray {
        val f = File(source)
        if (f.isFile) return f.inputStream().use { it.readUpTo(HEAD) }
        val req = okhttp3.Request.Builder().url(source).header("Range", "bytes=0-${HEAD - 1}").build()
        return okHttpClient.newCall(req).execute().use { it.body?.byteStream()?.readUpTo(HEAD) ?: ByteArray(0) }
    }

    // InputStream.readNBytes is Java 11, which older Android lacks
    private fun java.io.InputStream.readUpTo(n: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        while (out.size() < n) { val r = read(buf, 0, minOf(buf.size, n - out.size())); if (r < 0) break; out.write(buf, 0, r) }
        return out.toByteArray()
    }
}
