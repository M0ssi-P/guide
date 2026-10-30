package org.theguide.ui.phone

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.theguide.audio.BibleAudio
import org.theguide.data.BibleRepo
import parsers.bible.models.IBibleVersion
import org.theguide.data.Canon
import org.theguide.data.Prefs
import org.theguide.data.Verse
import org.theguide.ui.*

/** Verses as text with raised numbers, for pages and parallel rows. */
internal fun verseText(verses: List<Verse>, acc: Color, sup: Float) = buildAnnotatedString {
    verses.forEach { v ->
        withStyle(SpanStyle(fontFamily = Fonts.family(Face.Sans, 600, false, sup), fontWeight = FontWeight(600), fontSize = sup.sp, color = acc, baselineShift = BaselineShift.Superscript)) { append("${v.n}") }
        append(" ${v.text} ")
    }
}

private val versionCovers = listOf(0xFF7A3B2E, 0xFF2E3F6B, 0xFF3B5A3A, 0xFF5E3A6B, 0xFF6B5A2E, 0xFF2E5A5E)
internal fun coverOf(i: Int) = c(versionCovers[i % versionCovers.size])

/** PhoneBible board. */
@Composable
fun PhoneBible(haze: HazeState) {
    val st = BibleState
    LaunchedEffect(st.versionId, st.otherId, st.book, st.chapter) { st.load() }
    val ls = rememberLook("bible", 19)
    val k = ls.look
    var sheet by remember { mutableStateOf("") }
    val body by oneShot(st.flip, 550)
    val ctx = LocalContext.current
    val modes = listOf("scroll" to "Scroll", "pages" to "Pages", "verse" to "Verse", "parallel" to "Parallel")
    val font = if (ls.serif) Face.Serif else Face.Sans

    LaunchedEffect(BibleAudio.playing) { if (BibleAudio.playing) BibleAudio.follow() }
    Box(Modifier.fillMaxSize().background(k.bg).screenGlow(k.glow)) {
        Column(Modifier.fillMaxSize().hazeSource(haze).padding(top = statusTop()).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).press({ sheet = "pick" }, label = "Choose a book and chapter").padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        S(st.ref, 30f, k.tx, weight = 300, tracking = -.3f, maxLines = 1)
                        Ico(Icons.Down, 12f, k.tx)
                    }
                    Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(k.chip).press({ sheet = "ver" }, label = "Version, ${st.version?.title}").padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        T(st.version?.abbreviation ?: "", 12.5f, k.tx, weight = 600)
                    }
                    // the chapter read aloud, each verse lit as it is read
                    st.audio?.let { a ->
                        val on = BibleAudio.playing && BibleAudio.url == BibleAudio.mp3(a)
                        RoundButton({ BibleAudio.toggle(a) }, if (on) "Pause" else "Listen", bg = k.chip) { Ico(if (on) Icons.Pause else Icons.Play, 14f, k.tx) }
                    }
                    RoundButton({ sheet = "set" }, "Reading settings", bg = k.chip) { S("Aa", 16f, k.tx) }
                }
                Segmented(modes, st.mode, k, Modifier.padding(top = 14.dp)) { st.chooseMode(it) }
                Box(Modifier.padding(top = 22.dp).enter({ body }, Ease.Swap)) {
                    when (st.mode) {
                        "scroll" -> VerseFlow(st.verses, style(ls.size.toFloat(), font, color = k.tx, lineHeight = ls.lineHeight), k.acc, 11f,
                            { n -> st.sel = if (st.sel == n) -1 else n }, st.sel, { n -> highlightTint(Prefs.highlights[st.key(n)]) },
                            lit = BibleAudio.lit(st.audio), litColors = listOf(k.litStart, k.litEnd))
                        "pages" -> Pages(k, ls, font)
                        "verse" -> OneVerse(k, ls, font)
                        else -> Parallel(k, ls, font)
                    }
                }
            }
        }
        if (st.mode == "scroll" && st.sel > 0 && sheet.isEmpty()) SelectionBar(ctx)
        ChapterDeck(haze, k)
        when (sheet) {
            "pick" -> BookSheet(haze, k) { sheet = "" }
            "ver" -> VersionSheet(haze, k) { sheet = "" }
            "set" -> Sheet({ sheet = "" }, haze, k.sheet, k.line, base = k.bg) { ReadingSettings(ls) }
        }
    }
}

/** The boards' segmented control: a pill sliding under the chosen tab (.45s, a small overshoot). */
@Composable
internal fun Segmented(items: List<Pair<String, String>>, sel: String, k: Look, modifier: Modifier = Modifier, height: Float = 40f, font: Float = 13f, pillShadow: Boolean = true, bg: Color = k.chip, pill: Color = k.pill, on: Color = k.tx, off: Color = k.sub, onPick: (String) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape((height / 2).dp)).background(bg).padding(3.dp)) {
        val w = maxWidth / items.size
        val ix = items.indexOfFirst { it.first == sel }.coerceAtLeast(0)
        val x = animate(ix.toFloat(), 450, Ease.O115)
        Box(
            Modifier.offset(x = w * x).width(w).fillMaxHeight()
                .then(if (pillShadow) Modifier.cssShadow(0f, 4f, 12f, -4f, Color.Black.copy(alpha = .4f), RoundedCornerShape(((height - 6) / 2).dp)) else Modifier)
                .clip(RoundedCornerShape(((height - 6) / 2).dp)).background(pill)
        )
        Row(Modifier.fillMaxSize()) {
            items.forEach { (id, label) ->
                val col by animateColorAsState(if (id == sel) on else off, tw(300), label = "seg")
                Box(Modifier.weight(1f).fillMaxHeight().press({ onPick(id) }, role = Role.Tab), contentAlignment = Alignment.Center) { T(label, font, col, weight = 600) }
            }
        }
    }
}

@Composable
private fun Pages(k: Look, ls: LookState, font: Face) {
    val st = BibleState
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val innerW = with(density) { (maxWidth - 48.dp).roundToPx() }
        val pageH = with(density) { (470 - 26 - 46).dp.roundToPx() }
        val txt = style(ls.size.toFloat(), font, color = C.PaperInk, lineHeight = ls.lineHeight)
        // pages filled verse by verse, as many as fit the page
        val pages = remember(st.verses, ls.size, ls.face, ls.gap, innerW) {
            val out = mutableListOf<List<Verse>>(); var cur = mutableListOf<Verse>()
            st.verses.forEach { v ->
                val trial = cur + v
                val h = measurer.measure(verseText(trial, k.acc, 11f), txt, constraints = Constraints(maxWidth = innerW)).size.height
                if (h > pageH && cur.isNotEmpty()) { out += cur; cur = mutableListOf(v) } else cur = trial.toMutableList()
            }
            if (cur.isNotEmpty()) out += cur
            out.ifEmpty { listOf(emptyList()) }
        }
        val page = st.page.coerceIn(0, pages.lastIndex)
        val turn by oneShot(page to st.flip, 700)
        Box(Modifier.fillMaxWidth()) {
            // the stacked edges of the pages beneath
            val shape = corners(8f, 22f, 22f, 8f)
            Box(Modifier.matchParentSize().offset(8.dp, 8.dp).padding(end = 2.dp, bottom = 2.dp).clip(shape).background(k.pageEdge2))
            Box(Modifier.matchParentSize().offset(4.dp, 4.dp).padding(end = 1.dp, bottom = 1.dp).clip(shape).background(k.pageEdge))
            Box(
                Modifier.fillMaxWidth().heightIn(min = 470.dp)
                    .graphicsLayer { cameraDistance = perspective(1400f) * density.density }
                    .then(if (page % 2 == 1) Modifier.unfold({ turn }, Ease.Swap, TransformOrigin(0f, .5f)) else Modifier.enter({ turn }, Ease.Swap))
                    .cssShadow(0f, 30f, 50f, -30f, Color.Black.copy(alpha = .7f), shape)
                    .clip(shape).background(k.pageBg).grain(.25f, shape = shape)
                    .drawBehind { drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent), 0f, 14.dp.toPx())) }
                    .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 46.dp)
            ) {
                val text = verseText(pages[page], k.acc, 11f)
                if (page == 0) DropCap("${st.chapter}", text, txt, k.acc, innerW) else BasicText(text, style = txt)
            }
            T("page ${page + 1} of ${pages.size}", 13f, C.PaperInk.copy(alpha = .6f), Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), face = Face.Serif, italic = true)
            Box(
                Modifier.align(Alignment.BottomEnd).size(64.dp).cssShadow(-4f, -4f, 6f, 0f, Color.Black.copy(alpha = .25f), corners(0f, 0f, 22f, 0f))
                    .clip(corners(0f, 0f, 22f, 0f))
                    .drawBehind {
                        val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                        drawPath(p, Brush.linearGradient(0f to k.curl, .44f to k.pageBg, start = Offset(size.width / 2, size.height / 2), end = Offset(size.width, size.height)))
                    }
                    .press({ if (page < pages.lastIndex) st.page = page + 1 else st.go(ch = st.chapter + 1) }, label = "Turn the page")
            )
        }
    }
}

/** A drop cap floated left: the first lines of text wrap beside it, the rest run full width (CSS float). */
@Composable
private fun DropCap(cap: String, text: AnnotatedString, style: TextStyle, acc: Color, width: Int) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val capStyle = style(64f, Face.Serif, color = acc, lineHeight = .85f)
    val capW = measurer.measure(cap, capStyle).size.width + with(density) { 10.dp.roundToPx() }
    val capH = with(density) { (64 * .85f + 6).dp.toPx() }
    val beside = measurer.measure(text, style, constraints = Constraints(maxWidth = (width - capW).coerceAtLeast(1)))
    var lines = 0
    while (lines < beside.lineCount && beside.getLineTop(lines) < capH) lines++
    val cut = if (lines == 0) 0 else beside.getLineEnd(lines - 1, visibleEnd = false)
    Column {
        Row {
            BasicText(cap, Modifier.padding(top = 6.dp, end = 10.dp), style = capStyle)
            BasicText(text.subSequence(0, cut), style = style)
        }
        if (cut < text.length) BasicText(text.subSequence(cut, text.length), style = style)
    }
}

@Composable
private fun OneVerse(k: Look, ls: LookState, font: Face) {
    val st = BibleState
    val v = st.verses.getOrNull(st.verse) ?: return
    val swap by oneShot(st.verse, 500)
    Column(Modifier.fillMaxWidth().heightIn(min = 430.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        S("${v.n}", 120f, k.acc.copy(alpha = .22f), Modifier.padding(bottom = 14.dp).enter({ swap }, Ease.Swap), weight = 300, lineHeight = .9f)
        T(v.text, Math.round(ls.size * 1.35f).toFloat(), k.tx, Modifier.enter({ swap }, Ease.Swap), face = font, weight = 300, lineHeight = 1.4f, align = TextAlign.Center)
        S("${st.ref}:${v.n}", 14f, k.sub, Modifier.padding(top = 16.dp), italic = true)
        Row(Modifier.padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundButton({ st.verse = (st.verse - 1).coerceAtLeast(0) }, "Previous verse", bg = k.chip) { Ico(Icons.Back, 16f, k.tx) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val from = (st.verse - 4).coerceAtLeast(0); val to = (from + 9).coerceAtMost(st.verses.size)
                (from until to).forEach { i ->
                    val w = animate(if (i == st.verse) 20f else 6f, 350)
                    val col by animateColorAsState(if (i == st.verse) k.acc else k.line, tw(350), label = "vd")
                    Box(Modifier.size(w.dp, 6.dp).clip(RoundedCornerShape(3.dp)).background(col))
                }
            }
            RoundButton({ st.verse = (st.verse + 1).coerceAtMost(st.verses.lastIndex) }, "Next verse", bg = k.chip) { Ico(Icons.Forward, 16f, k.tx) }
        }
    }
}

@Composable
private fun Parallel(k: Look, ls: LookState, font: Face) {
    val st = BibleState
    val fs = Math.round(ls.size * .78f).toFloat()
    Column {
        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            T(st.version?.title ?: "", 12f, k.sub, Modifier.weight(1f)); T(st.otherVersion?.title ?: "", 12f, k.sub, Modifier.weight(1f))
        }
        st.verses.forEach { v ->
            Row(
                Modifier.fillMaxWidth().drawBehind { drawLine(k.hair, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BasicText(verseText(listOf(v), k.acc, 10f), Modifier.weight(1f), style = style(fs, font, color = k.tx, lineHeight = 1.5f))
                T(st.other.firstOrNull { it.n == v.n }?.text ?: "", fs, k.sub, Modifier.weight(1f), face = font, lineHeight = 1.5f)
            }
        }
    }
}

@Composable
private fun BoxScope.SelectionBar(ctx: android.content.Context) {
    val st = BibleState
    val key = st.key(st.sel)
    val t by oneShot(st.sel, 400)
    val v = st.verses.firstOrNull { it.n == st.sel } ?: return
    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp + (bottomInset() - 8.dp).coerceAtLeast(0.dp)).pop({ t }, Ease.O13)
            .cssShadow(0f, 20f, 40f, -14f, Color.Black.copy(alpha = .8f), RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp)).background(C.Sheet).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Highlights.forEach { (name, col) ->
            val on = Prefs.highlights[key] == name
            Box(
                Modifier.size(30.dp).drawBehind { if (on) { drawCircle(C.Ink, size.width / 2 + 4.dp.toPx()); drawCircle(C.Sheet, size.width / 2 + 2.dp.toPx()) } }
                    .clip(CircleShape).background(col.copy(alpha = .9f)).press({ Prefs.setHighlight(key, if (on) null else name) }, label = "Highlight $name")
            )
        }
        Box(Modifier.padding(horizontal = 4.dp).size(1.dp, 22.dp).background(white(.12f)))
        val saved = Prefs.isSaved("verse:$key")
        Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(white(.08f))
            .press({ Prefs.toggleSaved(Prefs.Saved(if (Prefs.highlights[key] != null) "hl" else "verse", v.text, "${st.ref}:${v.n}, ${st.version?.abbreviation}", "verse:$key")) }).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center) { T(if (saved) "Saved" else "Save", 12.5f, C.Ink, weight = 600) }
        RoundButton({ share(ctx, "${v.text}\n${st.ref}:${v.n}") }, "Share", size = 34f, bg = white(.08f)) { Ico(Icons.ShareSmall, 15f, C.Text) }
    }
}

@Composable
private fun BoxScope.ChapterDeck(haze: HazeState, k: Look) {
    val st = BibleState
    val shape = RoundedCornerShape(32.dp)
    val pct = animate(st.chapter / st.chapters.toFloat(), 500)
    Row(
        Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 24.dp + (bottomInset() - 8.dp).coerceAtLeast(0.dp)).fillMaxWidth().height(64.dp)
            .cssShadow(0f, 24f, 40f, -20f, Color.Black.copy(alpha = .8f), shape).clip(shape)
            .hazeEffect(haze, HazeStyle(k.bg, HazeTint(k.deck), 18.dp, noiseFactor = 0f)).topHighlight(white(.1f), shape).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.height(48.dp).graphicsLayer { alpha = if (st.chapter > 1) 1f else .35f }.press({ st.go(ch = st.chapter - 1) }, label = "Previous chapter").padding(start = 8.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { Ico(Icons.Back, 16f, C.Ink); S("${st.bookName} ${(st.chapter - 1).coerceAtLeast(1)}", 15f, C.Ink) }
        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(white(.12f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(pct).background(C.Gold))
        }
        Row(
            Modifier.height(48.dp).press({ st.go(ch = st.chapter + 1) }, label = "Next chapter").padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { S("${st.bookName} ${(st.chapter + 1).coerceAtMost(st.chapters)}", 15f, C.Ink); Ico(Icons.Forward, 16f, C.Ink) }
    }
}

@Composable
private fun BoxScope.BookSheet(haze: HazeState, k: Look, onDismiss: () -> Unit) {
    val st = BibleState
    var step by remember { mutableStateOf("books") }
    var book by remember { mutableStateOf(st.book) }
    var test by remember { mutableStateOf(if (Canon.isOld(st.book)) "ot" else "nt") }
    val books = st.version?.books.orEmpty().associateBy { it.usfm }
    Sheet(onDismiss, haze, k.sheet, k.line, base = k.bg) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step == "chapters") RoundButton({ step = "books" }, "Back to books", bg = k.chip) { Ico(Icons.Back, 16f, k.tx) }
            S(if (step == "books") "Books" else books[book]?.humanLong ?: books[book]?.human ?: book, 24f, k.tx, Modifier.weight(1f))
        }
        if (step == "books") {
            Segmented(listOf("ot" to "Old Testament", "nt" to "New Testament"), test, k, Modifier.padding(top = 14.dp), pillShadow = false) { test = it }
            (if (test == "ot") Canon.ot else Canon.nt).forEach { g ->
                Row(Modifier.padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(c(g.color))); T(g.name, 12.5f, k.sub)
                }
                g.books.filter { it in books }.chunked(4).forEach { row ->
                    Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { b ->
                            Box(
                                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(if (b == st.book) k.chip else Color.Transparent)
                                    .drawBehind { drawRect(c(g.color).copy(alpha = .53f), Offset(0f, size.height - 2.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())) }
                                    .press({ book = b; step = "chapters" }, label = books[b]?.human ?: b),
                                contentAlignment = Alignment.Center,
                            ) { S(Canon.short(b), 15f, k.tx) }
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        } else {
            val n = books[book]?.chapters?.count { it.canonical } ?: 1
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..n).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            val cur = book == st.book && c == st.chapter
                            val t by oneShot(book, 400, (c - 1) * 15)
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).pop({ t }, Ease.O13).clip(CircleShape).background(if (cur) k.acc else k.chip)
                                    .press({ st.go(book, c); onDismiss() }),
                                contentAlignment = Alignment.Center,
                            ) { S("$c", 17f, if (cur) k.onAcc else k.tx) }
                        }
                        repeat(6 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.VersionSheet(haze: HazeState, k: Look, onDismiss: () -> Unit) {
    val st = BibleState
    val scope = rememberCoroutineScope()
    Sheet(onDismiss, haze, k.sheet, k.line, base = k.bg) {
        S("Version", 24f, k.tx)
        T("Parallel reading puts your second choice beside it.", 13f, k.sub, Modifier.padding(top = 4.dp, bottom = 14.dp))
        var i = 0
        // the store's versions under their language, each one download
        BibleRepo.versions.groupBy { it.language.localName.ifBlank { it.language.name } }.forEach { (lang, list) ->
            T(lang.uppercase(), 11f, k.sub, Modifier.padding(start = 4.dp, top = 6.dp, bottom = 8.dp), weight = 600, tracking = 1.2f)
            list.forEach { v -> VersionRow(v, i++, k, { scope.launch { BibleRepo.download(v) } }) { st.pickVersion(v); onDismiss() } }
        }
        if (BibleRepo.versions.isEmpty()) T("The versions come from the internet the first time; check the connection.", 13f, k.sub)
    }
}

/**
 * A version as the board draws it (cover, name, meta, tag), with what the store adds: the gold fill while it downloads,
 * Get it, Update, Remove and Try again.
 */
@Composable
internal fun VersionRow(v: IBibleVersion, i: Int, k: Look, onGet: () -> Unit, tv: Boolean = false, onPick: () -> Unit) {
    val st = BibleState
    val sel = v.id == st.version?.id
    val dl = BibleRepo.downloading[v.id]
    val here = BibleRepo.offline(v)
    val lang = v.language.localName.ifBlank { v.language.name }
    val mb = BibleRepo.size(v).let { if (it > 0) " · ${maxOf(1, Math.round(it / 1e6))} MB" else "" }
    val (tag, action) = when {
        dl != null -> "${Math.round(dl * 100)}%" to null
        sel -> "Reading" to null
        v.id == st.otherVersion?.id -> "Beside it" to null
        BibleRepo.failed[v.id] == true -> "Try again" to onGet
        !here -> "Get it" to onGet
        BibleRepo.hasUpdate(v) -> "Update" to onGet
        else -> "Remove" to { BibleRepo.remove(v) }
    }
    val meta = when {
        BibleRepo.failed[v.id] == true -> "$lang · Couldn't download"
        here && BibleRepo.hasUpdate(v) -> "$lang · A newer edition$mb"
        here -> "$lang · On this ${if (tv) "TV" else "phone"}"
        else -> "$lang$mb"
    }
    val fill = animate(dl ?: 0f, 400, Ease.Linear)
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.padding(bottom = if (tv) 8.dp else 6.dp).fillMaxWidth().choiceOrFocus(tv, shape, { if (here) onPick() else if (dl == null) onGet() }, Role.RadioButton)
            .clip(shape).background(if (sel) k.chip else Color.Transparent)
            .drawBehind { if (dl != null) drawRect(gold(.35f), size = size.copy(width = size.width * fill)) }
            .padding(horizontal = if (tv) 18.dp else 16.dp, vertical = if (tv) 14.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (tv) 16.dp else 14.dp),
    ) {
        // the boards' covers: 44 px square on the phone, 52 x 60 on the TV
        val stripe = if (tv) 5 else 4
        Box(
            Modifier.size(if (tv) 52.dp else 44.dp, if (tv) 60.dp else 44.dp).clip(if (tv) corners(14f, 4f, 4f, 14f) else corners(12f, 4f, 4f, 12f)).background(coverOf(i))
                .drawBehind { drawRect(Color.Black.copy(alpha = .25f), Offset(size.width - stripe.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(stripe.dp.toPx(), size.height)) },
            contentAlignment = Alignment.Center,
        ) { S(v.abbreviation, if (tv) 14f else 12f, C.Ink, maxLines = 1) }
        Column(Modifier.weight(1f)) {
            S(v.localTitle.ifBlank { v.title }, if (tv) 21f else 17f, k.tx, maxLines = 2)
            T(meta, if (tv) 14f else 12f, if (BibleRepo.failed[v.id] == true) c(0xFFF28B8B) else k.sub)
        }
        // an action is its own target (the row reads the version)
        val tint = if (tag == "Remove") k.sub else k.acc
        if (action != null) Box(Modifier.choiceOrFocus(tv, RoundedCornerShape(14.dp), action).padding(horizontal = 8.dp, vertical = 6.dp)) { T(tag, if (tv) 14f else 12f, tint, weight = 600) }
        else T(tag, if (tv) 14f else 12f, tint, weight = 600)
    }
}

