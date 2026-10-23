package ui.screens.table

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun HighlightableParagraph(
    paragraph: ReadingParagraph,
    highlights: List<Highlight>,
    selection: TextRange,
    onSelectionChange: (TextRange) -> Unit,
    onSelectionSettled: (TextRange, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    var layoutResult by remember(paragraph.id) { mutableStateOf<TextLayoutResult?>(null) }
    var fieldBoundsInWindow by remember(paragraph.id) { mutableStateOf(Rect.Zero) }

    LaunchedEffect(selection, paragraph.id) {
        if (!selection.collapsed) {
            delay(140)
            layoutResult?.let { layout ->
                val safeStart = selection.min.coerceIn(0, paragraph.text.length)
                val safeEnd = (selection.max - 1).coerceIn(0, (paragraph.text.length - 1).coerceAtLeast(0))
                val startBox = layout.getBoundingBox(safeStart)
                val endBox = layout.getBoundingBox(safeEnd)
                val localRect = Rect(
                    left = minOf(startBox.left, endBox.left),
                    top = minOf(startBox.top, endBox.top),
                    right = maxOf(startBox.right, endBox.right),
                    bottom = maxOf(startBox.bottom, endBox.bottom)
                )
                val windowRect = localRect.translate(fieldBoundsInWindow.left, fieldBoundsInWindow.top)
                onSelectionSettled(selection, windowRect)
            }
        }
    }

    Row(modifier = modifier.wrapContentHeight()) {
        Text(
            "${paragraph.verseNumber}",
            style = TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = TableColors.Gold,
                baselineShift = BaselineShift.Superscript
            )
        )
        Spacer(Modifier.width(4.dp))

        val selectionColors = TextSelectionColors(
            handleColor = TableColors.Gold,
            backgroundColor = TableColors.Gold.copy(alpha = 0.25f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = TextFieldValue(text = paragraph.text, selection = selection),
                onValueChange = { new ->
                    // Read-only: the text itself never changes, only the selection can.
                    if (new.text == paragraph.text) onSelectionChange(new.selection)
                },
                readOnly = true,
                textStyle = TextStyle(
                    fontFamily = FrauncesFamily,
                    fontSize = 18.sp,
                    lineHeight = 33.sp,
                    color = TableColors.Ink
                ),
                cursorBrush = SolidColor(Color.Transparent),
                visualTransformation = highlightTransformation(highlights),
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .onGloballyPositioned { fieldBoundsInWindow = it.boundsInWindow() }
            )
        }
    }
}

private fun highlightTransformation(highlights: List<Highlight>): VisualTransformation = VisualTransformation { text ->
    val builder = AnnotatedString.Builder(text.text)
    for (h in highlights) {
        val start = h.start.coerceIn(0, text.text.length)
        val end = h.end.coerceIn(start, text.text.length)
        if (end > start) {
            builder.addStyle(SpanStyle(background = h.color.tint), start, end)
        }
    }
    TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
}
