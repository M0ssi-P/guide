package parsers.bible

import client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import parsers.bible.models.IBibleVersion
import parsers.bible.models.ILanguage
import parsers.vgr.VgrMedia
import store.FileStore
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * The Bibles on the file store (R2): bible/languages.json lists the languages, bible/<tag>/versions.json each language's
 * versions, and each version is one gzipped SQLite database (read with [BibleDb]).
 */
object BibleStore {
    const val BASE = "${FileStore.BASE}/files/bible"

    /** A version as versions.json lists it: [key] is its file on the store, [etag] changes when it is rebuilt. */
    @Serializable
    data class Entry(
        val id: Int, val abbreviation: String, val title: String, val localTitle: String = "", val hasAudio: Boolean = false,
        val key: String, val etag: String = "", val size: Long = 0, val uploadedAt: String = "",
    ) : java.io.Serializable

    @Serializable
    private data class Language(
        val id: Int? = null, val iso63901: String? = null, val iso63903: String? = null, val name: String, val localName: String = "",
        val tag: String, val hasAudio: Boolean? = null, val hasText: Boolean? = null, val totalVersions: Int? = null,
        val textDirection: String = "ltr", val font: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun parseLanguages(text: String): List<ILanguage> = json.decodeFromString<List<Language>>(text).map {
        ILanguage(it.id, null, it.iso63901, it.iso63903, it.name, it.localName.ifBlank { it.name }, it.tag, it.hasAudio, it.hasText, it.totalVersions, it.textDirection, it.font)
    }.sortedBy { it.name }

    fun parseVersions(text: String): List<Entry> = json.decodeFromString<List<Entry>>(text).sortedBy { it.abbreviation }

    suspend fun languages(): List<ILanguage> = withContext(Dispatchers.IO) { parseLanguages(client.get("$BASE/languages.json").text) }

    suspend fun versions(tag: String): List<Entry> = withContext(Dispatchers.IO) { parseVersions(client.get("$BASE/$tag/versions.json").text) }

    /** The version before it is downloaded: no books yet (those come from its database). */
    fun Entry.toVersion(language: ILanguage) = IBibleVersion(
        id = id, abbreviation = abbreviation, localAbbreviation = abbreviation, title = title, localTitle = localTitle.ifBlank { title },
        audio = hasAudio, audioCount = if (hasAudio) 1 else 0, text = true, language = language,
    )

    /** Where a version's database lives under [dir]; its etag sits beside it. */
    fun dbFile(dir: File, tag: String, e: Entry) = File(dir, "bible/$tag/${e.key.substringAfterLast('/').removeSuffix(".gz")}")
    private fun etagFile(db: File) = File(db.parentFile, db.name + ".etag")

    fun isDownloaded(db: File) = db.isFile
    /** True when the store has a newer build than the one on this device. */
    fun hasUpdate(db: File, e: Entry) = db.isFile && e.etag.isNotEmpty() && etagFile(db).takeIf { it.isFile }?.readText()?.trim() != e.etag

    /** Downloads (or updates) [e] into [db]: the gzip with progress (0..0.95), then unpacked beside it and swapped in. */
    suspend fun download(e: Entry, db: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        db.parentFile.mkdirs()
        val gz = File(db.parentFile, db.name + ".gz")
        val tmp = File(db.parentFile, db.name + ".tmp")
        try {
            VgrMedia.download("${FileStore.BASE}/files/${e.key}", gz) { onProgress(it * .95f) }
            GZIPInputStream(gz.inputStream().buffered()).use { input -> tmp.outputStream().use { input.copyTo(it, 64 * 1024) } }
            db.delete()
            if (!tmp.renameTo(db)) throw java.io.IOException("Couldn't save ${db.name}")
            etagFile(db).writeText(e.etag)
            onProgress(1f)
        } finally {
            gz.delete(); tmp.delete()
        }
    }

    fun remove(db: File) { db.delete(); etagFile(db).delete() }
}
