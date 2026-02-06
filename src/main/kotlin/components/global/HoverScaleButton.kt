package components.global

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.hamburger
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Icon
import ui.theme.LocalTheme

@Composable
fun HoverScaleButton(
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (hovered) 0.8f else 1f,
        animationSpec = tween(durationMillis = 250, easing = EaseInOut)
    )

    NavigationButton(
        disabled = false,
        animateBg = false,
        contentColor = theme.colors.light, modifier = Modifier.size(38.dp).scale(scale),
        onClick = {
            onClick()
        },
        onHover =  {
            hovered = it
        }
    ) {
            content()
    }
}