package navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import ui.theme.LocalTheme

data class modalData(
    val Composition: MutableState<(@Composable () -> Unit)?> = mutableStateOf(null),
    val data: MutableState<Any?> = mutableStateOf(null)
)

fun modalData.show(content: @Composable () -> Unit) {
    this.Composition.value = content
}

fun modalData.dismiss() {
    this.Composition.value = null
}

val LocalModal = staticCompositionLocalOf<modalData> {
    modalData()
}

val LocalWindow = staticCompositionLocalOf<ComposeWindow> {
    error("No ComposeWindow provided")
}

@Composable
fun ModalListener(window: ComposeWindow, content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    val modal = remember { modalData() }
    val visible = modal.Composition.value != null

    CompositionLocalProvider(
        LocalModal provides modal,
        LocalWindow provides window,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(theme.colors.background)) {
            content()
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(150)
                ),
                exit = fadeOut(animationSpec = tween(120)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(120)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            modal.dismiss()
                        }
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    modal.Composition.value?.invoke()
                }
            }
        }
    }
}