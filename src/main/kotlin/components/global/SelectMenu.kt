package components.global

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.composables.arrowDown
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme

data class Items<T>(
    val key: String,
    val value: T
)

@Composable
fun <T> SelectMenu(data: List<Items<T>>, fill: Boolean = true, default: Items<T>, onValueChange: (Items<T>) -> Unit = {}) {
    val theme = LocalTheme.current
    val selected= remember { mutableStateOf(default) }
    var widthPx by remember { mutableStateOf(0) }
    val widthDp = with(LocalDensity.current) { widthPx.toDp() }
    val interactionSource = remember { MutableInteractionSource() }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (show) 180f else 0f
    )

    val modifier = if(fill) Modifier.fillMaxWidth() else Modifier

    AnchoredDropdown(
        manifest = {
            Row(
                modifier = modifier
                    .clip(shape = RoundedCornerShape(4.dp))
                    .background(theme.colors.gray2nd)
                    .padding(horizontal = 11.dp, vertical = 8.dp)
                    .onSizeChanged {
                        widthPx = it.width
                    }.pointerHoverIcon(
                    PointerIcon(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    )
                ),

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(selected.value.key, style = theme.typography.button, color = theme.colors.deeming)
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = arrowDown(theme.colors.deeming),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp).rotate(rotation)
                )
            }
        },
        onValueChange = {
            show = it
        }
    ) {
        if(data.isNotEmpty()){
            Box(
                modifier = Modifier
                    .widthIn(max = if(widthDp < 100.dp) 200.dp else widthDp)
                    .heightIn(max = 400.dp)
                    .wrapContentHeight()
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(
                            shape = RoundedCornerShape(6.dp),
                            block = {
                                color = Color.Black
                                alpha = 0.10f
                                radius = 10f
                                offset = Offset(0f, 0f)
                            }
                        )
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.colors.surface)
                        .padding(10.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    scrollState.scrollBy(-delta)
                                }
                            },
                        )
                ) {
                    itemsIndexed(data) { index, item ->
                        val hovered = remember(index) { mutableStateOf(false) }

                        val bgOnHover = animateColorAsState(
                            targetValue = if(hovered.value) theme.colors.gray2nd else if (item.key == selected.value.key) {
                                theme.colors.gray2nd
                            } else theme.colors.surface
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    bgOnHover.value
                                )
                                .pointerHoverIcon(
                                    PointerIcon(
                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    )
                                )
                                .clickable(
                                    interactionSource = interactionSource
                                ) {
                                    selected.value = item
                                    onValueChange(item)
                                }
                                .onHover { hover -> hovered.value = hover}
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(item.key,style = theme.typography.subs, color = theme.colors.deeming)
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}