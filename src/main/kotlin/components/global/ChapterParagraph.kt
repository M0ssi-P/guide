package components.global

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import mvvm.ShareViewModels
import org.jetbrains.jewel.ui.component.Text
import parsers.bible.models.ChapterSection
import parsers.bible.models.Paragraph
import parsers.bible.models.VerseLineDisplay
import parsers.bible.models.VerseLineEnum
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import ui.theme.Inter
import ui.theme.LocalTheme

private enum class WordTiming { READ, CURRENT, UPCOMING }

@Composable
fun ChapterContent(
    sections: ChapterSection,
    highlightedVerse: Int? = null,
    // Index (0-based) of the word currently being spoken inside
    // `highlightedVerse` — feed this from currentWordIndex(...) /
    // buildVerseWordTimings(...) against that verse's real timing data,
    // the same helpers used for the other reading composable. -1 (default)
    // means "no live word position" (paused / no timing available), and
    // every verse renders exactly as it did before this change.
    activeWordIndexInVerse: Int = -1,
    underlinedVerses: Set<Int> = emptySet(),
    selectedVerse: MutableState<MutableList<Int>>,
    onActiveVersePositioned: (Rect) -> Unit = {},
    onVerseClick: (Int) -> Unit = {}
) {
    val theme = LocalTheme.current
    val globalModel = ShareViewModels.globalViewModel
    val localFullscreen = globalModel.localFullscreen.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (sections.hasHeading && sections.heading != null && !localFullscreen.value) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp)) {
                Text("${sections.heading.heading}", style = theme.typography.h2, color = theme.colors.night, fontSize = 28.sp)
            }
        }

        sections.content?.forEachIndexed { paragraphIndex, paragraph ->
            var activeVerseMidOffset: Int? = null

            val annotated = buildAnnotatedString {
                paragraph.verses.forEach { verse ->
                    val start = length

                    pushStyle(
                        SpanStyle(
                            fontSize = 12.sp,
                            baselineShift = BaselineShift.Superscript,
                            color = theme.colors.blue3
                        )
                    )
                    append(if(verse.displayNumber) verse.number.toString() else "")
                    pop()
                    append(" ")

                    val isTimedVerse = highlightedVerse != null &&
                            verse.number == highlightedVerse &&
                            activeWordIndexInVerse >= 0
                    var wordIndexInVerse = 0

                    verse.content.forEach { segment ->
                        if(segment.type == VerseLineEnum.Reference) return@forEach

                        val color = if(segment.isJesus) theme.colors.crimson else theme.colors.text
                        val isPartOf = selectedVerse.value.isNotEmpty() && verse.number in selectedVerse.value.first() .. selectedVerse.value.last()
                        val decoration = if(isPartOf) TextDecoration.Underline else null

                        fun styleFor(base: Color): SpanStyle = when(segment.display) {
                            VerseLineDisplay.Normal -> SpanStyle(color = base, textDecoration = decoration)
                            VerseLineDisplay.Italic -> SpanStyle(color = base, fontStyle = FontStyle.Italic, textDecoration = decoration)
                            VerseLineDisplay.SmallCaps -> SpanStyle(color = base, fontFeatureSettings = "smcp", textDecoration = decoration)
                            VerseLineDisplay.BdSmallCaps -> SpanStyle(color = base, fontWeight = FontWeight.Bold, fontFeatureSettings = "smcp", textDecoration = decoration)
                        }

                        if (!isTimedVerse) {
                            pushStyle(styleFor(color))
                            append("${segment.content ?: ""} ")
                            pop()
                            return@forEach
                        }

                        // Word-by-word only for the verse being read aloud right now.
                        val words = (segment.content ?: "").split(" ").filter { it.isNotEmpty() }
                        words.forEach { word ->
                            val timing = when {
                                wordIndexInVerse < activeWordIndexInVerse -> WordTiming.READ
                                wordIndexInVerse == activeWordIndexInVerse -> WordTiming.CURRENT
                                else -> WordTiming.UPCOMING
                            }
                            wordIndexInVerse++

                            val wordStyle = when (timing) {
                                WordTiming.READ -> styleFor(theme.colors.night)
                                WordTiming.CURRENT -> styleFor(theme.colors.blue3).copy(
                                    shadow = Shadow(
                                        color = theme.colors.blue3.copy(alpha = 0.35f),
                                        blurRadius = 18f
                                    )
                                )
                                WordTiming.UPCOMING -> styleFor(color.copy(alpha = 0.4f))
                            }

                            pushStyle(wordStyle)
                            append("$word ")
                            pop()
                        }
                    }
                    append(" ")

                    val end = length
                    addStringAnnotation(
                        tag = "VERSE",
                        annotation = verse.number.toString(),
                        start = start,
                        end = end
                    )

                    if (highlightedVerse != null && verse.number == highlightedVerse) {
                        activeVerseMidOffset = (start + end) / 2
                    }
                }
            }

            var textBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            val midOffset = activeVerseMidOffset

            LaunchedEffect(highlightedVerse) {
                val layout = textLayoutResult
                val bounds = textBoundsInWindow
                if (midOffset != null && layout != null && bounds != null) {
                    val safeOffset = midOffset.coerceIn(0, (layout.layoutInput.text.length - 1).coerceAtLeast(0))
                    val localRect = layout.getBoundingBox(safeOffset)
                    onActiveVersePositioned(
                        Rect(
                            left = bounds.left + localRect.left,
                            top = bounds.top + localRect.top,
                            right = bounds.left + localRect.right,
                            bottom = bounds.top + localRect.bottom
                        )
                    )
                }
            }

            val decoration =
                if (paragraphIndex in underlinedVerses) TextDecoration.Underline
                else TextDecoration.None

            ClickableText(
                text = annotated,
                style = TextStyle(
                    fontFamily = Inter,
                    textDecoration = decoration,
                    fontSize = if(localFullscreen.value) 53.sp else 26.sp,
                    lineHeight = (if (localFullscreen.value) 53.sp else 26.sp) * 2,
                    textIndent = when(paragraph.type) {
                        Paragraph.Normal -> TextIndent(firstLine = 20.sp)
                        Paragraph.Q2 -> TextIndent(firstLine = 16.sp)
                        else -> TextIndent(firstLine = 0.sp)
                    },
                    textAlign = when(paragraph.type) {
                        Paragraph.PC -> TextAlign.Center
                        else -> TextAlign.Start
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp)
                    .onGloballyPositioned { coordinates -> textBoundsInWindow = coordinates.boundsInWindow() },
                onTextLayout = { layoutResult -> textLayoutResult = layoutResult },
                onClick = { offset ->
                    val clickedVerse = annotated
                        .getStringAnnotations("VERSE", offset, offset)
                        .firstOrNull()
                        ?.item
                        ?.toInt()

                    clickedVerse?.let { verse ->
                        if (selectedVerse.value.isEmpty()) {
                            selectedVerse.value = mutableListOf(verse)
                            return@let
                        }

                        val anchor = selectedVerse.value.first()
                        val currentEnd = selectedVerse.value.last()

                        selectedVerse.value = when {
                            selectedVerse.value.size == 1 && verse == anchor -> {
                                mutableListOf()
                            }

                            selectedVerse.value.size > 1 && verse == currentEnd -> {
                                val end = verse - 1
                                if (end < anchor) {
                                    mutableListOf(anchor)
                                } else {
                                    (anchor..end).toMutableList()
                                }
                            }

                            verse in selectedVerse.value -> {
                                val start = minOf(anchor, verse)
                                val end = maxOf(anchor, verse)
                                (start..end).toMutableList()
                            }

                            else -> {
                                val start = minOf(anchor, verse)
                                val end = maxOf(anchor, verse)
                                (start..end).toMutableList()
                            }
                        }
                    }
                }
            )
        }
    }
}