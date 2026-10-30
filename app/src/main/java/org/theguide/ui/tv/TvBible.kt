package org.theguide.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import design.Palette
import kotlinx.coroutines.launch
import org.theguide.audio.BibleAudio
import org.theguide.data.BibleRepo
import org.theguide.data.Canon
import org.theguide.data.Verse
import org.theguide.ui.*
import org.theguide.ui.phone.VersionRow
import org.theguide.ui.phone.coverOf
import org.theguide.ui.phone.verseText

/** TVBible board. */
@Composable
fun TvBible() {
    val st = BibleState
    LaunchedEffect(st.versionId, st.otherId, st.book, st.chapter) { st.load() }
    LaunchedEffect(BibleAudio.playing) { if (BibleAudio.playing) BibleAudio.follow() }
    val ls = rememberLook("tv-bible", 28, "airy")
    val k = ls.look
    var sheet by remember { mutableStateOf("") }
    val screen by oneShot(Unit, 600)
    val body by oneShot(st.flip, 600)
    val font = if (ls.serif) Face.Serif else Face.Sans
    val modes = listOf("scroll" to "Scroll", "pages" to "Pages", "verse" to "Verse", "parallel" to "Parallel")
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().background(k.bg).ambient(k.glow, .5f, .2f)) {
        Box(Modifier.fillMaxSize().enter({ screen })) {
            Row(Modifier.padding(start = 120.dp, end = 60.dp, top = 36.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.tvFocus(RoundedCornerShape(20.dp), { sheet = "pick" }).clip(RoundedCornerShape(20.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    S(st.ref, 44f, k.tx, weight = 300, tracking = -.6f); Ico(Icons.Down, 16f, k.tx)
                }
                Box(Modifier.weight(1f))
                Box(Modifier.width(448.dp).height(50.dp).clip(RoundedCornerShape(25.dp)).background(k.chip).padding(4.dp)) {
                    val x = animate(modes.indexOfFirst { it.first == st.mode }.toFloat(), 450, Ease.O115)
                    Box(Modifier.offset(x = (110 * x).dp).size(110.dp, 42.dp).clip(RoundedCornerShape(21.dp)).background(k.pill))
                    Row {
                        modes.forEachIndexed { i, (id, label) ->
                            val col by animateColorAsState(if (st.mode == id) k.tx else k.sub, tw(300), label = "m")
                            Box(Modifier.size(110.dp, 42.dp).tvFocus(RoundedCornerShape(21.dp), { st.chooseMode(id) }, Role.Tab).then(if (i == 0) Modifier.focusRequester(first) else Modifier), contentAlignment = Alignment.Center) { T(label, 15f, col, weight = 600) }
                        }
                    }
                }
                TvChip(st.version?.abbreviation ?: "", { sheet = "ver" }, k)
                st.audio?.let { a -> TvChip(if (BibleAudio.playing && BibleAudio.url == BibleAudio.mp3(a)) "❚❚  Pause" else "▶  Listen", { BibleAudio.toggle(a) }, k) }
                TvChip("Aa", { sheet = "set" }, k, serif = true)
            }
            Box(Modifier.padding(start = 130.dp, end = 70.dp, top = 120.dp, bottom = 100.dp).fillMaxSize().enter({ body }, Ease.Swap)) {
                when (st.mode) {
                    "scroll" -> {
                        // two columns, verses shared out by length, scrolled with the remote
                        val half = remember(st.verses) { var n = 0; val total = st.verses.sumOf { it.text.length }; st.verses.indexOfFirst { n += it.text.length; n > total / 2 } + 1 }
                        Row(Modifier.fillMaxSize().focusable().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(60.dp)) {
                            listOf(st.verses.take(half), st.verses.drop(half)).forEach { part ->
                                VerseFlow(part, style(ls.size.toFloat(), font, color = k.tx, lineHeight = ls.lineHeight), k.acc, 13f, null, -1, { null }, Modifier.weight(1f),
                                    lit = BibleAudio.lit(st.audio), litColors = listOf(k.litStart, k.litEnd))
                            }
                        }
                    }
                    "pages" -> Spread(k, ls, font)
                    "verse" -> {
                        val v = st.verses.getOrNull(st.verse)
                        val vs by oneShot(st.verse, 550)
                        Column(Modifier.fillMaxSize().padding(horizontal = 60.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            S("${v?.n ?: ""}", 110f, k.acc.copy(alpha = .25f), Modifier.enter({ vs }), weight = 300, lineHeight = .9f)
                            T(v?.text ?: "", Math.round(ls.size * 1.45f).toFloat(), k.tx, Modifier.padding(top = 14.dp).widthIn(max = 900.dp).enter({ vs }, Ease.Swap), face = font, weight = 300, lineHeight = 1.32f, tracking = -.4f, align = TextAlign.Center)
                            Row(Modifier.padding(top = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                TvChip("‹", { st.verse = (st.verse - 1).coerceAtLeast(0) }, k, Modifier.width(54.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val from = (st.verse - 5).coerceAtLeast(0); val to = (from + 11).coerceAtMost(st.verses.size)
                                    (from until to).forEach { i -> val w = animate(if (i == st.verse) 26f else 8f, 350); Box(Modifier.size(w.dp, 8.dp).clip(RoundedCornerShape(4.dp)).background(if (i == st.verse) k.acc else k.line)) }
                                }
                                TvChip("›", { st.verse = (st.verse + 1).coerceAtMost(st.verses.lastIndex) }, k, Modifier.width(54.dp))
                            }
                        }
                    }
                    else -> Column(Modifier.fillMaxSize().focusable().verticalScroll(rememberScrollState())) {
                        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(50.dp)) { T(st.version?.title ?: "", 15f, k.sub, Modifier.weight(1f)); T(st.otherVersion?.title ?: "", 15f, k.sub, Modifier.weight(1f)) }
                        val fs = Math.round(ls.size * .6f).toFloat()
                        st.verses.forEach { v ->
                            Row(Modifier.fillMaxWidth().drawBehind { drawLine(k.hair, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(50.dp)) {
                                BasicText(verseText(listOf(v), k.acc, 11f), Modifier.weight(1f), style = style(fs, font, color = k.tx, lineHeight = 1.45f))
                                T(st.other.firstOrNull { it.n == v.n }?.text ?: "", fs, k.sub, Modifier.weight(1f), face = font, lineHeight = 1.45f)
                            }
                        }
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomStart).padding(start = 130.dp, end = 70.dp, bottom = 30.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.graphicsLayer { alpha = if (st.chapter > 1) 1f else .35f }) { TvChip("‹ ${st.bookName} ${(st.chapter - 1).coerceAtLeast(1)}", { st.go(ch = st.chapter - 1) }, k, serif = true) }
                val pct = animate(st.chapter / st.chapters.toFloat(), 500)
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(k.line)) { Box(Modifier.fillMaxHeight().fillMaxWidth(pct).clip(RoundedCornerShape(2.dp)).background(k.acc)) }
                TvChip("${st.bookName} ${(st.chapter + 1).coerceAtMost(st.chapters)} ›", { st.go(ch = st.chapter + 1) }, k, serif = true)
            }
        }
        when (sheet) {
            "pick" -> BookScreen { sheet = "" }
            "ver" -> TvPanel(k.sheet, { sheet = "" }) { Versions(k) { sheet = "" } }
            "set" -> TvPanel(k.sheet, { sheet = "" }, 500f) { ReadingSettings(ls, 22..40, tv = true) }
        }
    }
}

/** Pages: a real two-page spread; the corner of the right page turns to the next spread. */
@Composable
private fun Spread(k: Look, ls: LookState, font: Face) {
    val st = BibleState
    val measurer = rememberTextMeasurer()
    val d = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageW = with(d) { ((maxWidth - 168.dp) / 2).roundToPx() }
        val pageH = with(d) { (maxHeight - 74.dp).roundToPx() }
        val fs = Math.round(ls.size * .72f).toFloat()
        val txt = style(fs, font, color = C.PaperInk, lineHeight = 1.6f)
        val pages = remember(st.verses, fs, ls.face, pageW, pageH) {
            val out = mutableListOf<List<Verse>>(); var cur = mutableListOf<Verse>()
            st.verses.forEach { v ->
                val trial = cur + v
                if (measurer.measure(verseText(trial, C.Wax, 11f), txt, constraints = Constraints(maxWidth = pageW)).size.height > pageH && cur.isNotEmpty()) { out += cur; cur = mutableListOf(v) } else cur = trial.toMutableList()
            }
            if (cur.isNotEmpty()) out += cur
            out.ifEmpty { listOf(emptyList()) }
        }
        val spread = (st.page * 2).coerceIn(0, ((pages.size - 1) / 2) * 2)
        val turn by oneShot(spread to st.flip, 800)
        Row(Modifier.fillMaxSize().cssShadow(0f, 40f, 50f, 0f, Color.Black.copy(alpha = .55f), RoundedCornerShape(14.dp))) {
            listOf(spread, spread + 1).forEachIndexed { side, i ->
                val left = side == 0
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .then(if (!left) Modifier.graphicsLayer { cameraDistance = perspective(1800f) * density }.then(if (st.flip % 2 == 1) Modifier.unfold({ turn }, Ease.Swap, TransformOrigin(0f, .5f)) else Modifier.enter({ turn }, Ease.Swap)) else Modifier)
                        .clip(if (left) corners(14f, 4f, 4f, 14f) else corners(4f, 14f, 14f, 4f))
                        .background(if (left) Brush.horizontalGradient(.85f to k.pageBg, 1f to k.curl) else Brush.horizontalGradient(0f to k.curl, .15f to k.pageBg)).grain(.25f)
                        .padding(start = if (left) 44.dp else 40.dp, end = if (left) 40.dp else 44.dp, top = 34.dp, bottom = 40.dp)
                ) {
                    pages.getOrNull(i)?.let { BasicText(verseText(it, C.Wax, 11f), style = txt) }
                    T("${i + 1}", 14f, C.PaperInk.copy(alpha = .55f), Modifier.align(Alignment.BottomCenter).offset(y = 26.dp), face = Face.Serif, italic = true)
                    if (!left) Box(
                        Modifier.align(Alignment.BottomEnd).offset(44.dp, 40.dp).size(80.dp).clip(corners(0f, 0f, 14f, 0f))
                            .drawBehind {
                                val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                                drawPath(p, Brush.linearGradient(0f to k.curl, .48f to k.pageBg, start = Offset(size.width / 2, size.height / 2), end = Offset(size.width, size.height)))
                            }
                            .tvFocus(corners(0f, 0f, 14f, 0f), { if (spread + 2 < pages.size) st.page = st.page + 1 else st.go(ch = st.chapter + 1) }).label("Turn the page")
                    )
                }
            }
        }
    }
}

@Composable
private fun BookScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val st = BibleState
    var test by remember { mutableStateOf(if (Canon.isOld(st.book)) "ot" else "nt") }
    var book by remember { mutableStateOf(st.book) }
    val books = st.version?.books.orEmpty().associateBy { it.usfm }
    val t by oneShot(Unit, 400)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Row(Modifier.fillMaxSize().fade({ t }).background(c(Palette.TvOverlayStrong)).padding(start = 130.dp, end = 70.dp, top = 50.dp, bottom = 40.dp), horizontalArrangement = Arrangement.spacedBy(50.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ot" to "Old Testament", "nt" to "New Testament").forEach { (id, label) ->
                    Box(Modifier.height(48.dp).tvFocus(RoundedCornerShape(24.dp), { test = id }).clip(RoundedCornerShape(24.dp)).background(if (test == id) C.Gold else white(.06f)).padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                        T(label, 16f, if (test == id) C.OnGold else C.Soft, weight = 600)
                    }
                }
            }
            var firstSet = false
            (if (test == "ot") Canon.ot else Canon.nt).forEach { g ->
                Row(Modifier.padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(c(g.color))); T(g.name, 14f, C.Sub)
                }
                g.books.filter { it in books }.chunked(8).forEach { row ->
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { b ->
                            val fr = if (!firstSet) { firstSet = true; Modifier.focusRequester(first) } else Modifier
                            Box(
                                Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (b == book) gold(.16f) else white(.05f))
                                    .drawBehind { drawRect(c(g.color).copy(alpha = .6f), Offset(0f, size.height - 3.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())) }
                                    .tvFocus(RoundedCornerShape(16.dp), { book = b }).then(fr).label(books[b]?.human ?: b),
                                contentAlignment = Alignment.Center,
                            ) { S(Canon.short(b), 17f, c(Palette.Title)) }
                        }
                        repeat(8 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Column(Modifier.width(420.dp).fillMaxHeight().drawBehind { drawLine(white(.08f), Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx()) }.padding(start = 44.dp).verticalScroll(rememberScrollState())) {
            val info = books[book]
            S(info?.let { if (it.human.length > 3) it.human else it.humanLong.lowercase().replaceFirstChar { c -> c.uppercase() } } ?: book, 44f, c(Palette.Title), weight = 300)
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (1..(info?.chapters?.count { it.canonical } ?: 1)).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { c ->
                            val cur = book == st.book && c == st.chapter
                            val pt by oneShot(book, 400, (c - 1) * 15)
                            Box(Modifier.weight(1f).aspectRatio(1f).pop({ pt }, Ease.O13).tvFocus(CircleShape, { st.go(book, c); onClose() }).clip(CircleShape).background(if (cur) C.Gold else white(.06f))
                                , contentAlignment = Alignment.Center) { S("$c", 19f, if (cur) C.OnGold else c(Palette.Title)) }
                        }
                        repeat(6 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Versions(k: Look, onDone: () -> Unit) {
    val st = BibleState
    val scope = rememberCoroutineScope()
    S("Version", 32f, k.tx)
    T("Parallel puts your second choice beside it.", 15f, k.sub, Modifier.padding(top = 6.dp, bottom = 16.dp))
    var i = 0
    BibleRepo.versions.groupBy { it.language.localName.ifBlank { it.language.name } }.forEach { (lang, list) ->
        T(lang.uppercase(), 13f, k.sub, Modifier.padding(start = 4.dp, top = 6.dp, bottom = 10.dp), weight = 600, tracking = 1.4f)
        list.forEach { v -> VersionRow(v, i++, k, { scope.launch { BibleRepo.download(v) } }, tv = true) { st.pickVersion(v); onDone() } }
    }
    if (BibleRepo.versions.isEmpty()) T("The versions come from the internet the first time; check the connection.", 15f, k.sub)
}
