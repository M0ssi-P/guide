package navigation

sealed class Screen(route: String) {
    object Home: Screen("/home")
}