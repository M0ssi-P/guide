package components.global

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.arrowDown
import kotlinx.coroutines.launch
import loadData
import mvvm.BibleViewModel
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.skiko.Cursor
import saveData
import ui.settings.UserInterfaceSettings
import ui.theme.LocalTheme
import ui.theme.LocalUi
import java.time.LocalDate

@Composable
fun BookChapterSelector(model: BibleViewModel) {
    val theme = LocalTheme.current
    val interactionSource = remember { MutableInteractionSource() }
//    var show by remember { mutableStateOf(false) }

    PopoverAnchored(
        modifier = Modifier
            .dropShadow(
                shape = RoundedCornerShape(6.dp),
                block = {
                    color = theme.colors.border
                    spread = 1f
                    offset = Offset(0f, 0f)
                }
            )
            .dropShadow(
                shape = RoundedCornerShape(6.dp),
                block = {
                    color = Color.Black.copy(alpha = 0.12f)
                    alpha = 1f
                    spread = -6f
                    radius = 28f
                    offset = Offset(0f, 14f)
                }
            )
            .clip(RoundedCornerShape(6.dp))
            .background(
                theme.colors.popup
            ),
        hoverEnabled = false,
        popup = { state ->
            BookChapterSelectorPopup(state, model)
        },
        animate = false
    ) { show ->
        PickerPill(
            "${model.currentBook?.human ?: "Loading..."} ${model.currentChapter?.human ?: ""}",
            open = show,
            onClick = {
            }
        )
    }
}

@Composable
fun BookChapterSelectorPopup(state: MutableState<Boolean>, model: BibleViewModel) {
    val theme = LocalTheme.current
    val currentVersion = model.currentVersion
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .heightIn(max = 405.dp, min = 405.dp)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ){
        Column(Modifier.widthIn(min = 150.dp, max = 150.dp).fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Books", style = theme.typography.body)
            }
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    state = model.bookScrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    model.bookScrollState.scrollBy(-delta)
                                }
                            },
                        ),
                ) {
                    currentVersion?.let { version ->
                        if(!version.books.isNullOrEmpty()) {
                            itemsIndexed(version.books) { index, book ->
                                SelectorOption(book.human, isActive = model.currentBook?.human == book.human, onClick = {
                                    model.putCurrentBook(book)
                                })
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    scrollState = model.bookScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
        Column(Modifier.widthIn(max = 500.dp).fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Select Chapter", style = theme.typography.body)
            }
            Box {
                LazyVerticalGrid(
                    state = model.chapterScrollState,
                    columns = GridCells.Fixed(8),
                    modifier = Modifier,
                ) {
                    if (model.chapters.isNotEmpty()) {
                        itemsIndexed(model.chapters) { index, chapter ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isToday = model.currentChapter == chapter

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(53f / 56f)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.colors.popupLayer)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = ripple(),
                                    ) {
                                        model.onChapterClicked(chapter)
                                        state.value = false
                                    }
                                    .zIndex(1f)
                                    .pointerHoverIcon(
                                        PointerIcon(
                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(15.dp, 10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
//                                        if (recordedQotd != null) {
//                                            Box(modifier = Modifier.width(5.dp).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(
//                                                theme.colors.primaryHighlight
//                                            ))
//                                        }
                                    }
                                    Text(
                                        text = chapter.human,
                                        style = theme.typography.subs,
                                        color = theme.colors.text,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }

                                if(isToday) {
                                    val gapPx = with(density) { 5.dp.toPx() }

                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .align(Alignment.Center)
                                            .layout { measurable, constraints ->
                                                val placeable = measurable.measure(constraints)
                                                val width = placeable.width + (gapPx * 2).toInt()
                                                val height = placeable.height + (gapPx * 2).toInt()
                                                layout(width, height) {
                                                    placeable.place(gapPx.toInt(), gapPx.toInt())
                                                }
                                            }
                                            .border(2.dp, theme.colors.primary, RoundedCornerShape(14.dp))
                                            .zIndex(20f)
                                            .pointerInput(Unit) {}
                                    )
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    scrollState = model.chapterScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun SelectorOption(text: String, isActive: Boolean = false, onClick: () -> Unit = {}) {
    val theme = LocalTheme.current;
    var isHovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHovered || isActive) theme.colors.menuHoverColor else theme.colors.popup)
            .onHover{ bool ->
                isHovered = bool
            }
            .pointerHoverIcon(
                PointerIcon(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                )
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(
                interactionSource = interactionSource,
                onClick = {
                    onClick()
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = theme.typography.semiText, color = if(isHovered) theme.colors.night else theme.colors.text)
    }
}