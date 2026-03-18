package mvvm

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import db.ConfigViewModel
import parsers.bible.models.IBibleVersion
import parsers.bible.models.IChapterContent
import parsers.bible.models.ILanguage
import parsers.vgr.models.Calendar
import parsers.vgr.models.IQotd

object ShareViewModels {
    val cal = mutableStateOf<Calendar?>(null)
    val bitmaps = mutableMapOf<String, ImageBitmap>()
    val verseOfTheDay = mutableMapOf<Int, IQotd>()
    val userModal: UserViewModel by lazy { UserViewModel() }
    val globalViewModel by lazy {
        ConfigViewModel()
    }
    val bibleLanguages = mutableStateOf<List<ILanguage>?>(null)
    var currentLanguage: ILanguage? = null
    val bibleVersions = mutableMapOf<String, List<IBibleVersion>>()
    var currentVersion: IBibleVersion? = null
    var currentBook: IBibleVersion.Book? = null
    var currentChapter: IBibleVersion.Chapter? = null
    val bibleChapterContent = mutableMapOf<String, IChapterContent>()
}