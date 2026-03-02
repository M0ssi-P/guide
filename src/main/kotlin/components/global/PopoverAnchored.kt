package components.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import org.jetbrains.jewel.ui.component.Text

@Composable
fun PopoverAnchored(
    modifier: Modifier = Modifier,
    hoverEnabled: Boolean = false,
    onChange: ( e: MutableState<Boolean>) -> Unit = {},
    popup: @Composable ( e: MutableState<Boolean>) -> Unit,
    content: @Composable (e: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val showTooltip = remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(IntOffset.Zero) }

    LaunchedEffect(showTooltip.value) {
        onChange(showTooltip)
    }

    Box {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    val pos = coords.boundsInParent()
                    anchorPosition = IntOffset(
                        pos.left.toInt(),
                        pos.bottom.toInt()
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if(!hoverEnabled) {
                            showTooltip.value = !showTooltip.value
                        }
                    }
                )
                .onHover { hovered ->
                    if(!hoverEnabled) return@onHover
                    showTooltip.value = hovered
                    false
                }
        ) {
            content(showTooltip.value)
        }

        AnimatedTooltip(
            modifier,
            visible = showTooltip,
            hoverEnabled,
            offset = anchorPosition + IntOffset(0, 10)
        ) {
            popup(showTooltip)
        }
    }
}