package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

data class Route(val path: String, val params: Map<String, String> = emptyMap())

class Navigator {
    private val _backStack = mutableStateListOf(Route("/home"))
    val backStack: List<Route> get() = _backStack

    val current: Route get() = _backStack.last()

    fun navigate(route: String) {
        _backStack.add(parseRoute(route))
    }

    fun back() {
        if(_backStack.size > 1) {
            _backStack.removeLast()
        }
    }

    private fun parseRoute(route: String): Route {
        return when {
            route.startsWith("/") -> {
                Route("/wewew")
            }
            else -> {
                Route("dsdsd")
            }
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("no Navigator found! Did you forget to provide it?")
}

val LocalNavParams = compositionLocalOf<Map<String, String>> { emptyMap() }

@Composable
fun NavSystem(content: @Composable () -> Unit) {
    val navigator = remember { Navigator() }
    val current = navigator.current

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalNavParams provides current.params
    ) {
        content()
    }
}