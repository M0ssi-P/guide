package mvvm

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.collectAsState
import db.controller.songbooks.Books.getBooks
import db.controller.songbooks.Books.getSongs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import models.IProviderStats
import models.ISongDetails
import models.ISongLyricLine
import presentation.ProjectionViewModel
import ui.config.DB

class SongBookViewModal: ViewModel(), ProjectionViewModel {
    private val _db by lazy { DB.connection(dbNames.global) };
    private val _songBooks = MutableStateFlow<List<IProviderStats>>(emptyList())
    val songBooks = _songBooks.asStateFlow()

    private val _currentBook = MutableStateFlow<IProviderStats?>(null)
    val currentBook = _currentBook.asStateFlow()

    private val _songs = MutableStateFlow<List<ISongDetails>>(emptyList())
    val songs = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<ISongDetails?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _currentLine = MutableStateFlow<ISongLyricLine?>(null)
    val currentLine = _currentLine.asStateFlow()

    private val _isPresentationMode = MutableStateFlow(false)
    val isPresentationMode = _isPresentationMode.asStateFlow()
    var hasInitialised: Boolean = false
    val scrollState = ScrollState(0)
    val lazyScrollState = LazyListState()

    fun initilize() {
        if (hasInitialised) return;
        hasInitialised = true
        _songBooks.value = loadBooks()
        // no song books installed yet: Songs shows its empty screen instead of crashing
        _currentBook.value = _songBooks.value.firstOrNull()
        _songs.value = _currentBook.value?.let { loadSongsFrom(it) } ?: emptyList()
        _currentSong.value = _songs.value.firstOrNull()
    }

    /** Re-reads the books after the Workshop adds, imports or deletes one; keeps the current book and song when they still exist. */
    fun refreshBooks(selectId: String? = null) {
        hasInitialised = true
        _songBooks.value = loadBooks()
        val book = _songBooks.value.firstOrNull { it.id == selectId }
            ?: _songBooks.value.firstOrNull { it.id == _currentBook.value?.id } ?: _songBooks.value.firstOrNull()
        _currentBook.value = book
        _songs.value = book?.let { loadSongsFrom(it) } ?: emptyList()
        _currentSong.value = _songs.value.firstOrNull { it.id == _currentSong.value?.id } ?: _songs.value.firstOrNull()
    }

    fun loadBooks(): List<IProviderStats> {
        return _db.getBooks()
    }

    fun loadSongsFrom(stats: IProviderStats): List<ISongDetails> {
        return _db.getSongs(stats.id)
    }

    fun selectBook(book: IProviderStats) {
        _currentBook.value = book
        _songs.value = loadSongsFrom(book)
    }

    fun setCurrentSong(song: ISongDetails) {
        _currentSong.value = song
    }

    fun setCurrentLine(line: ISongLyricLine) {
        _currentLine.value = line
    }

    fun setPresentationMode(bool: Boolean) {
        _isPresentationMode.value = bool
    }
}