package ui.screens.bible

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.maximize
import com.composables.sizeIcon
import com.composables.soundIcon
import components.global.BookChapterSelector
import components.global.Button
import components.global.ChapterContent
import components.global.VersionSelector
import getCurrentVerseSafe
import kotlinx.coroutines.launch
import mvvm.BibleViewModel
import mvvm.ShareViewModels
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.component.plus
import org.jetbrains.skiko.Cursor
import parsers.bible.models.Paragraph
import parsers.bible.models.VerseLineDisplay
import parsers.bible.models.VerseLineEnum
import perVerseMode
import player.LocalPlayerState
import player.loadChapters
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun BibleContents(model: BibleViewModel) {
    val theme = LocalTheme.current
    val player = LocalPlayerState.current
    val configModel = ShareViewModels.globalViewModel
    val localFullscreen = configModel.localFullscreen.collectAsState()
    val selectedVerse = remember { mutableStateOf<MutableList<Int>>(mutableListOf()) }
    val chapterName = model.currentChapter?.usfm?.replace(".", "-")
    val duration by player.duration.collectAsState()
    val currentTime by player.currentTime.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val sub = loadChapters(chapterName)

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(vertical = if(localFullscreen.value) 0.dp else 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if(!localFullscreen.value) {
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
                                        configModel.toggleLocalFullscreen()
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
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .then(
                        if (!localFullscreen.value) {
                            Modifier.widthIn(min = 200.dp, max = 1200.dp)
                                .padding(horizontal = 30.dp)
                        } else Modifier
                            .padding(horizontal = 165.dp)
                    )
                    .fillMaxWidth(),
            ) {
                val vh = maxHeight / 100

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
                        val lyrics = sub?.find { it.default }?.lyrics
                        val lyric = getCurrentVerseSafe(currentTime, lyrics)

                        if(!localFullscreen.value) {
                            itemsIndexed(model.chapterContent?.chapter!!) { index, chapter ->
                                ChapterContent(
                                    chapter,
                                    selectedVerse = selectedVerse,
                                )
                            }
                            item {
                                Spacer(Modifier.height(100.dp))
                            }
                        } else {
                            val verses = perVerseMode(model.chapterContent?.chapter!!)

                            itemsIndexed(verses) { index, verse ->
                                val isCurrent = lyric?.verse?.toIntOrNull() == verse.number

                                val annotated = buildAnnotatedString {
                                    pushStyle(
                                        SpanStyle(
                                            fontSize = if(localFullscreen.value) 24.sp else 12.sp,
                                            baselineShift = BaselineShift.Superscript,
                                            color = theme.colors.blue3
                                        )
                                    )
                                    append(verse.number.toString())
                                    pop()
                                    append(" ")

                                    verse.content.forEach { segment ->
                                        if(segment.type == VerseLineEnum.Reference) return@forEach

                                        val color by animateColorAsState(
                                            targetValue = if(segment.isJesus) theme.colors.crimson else if(isCurrent) theme.colors.night else theme.colors.text,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = Ease
                                            )
                                        )

                                        val spanStyle = when(segment.display) {
                                            VerseLineDisplay.Normal -> SpanStyle(
                                                color = color,
                                            )
                                            VerseLineDisplay.Italic -> SpanStyle(color = color, fontStyle = FontStyle.Italic,)
                                            VerseLineDisplay.SmallCaps -> SpanStyle(color = color, fontFeatureSettings = "smcp")
                                            VerseLineDisplay.BdSmallCaps -> SpanStyle(color = color, fontWeight = FontWeight.Bold, fontFeatureSettings = "smcp")
                                        }

                                        pushStyle(spanStyle)
                                        append("${segment.content ?: ""} ")
                                        pop()
                                    }
                                    append(" ")
                                }

                                val paddindTop = if(index == 0) (vh * 50).value.dp else 48.dp
                                val paddingBottom = if (index == verses.lastIndex) (vh * 50).value.dp else 48.dp

                                if (isCurrent) {
                                    LaunchedEffect(index) {
                                        val halfScreen = (maxHeight / 2).value.toInt()
                                        val centerOffset = -(halfScreen - 60)

                                        model.contentScrollState.animateScrollToItem(
                                            index = index,
                                            scrollOffset = centerOffset
                                        )
                                    }
                                }

                                Box(Modifier.padding(top = paddindTop, bottom = paddingBottom)) {
                                    ClickableText(
                                        text = annotated,
                                        style = TextStyle(
                                            fontFamily = Inter,
                                            fontSize = if(localFullscreen.value) 53.sp else 26.sp,
                                            lineHeight = (if (localFullscreen.value) 53.sp else 26.sp) * 2,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        onClick = { offset ->

                                        }
                                    )
                                }
                            }
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