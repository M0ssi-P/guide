package ui.screens

import androidx.compose.runtime.Composable
import navigation.LibraryScreen
import navigation.Screen
import navigation.SongScreen
import navigation.Tabs
import ui.screens.home.Home
import ui.screens.hymns.Hymns
import ui.screens.myactivity.MyActivity
import ui.screens.saved.Saved
import ui.screens.settings.Setting

@Composable
fun NavHost(tab: Tabs) {
    when(val page = tab.current.current) {
        Screen.Home -> Home()
        Screen.Saved -> Saved()
        Screen.MyActivity -> MyActivity()
        Screen.Setting -> Setting()

        is Screen.Library -> when(page.page) {
            is LibraryScreen.Table -> Hymns()
            is LibraryScreen.Bible -> Hymns()
            is LibraryScreen.Hymns -> Hymns()
        }
    }
}