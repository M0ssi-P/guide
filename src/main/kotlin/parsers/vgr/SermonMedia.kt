package parsers.vgr

import client
import db.controller.table.Languages.getCurrentTable
import defaultHeaders
import loadData
import saveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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

    /** Every folder sermon audio is kept in: The Table's per-language folders and ~/TheGuide. */
    fun mediaRoots(): List<File> {
        val base = File(System.getProperty("user.home"), if (os.contains("win")) "Music/MessageAudio" else "Music/org.branham.thetable.macos")
        return (base.listFiles()?.map { File(it, "Sermons") }?.filter { it.isDirectory } ?: emptyList()) +
            listOf(File(System.getProperty("user.home"), "TheGuide")).filter { it.isDirectory }
    }

    private fun mediaDir(lang: String): File {
        val dr = if(os.contains("win")) "Music/MessageAudio/${lang}/Sermons" else "Music/org.branham.thetable.macos/${lang}/Sermons"
        return File(System.getProperty("user.home"), dr).apply { mkdirs() }
    }

    /**
     * Where we save this sermon's audio, with the extension of [url] (.opus from the catalog, .m4a otherwise).
     */
    fun audioFile(sermon: Sermon, url: String? = null): File {
        val title = if(os.contains("win")) sermon.title else sermon.title.replace(Regex("[^\\w\\s]"), "")
        val iso3 = DB.connection("main.db").getCurrentTable(languageTag(sermon))?.iso63903?.uppercase() ?: "ENG"
        val ext = if (url == null || url.substringBefore('?').endsWith(".opus", true)) "opus" else "m4a"
        return File(mediaDir(iso3), "${sermon.dateCode} $title VGR.$ext")
    }

    /**
     * Our own saved copy or VGR's (The Table names its downloads "SWA62-1014E A Guide VGR.opus") in the language's folder.
     * The opus comes first because it carries VGR's time map; [sizes] (from the media catalog) pick the right file when
     * several share the date code.
     */
    fun ownCopy(sermon: Sermon, sizes: Set<Long> = emptySet()): File? {
        val base = audioFile(sermon)
        val iso3 = base.parentFile.parentFile.name
        val candidates = listOf(File(base.parentFile, base.nameWithoutExtension + ".opus"), base) +
            (base.parentFile.listFiles { f ->
                f.isFile && f.extension.lowercase() in audioExtensions &&
                    (f.name.startsWith("${sermon.dateCode} ") || f.name.startsWith("$iso3${sermon.dateCode} "))
            }?.sortedBy { if (it.extension.equals("opus", true)) 0 else 1 } ?: emptyList())
        val present = candidates.filter { it.isFile }.distinct()
        return present.firstOrNull { it.length() in sizes } ?: present.firstOrNull()
    }

    private suspend fun catalogFiles(sermon: Sermon): List<OmegaFile> {
        val iso3 = DB.connection("main.db").getCurrentTable(languageTag(sermon))?.iso63903 ?: return emptyList()
        val cat = OmegaCatalog.get() ?: return emptyList()
        return listOfNotNull(cat.find(sermon.i, iso3, OmegaCatalog.OPUS), cat.find(sermon.i, iso3, OmegaCatalog.M4A))
    }

    private suspend fun urlFor(fileId: String, mediaTypeId: Int, fileTypeId: Int): String? {
        val res = client.get("$updaterUrl/api/table/omega-media-catalog/media-files/$fileId/url?mediaTypeId=$mediaTypeId&fileTypeId=$fileTypeId&bulkDownload=False")
        // works whether the body is the bare link, a JSON string, or a JSON object holding it
        return if (res.isSuccessful) urlRegex.find(res.text)?.value?.replace("\\/", "/") else null
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
     * Resolves the stream link. From VGR's media catalog: the opus first (it carries the time map that lines the
     * Table's sentences up with this recording), then the m4a. Without a catalog entry, the m4a the web page links to.
     * Null means the sermon has no audio in this language.
     */
    suspend fun audioUrl(sermon: Sermon): String? = withContext(Dispatchers.IO) {
        catalogFiles(sermon).firstNotNullOfOrNull { f -> runCatching { urlFor(f.fileId, f.mediaTypeId, f.fileTypeId) }.getOrNull() }
            ?.let { return@withContext it }
        val id = mediaFileId(sermon) ?: return@withContext null
        urlFor(id, 9, OmegaCatalog.M4A)
    }

    /** A VGR language id as a name for labels: the installed Table language's own name (e.g. "Kiswahili"). */
    suspend fun languageName(languageId: Int): String? {
        val iso = OmegaCatalog.get()?.languages?.entries?.firstOrNull { it.value == languageId }?.key ?: return null
        return live.TableInstaller.installed().firstOrNull { it.iso3.equals(iso, true) }?.let { it.localName.ifBlank { it.name } }
    }

    /** The English recording of VGR sermon [sermonId] (catalog opus first): a file on this computer, else its stream link. */
    suspend fun englishCompanion(sermonId: Int): String? = withContext(Dispatchers.IO) {
        val cat = OmegaCatalog.get() ?: return@withContext null
        val f = cat.find(sermonId, "eng", OmegaCatalog.OPUS) ?: cat.find(sermonId, "eng", OmegaCatalog.M4A) ?: return@withContext null
        englishLocal(cat, f)?.absolutePath ?: runCatching { urlFor(f.fileId, f.mediaTypeId, f.fileTypeId) }.getOrNull()
    }

    private fun englishLocal(cat: OmegaCatalog.Parsed, f: OmegaFile): File? {
        val dir = mediaDir("ENG")
        val named = cat.englishName(f.sermonId, if (f.fileTypeId == OmegaCatalog.OPUS) "opus" else "m4a")?.let { File(dir, it) }
        return named?.takeIf { it.isFile } ?: dir.listFiles { x -> x.isFile && x.length() == f.size }?.firstOrNull()
    }

    /**
     * Downloads the English recording next to a translated one, so its singing and shouting (quiet in other languages)
     * can be mixed in. Nothing for English sermons, or when it's already on this computer.
     */
    fun downloadEnglishCompanionLater(sermon: Sermon) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { downloadEnglishCompanion(sermon) }
    }

    suspend fun downloadEnglishCompanion(sermon: Sermon) = withContext(Dispatchers.IO) {
        if (languageTag(sermon) == "en") return@withContext
        val cat = OmegaCatalog.get() ?: return@withContext
        val f = cat.find(sermon.i, "eng", OmegaCatalog.OPUS) ?: return@withContext
        if (englishLocal(cat, f) != null) return@withContext
        val url = urlFor(f.fileId, f.mediaTypeId, f.fileTypeId) ?: return@withContext
        val name = cat.englishName(f.sermonId, "opus") ?: "${sermon.dateCode} VGR.opus"
        runCatching { download(url, File(mediaDir("ENG"), name)) {} }.onFailure { it.printStackTrace() }
    }

    private val audioExtensions = setOf("opus", "m4a", "mp3", "ogg", "aac", "wav")

    /**
     * This sermon's audio if it's already on disk: our own download first, then a copy any other app saved,
     * matched by file name containing the media id (needs network to resolve) or the date code.
     */
    suspend fun findLocal(sermon: Sermon): File? = withContext(Dispatchers.IO) {
        val sizes = runCatching { catalogFiles(sermon) }.getOrDefault(emptyList()).map { it.size }.toSet()
        ownCopy(sermon, sizes)?.let { return@withContext it }
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
            // remembered so Saved › Downloads can tell our files from other apps' copies in the same folder
            saveData("guide_downloads", HashSet(loadData<HashSet<String>>("guide_downloads") ?: hashSetOf()).apply { add(target.absolutePath) })
        } finally {
            part.delete()
        }
    }
}
