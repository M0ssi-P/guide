package parsers.vgr

import client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okHttpClient
import okhttp3.Request
import java.io.File

/** One recording file in VGR's media catalog. */
data class OmegaFile(val mediaTypeId: Int, val fileTypeId: Int, val sermonId: Int, val languageId: Int, val size: Long, val fileId: String)

/**
 * VGR's media catalog (what The Table keeps as Omni.bin): every recording file per sermon, language and format, with
 * the file id the media-files/{id}/url endpoint needs. The web page only gives the m4a's id, so this is how the opus
 * (the version that carries the time map) is found. Cached in ~/TheGuide/omega and re-downloaded only for a new version.
 */
object OmegaCatalog {
    const val OPUS = 6
    const val M4A = 2
    private const val INFO = "https://updater.branham.org/api/table/omega-media-catalog"
    private val dir get() = File(System.getProperty("user.home"), "TheGuide/omega").apply { mkdirs() }
    private val lock = Mutex()
    @Volatile private var loaded: Parsed? = null

    class Parsed(val languages: Map<String, Int>, val files: Map<Triple<Int, Int, Int>, OmegaFile>) {
        /** [iso3] like "swa"; [fileTypeId] 6 = opus, 2 = m4a. */
        fun find(sermonId: Int, iso3: String, fileTypeId: Int): OmegaFile? =
            languages[iso3.lowercase()]?.let { files[Triple(sermonId, it, fileTypeId)] }
    }

    suspend fun get(): Parsed? = loaded ?: lock.withLock {
        loaded ?: withContext(Dispatchers.IO) {
            runCatching {
                val info = Regex(""""version"\s*:\s*(\d+).*?"downloadUrl"\s*:\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
                    .find(client.get(INFO).text)
                val cached = dir.listFiles { f -> f.name.startsWith("omni-") }?.maxByOrNull { it.lastModified() }
                val file = if (info == null) cached else File(dir, "omni-${info.groupValues[1]}.bin").also { f ->
                    if (!f.isFile) {
                        okHttpClient.newCall(Request.Builder().url(info.groupValues[2]).build()).execute().use { res ->
                            val tmp = File(dir, f.name + ".part")
                            res.body!!.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                            tmp.renameTo(f)
                        }
                        dir.listFiles { o -> o.name.startsWith("omni-") && o != f }?.forEach { it.delete() }
                    }
                }
                file?.let { parse(it.readBytes()) }
            }.onFailure { it.printStackTrace() }.getOrNull()?.also { loaded = it }
        }
    }

    /**
     * Layout: a length-prefixed header (field 7 = language {1: id, 2: iso3}), then length-prefixed entries
     * {1: media type, 2: file type, 3: sermon id, 4: language id, 6: size, 7: 64-byte file id}.
     */
    fun parse(b: ByteArray): Parsed {
        val (hLen, hAt) = Proto.varint(b, 0)
        val languages = HashMap<String, Int>()
        for ((f, t, v) in Proto.fields(b, hAt, hAt + hLen.toInt())) {
            if (f != 7 || t != 2) continue
            val l = Proto.fields(v as ByteArray)
            val id = l.firstOrNull { it.first == 1 }?.third as? Long ?: continue
            val iso = (l.firstOrNull { it.first == 2 }?.third as? ByteArray)?.let { String(it) } ?: continue
            languages[iso.lowercase()] = id.toInt()
        }
        val files = HashMap<Triple<Int, Int, Int>, OmegaFile>()
        var i = hAt + hLen.toInt()
        while (i < b.size) {
            val (len, at) = Proto.varint(b, i)
            val end = at + len.toInt()
            var mt = 0; var ft = 0; var sermon = 0; var lang = 0; var size = 0L; var id: ByteArray? = null
            for ((f, t, v) in Proto.fields(b, at, end)) when {
                t == 0 && f == 1 -> mt = (v as Long).toInt()
                t == 0 && f == 2 -> ft = (v as Long).toInt()
                t == 0 && f == 3 -> sermon = (v as Long).toInt()
                t == 0 && f == 4 -> lang = (v as Long).toInt()
                t == 0 && f == 6 -> size = v as Long
                t == 2 && f == 7 -> id = v as ByteArray
            }
            id?.let { bytes -> files.putIfAbsent(Triple(sermon, lang, ft), OmegaFile(mt, ft, sermon, lang, size, bytes.joinToString("") { "%02x".format(it) })) }
            i = end
        }
        return Parsed(languages, files)
    }
}
