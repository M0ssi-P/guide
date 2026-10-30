package org.theguide.data

import parsers.vgr.models.Paragraph
import parsers.vgr.models.Sermon

/**
 * One spoken sentence of a sermon (the unit the Table lights and Live shares): its paragraph [number], the Table's
 * segment [key] ([section, block, sentence]) and its [start]/[end] on the Table timeline, in seconds, when timed.
 */
data class Sentence(val number: Int, val text: String, val key: List<Int>?, val start: Double?, val end: Double?)

/** The sermon's sentences in reading order (questions left out), as the desktop's deck items. */
fun Sermon.sentences(): List<Sentence> =
    (sections ?: emptyList()).flatMap { sec -> sec.content.filter { it.number != null && !it.isQuestion }.flatMap { it.sentences() } }

private val abbreviations = setOf("bro", "mr", "mrs", "dr", "st", "sr", "jr", "vs", "no", "rev", "sis")
private val ends = Regex("""[.!?…]["”’)]*\s+""")

/** Consecutive segments with the same index are one sentence; untimed paragraphs are split at full stops. */
private fun Paragraph.sentences(): List<Sentence> {
    val n = number ?: 0
    val segs = content.flatMapIndexed { li, line -> line.segments.mapIndexed { si, seg -> (li > 0 && si == 0) to seg } }
    if (segs.none { !it.second.index.isNullOrEmpty() }) {
        val whole = content.joinToString(" ") { l -> l.segments.joinToString("") { it.content } }.trim().replace(Regex("\\s+"), " ")
        val cuts = ends.findAll(whole).filter { m ->
            val word = whole.substring(0, m.range.first).substringAfterLast(' ').lowercase().trimStart('(', '“', '"')
            word !in abbreviations
        }.map { it.range.last + 1 }.toList()
        return (listOf(0) + cuts + whole.length).distinct().zipWithNext().mapNotNull { (a, b) ->
            whole.substring(a, b).trim().takeIf { it.isNotEmpty() }?.let { Sentence(n, it, null, null, null) }
        }
    }
    val groups = mutableListOf<Pair<List<Int>?, MutableList<Pair<Boolean, parsers.vgr.models.Segment>>>>()
    for (pair in segs) {
        val idx = pair.second.index?.takeIf { it.isNotEmpty() }
        val last = groups.lastOrNull()
        if (last != null && (idx == null || idx == last.first)) last.second += pair else groups += idx to mutableListOf(pair)
    }
    return groups.mapNotNull { (key, parts) ->
        val text = buildString { parts.forEachIndexed { k, (newLine, seg) -> if (newLine && k > 0) append(' '); append(seg.content) } }.trim()
        if (text.isEmpty()) null
        else Sentence(n, text, key, parts.mapNotNull { it.second.start }.minOrNull(), parts.mapNotNull { it.second.end }.maxOrNull())
    }
}

fun isTimed(items: List<Sentence>) = items.isNotEmpty() && items.count { it.start != null } >= items.size * 0.9

/** The sentence whose start has passed at [time] seconds on the timeline, and how far into it. */
fun locateTime(items: List<Sentence>, time: Double, duration: Double): Pair<Int, Float> {
    val i = items.indexOfLast { it.start != null && it.start <= time }.coerceAtLeast(0)
    val it = items.getOrNull(i) ?: return 0 to 0f
    val start = it.start ?: time
    val end = it.end ?: items.drop(i + 1).firstNotNullOfOrNull { n -> n.start } ?: duration
    return i to ((time - start) / (end - start).coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f)
}

/** The other way round: the sentence and progress inside it for [fraction] of paragraph [number]. */
fun locate(items: List<Sentence>, number: Int?, fraction: Float): Pair<Int, Float> {
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

/** Where each sentence ends as a share of all the text: untimed text maps audio by length. */
fun cumulativeShares(items: List<Sentence>): List<Float> {
    val total = items.sumOf { it.text.length }.coerceAtLeast(1).toFloat()
    var run = 0f
    return items.map { run += it.text.length / total; run }
}
