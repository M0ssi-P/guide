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
import ui.screens.saved.Saved
import ui.screens.settings.AppearanceSettingsScreen

@Composable
fun NavHost(tab: Tabs) {
    when(val page = tab.current.current) {
        Screen.Home -> Home()
        Screen.Saved -> Saved()
        Screen.MyActivity -> MyActivity()
        Screen.Setting -> AppearanceSettingsScreen()

        is Screen.Library -> when(page.page) {
            is LibraryScreen.Table -> TableDesktopScreen()
            is LibraryScreen.Bible -> BibleHome()
            is LibraryScreen.Hymns -> Hymns()
        }
    }
}