package ui.screens

import androidx.compose.runtime.Composable
import navigation.Navigator
import ui.db.DBConfig
import ui.screens.home.Home

@Composable
fun NavHost(navigator: Navigator) {
    val route = navigator.current

    when {
        route.path.contains("/home") -> Home()
        route.path.contains("/db") -> DBConfig()
    }
}