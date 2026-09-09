package components.global

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.foundation.modifier.onHover

@Composable
fun AnimatedTooltip(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hoverEnabled: Boolean = false,
    offset: IntOffset = IntOffset.Zero,
    animate: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!animate && !visible) return

    Popup(
        offset = offset,
        onDismissRequest = { onVisibleChange(false) }
    ) {
        if (animate) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut(animationSpec = tween(delayMillis = 100)) +
                        scaleOut(targetScale = 0.95f, animationSpec = tween(delayMillis = 100))
            ) {
                TooltipContainer(
                    modifier = modifier,
                    hoverEnabled = hoverEnabled,
                    onVisibleChange = onVisibleChange,
                    content = content
                )
            }
        } else {
            TooltipContainer(
                modifier = modifier,
                hoverEnabled = hoverEnabled,
                onVisibleChange = onVisibleChange,
                content = content
            )
        }
    }
}

@Composable
private fun TooltipContainer(
    modifier: Modifier,
    hoverEnabled: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.onHover { hovered ->
            if (hoverEnabled) {
                onVisibleChange(hovered)
            }
        }
    ) {
        content()
    }
}