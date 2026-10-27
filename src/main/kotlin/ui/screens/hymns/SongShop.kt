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
import mvvm.ShareViewModels
import rs.Bucket
import rs.RSResponse
import ui.config.DB

/** A song book offered by the file store (table/db/list/hymns), downloaded as songbooks/<id>.zip. */
data class ShopBook(val id: String, val name: String, val language: String)

object SongShop {
    val books = mutableStateOf<List<ShopBook>?>(null)
    val error = mutableStateOf<String?>(null)
    /** 0..1 while downloading, by book id; above 0.7 it is being added to the library. */
    val progress = mutableStateMapOf<String, Float>()
    private val json = Json { ignoreUnknownKeys = true }

    private fun nameOf(id: String) = id.split('-', '_').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        error.value = null
        runCatching {
            val res = ShareViewModels.globalViewModel.rs.callApi("table/db/list/hymns")
            val ids = json.decodeFromString<RSResponse>(res.text).ids ?: emptyList()
            books.value = ids.map { ShopBook(it, nameOf(it), "") }
        }.onFailure { error.value = "Couldn't reach the song book store." ; books.value = books.value ?: emptyList() }
    }

    /** Downloads one book and adds it to the library in the same shape first-run setup used. */
    suspend fun download(book: ShopBook): Boolean = withContext(Dispatchers.IO) {
        progress[book.id] = 0.01f
        try {
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
