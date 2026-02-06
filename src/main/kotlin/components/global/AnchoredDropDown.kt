package components.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AnchoredDropdown(
    visible: Boolean = false,
    manifest: @Composable () -> Unit,
    onValueChange: (bool: Boolean) -> Unit? = { },
    content: @Composable BoxScope.() -> Unit,
) {
    var show by remember { mutableStateOf(visible) }
    var anchorPosition by remember { mutableStateOf(IntOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource
            ) {
                show = true
                onValueChange(show)
            }
                .onGloballyPositioned { coords ->
                    val pos = coords.boundsInParent()
                    anchorPosition = IntOffset(
                        pos.left.toInt(),
                        pos.bottom.toInt()
                    )
                }
        ) {
            manifest()
        }

        if (show) {
            Popup(onDismissRequest = {
                show = false; onValueChange(show) },
                properties = PopupProperties(
                    focusable = true
                ), content = {
                    content()
                },
                offset = anchorPosition + IntOffset(0, 10)
                )
        }
    }
}