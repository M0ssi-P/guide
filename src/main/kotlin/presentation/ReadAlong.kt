package presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import ui.theme.Fraunces
import kotlin.math.abs
import kotlin.math.roundToInt

private val sentenceEnd = Regex("""[.!?…]+["”’)\]]*\s+""")
private val abbreviations = setOf("mr", "mrs", "dr", "st", "bro", "sis", "rev", "vs", "jr", "sr", "no")

/** Splits one paragraph or verse into sentences, keeping its text styles (italics, quotes…) on each piece. */
fun DeckItem.sentences(): List<DeckItem> {
    val src = rich ?: AnnotatedString(text)
    val cuts = sentenceEnd.findAll(src.text).filter { m ->
        val word = src.text.substring(0, m.range.first).substringAfterLast(' ').lowercase().trimStart('(', '“', '"')
        word !in abbreviations
    }.map { it.range.last + 1 }.toList()
    val bounds = (listOf(0) + cuts + src.length).distinct()
    return bounds.zipWithNext().mapNotNull { (a, b) ->
        val piece = src.subSequence(a, b)
        val trimmed = piece.text.trimEnd().length
        if (trimmed == 0) null else copy(text = piece.text.substring(0, trimmed), rich = piece.subSequence(0, trimmed))
    }.ifEmpty { listOf(this) }
}

/** Where in paragraph [number] the reader is, 0..1 by characters, given the live sentence and how far into it. */
fun paragraphFraction(items: List<DeckItem>, index: Int, progress: Float): Float {
    val cur = items.getOrNull(index) ?: return 0f
    val para = items.indices.filter { items[it].number == cur.number }
    val total = para.sumOf { items[it].text.length }.coerceAtLeast(1)
    val before = para.filter { it < index }.sumOf { items[it].text.length }
    return (before + progress * cur.text.length) / total
}

/** The other way round: the sentence and progress inside it for [fraction] of paragraph [number]. */
fun locate(items: List<DeckItem>, number: Int?, fraction: Float): Pair<Int, Float> {
    val para = items.indices.filter { items[it].number == number }
    if (para.isEmpty()) return (items.indexOfFirst { number != null && it.number >= number }.coerceAtLeast(0)) to 0f
    val total = para.sumOf { items[it].text.length }.coerceAtLeast(1)
    var run = 0f
    for (i in para) {
        val len = items[i].text.length / total.toFloat()
        if (fraction <= run + len || i == para.last()) return i to ((fraction - run) / len.coerceAtLeast(1e-6f)).coerceIn(0f, 1f)
        run += len
    }
    return para.last() to 1f
}

/**
 * The live sentence: the spoken part lit with a soft glow, the rest dim. [progress] is 0..1 by characters.
 * Existing styles stay underneath; colour spans are laid on top.
 */
fun glowText(item: DeckItem, progress: Float, lit: Color, dim: Color): AnnotatedString {
    val base = item.rich ?: AnnotatedString(item.text)
    val cut = (base.length * progress.coerceIn(0f, 1f)).roundToInt()
    return buildAnnotatedString {
        append(base)
        if (cut > 0) addStyle(SpanStyle(color = lit, shadow = Shadow(lit.copy(alpha = 0.55f), blurRadius = 18f)), 0, cut)
        if (cut < base.length) addStyle(SpanStyle(color = dim), cut, base.length)
    }
}

private fun plain(item: DeckItem, color: Color): AnnotatedString =
    buildAnnotatedString { append(item.rich ?: AnnotatedString(item.text)); addStyle(SpanStyle(color = color), 0, length) }

/**
 * Every sentence in a column that smooth-scrolls so the live one sits in the middle, large, with the glow;
 * earlier sentences stay calmly lit, later ones dim. Sizes are the design's (38 / 20 / 16) times [scale].
 */
@Composable
fun FocusReader(
    items: List<DeckItem>,
    index: Int,
    progress: Float,
    lit: Color,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    // the listener page passes its reading settings; the projector keeps the defaults
    family: androidx.compose.ui.text.font.FontFamily = Fraunces,
    sizes: Triple<Float, Float, Float> = Triple(38f, 20f, 16f),
    fg: Color = Color(0xFFE8DFC8),
    glow: Boolean = true,
    rest: Color = Color(0xFFFFF6E2),
    under: ((Int) -> String?)? = null,
) {
    val list = rememberLazyListState()
    // audio updates arrive a few times a second; glide between them so the glow moves by character
    val shown by animateFloatAsState(progress, tween(if (progress == 0f) 0 else 400, easing = LinearEasing))
    BoxWithConstraints(modifier) {
        val half = maxHeight / 2
        LaunchedEffect(index, items.size) {
            if (items.isEmpty()) return@LaunchedEffect
            val visible = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (visible == null) list.scrollToItem(index)
            val info = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return@LaunchedEffect
            val mid = (list.layoutInfo.viewportStartOffset + list.layoutInfo.viewportEndOffset) / 2
            list.animateScrollBy((info.offset + info.size / 2 - mid).toFloat(), tween(700, easing = components.global.SoftEase))
        }
        LazyColumn(
            state = list, userScrollEnabled = false, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = half),
            verticalArrangement = Arrangement.spacedBy((22 * scale).dp),
        ) {
            itemsIndexed(items) { i, item ->
                val d = abs(i - index)
                val size by animateFloatAsState(if (d == 0) sizes.first else if (d == 1) sizes.second else sizes.third, tween(500))
                androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        when {
                            i == index -> if (glow) glowText(item, shown, lit, rest.copy(alpha = 0.42f)) else plain(item, rest)
                            i < index -> plain(item, fg.copy(alpha = if (d == 1) 0.55f else 0.3f))
                            else -> plain(item, fg.copy(alpha = if (d == 1) 0.4f else 0.16f))
                        },
                        fontFamily = family,
                        fontSize = (size * scale).sp,
                        lineHeight = (size * 1.3f * scale).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (i == index) under?.invoke(i)?.let {
                        Text(it, color = Color(0xFF8A93A6), fontSize = 15.sp, lineHeight = 22.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

/** A whole paragraph (or verse) with its live sentence glowing, for paragraph style and read-along. */
fun paragraphGlow(items: List<DeckItem>, index: Int, progress: Float, lit: Color, text: Color): AnnotatedString {
    val cur = items.getOrNull(index) ?: return AnnotatedString("")
    val para = items.indices.filter { items[it].number == cur.number }
    return buildAnnotatedString {
        para.forEachIndexed { k, i ->
            if (k > 0) append(" ")
            append(
                when {
                    i == index -> glowText(items[i], progress, lit, text.copy(alpha = 0.55f))
                    i < index -> plain(items[i], lit.copy(alpha = 0.75f))
                    else -> plain(items[i], text.copy(alpha = 0.55f))
                }
            )
        }
    }
}
