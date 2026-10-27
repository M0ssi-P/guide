package components.global

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.Fraunces

@Composable
fun DropCapParagraph(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    capColor: Color = Color(0xFFF0E4C8),
    bodyColor: Color = Color.White.copy(alpha = 0.86f),
    capFontSize: TextUnit = 58.sp,
    bodyFontSize: TextUnit = 19.sp,
    fontFamily: FontFamily = Fraunces,
    bodyLineHeight: TextUnit = 34.sp,
    capLines: Int = 2
) {
    if (text.isEmpty()) return
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    val firstNonSpaceIndex = text.text.indexOfFirst { !it.isWhitespace() }

    val firstChar = if (firstNonSpaceIndex != -1) {
        text.subSequence(firstNonSpaceIndex, firstNonSpaceIndex + 1)
    } else {
        AnnotatedString("")
    }

    val rest = text.subSequence(firstNonSpaceIndex + 1, text.length)

    BoxWithConstraints(modifier) {
        val fullWidthPx = with(density) { maxWidth.roundToPx() }

        // how wide the cap glyph itself renders, so the narrow column starts right after it
        val capLayout = remember(firstChar, capFontSize) {
            measurer.measure(firstChar, TextStyle(fontSize = capFontSize, fontWeight = FontWeight.Medium))
        }
        val gapPx = with(density) { 10.dp.roundToPx() }
        val narrowWidthPx = (fullWidthPx - capLayout.size.width - gapPx).coerceAtLeast(1)

        // pass 1: measure the rest of the paragraph AT the narrow width, purely to find
        // where line `capLines` ends — this is the "float wrap" calculation
        val narrowLayout = remember(rest, narrowWidthPx) {
            measurer.measure(
                rest,
                TextStyle(fontSize = bodyFontSize, lineHeight = bodyLineHeight),
                constraints = Constraints(maxWidth = narrowWidthPx)
            )
        }
        val splitLine = (capLines - 1).coerceIn(0, narrowLayout.lineCount - 1)
        val splitOffset = narrowLayout.getLineEnd(splitLine, visibleEnd = true)

        val wrapped = rest.subSequence(0, splitOffset)     // lines beside the cap
        val remainder = rest.subSequence(splitOffset, rest.length) // everything after

        Column {
            Row {
                Text(firstChar, color = capColor, fontSize = capFontSize,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium, lineHeight = capFontSize * 0.95f,
                    modifier = Modifier.padding(end = 10.dp))
                Text(wrapped, color = bodyColor, fontSize = bodyFontSize, fontFamily = fontFamily, lineHeight = bodyLineHeight,
                    modifier = Modifier.width(with(density) { narrowWidthPx.toDp() }))
            }
            if (remainder.isNotEmpty()) {
                Text(remainder, color = bodyColor, fontSize = bodyFontSize, lineHeight = bodyLineHeight,
                    fontFamily = fontFamily,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun NumberedParagraph(
    number: Int,
    text: AnnotatedString,
    bodyColor: Color,
    numColor: Color = Color(0xFFF0E4C8),
    bodyFontSize: TextUnit = 19.sp,
    bodyLineHeight: TextUnit = 34.sp,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
) {
    if (text.isEmpty()) return
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier) {
        val fullWidthPx = with(density) { maxWidth.roundToPx() }

        // measure the badge itself — "10" reserves more width than "1" automatically
        val numberLayout = remember(number) {
            measurer.measure("$number", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
        }
        val gapPx = with(density) { 6.dp.roundToPx() }
        val narrowWidthPx = (fullWidthPx - numberLayout.size.width - gapPx).coerceAtLeast(1)

        // pass 1: only need to know where LINE 1 ends at the narrow width —
        // everything after that goes back to full width
        val narrowLayout = remember(text, narrowWidthPx) {
            measurer.measure(
                text,
                TextStyle(fontSize = bodyFontSize, lineHeight = bodyLineHeight, fontFamily = fontFamily),
                constraints = Constraints(maxWidth = narrowWidthPx)
            )
        }
        val splitOffset = narrowLayout.getLineEnd(0, visibleEnd = true)

        val firstLine = text.subSequence(0, splitOffset)
        val remainder = text.subSequence(splitOffset, text.length)

        Column {
            Row(verticalAlignment = Alignment.Top) {
                Text("$number", color = numColor, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.padding(end = 6.dp, top = 3.dp))
                Text(firstLine, color = bodyColor, fontSize = bodyFontSize, lineHeight = bodyLineHeight, fontFamily = fontFamily,
                    modifier = Modifier.width(with(density) { narrowWidthPx.toDp() }))
            }
            if (remainder.isNotEmpty()) {
                Text(remainder, color = bodyColor, fontSize = bodyFontSize, lineHeight = bodyLineHeight, fontFamily = fontFamily,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}