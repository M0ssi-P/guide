package parsers.vgr

import client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okHttpClient
import okhttp3.Request
import parsers.vgr.models.Sermon
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

object SermonMedia {
    private const val tUrl = "https://table.branham.org"
    private val mediaDir = File(System.getProperty("user.home"), "Music/MessageAudio/ENG").apply { mkdirs() }

    fun audioFile(sermon: Sermon): File = File(mediaDir, "Sermons/${sermon.dateCode} ${sermon.title} VGR.opus")

    /**
     * The 128-char id in `/media-files/<id>/url`.
     * TODO: not stored anywhere yet. Find which Table request returns it (DevTools → Network on table.branham.org,
     *  search responses for the id) and either save it on Sermon or fetch it here.
     */
    private suspend fun mediaFileId(sermon: Sermon): String? = null

    /** Resolves a playable audio link, or null when the media id or the link can't be found. */
    suspend fun audioUrl(sermon: Sermon): String? = withContext(Dispatchers.IO) {
        val id = mediaFileId(sermon) ?: return@withContext null
        val res = client.get(
            "$tUrl/api/table/omega-media-catalog/media-files/$id/url?mediaTypeId=9&fileTypeId=6&bulkDownload=False",
            headers = mapOf("X-Requested-With" to "XMLHttpRequest", "referer" to tUrl)
        )
        // shortcut: assumes the body is the link itself (optionally JSON-quoted); parse JSON here if it's an object
        res.text.trim().trim('"').takeIf { it.startsWith("http") }
    }

    /** Downloads to a .part file first so a cancelled or failed download never leaves a broken mp3 behind. */
    suspend fun download(url: String, target: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val part = File(target.parentFile, target.name + ".part")
        try {
            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { res ->
                if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
                val body = res.body ?: throw IOException("Empty response")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var read = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            if (total > 0) onProgress(read.toFloat() / total)
                        }
                    }
                }
            }
            if (!part.renameTo(target)) throw IOException("Couldn't save ${target.name}")
        } finally {
            part.delete()
        }
    }
}
