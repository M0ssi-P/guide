package presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mvvm.SongBookViewModal
import java.awt.GraphicsEnvironment
import java.awt.GraphicsDevice

class WindowController {
    var presentation by mutableStateOf(false)
        private set;

    var currentScreen by mutableStateOf(1)
        private set;

    var fullScreen by mutableStateOf(true)
        private set;

    private val _model = MutableStateFlow(SongBookViewModal())
    val model = _model.asStateFlow()

    fun openPresentation() {
        presentation = true
    }

    fun switchScreen(screen: Int) {
        currentScreen = screen
    }

    fun switchModel(model: SongBookViewModal) {
        _model.value = model
    }

    fun toFullScreen(bool: Boolean = true) {
        fullScreen = bool
    }

    fun closePresentation() {
        presentation = false
    }
}

val LocalWindowController = compositionLocalOf<WindowController> {
    error("WindowController not provided")
}

@Composable
fun rememberScreens(): List<GraphicsDevice> {
    var screens by remember {
        mutableStateOf(
            GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .screenDevices
                .toList()
        )
    }

    return screens
}

@Composable
fun PresentationHost(
    content: @Composable () -> Unit
) {
    val windowController = remember { WindowController() }

    CompositionLocalProvider(
        LocalWindowController provides windowController
    ) {
        content()
    }
}