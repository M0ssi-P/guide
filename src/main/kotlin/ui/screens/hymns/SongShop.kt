package ui.screens.hymns

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import db.controller.songbooks.Books.importBook
import db.controller.songbooks.Books.importLyrics
import db.controller.songbooks.Books.importSong
import extractZipToMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import models.IProviderStats
import models.ISongDetails
import models.ISongLyricLine
import models.ISongTitle
import models.LyricType
import loadData
import saveData
import storeDir
import db.run
import mvvm.ShareViewModels
import rs.Bucket
import rs.RSResponse
import ui.config.DB

/**
 * A song book on offer: from the song store (songbooks/index.json, one SQLite file each) when [entry] is set, else an
 * older one only offered as songbooks/<id>.zip.
 */
data class ShopBook(val id: String, val name: String, val language: String, val entry: store.SongStore.Entry? = null)

object SongShop {
    val books = mutableStateOf<List<ShopBook>?>(null)
    val error = mutableStateOf<String?>(null)
    /** 0..1 while downloading, by book id; above 0.7 it is being added to the library. */
    val progress = mutableStateMapOf<String, Float>()
    private val json = Json { ignoreUnknownKeys = true }

    private fun nameOf(id: String) = id.split('-', '_').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        error.value = null
        val listed = store.SongStore.list().map { ShopBook(it.id, it.title, it.language, it) }
        val older = runCatching {
            val res = ShareViewModels.globalViewModel.rs.callApi("table/db/list/hymns")
            json.decodeFromString<RSResponse>(res.text).ids.orEmpty().filter { id -> listed.none { it.id == id } }.map { ShopBook(it, nameOf(it), "") }
        }.getOrNull()
        if (listed.isEmpty() && older == null) error.value = "Couldn't reach the song book store."
        books.value = (listed + older.orEmpty()).ifEmpty { books.value ?: emptyList() }
    }

    /** The store's version of each book downloaded from it, by id. */
    private val versions: HashMap<String, String> = loadData<HashMap<String, String>>("songbook-versions") ?: HashMap()

    /** True when the store has a newer edition of a book already in the library. */
    fun hasUpdate(b: ShopBook) = b.entry?.version?.takeIf { it.isNotEmpty() }?.let { v -> versions[b.id]?.let { it != v } } == true

    /** A book from the song store: its database read and added to the library, replacing an older edition. */
    private suspend fun fromStore(b: ShopBook, e: store.SongStore.Entry): Boolean {
        val book = store.SongStore.fetch(e, java.io.File(storeDir, "songstore")) { progress[b.id] = (it * .7f).coerceAtLeast(.01f) }
        progress[b.id] = .75f
        val db = DB.connection("main.db")
        // an update replaces the book: its songs and words go with it (ON DELETE CASCADE)
        db.run("DELETE FROM books WHERE save_name = ?", book.id)
        var bookId: String? = null
        db.importBook(IProviderStats(language = book.language, name = book.title, saveName = book.id, baseURI = "", isWorking = true)) { bookId = it }
        val id = bookId ?: return false
        val sw = book.language.startsWith("sw")
        book.songs.forEach { s ->
            val song = ISongDetails(
                titles = listOf(ISongTitle(book.language, book.language, s.title)), songNumber = s.number,
                lyrics = s.parts.map { p -> ISongLyricLine(type = if (p.chorus) LyricType.CHORUS else LyricType.VERSE, key = null, verseNumber = p.n, lines = p.text.lines()) },
                englishTitle = s.englishTitle, category = s.category?.let { if (sw) it.sw else it.en }, author = s.author, year = s.year, songUrl = s.songUrl, label = s.label.takeIf { it != "${s.number}" },
            )
            db.importSong(song, id) { songId, lyrics -> db.importLyrics(lyrics, songId) }
        }
        versions[book.id] = book.version
        saveData("songbook-versions", versions)
        return true
    }

    /** Downloads one book and adds it to the library in the same shape first-run setup used. */
    suspend fun download(book: ShopBook): Boolean = withContext(Dispatchers.IO) {
        progress[book.id] = 0.01f
        try {
            book.entry?.let { return@withContext fromStore(book, it) }
            val bytes = Bucket(ShareViewModels.globalViewModel.rs).file("songbooks/${book.id}.zip")
                .createReadStream { read, total -> if (total > 0) progress[book.id] = (read.toFloat() / total * 0.7f).coerceAtLeast(0.01f) }
                .readBytes()
            progress[book.id] = 0.75f
            val db = DB.connection("main.db")
            var bookId: String? = null
            val files = extractZipToMemory(bytes)
            files.filterKeys { it.startsWith("stat") }.values.firstOrNull()?.let { stat ->
                db.importBook(json.decodeFromString<IProviderStats>(stat)) { bookId = it }
            }
            val id = bookId ?: return@withContext false
            files.filterKeys { !it.startsWith("stat") }.values.forEach { content ->
                runCatching { json.decodeFromString<List<ISongDetails>>(content) }.getOrNull()?.forEach { song ->
                    db.importSong(song, id) { songId, lyrics -> db.importLyrics(lyrics, songId) }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            progress.remove(book.id)
        }
    }
}
