package parsers.vgr

import kotlin.concurrent.Volatile

/** One recording file in VGR's media catalog. */
data class OmegaFile(val mediaTypeId: Int, val fileTypeId: Int, val sermonId: Int, val languageId: Int, val size: Long, val fileId: String)

/**
 * VGR's media catalog (what The Table keeps as Omni.bin): every recording file per sermon, language and format, with
 * the file id the media-files/{id}/url endpoint needs. The web page only gives the m4a's id, so this is how the opus
 * (the version that carries the time map) is found.
 */
object OmegaCatalog {
    const val OPUS = 6
    const val M4A = 2
    /** The catalog once read (null before); for code that can't wait for [get]. */
    @Volatile var loaded: Parsed? = null

    class Parsed(val languages: Map<String, Int>, val files: Map<Triple<Int, Int, Int>, OmegaFile>, val sermons: Map<Int, Pair<String, String>> = emptyMap()) {
        /** VGR's English file name for a sermon, e.g. "62-1014E A Guide VGR.opus". */
        // VGR drops punctuation from file names ("Gods Way Thats Been Made For Us")
        fun englishName(sermonId: Int, ext: String) = sermons[sermonId]?.let { (code, title) -> "$code ${vgrSafeTitle(title)} VGR.$ext" }
        /** [iso3] like "swa"; [fileTypeId] 6 = opus, 2 = m4a. */
        fun find(sermonId: Int, iso3: String, fileTypeId: Int): OmegaFile? =
            languages[iso3.lowercase()]?.let { files[Triple(sermonId, it, fileTypeId)] }
    }

    /**
     * Layout: a length-prefixed header (field 7 = language {1: id, 2: iso3}; field 8 = sermon {1: id, 3: date code,
     * 4: English title}), then length-prefixed entries
     * {1: media type, 2: file type, 3: sermon id, 4: language id, 6: size, 7: 64-byte file id}.
     */
    fun parse(b: ByteArray): Parsed {
        val (hLen, hAt) = Proto.varint(b, 0)
        val languages = HashMap<String, Int>()
        val sermons = HashMap<Int, Pair<String, String>>()
        for ((f, t, v) in Proto.fields(b, hAt, hAt + hLen.toInt())) {
            if (f == 8 && t == 2) {
                val m = Proto.fields(v as ByteArray)
                val id = m.firstOrNull { it.first == 1 }?.third as? Long
                val code = (m.firstOrNull { it.first == 3 }?.third as? ByteArray)?.decodeToString()
                val title = (m.firstOrNull { it.first == 4 }?.third as? ByteArray)?.decodeToString()
                if (id != null && code != null && title != null) sermons[id.toInt()] = code to title
                continue
            }
            if (f != 7 || t != 2) continue
            val l = Proto.fields(v as ByteArray)
            val id = l.firstOrNull { it.first == 1 }?.third as? Long ?: continue
            val iso = (l.firstOrNull { it.first == 2 }?.third as? ByteArray)?.decodeToString() ?: continue
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
            val key = Triple(sermon, lang, ft)
            if (id != null && key !in files) files[key] = OmegaFile(mt, ft, sermon, lang, size, id.toHex())
            i = end
        }
        return Parsed(languages, files, sermons)
    }
}

private fun ByteArray.toHex() = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
