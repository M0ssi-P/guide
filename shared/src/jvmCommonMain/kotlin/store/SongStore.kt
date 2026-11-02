package store

import client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import parsers.bt.openSql
import parsers.vgr.VgrMedia
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * The song store on the file store (R2): songbooks/index.json lists the books, each one SQLite database
 * (songbooks/<id>.db.gz) in the songbook format the essentials worker publishes: book, category, song, verse.
 */
object SongStore {
    @Serializable
    data class Entry(
        val id: String, val title: String, val description: String = "", val language: String = "",
        val songCount: Int = 0, val hasAudio: Int = 0, val file: String = "", val size: Long = 0, val version: String = "", val updated: String = "",
    ) : java.io.Serializable

    data class Category(val id: String, val en: String, val sw: String)
    data class Part(val chorus: Boolean, val n: Int?, val text: String)
    /** [label] is the number as printed: "25", or "25B" where a book prints two songs under 25 ([number] sorts them). */
    data class Song(
        val number: Int, val label: String, val title: String, val englishTitle: String?, val category: Category?,
        val author: String?, val year: String?, val songUrl: String?, val parts: List<Part>,
        val reference: String? = null,
    )
    data class Book(val id: String, val title: String, val description: String, val language: String, val version: String, val songs: List<Song>)

    private val json = Json { ignoreUnknownKeys = true }

    /** True when a song's [url] is its tune played on instruments (MIDI, or a "polyinstru"/"piano" file) rather than people singing it. */
    fun isTune(url: String) = url.endsWith(".mid", true) || url.contains("instru", true) || url.contains("piano", true)

    fun parseIndex(text: String): List<Entry> = json.decodeFromString<List<Entry>>(text).sortedBy { it.title }

    /** The books on offer; empty when the store can't be reached or lists none yet. */
    suspend fun list(): List<Entry> = withContext(Dispatchers.IO) {
        runCatching {
            val res = client.get("${FileStore.BASE}/files/songbooks/index.json", cacheTime = 0)
            if (res.isSuccessful) parseIndex(res.text) else { res.body.close(); emptyList() }
        }.getOrDefault(emptyList())
    }

    /** Downloads [e] (resuming a cut-off download) and reads it. */
    suspend fun fetch(e: Entry, dir: File, onProgress: (Float) -> Unit): Book = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val gz = File(dir, "${e.id}.db.gz")
        val db = File(dir, "${e.id}.db")
        try {
            VgrMedia.download("${FileStore.BASE}/files/${e.file}", gz) { onProgress(it * .9f) }
            GZIPInputStream(gz.inputStream().buffered()).use { i -> db.outputStream().use { i.copyTo(it) } }
            read(db)
        } finally {
            gz.delete(); db.delete()
        }
    }

    /** A songbook database in the store's format. */
    fun read(file: File): Book {
        val sql = openSql(file)
        try {
            val b = sql.rows("SELECT id, title, description, language, version FROM book LIMIT 1").first()
            val categories = sql.rows("SELECT id, name_en, name_sw FROM category").associate { it[0] as String to Category(it[0] as String, it[1] as String, it[2] as String) }
            val parts = sql.rows("SELECT label, type, n, text FROM verse ORDER BY label, pos")
                .groupBy({ it[0] as String }, { Part(it[1] == "chorus", (it[2] as Number?)?.toInt(), it[3] as String) })
            val songs = sql.rows("SELECT number, label, title, english_title, category, author, year, song_url, reference FROM song ORDER BY number, label").map {
                val label = it[1] as String
                Song((it[0] as Number).toInt(), label, it[2] as String, it[3] as String?, categories[it[4] as String?], it[5] as String?, it[6] as String?, it[7] as String?, parts[label].orEmpty(), it[8] as String?)
            }
            return Book(b[0] as String, b[1] as String, (b[2] as String?).orEmpty(), b[3] as String, b[4] as String, songs)
        } finally {
            sql.close()
        }
    }
}
