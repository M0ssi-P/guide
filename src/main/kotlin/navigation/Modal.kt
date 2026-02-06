package navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import ui.theme.LocalTheme

data class modalData(
    val Composition: (@Composable () -> Unit)? = null,
    val data: Any? = null
)

val LocalModal = staticCompositionLocalOf<modalData> {
    modalData()
}

@Composable
fun ModalListener(content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    val modal = remember { modalData() }

    CompositionLocalProvider(
        LocalModal provides modal
    ) {
        Box(modifier = Modifier.fillMaxSize().background(theme.colors.background)) {
            content()
            modal.Composition?.let { composition ->
                Box(modifier = Modifier.fillMaxSize().background(theme.colors.background.copy(alpha = 0.10f)).zIndex(1f)) {
                    composition.invoke()
                }
            }
        }
    }
}