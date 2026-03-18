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
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun ChapterContent(
    sections: ChapterSection,
    highlightedVerse: Int? = null,
    underlinedVerses: Set<Int> = emptySet(),
    selectedVerse: MutableState<MutableList<Int>>,
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

                    verse.content.forEach { segment ->
                        if(segment.type == VerseLineEnum.Reference) return@forEach

                        val color = if(segment.isJesus) theme.colors.crimson else theme.colors.text
                        val isPartOf = selectedVerse.value.isNotEmpty() && verse.number in selectedVerse.value.first() .. selectedVerse.value.last()

                        val spanStyle = when(segment.display) {
                            VerseLineDisplay.Normal -> SpanStyle(
                                color = color,
                                textDecoration = if(isPartOf) TextDecoration.Underline else null
                            )
                            VerseLineDisplay.Italic -> SpanStyle(color = color, fontStyle = FontStyle.Italic,
                                textDecoration = if(isPartOf) TextDecoration.Underline else null)
                            VerseLineDisplay.SmallCaps -> SpanStyle(color = color, fontFeatureSettings = "smcp",
                                textDecoration = if(isPartOf) TextDecoration.Underline else null)
                            VerseLineDisplay.BdSmallCaps -> SpanStyle(color = color, fontWeight = FontWeight.Bold, fontFeatureSettings = "smcp",
                                textDecoration = if(isPartOf) TextDecoration.Underline else null)
                        }

                        pushStyle(spanStyle)
                        append("${segment.content ?: ""} ")
                        pop()
                    }
                    append(" ")

                    val end = length
                    addStringAnnotation(
                        tag = "VERSE",
                        annotation = verse.number.toString(),
                        start = start,
                        end = end
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
                    .padding(vertical = 15.dp),
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