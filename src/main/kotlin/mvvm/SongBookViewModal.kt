package mvvm

import androidx.compose.runtime.collectAsState
import db.controller.songbooks.Books.getBooks
import db.controller.songbooks.Books.getSongs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import models.IProviderStats
import models.ISongDetails
import models.ISongLyricLine
import ui.config.DB

class SongBookViewModal: ViewModel() {
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

    fun initilize() {
        _songBooks.value = loadBooks()
        _currentBook.value = _songBooks.value.first()
        _songs.value = loadSongsFrom(_songBooks.value.first())
        _currentSong.value = _songs.value.first()
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