package ui.screens.bible

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.composables.maximize
import com.composables.sizeIcon
import com.composables.soundIcon
import components.global.BookChapterSelector
import components.global.Button
import components.global.ChapterContent
import components.global.VersionSelector
import kotlinx.coroutines.launch
import mvvm.BibleViewModel
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme

@Composable
fun BibleContents(model: BibleViewModel) {
    val theme = LocalTheme.current
    val selectedVerse = remember { mutableStateOf<MutableList<Int>>(mutableListOf()) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 1200.dp)
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        BookChapterSelector(model)
                        VersionSelector(model)
                    }
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(theme.colors.surface)
                                .border(width = 1.dp, color = theme.colors.border, shape = RoundedCornerShape(50.dp))
                                .pointerHoverIcon(
                                    PointerIcon(
                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple()
                                ) {

                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            fillMaxSize = false
                        ) {
                            Icon(
                                imageVector = soundIcon(theme.colors.deeming),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Button(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(theme.colors.surface)
                                .border(width = 1.dp, color = theme.colors.border, shape = RoundedCornerShape(50.dp))
                                .pointerHoverIcon(
                                    PointerIcon(
                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple()
                                ) {

                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            fillMaxSize = false
                        ) {
                            Icon(
                                imageVector = sizeIcon(theme.colors.deeming),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Button(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4))
                                .background(theme.colors.surface)
                                .border(width = 1.dp, color = theme.colors.border, shape = RoundedCornerShape(4.dp))
                                .pointerHoverIcon(
                                    PointerIcon(
                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple()
                                ) {

                                }
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            fillMaxSize = false,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = maximize(theme.colors.deeming),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )

                            Text("Fullscreen", style = theme.typography.button, color = theme.colors.deeming)
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 1200.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
            ) {
                LazyColumn(
                    state = model.contentScrollState,
                    modifier = Modifier.fillMaxWidth()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    model.contentScrollState.scrollBy(-delta)
                                }
                            },
                        )
                ) {
                    if(!model.chapterContent?.chapter.isNullOrEmpty()) {
                        itemsIndexed(model.chapterContent?.chapter!!) { index, chapter ->
                            ChapterContent(
                                chapter,
                                selectedVerse = selectedVerse,
                            )
                        }
                    }
                }
            }

            VerticalScrollbar(
                scrollState = model.contentScrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }
}