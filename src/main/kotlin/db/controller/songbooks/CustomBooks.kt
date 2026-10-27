package db.controller.songbooks

import db.controller.songbooks.Books.getBooks
import db.controller.songbooks.Books.getSongs
import db.controller.songbooks.Books.importBook
import db.controller.songbooks.Books.importLyrics
import db.controller.songbooks.Books.importSong
import db.run
import db.runAndReturn
import kotlinx.serialization.json.Json
import models.IProviderStats
import models.ISongDetails
import models.ISongLyricLine
import models.ISongTitle
import models.LyricType
import java.io.File
import java.io.Serializable

/** What a book you made yourself has beyond a downloaded one: the cover, the owner page and its backup folder. */
data class BookMeta(
    val bookId: String,
    val title: String,
    val author: String = "",
    val owner: String = "",
    val address: String = "",
    val logo: String? = null,
    val created: Long = System.currentTimeMillis(),
    val backup: Boolean = false,
    val backupDir: String? = null,
    val language: String = "en",
) : Serializable

/** One song as the Workshop edits it: sections typed Verse / Chorus / Bridge / Other, in singing order. */
data class DraftSong(val id: String?, val number: Int, val title: String, val parts: List<Pair<LyricType, String>>) : Serializable

/** Books made in the Song Workshop. Songs live in the same tables as downloaded books; [BookMeta] in custom_books. */
object CustomBooks {
    private val db get() = ui.config.DB.connection("main.db")

    private fun ensure() = db.run(
        """
        CREATE TABLE IF NOT EXISTS custom_books (
            book_id TEXT PRIMARY KEY, author TEXT, owner TEXT, address TEXT, logo TEXT,
            created INTEGER, backup INTEGER, backup_dir TEXT
        )
        """.trimIndent()
    )

    fun all(): List<BookMeta> {
        ensure()
        val names = db.getBooks().associateBy { it.id }
        return db.runAndReturn("SELECT * FROM custom_books") { rs ->
            BookMeta(
                rs.getString("book_id"), "", rs.getString("author") ?: "", rs.getString("owner") ?: "", rs.getString("address") ?: "",
                rs.getString("logo"), rs.getLong("created"), rs.getInt("backup") == 1, rs.getString("backup_dir")
            )
        }.mapNotNull { m -> names[m.bookId]?.let { m.copy(title = it.name, language = it.language) } }
    }

    fun meta(bookId: String): BookMeta? = all().firstOrNull { it.bookId == bookId }

    fun create(m: BookMeta): BookMeta {
        ensure()
        var id = ""
        db.importBook(IProviderStats(language = m.language, name = m.title, saveName = "custom-${System.currentTimeMillis()}", baseURI = "", isWorking = true)) { id = it }
        val out = m.copy(bookId = id)
        saveMeta(out)
        return out
    }

    fun saveMeta(m: BookMeta) {
        ensure()
        db.run("UPDATE books SET name = ? WHERE id = ?", m.title, m.bookId)
        db.run(
            "INSERT OR REPLACE INTO custom_books (book_id, author, owner, address, logo, created, backup, backup_dir) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            m.bookId, m.author, m.owner, m.address, m.logo, m.created, if (m.backup) 1 else 0, m.backupDir
        )
        backup(m.bookId)
    }

    fun songs(bookId: String): List<ISongDetails> = db.getSongs(bookId).sortedBy { it.songNumber }

    private fun lines(type: LyricType, text: String, verse: Int) =
        ISongLyricLine(type = type, key = null, verseNumber = verse, lines = text.trimEnd().lines())

    /** Saves [song]; a number already used by another song moves to the next free one. Returns the number used. */
    fun saveSong(bookId: String, song: DraftSong): Pair<String, Int> {
        val taken = songs(bookId).filter { it.id != song.id }.map { it.songNumber }.toSet()
        var number = song.number.coerceAtLeast(1)
        while (number in taken) number++
        var verse = 0
        val lyrics = song.parts.map { (t, text) -> lines(t, text, if (t == LyricType.VERSE) ++verse else 0) }
        val titles = listOf(ISongTitle(language = "", languageCode = "", title = song.title.ifBlank { "Untitled" }))
        val id = if (song.id != null) {
            db.run("UPDATE songs SET titles = ?, number = ? WHERE id = ?", Json.encodeToString(titles), number, song.id)
            db.run("DELETE FROM lyrics WHERE song_id = ?", song.id)
            db.importLyrics(lyrics, song.id)
            song.id
        } else {
            var newId = ""
            db.importSong(ISongDetails(titles = titles, songNumber = number, lyrics = lyrics), bookId) { sid, l -> newId = sid; db.importLyrics(l, sid) }
            newId
        }
        backup(bookId)
        return id to number
    }

    fun deleteSong(bookId: String, songId: String) {
        db.run("DELETE FROM lyrics WHERE song_id = ?", songId)
        db.run("DELETE FROM songs WHERE id = ?", songId)
        backup(bookId)
    }

    fun deleteBook(bookId: String) {
        songs(bookId).forEach { db.run("DELETE FROM lyrics WHERE song_id = ?", it.id) }
        db.run("DELETE FROM songs WHERE book_id = ?", bookId)
        db.run("DELETE FROM books WHERE id = ?", bookId)
        db.run("DELETE FROM custom_books WHERE book_id = ?", bookId)
    }

    /** Puts back songs that were there before (Undo after a save or delete). */
    fun restore(bookId: String, snapshot: List<ISongDetails>) {
        songs(bookId).forEach { db.run("DELETE FROM lyrics WHERE song_id = ?", it.id); db.run("DELETE FROM songs WHERE id = ?", it.id) }
        snapshot.forEach { s -> db.importSong(s, bookId) { sid, l -> db.importLyrics(l, sid) } }
        backup(bookId)
    }

    val backupLog = androidx.compose.runtime.mutableStateListOf<Pair<String, Long>>()

    /** With auto backup on, rewrites the book's PDF in its folder, songs re-sorted by number. */
    fun backup(bookId: String) {
        val m = meta(bookId) ?: return
        if (!m.backup || m.backupDir.isNullOrBlank()) return
        runCatching {
            val f = File(m.backupDir, SongBookPdf.fileName(m.title))
            SongBookPdf.write(m, songs(bookId), f)
            backupLog.add(0, "${f.name} updated" to System.currentTimeMillis())
        }.onFailure { it.printStackTrace() }
    }

    /** Builds a book from an imported PDF (or a Shop download) and returns its id. */
    fun importBook(m: BookMeta, list: List<ISongDetails>): String {
        val created = create(m.copy(backup = false))
        list.forEach { s -> db.importSong(s, created.bookId) { sid, l -> db.importLyrics(l, sid) } }
        return created.bookId
    }
}
