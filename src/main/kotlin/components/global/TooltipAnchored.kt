package components.global

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Text

@Composable
fun TooltipAnchor(
    tooltipText: String,
    content: @Composable () -> Unit
) {
    val showTooltip = remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                anchorPosition = IntOffset(
                    pos.x.toInt(),
                    pos.y.toInt()
                )
            }
            .onHover { hovered ->
                showTooltip.value = hovered
                false
            }
            .padding(20.dp)
    ) {
        content()
    }

    AnimatedTooltip(
        visible = showTooltip,
        offset = anchorPosition + IntOffset(0, 32)
    ) {
        Text(
            tooltipText,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}