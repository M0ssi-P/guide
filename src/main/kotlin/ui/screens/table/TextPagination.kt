package ui.screens.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import parsers.vgr.models.Line
import parsers.vgr.models.Paragraph
import parsers.vgr.models.Section
import parsers.vgr.models.Segment
import parsers.vgr.models.Style

sealed class ContentBlock {
    data class Heading(val key: String, val text: String) : ContentBlock()
    data class ParagraphBlock(
        val key: String,
        val number: Int?,
        val isFirst: Boolean,
        val isQuestion: Boolean,
        val text: AnnotatedString
    ) : ContentBlock()
}

data class PaginatedPage(val blocks: List<ContentBlock>)

/**
 * TODO: match this to your real Style enum's actual case names — matching by `.name` so this
 * compiles regardless of exactly which values exist, but the names below are a guess (BOLD,
 * ITALIC, UNDERLINE, STRIKETHROUGH, SUPERSCRIPT). Anything unmatched is a no-op, so worst case
 * an unrecognized style is silently dropped rather than crashing.
 */
internal fun styleToSpanStyle(style: Style): SpanStyle = when (style.toString().uppercase()) {
    "BOLD" -> SpanStyle(fontWeight = FontWeight.Bold)
    "ITALIC" -> SpanStyle(fontStyle = FontStyle.Italic)
    "UNDERLINE" -> SpanStyle(textDecoration = TextDecoration.Underline)
    "STRIKETHROUGH", "STRIKE" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    "SUPERSCRIPT" -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em)
    else -> SpanStyle()
}

/**
 * Flattens a Paragraph's Lines/Segments into one styled AnnotatedString.
 *
 * JUDGMENT CALL: lines are joined with a plain space, i.e. treated as soft-wrapped prose rather
 * than forced line breaks. If your Lines are meant to break visually where they break in the
 * data (poetic/hymn-style formatting rather than word-sync bookkeeping), change the `" "` below
 * to `"\n"`.
 */
fun Paragraph.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    content.forEachIndexed { i, line: Line ->
        if (i > 0) append(" ")
        line.segments.forEachIndexed { ind,  seg: Segment ->
            val merged = seg.style.fold(SpanStyle()) { acc, s -> acc.merge(styleToSpanStyle(s)) }
            withStyle(merged) { append(seg.content) }
        }
    }
}

/**
 * Maps your real content tree to the flat block list the paginator packs into pages. Order is
 * preserved, so whichever Paragraph has `isFirst == true` will land on page 1 as long as it's
 * genuinely first in `sections` — the drop cap in TableReadingScreen relies on that.
 */
fun sectionsToBlocks(sections: List<Section>): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    sections.forEachIndexed { si, section ->
        if (section.isHeader) {
            blocks += ContentBlock.Heading(key = section.key, text = section.paragraph)
        }
        section.content.forEachIndexed { pi, paragraph ->
            blocks += ContentBlock.ParagraphBlock(
                key = "${section.key}-${paragraph.id ?: pi}",
                number = paragraph.number,
                isFirst = paragraph.isFirst,
                isQuestion = paragraph.isQuestion,
                text = paragraph.toAnnotatedString()
            )
        }
        // Guard for a shape I'm not fully sure about: a non-header section with no parsed
        // Paragraphs but a non-blank `paragraph` field. Shouldn't normally happen given the
        // model, but this keeps that text from silently disappearing if it does.
        if (!section.isHeader && section.content.isEmpty() && section.paragraph.isNotBlank()) {
            blocks += ContentBlock.ParagraphBlock(
                key = "${section.key}-fallback",
                number = null,
                isFirst = false,
                isQuestion = false,
                text = AnnotatedString(section.paragraph)
            )
        }
    }
    return blocks
}

/**
 * Packs [blocks] into pages that fit [availableWidth] x [availableHeight], re-measuring
 * whenever the size, styles, or blocks change. Call this from inside a BoxWithConstraints (or
 * anywhere you already know the reading column's measured size) — see TableReadingScreen.kt.
 */
@Composable
fun rememberPaginatedBlocks(
    blocks: List<ContentBlock>,
    availableWidth: Dp,
    availableHeight: Dp,
    bodyStyle: TextStyle,
    headingStyle: TextStyle,
    blockSpacing: Dp,
    // The first page usually carries extra chrome above the body (a title/meta row, a pull
    // quote) that isn't part of `blocks` at all — reserve room for it so body text on page 1
    // doesn't overflow under that chrome. This is a flat estimate, not a measurement of that
    // chrome's real composables (TextMeasurer only measures text) — widen it if page 1 still
    // looks cramped for your actual header height.
    firstPageReserved: Dp = 0.dp
): List<PaginatedPage> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    return remember(blocks, availableWidth, availableHeight, bodyStyle, headingStyle, blockSpacing, firstPageReserved) {
        val widthPx = with(density) { availableWidth.roundToPx() }.coerceAtLeast(1)
        val fullHeightPx = with(density) { availableHeight.roundToPx() }.coerceAtLeast(1)
        val firstPageHeightPx = (fullHeightPx - with(density) { firstPageReserved.roundToPx() }).coerceAtLeast(1)
        val spacingPx = with(density) { blockSpacing.roundToPx() }
        val constraints = Constraints(maxWidth = widthPx)

        fun blockHeightPx(block: ContentBlock): Int {
            val (text, style) = when (block) {
                is ContentBlock.Heading -> AnnotatedString(block.text) to headingStyle
                is ContentBlock.ParagraphBlock -> block.text to bodyStyle
            }
            return measurer.measure(text = text, style = style, constraints = constraints).size.height
        }

        val pages = mutableListOf<MutableList<ContentBlock>>()
        var current = mutableListOf<ContentBlock>()
        var usedHeight = 0

        for (block in blocks) {
            val pageLimit = if (pages.isEmpty()) firstPageHeightPx else fullHeightPx
            val h = blockHeightPx(block)
            val addIfAppended = if (current.isEmpty()) h else h + spacingPx

            if (current.isNotEmpty() && usedHeight + addIfAppended > pageLimit) {
                pages += current
                current = mutableListOf()
                usedHeight = 0
            }

            val addFinal = if (current.isEmpty()) h else h + spacingPx
            current += block
            usedHeight += addFinal
            // Note: a single block taller than the whole page (a very long unbroken paragraph)
            // still lands alone on its own page and may overflow it — split such Paragraphs
            // smaller upstream (e.g. by Line) if that turns out to matter for your content.
        }
        if (current.isNotEmpty()) pages += current
        if (pages.isEmpty()) {
            pages += mutableListOf<ContentBlock>()
        }

        pages.map { PaginatedPage(it) }
    }
}
