package components.global

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import org.jetbrains.jewel.foundation.modifier.onHover

@Composable
fun PopoverAnchored(
    modifier: Modifier = Modifier,
    popup: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val showTooltip = remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(IntOffset.Zero) }

    Box {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    val pos = coords.boundsInParent()
                    anchorPosition = IntOffset(
                        pos.right.toInt(),
                        pos.top.toInt()
                    )
                }
                .onHover { hovered ->
                    showTooltip.value = hovered
                    false
                }
        ) {
            content()
        }

        AnimatedTooltip(
            modifier,
            visible = showTooltip,
            offset = anchorPosition + IntOffset(0, 32)
        ) {
            popup()
        }
    }
}