package mvvm

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
import kotlinx.serialization.json.JsonNull.content
import loadData
import parsers.bible.Bible
import parsers.bible.models.IBibleVersion
import parsers.bible.models.IChapterContent
import parsers.bible.models.ILanguage
import presentation.ProjectionViewModel
import saveData

class BibleViewModel : ViewModel(), ProjectionViewModel {
    private val bible = Bible()
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
            loadBook()
            loadCurrentChapter()
        }
    }

    suspend fun loadLanguages() {
        ShareViewModels.bibleLanguages.value?.let { langs ->
            languages = langs
            return
        }

        loadData<List<ILanguage>>("bible-languages")?.let {
            languages = it
            return
        }

        try {
            val networkLanguages = bible.getAllLanguages()
            ShareViewModels.bibleLanguages.value = networkLanguages
            languages = networkLanguages
            saveData("bible-languages", networkLanguages)
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
        }
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

        loadData<ILanguage>("current-bible-language")?.let {
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
        val content = ShareViewModels.bibleChapterContent.getOrPut(key) {
            loadData<IChapterContent>(key)?.let {
                return@getOrPut it
            }

            val res = bible.getChapterDoc(version, book = chapter.usfm.split(".").first(), chapter.human.toInt())
            saveData(key, res)

            res
        }

        chapterContent = content
    }

    fun loadCurrentVersion() {
        ShareViewModels.currentVersion?.let { version ->
            currentVersion = version
            return@loadCurrentVersion
        }

        loadData<IBibleVersion>("current-bible-version")?.let {
            currentVersion = it
            return@loadCurrentVersion
        }

        putCurrentVersion(versions.find { it.abbreviation == "NKJV" })
    }

    suspend fun loadVersions(key: String?) {
        if (key == null) return

        val vs = ShareViewModels.bibleVersions.getOrPut(key) {
            val oldCachedVersionsData = loadData<MutableMap<String, List<IBibleVersion>>>("bible-versions")
            oldCachedVersionsData?.get(key)?.let { cached ->
                return@getOrPut cached
            }

            val v = bible.getVersionsFromLanguage(key)
            val fullV = v.map { ver ->
                bible.getFullVersion(ver)
            }

            val updatedCache = oldCachedVersionsData ?: mutableMapOf()
            updatedCache[key] = fullV

            saveData("bible-versions", updatedCache)

            fullV
        }

        versions = vs
    }
}