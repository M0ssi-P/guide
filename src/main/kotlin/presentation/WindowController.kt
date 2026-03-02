package presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mvvm.SongBookViewModal
import java.awt.GraphicsEnvironment
import java.awt.GraphicsDevice
import java.awt.Rectangle

interface ProjectionViewModel

data class ProjectionEntry(
    val device: GraphicsDevice,
    val bounds: Rectangle,
    val fullScreen: Boolean = false,
    var viewModel: ProjectionViewModel
)

class WindowController {
    var presentation by mutableStateOf(false)
        private set;

    val availableProjection = mutableStateMapOf<Int, ProjectionEntry>()

    private val _model = MutableStateFlow(SongBookViewModal())
    val model = _model.asStateFlow()

    fun openPresentation() {
        presentation = true
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrCreate(key: Int, factory: () -> T): T {
        return availableProjection.getOrPut(key) { factory() as ProjectionEntry } as T
    }

    fun removeScreen(key: Int) {
        if(availableProjection.size <= 1) {
            closePresentation()
        } else {
            availableProjection.remove(key)
        }
    }

    fun clear() {
        availableProjection.clear()
    }

    fun containsKey(key: Int): Boolean {
        return availableProjection.containsKey(key)
    }

    fun updateProjection(key: Int, model: ProjectionEntry) {
        availableProjection[key]?.let { entry ->
            availableProjection[key] = model
        }
    }

    fun switchModel(model: SongBookViewModal) {
        _model.value = model
    }

    fun toFullScreen(key: Int, bool: Boolean = true) {
        availableProjection[key] = availableProjection[key]!!.copy(fullScreen = bool)
    }

    fun closePresentation() {
        presentation = false
        clear()
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