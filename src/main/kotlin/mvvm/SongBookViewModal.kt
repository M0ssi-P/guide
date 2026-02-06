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
    private val _db = DB.connection(dbNames.songBooks);
    private val _songBooks = MutableStateFlow<List<IProviderStats>>(loadBooks())
    private val _currentBook = MutableStateFlow(_songBooks.value.first())
    private val _songs = MutableStateFlow<List<ISongDetails>>(loadSongsFrom(_songBooks.value.first()))
    private val _isPresentationMode = MutableStateFlow<Boolean>(false)
    val songBooks = _songBooks.asStateFlow()
    val songs = _songs.asStateFlow()
    val currentBook = _currentBook.asStateFlow()
    private val _currentSong = MutableStateFlow<ISongDetails?>(_songs.value.first())
    val currentSong = _currentSong.asStateFlow()
    val isPresentationMode = _isPresentationMode.asStateFlow()

    private val _currentLine = MutableStateFlow(_currentSong.value?.lyrics?.first())
    val currentLine = _currentLine.asStateFlow()

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