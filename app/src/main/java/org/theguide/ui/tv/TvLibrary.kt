package org.theguide.ui.tv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import design.Palette
import org.theguide.data.Prefs
import org.theguide.data.Setup
import org.theguide.ui.*
import org.theguide.ui.phone.LampField
import org.theguide.ui.phone.heat
import org.theguide.ui.phone.kindColor
import org.theguide.ui.phone.latestLetter
import org.theguide.ui.phone.savedKinds
import org.theguide.ui.phone.sparkPath
import org.theguide.ui.phone.stats
import org.theguide.ui.phone.whenText
import parsers.bt.Letter
import parsers.bt.Letters
import java.time.format.DateTimeFormatter
import java.util.Locale

/** TVLibrary board. */
@Composable
fun TvLibrary() {
    var tab by remember { mutableStateOf("week") }
    var flip by remember { mutableIntStateOf(0) }
    val screen by oneShot(Unit, 600)
    val swapA by oneShot(flip, 550)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val letters by Letters.book(Prefs.letterLang).all.collectAsState()
    val tabs = listOf(Triple("week", "This week", ""), Triple("letters", "Letters", "${letters.size}"), Triple("activity", "Activity", ""), Triple("saved", "Saved", "${Prefs.saved.size}"))

    Box(Modifier.fillMaxSize().background(C.Backdrop).ambient(gold(.12f), .6f, .3f)) {
        Box(Modifier.fillMaxSize().enter({ screen })) {
            Column(Modifier.padding(start = 130.dp, top = 44.dp, bottom = 40.dp).width(220.dp)) {
                S("Library", 44f, c(Palette.Title), Modifier.padding(bottom = 24.dp), weight = 300, tracking = -.6f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEachIndexed { i, (id, label, count) ->
                        val on = tab == id
                        Row(
                            Modifier.fillMaxWidth().height(54.dp).tvFocus(RoundedCornerShape(27.dp), { tab = id; flip++ }, Role.Tab).clip(RoundedCornerShape(27.dp)).background(if (on) gold(.16f) else Color.Transparent)
                                .then(if (i == 0) Modifier.focusRequester(first) else Modifier).padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                        ) { T(label, 18f, if (on) C.Gold else C.Soft, weight = 600); T(count, 14f, if (on) C.Gold else C.Faint) }
                    }
                }
            }
            Box(Modifier.padding(start = 400.dp, end = 60.dp, top = 44.dp, bottom = 40.dp).fillMaxSize().enter({ swapA }, Ease.Swap)) {
                when (tab) {
                    "week" -> Week()
                    "letters" -> LettersTab()
                    "activity" -> Activity()
                    else -> SavedTab()
                }
            }
        }
    }
}

@Composable
private fun Week() {
    val (l, startLang) = latestLetter() ?: run { T("This week’s letter arrives on Saturday.", 20f, C.Sub); return }
    var lang by remember { mutableStateOf(startLang) }
    val shown = (if (lang == startLang) l else letterIn(l, lang)) ?: l
    val t by oneShot(Unit, 800)
    Box(Modifier.fillMaxSize().unfold({ t }, Ease.Swap).clip(tvTorn).background(C.Paper).grain(.35f)) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 44.dp).graphicsLayer { rotationZ = -8f }) { org.theguide.ui.phone.WaxSeal(74f, 24f) }
        Column(Modifier.fillMaxSize().focusable().verticalScroll(rememberScrollState()).padding(horizontal = 50.dp, vertical = 40.dp)) { LetterBody(shown, lang, { lang = it }, LetterSpecs.TvWeek, tv = true) }
    }
}

@Composable
private fun LettersTab() {
    var lang by remember { mutableStateOf(Prefs.letterLang) }
    val letters by Letters.book(lang).all.collectAsState()
    LaunchedEffect(lang) { Letters.book(lang).sync() }
    var sel by remember { mutableIntStateOf(0) }
    val l: Letter? = letters.getOrNull(sel)
    var llang by remember(sel, lang) { mutableStateOf(lang) }
    val swap by oneShot(sel to lang, 700)
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.width(360.dp).padding(top = 4.dp)) {
            Row(Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Prefs.letterLangs.take(4).forEach { code ->
                    val on = code == lang
                    Box(Modifier.height(42.dp).tvFocus(RoundedCornerShape(21.dp), { lang = code; sel = 0 }, Role.RadioButton).clip(RoundedCornerShape(21.dp)).background(if (on) C.Gold else white(.06f)).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        T(Setup.letterLanguages.firstOrNull { it.first == code }?.second ?: code, 15f, if (on) C.OnGold else C.Soft, weight = 600)
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy((-14).dp)) {
                letters.take(40).forEachIndexed { i, x ->
                    val shift = animate(if (i == sel) 24f else 0f, 350, Ease.O12)
                    Box(
                        Modifier.zIndex(i.toFloat()).graphicsLayer { translationX = shift * density; rotationZ = if (i % 2 == 1) .7f else -.6f }.fillMaxWidth().height(78.dp)
                            .cssShadow(0f, -8f, 20f, -10f, Color.Black.copy(alpha = .7f), RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(c(Palette.LetterPapers[i % 4]))
                            .drawBehind {
                                val p = Path().apply { moveTo(0f, 0f); lineTo(size.width * 328 / 360, size.height * 36 / 74); lineTo(size.width, 0f) }
                                drawPath(p, Color(42, 36, 24).copy(alpha = .18f), style = Stroke(1.2.dp.toPx()))
                            }
                            .tvFocus(RoundedCornerShape(6.dp), { sel = i; Prefs.markLetterRead(x.id) })
                    ) {
                        Box(Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 20.dp)) { org.theguide.ui.phone.WaxSeal(24f, .1f, shadow = false) }
                        S(x.salutation ?: x.title, 16f, C.PaperInk, Modifier.padding(start = 18.dp, top = 10.dp).fillMaxWidth(.7f), maxLines = 1, ellipsis = true)
                        T("${x.code} ${x.title}", 12.5f, C.PaperMuted, Modifier.padding(start = 18.dp, top = 34.dp).fillMaxWidth(.8f), maxLines = 1, ellipsis = true)
                        T(x.letterDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)), 13f, C.PaperMuted, Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp))
                    }
                }
            }
        }
        if (l != null) {
            val shown = (if (llang == lang) l else letterIn(l, llang)) ?: l
            Box(
                Modifier.weight(1f).fillMaxHeight().then(if (sel % 2 == 1) Modifier.unfold({ swap }, Ease.Swap) else Modifier.enter({ swap }, Ease.Swap))
                    .cssShadow(0f, 40f, 70f, -30f, Color.Black.copy(alpha = .9f), RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(C.Paper).grain(.35f)
            ) { Column(Modifier.fillMaxSize().focusable().verticalScroll(rememberScrollState()).padding(horizontal = 44.dp, vertical = 36.dp)) { LetterBody(shown, llang, { llang = it }, LetterSpecs.TvLetters, tv = true) } }
        }
    }
}

@Composable
private fun Activity() {
    val s = remember(Prefs.activity) { stats() }
    val h = remember(Prefs.activity, Prefs.readDays) { heat(364) }
    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            listOf(Triple("${s.hours}", "hours listened", C.Gold), Triple("${s.sermons}", "sermons heard", C.English), Triple("${s.chapters}", "chapters read", C.CoralSoft)).forEachIndexed { i, (n, label, col) ->
                Column(Modifier.weight(1f)) {
                    S(n, 70f, col, weight = 300, lineHeight = 1f)
                    T(label, 16f, C.Sub, Modifier.padding(top = 6.dp, bottom = 10.dp))
                    Canvas(Modifier.fillMaxWidth().height(34.dp)) { drawPath(sparkPath(s.weekly[i], size.width, size.height), col, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 34.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { T("Lamps lit this year", 16f, C.Sub); T("${h.count { it > 0 }} days", 16f, C.Sub) }
        LampField(h, 52, 4f, 6f)
        Row(Modifier.padding(top = 34.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Prefs.activity.take(4).forEach { a ->
                Column(Modifier.weight(1f).drawBehind { drawRect(kindColor[a.kind] ?: C.Gold, size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())) }.padding(top = 12.dp)) {
                    S(a.what, 19f, c(Palette.Title), maxLines = 2); T(whenText(a.at, a.minutes), 14f, C.Meta, Modifier.padding(top = 4.dp))
                }
            }
            repeat((4 - Prefs.activity.size).coerceAtLeast(0)) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SavedTab() {
    var filter by remember { mutableStateOf("all") }
    val shown = Prefs.saved.filter { filter == "all" || it.kind == filter }
    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "quote" to "Quotes", "verse" to "Verses", "hl" to "Highlights").forEach { (id, label) ->
                Box(Modifier.height(46.dp).tvFocus(RoundedCornerShape(23.dp), { filter = id }).clip(RoundedCornerShape(23.dp)).background(if (filter == id) C.Gold else white(.06f)).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    T(label, 16f, if (filter == id) C.OnGold else C.Soft, weight = 600)
                }
            }
        }
        Row(Modifier.padding(top = 24.dp).focusable().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            listOf(shown.filterIndexed { i, _ -> i % 2 == 0 }, shown.filterIndexed { i, _ -> i % 2 == 1 }).forEach { col ->
                Column(Modifier.weight(1f)) {
                    col.forEachIndexed { j, s ->
                        val (kind, cc, hl) = savedKinds[s.kind] ?: savedKinds.getValue("quote")
                        val t by oneShot(filter to j, 450, j * 50)
                        Row(Modifier.rise({ t }, Ease.Swap).padding(bottom = 26.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Box(Modifier.width(4.dp).fillMaxHeight().cssShadow(0f, 0f, 14f, 0f, cc, RoundedCornerShape(2.dp)).clip(RoundedCornerShape(2.dp)).background(cc))
                            Column {
                                S(s.text, 25f, c(Palette.Title), Modifier.background(hl, RoundedCornerShape(4.dp)).padding(horizontal = 3.dp), weight = 300, lineHeight = 1.4f)
                                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { T(kind, 15f, cc, weight = 600); T(s.source, 15f, C.Meta) }
                            }
                        }
                    }
                }
            }
        }
        if (shown.isEmpty()) S("Nothing here yet. Save a quote or a verse and it stays here.", 22f, C.Meta, italic = true, weight = 300)
    }
}

