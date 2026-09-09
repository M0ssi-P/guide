package ui.screens.bible

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.mossip.auraplayer.engine.PlayerState
import components.global.BookChapterSelector
import components.global.Button
import components.global.ChapterContent
import components.global.PlayerView
import components.global.ReaderTab
import components.global.TopBar
import components.global.VersionSelector
import components.global.layeredRadialBackground
import getCurrentVerseSafe
import kotlinx.coroutines.launch
import mvvm.BibleViewModel
import mvvm.ShareViewModels
import navigation.LocalWindow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.plus
import org.jetbrains.skiko.Cursor
import parsers.bible.models.ChapterSection
import parsers.bible.models.Paragraph
import parsers.bible.models.VerseLineDisplay
import parsers.bible.models.VerseLineEnum
import parsers.vgr.models.Segment
import perVerseMode
import player.Chapter
import player.LocalPlayerState
import player.loadChapters
import ui.theme.Inter
import ui.theme.LocalTheme
import kotlin.collections.forEachIndexed

private val FONT_SIZES = listOf(26, 30, 34, 40, 46)

@Composable
fun BibleContents(
    model: BibleViewModel,
    modifier: Modifier = Modifier,
    fontStep: MutableState<Int>,
    sub: List<Chapter>?,
    selectedVerse: MutableState<MutableList<Int>>,
    verseCount: Int,
) {
    val theme = LocalTheme.current
    val window = LocalWindow.current
    val player = LocalPlayerState.current
    val playerState by player.playerState.collectAsState()
    val isPlaying = playerState == PlayerState.PLAYING
    val duration by player.duration.collectAsState()
    val currentTime by player.currentTime.collectAsState()
    val configModel = ShareViewModels.globalViewModel
    val localFullscreen = configModel.localFullscreen.collectAsState()
    var tab by remember { mutableStateOf(ReaderTab.READING) }
    val coroutineScope = rememberCoroutineScope()
    val scrollbarInteractionSource = remember { MutableInteractionSource() }
    val lazyColumnBoundsInWindow = remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = modifier.fillMaxSize().background(theme.colors.surface).layeredRadialBackground(),
    ) {
        if(!localFullscreen.value) {
            Column {
                TopBar(
                    model,
                    tab,
                    onTabChange = { tab = it },
                    fontStep = fontStep.value,
                    onFontStepChange = { fontStep.value = it.coerceIn(0, FONT_SIZES.lastIndex) },
                    projectionOn = false,
                    onToggleProjection = {
//                            projection = !projection
//                            showChrome = true
//                            if (projection) resetAutoHide() else hideJob?.cancel()
                    },
                    onToggleFullscreen = {  },
                    visible = true,
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when(tab) {
                ReaderTab.READING -> {
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

                        val lyrics = sub?.find { it.default }?.lyrics
                        val lyric = getCurrentVerseSafe(currentTime, lyrics)

                        val flatVerses = remember(model.chapterContent) {
                            model.chapterContent?.chapter?.let { perVerseMode(it) } ?: emptyList()
                        }
                        val highlightedVerseNumber = lyric?.verse?.toIntOrNull()
                        val activeVerse = remember(flatVerses, highlightedVerseNumber) {
                            highlightedVerseNumber?.let { num -> flatVerses.find { it.number == num } }
                        }
                        val wordTimings = if (activeVerse != null && lyric != null) {
                            remember(activeVerse, lyric.timing.start, lyric.timing.end) {
                                buildVerseWordTimings(activeVerse.content, lyric.timing.start, lyric.timing.end)
                            }
                        } else emptyList()
                        val activeWordIdx = if (wordTimings.isNotEmpty())
                            currentWordIndex(wordTimings, currentTime.toFloat())
                        else -1

                        LazyColumn(
                            state = model.contentScrollState,
                            contentPadding = PaddingValues(bottom = 100.dp, top = 64.dp),
                            modifier = Modifier.fillMaxWidth()
                                .onGloballyPositioned { coordinates -> lazyColumnBoundsInWindow.value = coordinates.boundsInWindow() }
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
                                if(!localFullscreen.value) {
                                    itemsIndexed(model.chapterContent?.chapter!!) { index, chapter ->
                                        ChapterContent(
                                            chapter,
                                            highlightedVerse = highlightedVerseNumber,
                                            activeWordIndexInVerse = activeWordIdx,
                                            selectedVerse = selectedVerse,
                                            onActiveVersePositioned = { verseBounds ->
                                                val viewport = lazyColumnBoundsInWindow.value ?: return@ChapterContent
                                                val delta = (verseBounds.top + verseBounds.height / 2f) -
                                                        (viewport.top + viewport.height / 2f)
                                                coroutineScope.launch {
                                                    model.contentScrollState.animateScrollBy(delta)
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    val verses = perVerseMode(model.chapterContent?.chapter!!)

                                    itemsIndexed(verses) { index, verse ->
                                        val isCurrent = lyric?.verse?.toIntOrNull() == verse.number

                                        val wordTimings = if (isCurrent && lyric != null) {
                                            remember(verse, lyric.timing.start, lyric.timing.end) {
                                                buildVerseWordTimings(verse.content, lyric.timing.start, lyric.timing.end)
                                            }
                                        } else emptyList()
                                        val activeWordIdx = if (isCurrent) currentWordIndex(wordTimings, currentTime.toFloat()) else -1

                                        val annotated = buildAnnotatedString {
                                            pushStyle(
                                                SpanStyle(
                                                    fontSize = if (localFullscreen.value) 24.sp else 12.sp,
                                                    baselineShift = BaselineShift.Superscript,
                                                    color = theme.colors.blue3
                                                )
                                            )
                                            append(verse.number.toString())
                                            pop()
                                            append(" ")

                                            if (isCurrent && wordTimings.isNotEmpty()) {
                                                wordTimings.forEachIndexed { wi, word ->
                                                    val segment = verse.content[word.segmentIndex]
                                                    val targetColor = when {
                                                        segment.isJesus -> theme.colors.crimson
                                                        wi == activeWordIdx -> theme.colors.night
                                                        wi < activeWordIdx -> theme.colors.text
                                                        else -> theme.colors.text.copy(alpha = 0.35f)
                                                    }
                                                    val color by animateColorAsState(
                                                        targetValue = targetColor,
                                                        animationSpec = tween(durationMillis = 200, easing = Ease)
                                                    )
                                                    val spanStyle = when (segment.display) {
                                                        VerseLineDisplay.Normal -> SpanStyle(color = color)
                                                        VerseLineDisplay.Italic -> SpanStyle(color = color, fontStyle = FontStyle.Italic)
                                                        VerseLineDisplay.SmallCaps -> SpanStyle(color = color, fontFeatureSettings = "smcp")
                                                        VerseLineDisplay.BdSmallCaps -> SpanStyle(color = color, fontWeight = FontWeight.Bold, fontFeatureSettings = "smcp")
                                                    }
                                                    pushStyle(spanStyle)
                                                    append("${word.text} ")
                                                    pop()
                                                }
                                            } else {
                                                // not the active verse — unchanged, whole-segment coloring
                                                verse.content.forEach { segment ->
                                                    if (segment.type == VerseLineEnum.Reference) return@forEach
                                                    val color by animateColorAsState(
                                                        targetValue = if (segment.isJesus) theme.colors.crimson else theme.colors.text,
                                                        animationSpec = tween(durationMillis = 200, easing = Ease)
                                                    )
                                                    val spanStyle = when (segment.display) {
                                                        VerseLineDisplay.Normal -> SpanStyle(color = color)
                                                        VerseLineDisplay.Italic -> SpanStyle(color = color, fontStyle = FontStyle.Italic)
                                                        VerseLineDisplay.SmallCaps -> SpanStyle(color = color, fontFeatureSettings = "smcp")
                                                        VerseLineDisplay.BdSmallCaps -> SpanStyle(color = color, fontWeight = FontWeight.Bold, fontFeatureSettings = "smcp")
                                                    }
                                                    pushStyle(spanStyle)
                                                    append("${segment.content ?: ""} ")
                                                    pop()
                                                }
                                            }
                                            append(" ")
                                        }

                                        if (isCurrent) {
                                            LaunchedEffect(index) {
                                                val halfScreen = (maxHeight / 2).value.toInt()
                                                model.contentScrollState.animateScrollToItem(index = index, scrollOffset = -(halfScreen - 60))
                                            }
                                        }

                                        Box(Modifier.padding(vertical = 48.dp)) {
                                            ClickableText(
                                                text = annotated,
                                                style = TextStyle(
                                                    fontFamily = Inter,
                                                    fontSize = if (localFullscreen.value) 53.sp else 26.sp,
                                                    lineHeight = (if (localFullscreen.value) 53.sp else 26.sp) * 2,
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                onClick = { }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(model.contentScrollState),
                        style = LocalScrollbarStyle.current.copy(
                            unhoverColor = theme.colors.popup,
                            hoverColor = theme.colors.popup.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        interactionSource = scrollbarInteractionSource,
                    )
                }
                ReaderTab.PLAYER -> PlayerView(
                    elapsedSeconds = currentTime.toFloat(),
                    totalDurationSeconds = duration.toFloat(),
                    playing = isPlaying,
                )
            }
        }
    }
}

private data class TimedWord(
    val segmentIndex: Int,
    val text: String,
    val start: Float,
    val end: Float
)

private fun buildVerseWordTimings(
    segments: List<ChapterSection.IBibleVerseLine>,
    verseStart: Double,
    verseEnd: Double
): List<TimedWord> {
    data class Raw(val segmentIndex: Int, val text: String, val weight: Int)
    val raw = mutableListOf<Raw>()
    segments.forEachIndexed { si, seg ->
        if (seg.type == VerseLineEnum.Reference) return@forEachIndexed
        seg.content?.split(" ")?.filter { it.isNotBlank() }?.forEach { w ->
            raw += Raw(si, w, w.length + 1)
        }
    }
    if (raw.isEmpty()) return emptyList()
    val totalWeight = raw.sumOf { it.weight }.toFloat().coerceAtLeast(1f)
    val duration = (verseEnd - verseStart).toFloat().coerceAtLeast(0.01f)
    var cum = 0f
    return raw.map {
        val s = verseStart.toFloat() + (cum / totalWeight) * duration
        cum += it.weight
        val e = verseStart.toFloat() + (cum / totalWeight) * duration
        TimedWord(it.segmentIndex, it.text, s, e)
    }
}

private fun currentWordIndex(words: List<TimedWord>, currentTime: Float): Int {
    if (words.isEmpty()) return -1
    if (currentTime <= words.first().start) return 0
    if (currentTime >= words.last().end) return words.size - 1
    var lo = 0; var hi = words.size - 1
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (words[mid].end < currentTime) lo = mid + 1 else hi = mid
    }
    return lo
}