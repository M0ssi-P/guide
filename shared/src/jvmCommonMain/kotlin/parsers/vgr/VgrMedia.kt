package parsers.vgr

import client
import defaultHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

/** Finding, resolving and downloading VGR sermon recordings; the same on the desktop and on Android. */
object VgrMedia {
    private const val updaterUrl = "https://updater.branham.org"
    private val mediaIdRegex = Regex("""/repo/[^/"']+/([0-9a-fA-F]{32,})\.(?:m4a|mp3|opus)""")
    private val urlRegex = Regex("""https?:(?:\\?/)+[^"'\s<>]+""")

    /** A catalog file's stream link. */
    suspend fun urlFor(fileId: String, mediaTypeId: Int, fileTypeId: Int): String? {
        val res = client.get("$updaterUrl/api/table/omega-media-catalog/media-files/$fileId/url?mediaTypeId=$mediaTypeId&fileTypeId=$fileTypeId&bulkDownload=False")
        // works whether the body is the bare link, a JSON string, or a JSON object holding it
        return if (res.isSuccessful) urlRegex.find(res.text)?.value?.replace("\\/", "/") else null
    }

    /**
     * branham.org's message stream page is server-rendered with an audio link like
     * `https://….cloudfront.net/repo/947/<media id>.m4a`; the media id is the file name.
     */
    suspend fun mediaFileId(iso3: String, dateCode: String): String? {
        val res = client.get("https://branham.org/en/messagestream/${iso3.uppercase()}=$dateCode", headers = defaultHeaders)
        if (!res.isSuccessful) return null
        return mediaIdRegex.find(res.text)?.groupValues?.get(1)
    }

    /** The catalog files of sermon [sermonId] in [iso3]: the opus first (it carries the time map), then the m4a. */
    suspend fun catalogFiles(sermonId: Int, iso3: String): List<OmegaFile> {
        val cat = OmegaCatalog.get() ?: return emptyList()
        return listOfNotNull(cat.find(sermonId, iso3, OmegaCatalog.OPUS), cat.find(sermonId, iso3, OmegaCatalog.M4A))
    }

    /**
     * The stream link: from VGR's media catalog, the opus first, then the m4a. Without a catalog entry, the m4a the web
     * page links to. Null means the sermon has no audio in this language.
     */
    suspend fun audioUrl(sermonId: Int, iso3: String, dateCode: String): String? = withContext(Dispatchers.IO) {
        catalogFiles(sermonId, iso3).firstNotNullOfOrNull { f -> runCatching { urlFor(f.fileId, f.mediaTypeId, f.fileTypeId) }.getOrNull() }
            ?.let { return@withContext it }
        val id = mediaFileId(iso3, dateCode) ?: return@withContext null
        urlFor(id, 9, OmegaCatalog.M4A)
    }

    /**
     * The one local finder: "[<ISO3>]<date code> <title> VGR.opus" (or .m4a) in [dir], for language [iso3]. No prefix
     * means English. The title is never required (VGR uses the English title for every language, and Windows drops
     * characters like ? # ! from names). Our older downloads in a non-English folder have no prefix; they count only
     * when nothing prefixed is there and, if the catalog size is known, the size matches. Ranking: catalog size, then
     * the title (compared with [vgrSafeTitle] on both sides), then opus over m4a.
     */
    fun findVgrFile(dir: File, iso3: String, dateCode: String, sizes: Set<Long> = emptySet(), title: String? = null): File? {
        val named = dir.listFiles { f -> f.isFile }?.mapNotNull { f -> vgrFileLanguage(f.name, dateCode)?.let { f to it } } ?: return null
        val mine = named.filter { it.second.equals(iso3, true) }.map { it.first }.ifEmpty {
            if (iso3.equals("ENG", true)) emptyList()
            else named.filter { it.second == "ENG" }.map { it.first }.filter { sizes.isEmpty() || it.length() in sizes }
        }
        val want = title?.let { vgrSafeTitle(it).lowercase() }
        return mine.sortedWith(compareBy<File>(
            { it.length() !in sizes },
            { want == null || !vgrSafeTitle(it.name).lowercase().contains(want) },
            { !it.extension.equals("opus", true) },
        )).firstOrNull()
    }

    /** VGR's file name for a recording: "<ISO3 unless English><date code> <English title> VGR.<ext>". */
    fun fileName(iso3: String, dateCode: String, title: String, ext: String) =
        "${if (iso3.equals("ENG", true)) "" else iso3.uppercase()}$dateCode ${vgrSafeTitle(title)} VGR.$ext"

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
