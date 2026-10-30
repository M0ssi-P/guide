package mvvm

import activity.ActivityKind
import activity.ActivityLog
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.composables.book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull.content
import loadData
import parsers.bible.BibleStore
import parsers.bible.models.IBibleVersion
import parsers.bible.models.IChapterContent
import parsers.bible.models.ILanguage
import presentation.ProjectionViewModel
import saveData

class BibleViewModel : ViewModel(), ProjectionViewModel {
    var languages: List<ILanguage> by mutableStateOf(emptyList())
       private set

    var currentLanguage: ILanguage? by mutableStateOf(null)
        private set

    var versions: List<IBibleVersion> by mutableStateOf(emptyList())
        private set
    var currentVersion: IBibleVersion? by mutableStateOf(null)
        private set
    var currentBook: IBibleVersion.Book? by mutableStateOf(null)
        private set
    var currentChapter: IBibleVersion.Chapter? by mutableStateOf(null)
        private set
    var chapters: List<IBibleVersion.Chapter> by mutableStateOf(emptyList())
        private set
    var chapterContent: IChapterContent? by mutableStateOf(null)
        private set

    val bookScrollState = LazyListState()
    val chapterScrollState = LazyGridState()
    val languageScrollState = LazyListState()
    val versionScrollState = LazyListState()
    val contentScrollState = LazyListState()

    fun initilize() {
        viewModelScope.launch {
            loadLanguages()
            loadCurrentLanguage()
            loadVersions(currentLanguage?.tag)
            loadCurrentVersion()
            // the version is read from its database on this computer: fetched the first time
            currentVersion?.let { BibleLibrary.ready(it) }?.let { putCurrentVersion(it) }
            loadBook()
            loadCurrentChapter()
        }
    }

    suspend fun loadLanguages() {
        ShareViewModels.bibleLanguages.value?.let { langs ->
            languages = langs
            return
        }

        // the file store's list, kept for opening offline
        val list = runCatching { BibleStore.languages() }.getOrNull()?.also { saveData("bible-languages-r2", ArrayList(it)) }
            ?: loadData<ArrayList<ILanguage>>("bible-languages-r2") ?: emptyList()
        if (list.isNotEmpty()) ShareViewModels.bibleLanguages.value = list
        languages = list
    }

    fun putCurrentLanguage(language: ILanguage?) {
        currentLanguage = language
        ShareViewModels.currentLanguage = language
        saveData("current-bible-language", language?.tag)
    }

    fun putCurrentVersion(version: IBibleVersion?) {
        currentVersion = version
        ShareViewModels.currentVersion = version
        saveData("current-bible-version", version)
    }

    fun loadCurrentLanguage() {
        ShareViewModels.currentLanguage?.let { language ->
            currentLanguage = language
            return
        }

        // saved as the tag (older builds saved the whole language)
        when (val saved = loadData<Any>("current-bible-language")) {
            is ILanguage -> languages.find { it.tag == saved.tag } ?: saved
            is String -> languages.find { it.tag == saved }
            else -> null
        }?.let {
            currentLanguage = it
            return
        }

        putCurrentLanguage(languages.find { it.tag == "eng" })
    }

    fun putCurrentBook(book: IBibleVersion.Book?) {
        currentBook = book
        chapters = currentBook?.chapters ?: emptyList()
        ShareViewModels.currentBook = book
        saveData("current-bible-book", book)
    }

    suspend fun putCurrentChapter(chapter: IBibleVersion.Chapter?) {
        currentChapter = chapter
        ShareViewModels.currentChapter = chapter
        saveData("current-bible-chapter", chapter)
        loadContent(currentVersion, chapter)
        if (chapter != null) {
            val book = currentBook?.human ?: chapter.usfm.substringBefore(".")
            ActivityLog.record(
                ActivityKind.BIBLE, "Read $book ${chapter.human}", currentVersion?.localAbbreviation ?: "Bible",
                "bible:${chapter.usfm}", "Continue"
            )
        }
    }

    fun loadBook() {
        ShareViewModels.currentBook?.let { book ->
            currentBook = book
            chapters = book.chapters
            return
        }

        loadData<IBibleVersion.Book>("current-bible-book")?.let {
            currentBook = it
            chapters = it.chapters
            return
        }

        putCurrentBook(currentVersion?.books?.find { it.usfm == "GEN" })
    }

    /** Reads the same book and chapter in [version], which may be in another language. */
    fun switchVersion(listed: IBibleVersion) {
        viewModelScope.launch {
            // downloaded first when it isn't on this computer yet
            BibleLibrary.ready(listed)?.let { show(it) }
        }
    }

    private fun show(version: IBibleVersion) {
        val usfm = currentChapter?.usfm
        val otherLanguage = version.language.tag != currentLanguage?.tag
        putCurrentVersion(version)
        if (otherLanguage) putCurrentLanguage(version.language)
        val book = version.books?.find { it.usfm == usfm?.substringBefore(".") } ?: version.books?.firstOrNull()
        putCurrentBook(book)
        viewModelScope.launch {
            if (otherLanguage) loadVersions(version.language.tag)
            putCurrentChapter(book?.chapters?.find { it.usfm == usfm } ?: book?.chapters?.firstOrNull { it.human == "1" })
        }
    }

    fun onChapterClicked(chapter: IBibleVersion.Chapter?) {
        viewModelScope.launch {
            putCurrentChapter(chapter)
        }
    }

    suspend fun loadCurrentChapter() {
        ShareViewModels.currentChapter?.let { chapter ->
            currentChapter = chapter
            loadContent(currentVersion, chapter)
            return
        }

        loadData<IBibleVersion.Chapter>("current-bible-chapter")?.let {
            currentChapter = it
            loadContent(currentVersion, it)
            return
        }

        putCurrentChapter(currentBook?.chapters?.find { it.human == "1" })
    }

    suspend fun loadContent(version: IBibleVersion?, chapter: IBibleVersion.Chapter?) {
        if(version == null || chapter == null) return

        val key = "${version.id}.${chapter.usfm}"
        chapterContent = ShareViewModels.bibleChapterContent[key]
            ?: BibleLibrary.chapter(version, chapter.usfm)?.also { ShareViewModels.bibleChapterContent[key] = it }
    }

    fun loadCurrentVersion() {
        ShareViewModels.currentVersion?.let { version ->
            currentVersion = version
            return@loadCurrentVersion
        }

        // the saved one when the store still lists it, else NKJV
        val saved = loadData<IBibleVersion>("current-bible-version")
        putCurrentVersion(versions.find { it.id == saved?.id } ?: versions.find { it.abbreviation == "NKJV" } ?: versions.firstOrNull())
    }

    suspend fun loadVersions(key: String?) {
        if (key == null) return
        versions = versionsOf(key)
    }

    /** A language's versions on the file store (the last list fetched when offline), without switching to it. */
    suspend fun versionsOf(key: String): List<IBibleVersion> {
        ShareViewModels.bibleVersions[key]?.let { return it }
        val language = languages.find { it.tag == key } ?: return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching { BibleStore.versions(key) }.getOrNull()?.let { BibleLibrary.register(language, it) }
            BibleLibrary.listed(key)
        }.also { if (it.isNotEmpty()) ShareViewModels.bibleVersions[key] = it }
    }
}
