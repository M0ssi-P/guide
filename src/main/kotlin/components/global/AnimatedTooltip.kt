package components.global

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.jetbrains.jewel.foundation.modifier.onHover

@Composable
fun AnimatedTooltip(
    modifier: Modifier = Modifier,
    visible: MutableState<Boolean>,
    offset: IntOffset,
    content: @Composable () -> Unit
) {
    Popup(offset = offset) {
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut(animationSpec = tween(
                delayMillis = 100
            )) + scaleOut(targetScale = 0.95f, animationSpec = tween(
                delayMillis = 100
            ))
        ) {
            Box(
                modifier = modifier
                    .onHover { hovered -> visible.value = hovered }
            ) {
                content()
            }
        }
    }
}