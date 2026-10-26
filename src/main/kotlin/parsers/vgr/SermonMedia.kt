package parsers.vgr

import client
import db.controller.table.Languages.getCurrentTable
import defaultHeaders
import loadData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okHttpClient
import okhttp3.Request
import parsers.vgr.models.Sermon
import ui.config.DB
import ui.settings.UserInterfaceSettings
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

object SermonMedia {
    private const val updaterUrl = "https://updater.branham.org"
    private val mediaIdRegex = Regex("""/repo/[^/"']+/([0-9a-fA-F]{32,})\.(?:m4a|mp3|opus)""")
    private val urlRegex = Regex("""https?:(?:\\?/)+[^"'\s<>]+""")

    private val os = System.getProperty("os.name").lowercase()

    /** The sermon's 2-letter table language, falling back to the language picked in settings. */
    private fun languageTag(sermon: Sermon): String =
        sermon.language.ifEmpty { loadData<UserInterfaceSettings>("ui_settings")?.currentTableLanguage ?: "en" }

    private fun mediaDir(lang: String): File {
        val dr = if(os.contains("win")) "Music/MessageAudio/${lang}/Sermons" else "Music/org.branham.thetable.macos/${lang}/Sermons"
        return File(System.getProperty("user.home"), dr).apply { mkdirs() }
    }

    fun audioFile(sermon: Sermon): File {
        val title = if(os.contains("win")) sermon.title else sermon.title.replace(Regex("[^\\w\\s]"), "")
        val iso3 = DB.connection("main.db").getCurrentTable(languageTag(sermon))?.iso63903?.uppercase() ?: "ENG"
        return File(mediaDir(iso3), "${sermon.dateCode} $title VGR.${if(os.contains("win")) "opus" else "m4a"}")
    }

    /**
     * branham.org's message stream page is server-rendered with an audio link like
     * `https://….cloudfront.net/repo/947/<media id>.m4a`; the media id is the file name.
     */
    private suspend fun mediaFileId(sermon: Sermon): String? {
        val iso3 = DB.connection("main.db").getCurrentTable(languageTag(sermon))?.iso63903?.uppercase() ?: return null
        val res = client.get("https://branham.org/en/messagestream/$iso3=${sermon.dateCode}", headers = defaultHeaders)
        if (!res.isSuccessful) return null
        return mediaIdRegex.find(res.text)?.groupValues?.get(1)
    }

    /**
     * Resolves the opus stream link. Null means the sermon has no audio in this language
     * (no media id on the page, a 404, or no url in the response).
     */
    suspend fun audioUrl(sermon: Sermon): String? = withContext(Dispatchers.IO) {
        val id = mediaFileId(sermon) ?: return@withContext null
        val res = client.get("$updaterUrl/api/table/omega-media-catalog/media-files/$id/url?mediaTypeId=9&fileTypeId=${if(os.contains("win")) "6" else "2"}&bulkDownload=False")
        if (!res.isSuccessful) return@withContext null
        // works whether the body is the bare link, a JSON string, or a JSON object holding it
        urlRegex.find(res.text)?.value?.replace("\\/", "/")
    }

    private val audioExtensions = setOf("opus", "m4a", "mp3", "ogg", "aac", "wav")

    /**
     * This sermon's audio if it's already on disk: our own download first, then a copy any other app saved,
     * matched by file name containing the media id (needs network to resolve) or the date code.
     */
    suspend fun findLocal(sermon: Sermon): File? = withContext(Dispatchers.IO) {
        audioFile(sermon).takeIf { it.exists() }?.let { return@withContext it }
        val names = listOfNotNull(runCatching { mediaFileId(sermon) }.getOrNull(), sermon.dateCode)
        names.firstNotNullOfOrNull { systemSearch(it) }
    }

    /**
     * Asks the OS file index instead of walking the disk: Spotlight on macOS, Everything's `es.exe` on Windows,
     * plocate on Linux.
     * shortcut: on Windows this finds nothing unless Everything is installed; add the other app's download folder as a direct check if that's common.
     */
    private fun systemSearch(name: String): File? = runCatching {
        val cmd = when {
            "mac" in os -> listOf("mdfind", "-name", name)
            "win" in os -> listOf("es.exe", "-n", "50", name)
            else -> listOf("plocate", "-l", "50", name)
        }
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val paths = process.inputStream.bufferedReader().readLines()
        process.waitFor(5, TimeUnit.SECONDS)
        paths.map(::File).firstOrNull { it.isFile && it.extension.lowercase() in audioExtensions }
    }.getOrNull()

    /** Downloads to a .part file first so a cancelled or failed download never leaves a broken file behind. */
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
