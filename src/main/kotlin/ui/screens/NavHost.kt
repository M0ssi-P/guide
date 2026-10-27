package ui.screens

import ui.screens.table.TableDesktopScreen
import androidx.compose.runtime.Composable
import navigation.LibraryScreen
import navigation.Screen
import navigation.Tabs
import ui.screens.bible.BibleHome
import ui.screens.home.Home
import ui.screens.hymns.Hymns
import ui.screens.myactivity.MyActivity
import ui.screens.projection.Listener
import ui.screens.projection.Projection
import ui.screens.saved.Saved
import ui.screens.settings.AppearanceSettingsScreen
import ui.screens.thisweek.LetterArchive
import ui.screens.thisweek.LetterDetail
import ui.screens.thisweek.ThisWeek

@Composable
fun NavHost(tab: Tabs) {
    when(val page = tab.current.current) {
        Screen.Home -> Home()
        Screen.Saved -> Saved()
        Screen.MyActivity -> MyActivity()
        Screen.Setting -> AppearanceSettingsScreen()
        Screen.ThisWeek -> ThisWeek()
        Screen.LetterArchive -> LetterArchive()
        Screen.Projection -> Projection()
        Screen.Listener -> Listener()
        Screen.SongWorkshop -> ui.screens.hymns.SongWorkshop()
        is Screen.Letter -> LetterDetail(page.id)

        is Screen.Library -> when(page.page) {
            is LibraryScreen.Table -> TableDesktopScreen()
            is LibraryScreen.Bible -> BibleHome()
            is LibraryScreen.Hymns -> ui.screens.hymns.Songs()
        }
    }
}