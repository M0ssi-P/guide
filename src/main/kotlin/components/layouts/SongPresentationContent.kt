package components.layouts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.play
import com.composables.plusIcon
import components.global.Button
import components.global.PopoverAnchored
import components.global.PostsGrid
import components.global.ScreensModal
import components.global.SegmentedToggle
import kotlinx.coroutines.launch
import models.LyricType
import mvvm.ShareViewModels
import mvvm.SongBookViewModal
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.skiko.Cursor
import presentation.LocalWindowController
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.Inter
import ui.theme.LocalTheme
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SongPresentationContent(model: SongBookViewModal) {
    val theme = LocalTheme.current
    val windowController = LocalWindowController.current
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentSong = model.currentSong.collectAsState()
    val currentLine = model.currentLine.collectAsState()
    var selected by remember { mutableStateOf("Songs") }
    val interactionSource = remember { MutableInteractionSource() }
    val userModal = ShareViewModels.userModal

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .newBorder(width = 1.dp, sides = setOf<BorderSide>(BorderSide.Bottom), color = theme.colors.border),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 1200.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 30.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            currentSong.value?.titles?.first()?.title ?: "Unknown",
                            style = theme.typography.h2,
                            color = theme.colors.primaryText
                        )
                        Text("Number: ${currentSong.value!!.songNumber}", style = theme.typography.body, color = theme.colors.text)
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if(selected == "Posts") {
                            Button(
                                modifier = Modifier
                                    .clip(shape = RoundedCornerShape(21.dp))
                                    .background(
                                        color = theme.colors.blue100,
                                    ).size(38.dp).pointerHoverIcon(
                                        PointerIcon(
                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(),
                                        onClick = {
                                            val fd = FileDialog(Frame(), "Select an image", FileDialog.LOAD)
                                            fd.isVisible = true
                                            fd.isMultipleMode = true
                                            fd.filenameFilter = FilenameFilter { _, name ->
                                                val ext = name.substringAfterLast('.', "").lowercase()
                                                ext in listOf("png", "jpg", "jpeg", "bmp", "gif")
                                            }

                                            fd.files.firstOrNull()?.let { file ->
                                                val ext = file.extension.lowercase()
                                                if (ext in listOf("png", "jpg", "jpeg", "bmp", "gif")) {
                                                    userModal.saveImage(file.absolutePath)
                                                }
                                            }
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = plusIcon(theme.colors.blue3),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        SegmentedToggle(
                            onChange = {
                                selected = it
                            }
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) with
                            fadeOut(animationSpec = tween(150))
                }
            ) {
                if(selected == "Songs") {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.widthIn(min = 200.dp, max = 1200.dp)
                            .fillMaxSize()
                            .padding(horizontal = 30.dp, vertical = 30.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    scope.launch {
                                        scrollState.scrollBy(-delta)
                                    }
                                },
                            ),

                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            currentSong.value?.lyrics ?: emptyList(),
                        ) { index, line ->
                            val isActive = line.id == currentLine.value?.id
                            val hovered = remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .border(width = 2.dp, color = if(isActive) {
                                        theme.colors.crimson
                                    } else if(hovered.value) {
                                        theme.colors.blue
                                    } else theme.colors.surface, shape = RoundedCornerShape(4.dp))
                                    .onHover {
                                        hovered.value = it
                                    }
                                    .padding(5.dp)
                            ) {
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
                                        .background(
                                            theme.colors.popup,
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    hoverEnabled = true,
                                    popup = { state ->
                                        ScreensModal(state, currentSong.value!!, line)
                                    }
                                ) {
                                    Button(
                                        modifier = Modifier.background(
                                            brush = Brush.verticalGradient(
                                                colors = if(!isActive) listOf(Color(0xFF109DF5), Color(0xFF1B55F7)) else {
                                                    listOf(theme.colors.crimson, Color(0xFF890808))
                                                }
                                            ),
                                            shape = RoundedCornerShape(21.dp)
                                        ).size(20.dp).pointerHoverIcon(
                                            PointerIcon(
                                                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                            )
                                        ).clickable(
                                            interactionSource = interactionSource
                                        ) {
                                            model.setCurrentLine(line)
                                            windowController.switchModel(model)
                                        }
                                    ) {
                                        if(!isActive) {
                                            Icon(
                                                imageVector = play(theme.colors.light),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.size(8.dp)
                                                    .background(theme.colors.surface)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = (if(line.type == LyricType.VERSE) "${line.verseNumber} - " else "Chorus - ") + line.lines.joinToString(" "),
                                    style = TextStyle(
                                        fontFamily = Inter,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = theme.colors.deeming
                                )
                            }
                        }
                    }
                } else {
                    PostsGrid(userModal.images)
                }
            }

            VerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }
}