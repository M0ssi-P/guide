package org.theguide.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.theguide.audio.BibleAudio
import org.theguide.data.BibleRepo
import org.theguide.data.Tables
import parsers.bible.models.ChapterAud
import org.theguide.data.Prefs
import org.theguide.data.Verse
import parsers.bible.models.IBibleVersion

/** The Bible being read, shared by the phone and the TV: version, the one beside it, book, chapter and mode. */
object BibleState {
    var versionId by mutableIntStateOf(Prefs.bibleVersion); private set
    var otherId by mutableIntStateOf(Prefs.bibleOther); private set
    var book by mutableStateOf(Prefs.bibleBook); private set
    var chapter by mutableIntStateOf(Prefs.bibleChapter); private set
    var mode by mutableStateOf(Prefs.bibleMode)
    var verses by mutableStateOf<List<Verse>>(emptyList()); private set
    var other by mutableStateOf<List<Verse>>(emptyList()); private set
    var page by mutableIntStateOf(0)
    var verse by mutableIntStateOf(0)
    var sel by mutableIntStateOf(-1)
    var flip by mutableIntStateOf(0)

    /** The chapter's recording with its verse timings, when the version has one. */
    var audio by mutableStateOf<ChapterAud?>(null); private set

    // only versions on the device can be read
    val version: IBibleVersion? get() = BibleRepo.version(versionId)?.takeIf(BibleRepo::offline) ?: BibleRepo.versions.firstOrNull(BibleRepo::offline)
    val otherVersion: IBibleVersion? get() = BibleRepo.version(otherId)?.takeIf { it.id != version?.id && BibleRepo.offline(it) } ?: BibleRepo.versions.firstOrNull { it.id != version?.id && BibleRepo.offline(it) }
    val bookInfo get() = version?.books?.firstOrNull { it.usfm == book }
    /** The book's name in the version's language ("Yohana"). */
    val bookName get() = bookInfo?.let { b ->
        // some versions give only an abbreviation ("Yn") as the short name; the long one then reads "YOHANA"
        if (b.human.length > 3) b.human else b.humanLong.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    } ?: book
    val chapters get() = bookInfo?.chapters?.count { it.canonical } ?: 1
    val ref get() = "$bookName $chapter"
    fun key(n: Int) = "${version?.id}:$book.$chapter.$n"

    suspend fun load() {
        BibleRepo.loadVersions()
        // nothing downloaded yet: the reader's own language first, else English
        if (version == null) firstVersion()?.let { BibleRepo.download(it) }
        val v = version ?: return
        if (versionId != v.id) versionId = v.id
        if (v.books?.none { it.usfm == book } == true) book = v.books!!.first().usfm
        val doc = BibleRepo.content(v, book, chapter)
        verses = BibleRepo.verses(doc)
        audio = doc?.audio?.firstOrNull { it.default } ?: doc?.audio?.firstOrNull()
        if (BibleAudio.url != audio?.let(BibleAudio::mp3)) BibleAudio.stop()
        other = otherVersion?.let { BibleRepo.chapter(it, book, chapter) } ?: emptyList()
        Prefs.log("Read $ref", "bible")
    }

    private fun firstVersion(): IBibleVersion? {
        val lang = Tables.tag(Prefs.readingLang)
        val mine = BibleRepo.versions.filter { it.language.iso63901 == lang }
        return mine.firstOrNull() ?: BibleRepo.versions.firstOrNull { it.abbreviation == "KJV" } ?: BibleRepo.versions.firstOrNull()
    }

    fun go(b: String = book, ch: Int) {
        book = b; chapter = ch.coerceIn(1, chapters); page = 0; verse = 0; sel = -1; flip++
        Prefs.bibleBook = book; Prefs.bibleChapter = chapter
    }

    /** Read [v]; the one read before goes beside it. */
    fun pickVersion(v: IBibleVersion) {
        if (v.id == versionId) return
        otherId = versionId; versionId = v.id; flip++
        Prefs.bibleVersion = versionId; Prefs.bibleOther = otherId
    }

    fun chooseMode(m: String) { mode = m; sel = -1; flip++; Prefs.bibleMode = m }
}

/** Highlight colours: name → the verse tint (.38/.32/.34) and the swatch (.9). */
val Highlights = listOf("gold" to Color(243, 201, 105), "rose" to Color(255, 122, 102), "sky" to Color(143, 183, 255), "sage" to Color(140, 200, 150))
fun highlightTint(name: String?) = Highlights.firstOrNull { it.first == name }?.second?.let { it.copy(alpha = when (name) { "gold" -> .38f; "rose" -> .32f; else -> .34f }) }

/**
 * Verses as one flowing paragraph, each with its small raised number; tapping a verse selects it (underlined in the
 * accent), and highlighted verses sit on their colour.
 */
@Composable
fun VerseFlow(
    verses: List<Verse>, style: TextStyle, acc: Color, supSize: Float, onTap: ((Int) -> Unit)?, sel: Int, tint: (Int) -> Color?, modifier: Modifier = Modifier,
    lit: Pair<Int, Float>? = null, litColors: List<Color> = emptyList(),
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val ranges = remember(verses) { mutableListOf<IntRange>() }
    val text = remember(verses, acc, supSize) {
        ranges.clear()
        buildAnnotatedString {
            verses.forEach { v ->
                val from = length
                withStyle(SpanStyle(fontFamily = Fonts.family(Face.Sans, 600, false, supSize), fontWeight = FontWeight(600), fontSize = supSize.sp, color = acc, baselineShift = BaselineShift.Superscript)) { append("${v.n}") }
                append(" ${v.text} ")
                ranges += from until length
            }
        }
    }
    BasicText(
        text,
        modifier
            .then(if (onTap != null) Modifier.pointerInput(verses) {
                detectTapGestures { pos -> layout?.getOffsetForPosition(pos)?.let { o -> ranges.indexOfFirst { o in it }.takeIf { it >= 0 }?.let { onTap(verses[it].n) } } }
            } else Modifier)
            .drawBehind {
                val l = layout ?: return@drawBehind
                ranges.forEachIndexed { i, r ->
                    val n = verses[i].n
                    val col = tint(n)
                    if (col == null && n != sel && n != lit?.first) return@forEachIndexed
                    for (line in l.getLineForOffset(r.first)..l.getLineForOffset(r.last)) {
                        val s = maxOf(r.first, l.getLineStart(line)); val e = minOf(r.last, l.getLineEnd(line, true))
                        if (e <= s) continue
                        val x0 = l.getHorizontalPosition(s, true); val x1 = l.getHorizontalPosition(e, true)
                        val top = l.getLineTop(line) + 1.dp.toPx(); val bottom = l.getLineBottom(line) - 1.dp.toPx()
                        if (col != null) drawRoundRect(col, Offset(x0, top), Size(x1 - x0, bottom - top), CornerRadius(6.dp.toPx()))
                        // as the Table lights its spoken sentence: each line fills as the verse is read
                        val w = (x1 - x0) * (lit?.second ?: 0f)
                        if (n == lit?.first && litColors.size == 2 && w > 0) drawRoundRect(androidx.compose.ui.graphics.Brush.horizontalGradient(litColors, x0, x0 + w), Offset(x0, top), Size(w, bottom - top), CornerRadius(6.dp.toPx()))
                        if (n == sel) drawRect(acc, Offset(x0, bottom), Size(x1 - x0, 2.dp.toPx()))
                    }
                }
            },
        style = style, onTextLayout = { layout = it },
    )
}

