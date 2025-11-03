package ui.screens

import androidx.compose.runtime.Composable
import navigation.Navigator
import navigation.Screen
import ui.screens.home.Home

@Composable
fun NavHost(navigator: Navigator) {
    val route = navigator.current

    when {
        route.path.contains("/home") -> Home()
    }
}