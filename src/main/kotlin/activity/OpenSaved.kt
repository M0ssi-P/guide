package activity

import mvvm.BibleViewModel
import navigation.LibraryScreen
import navigation.Navigator
import navigation.Screen
import parsers.vgr.VgrViewModel

/** Opens the chapter or sermon a saved item came from, in this tab. */
fun Navigator.openTarget(target: SavedTarget) {
    when (target.source) {
        SavedSource.BIBLE -> {
            val model = viewModelStore.getOrCreate("BibleVM-$id") { BibleViewModel().apply { initilize() } }
            val usfm = target.chapterUsfm ?: return
            val book = model.currentVersion?.books?.find { it.usfm == usfm.substringBefore(".") }
            book?.chapters?.find { it.usfm == usfm }?.let { chapter ->
                model.putCurrentBook(book)
                model.onChapterClicked(chapter)
            }
            navigate(Screen.Library(LibraryScreen.Bible()))
        }
        SavedSource.TABLE -> {
            target.sermon?.let { viewModelStore.getOrCreate("HomeVM-$id") { VgrViewModel() }.setCurrentSermon(it) }
            navigate(Screen.Library(LibraryScreen.Table()))
        }
    }
}
