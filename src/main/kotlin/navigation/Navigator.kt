package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

class Navigator(page: Screen = Screen.Library(LibraryScreen.Hymns())) {
    val id: String = UUID.randomUUID().toString()
    private val _backStack = mutableStateListOf<Screen>(page)
    private val _query = mutableStateListOf(QueryParams())
    private val _forwardStack = mutableStateListOf<Screen>()
    private val _forwardQuery = mutableStateListOf<QueryParams>()
    private val _metadata = mutableStateOf<PageMetadata>(PageMetadata())
    val backStack: List<Screen> get() = _backStack

    val current: Screen get() = _backStack.last()
    val query: QueryParams get() = _query.last()
    val metadata: PageMetadata get() = _metadata.value

    val canGoBack get() = _backStack.size > 1
    val canGoNext get() = _forwardStack.isNotEmpty()

    fun navigate(page: Screen) {
        _backStack.add(page)
        _query.add(QueryParams())

        _forwardStack.clear()
        _forwardQuery.clear()
    }

    fun back() {
        if (canGoBack) {
            _forwardStack.add(_backStack.removeLast())
            _forwardQuery.add(_query.removeLast())
        }
    }

    fun next() {
        if (canGoNext) {
            _backStack.add(_forwardStack.removeLast())
            _query.add(_forwardQuery.removeLast())
        }
    }

    fun setQuery(key: String, value: String?) {
        val index = _query.lastIndex
        val current = _query[index]

        val newParams = current.params.toMutableMap().apply {
            if (value == null) remove(key)
            else put(key, value)
        }

        _query[index] = current.copy(params = newParams)
    }

    fun setMetadata(title: String? = null, icon: ImageVector? = null){
        val newVal = _metadata.value.copy(
            icon = icon,
            title = title,
        )
        _metadata.value = newVal
    }
}

data class QueryParams(
    val params: Map<String, String> = emptyMap()
) {
    operator fun get(key: String): String? = params[key]
}

data class PageMetadata(
    val icon: ImageVector? = null,
    val title: String? = null,
    val description: String? = null
)

class Tabs {
    private val _tabs = mutableStateListOf(Navigator())
    private val _currentIndex = mutableStateOf(0)

    val tabs: List<Navigator> get() = _tabs
    val current: Navigator get() = _tabs[_currentIndex.value]

    fun newTab(page: Screen = Screen.Home) {
        _tabs.add(Navigator(page))
        _currentIndex.value = _tabs.lastIndex
    }

    fun setCurrent(nav: Navigator) {
        _currentIndex.value = _tabs.indexOf(nav)
    }
}

val LocalTabs = staticCompositionLocalOf<Tabs> {
    error("no Navigator found! Did you forget to provide it?")
}

@Composable
fun NavSystem(content: @Composable () -> Unit) {
    val tabs = remember { Tabs() }

    CompositionLocalProvider(
        LocalTabs provides tabs
    ) {
        content()
    }
}