package org.theguide.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import design.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.theguide.data.Prefs
import org.theguide.data.Setup
import org.theguide.ui.*
import parsers.bt.Letter
import parsers.bt.Letters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Kind of saved item → (label, colour, highlight behind the words). */
internal val savedKinds = mapOf(
    "quote" to Triple("Quote", C.Gold, Color.Transparent),
    "verse" to Triple("Verse", C.English, Color.Transparent),
    "hl" to Triple("Highlight", C.CoralSoft, Color(255, 122, 102).copy(alpha = .22f)),
)

/** Activity in numbers: hours listened, sermons listened to, chapters read; and per-week series for the sparklines. */
internal data class Stats(val hours: Int, val sermons: Int, val chapters: Int, val weekly: List<List<Int>>)

internal fun stats(): Stats {
    val a = Prefs.activity
    val now = System.currentTimeMillis()
    fun week(at: Long) = ((now - at) / (7L * 86_400_000)).toInt()
    fun series(f: (Prefs.Activity) -> Int) = (11 downTo 0).map { w -> a.filter { week(it.at) == w }.sumOf(f) }
    return Stats(
        a.filter { it.kind == "listen" }.sumOf { it.minutes } / 60,
        a.filter { it.kind == "listen" }.map { it.what.substringAfter("Listened to ").substringBefore(",") }.distinct().size,
        a.filter { it.kind == "bible" }.map { it.what }.distinct().size,
        listOf(series { if (it.kind == "listen") it.minutes else 0 }, series { if (it.kind == "listen") 1 else 0 }, series { if (it.kind == "bible") 1 else 0 }),
    )
}

/** How bright each of the last [days] days is: 0 none, 1–3 by how much was read or heard. */
internal fun heat(days: Int): List<Int> {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val per = Prefs.activity.groupingBy { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }.eachCount()
    return (days - 1 downTo 0).map { back ->
        val d = today.minusDays(back.toLong())
        val n = per[d] ?: if (d.toEpochDay() in Prefs.readDays) 1 else 0
        when { n == 0 -> 0; n == 1 -> 1; n <= 3 -> 2; else -> 3 }
    }
}
internal val heatColors = listOf(white(.07f), gold(.35f), gold(.65f), C.Gold)

/** "Tonight", "This morning", "Saturday", "Oct 12". */
internal fun whenText(at: Long, minutes: Int): String {
    val t = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
    val days = LocalDate.now().toEpochDay() - t.toLocalDate().toEpochDay()
    val day = when {
        days == 0L -> if (t.hour >= 17) "Tonight" else if (t.hour < 12) "This morning" else "This afternoon"
        days == 1L -> "Yesterday"
        days < 7 -> t.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        else -> t.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
    }
    return if (minutes > 0) "$day, ${minutesText(minutes)}" else day
}
internal val kindColor = mapOf("listen" to C.Gold, "read" to C.Gold, "bible" to C.English, "live" to C.Coral, "song" to C.Gold)

/** The sparkline of the boards: 12 points across 100×24. */
internal fun sparkPath(values: List<Int>, w: Float, h: Float): Path {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    return Path().apply { values.forEachIndexed { i, v -> val x = i * w / 11f; val y = h * (20 - 16 * v / max.toFloat()) / 24f; if (i == 0) moveTo(x, y) else lineTo(x, y) } }
}

private val tornPage = GenericShape { s, _ ->
    val pts = listOf(0f to 2f, 6f to 0f, 14f to 1.2f, 25f to .2f, 37f to 1.4f, 50f to .3f, 63f to 1.1f, 76f to 0f, 88f to 1.3f, 100f to .4f,
        100f to 98.6f, 92f to 100f, 80f to 98.9f, 66f to 99.8f, 52f to 98.7f, 38f to 99.9f, 24f to 98.8f, 11f to 99.7f, 0f to 98.5f)
    // the board's tear is a share of a ~900 px page; keep that depth however long the letter is
    val unit = minOf(s.height, 900f * (s.width / 350f)) / 100f
    pts.forEachIndexed { i, (x, y) -> val px = s.width * x / 100; val py = if (y < 50) y * unit else s.height - (100 - y) * unit; if (i == 0) moveTo(px, py) else lineTo(px, py) }; close()
}

/** PhoneLibrary board. */
@Composable
fun PhoneLibrary(haze: HazeState) {
    var tab by remember { mutableStateOf(Nav.libraryTab) }
    var flip by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf<Pair<Letter, String>?>(null) }
    val swapA by oneShot(flip, 500)
    val screen by oneShot(Unit, 500)
    LaunchedEffect(Unit) { Nav.libraryTab = "week" }
    val tabs = listOf("week" to "This week", "letters" to "Letters", "activity" to "Activity", "saved" to "Saved")

    Box(Modifier.fillMaxSize().background(C.Night).screenGlow(gold(.12f))) {
        Column(Modifier.fillMaxSize().hazeSource(haze).padding(top = statusTop()).verticalScroll(rememberScrollState()).enter({ screen })) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp)) {
                S("Library", 32f, C.Ink, weight = 300, tracking = -.4f)
                JoinPill()
                Segmented(tabs, tab, Look(design.ReadingThemes.Night, design.Tones.Lamp), Modifier.padding(top = 14.dp), font = 12.5f,
                    pillShadow = false, bg = white(.06f), pill = white(.14f), on = C.Ink, off = C.Sub) { tab = it; flip++ }
                Column(Modifier.enter({ swapA }, Ease.Swap)) {
                    when (tab) {
                        "week" -> latestLetter()?.let { (l, lang) -> WeekLetter(l, lang) } ?: T("This week’s letter arrives on Saturday.", 14f, C.Sub, Modifier.padding(top = 22.dp))
                        "letters" -> LetterStacks { l, lang -> open = l to lang }
                        "activity" -> Activity()
                        else -> Saved()
                    }
                }
            }
        }
        open?.let { (l, lang) -> LetterDialog(l, lang) { open = null } }
    }
}

@Composable
private fun WeekLetter(l: Letter, startLang: String) {
    var lang by remember { mutableStateOf(startLang) }
    val t by oneShot(Unit, 800)
    val shown = (if (lang == startLang) l else letterIn(l, lang)) ?: l
    Box(Modifier.padding(top = 22.dp).fillMaxWidth().unfold({ t }, Ease.Swap).clip(tornPage).background(C.Paper).grain(.35f).padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 26.dp)) {
        WaxSeal(54f, 18f, Modifier.align(Alignment.TopEnd).offset(6.dp, (-14).dp).graphicsLayer { rotationZ = -8f })
        LetterBody(shown, lang, { lang = it }, LetterSpecs.PhoneLibrary)
    }
}

@Composable
private fun LetterStacks(onOpen: (Letter, String) -> Unit) {
    var lang by remember { mutableStateOf(Prefs.letterLang) }
    val letters by Letters.book(lang).all.collectAsState()
    LaunchedEffect(lang) { Letters.book(lang).sync() }
    Row(Modifier.padding(top = 18.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Prefs.letterLangs.forEach { code -> Chip(Setup.letterLanguages.firstOrNull { it.first == code }?.second ?: code, code == lang, height = 34f, size = 12.5f) { lang = code } }
    }
    var n = 0
    letters.take(60).groupBy { it.letterDate.withDayOfMonth(1) }.forEach { (month, list) ->
        S("${monthName(month)}${if (month.year != LocalDate.now().year) " ${month.year}" else ""}", 20f, C.Ink, Modifier.padding(top = 22.dp, bottom = 10.dp))
        // envelopes overlap by 30 px, each a little tilted, newest on top of the one before
        Column(verticalArrangement = Arrangement.spacedBy((-30).dp)) { list.forEachIndexed { j, l ->
            val idx = n++
            val t by oneShot(lang to idx, 500, idx * 50)
            Box(
                Modifier.zIndex(j + 1f).rise({ t }, Ease.Swap)
                    .graphicsLayer { rotationZ = if (j % 2 == 1) .6f else -.5f }.fillMaxWidth().height(84.dp)
                    .cssShadow(0f, -6f, 18f, -8f, Color.Black.copy(alpha = .6f), RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp))
                    .background(c(Palette.LetterPapers[idx % 4]))
                    .drawBehind {
                        val p = Path().apply { moveTo(0f, 0f); lineTo(size.width * 316 / 350, size.height * 34 / 84); lineTo(size.width, 0f) }
                        drawPath(p, Color(42, 36, 24).copy(alpha = .18f), style = Stroke(1.2.dp.toPx()))
                    }
                    .press({ onOpen(l, lang); Prefs.markLetterRead(l.id) })
            ) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 22.dp)) { WaxSeal(24f, 0.1f) }
                S(l.salutation ?: l.title, 15f, C.PaperInk, Modifier.padding(start = 16.dp, top = 12.dp).fillMaxWidth(.72f), maxLines = 1, ellipsis = true)
                T("${l.code} ${l.title}", 11.5f, C.PaperMuted, Modifier.padding(start = 16.dp, top = 33.dp))
                T(l.letterDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)), 11.5f, C.PaperMuted, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 16.dp))
                if (!Prefs.isLetterRead(l.id) && idx == 0) T("Unread", 11.5f, C.Wax, Modifier.align(Alignment.BottomEnd).padding(bottom = 12.dp, end = 16.dp), weight = 600)
            }
        } }
    }
}


@Composable
private fun Activity() {
    val s = remember(Prefs.activity) { stats() }
    val h = remember(Prefs.activity, Prefs.readDays) { heat(182) }
    Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("${s.hours}", "hours listened", C.Gold), Triple("${s.sermons}", "sermons heard", C.English), Triple("${s.chapters}", "chapters read", C.CoralSoft))
            .forEachIndexed { i, (n, label, col) ->
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    S(n, 40f, col, weight = 300, lineHeight = 1f)
                    T(label, 12f, C.Sub, Modifier.padding(top = 4.dp, bottom = 6.dp))
                    Canvas(Modifier.fillMaxWidth().height(24.dp)) { drawPath(sparkPath(s.weekly[i], size.width, size.height), col.copy(alpha = .8f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                }
            }
    }
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        T("Lamps lit, last six months", 13f, C.Sub); T("${h.count { it > 0 }} days", 13f, C.Sub)
    }
    LampField(h, 26, 3f, 6f)
    S("Lately", 20f, C.Ink, Modifier.padding(top = 24.dp, bottom = 6.dp))
    Box(Modifier.padding(start = 22.dp)) {
        Box(Modifier.offset((-17).dp, 8.dp).width(1.5.dp).height(((Prefs.activity.take(4).size) * 50).dp).background(Brush.verticalGradient(listOf(C.Gold, gold(0f)))))
        Column {
            Prefs.activity.take(4).forEach { a ->
                Box(Modifier.padding(vertical = 9.dp)) {
                    Box(Modifier.offset((-21).dp, 6.dp).size(10.dp).clip(CircleShape).background(C.Night).insetRing(2.dp, kindColor[a.kind] ?: C.Gold, CircleShape))
                    Column { S(a.what, 16f, C.Ink); T(whenText(a.at, a.minutes), 12f, C.Meta, Modifier.padding(top = 1.dp)) }
                }
            }
            if (Prefs.activity.isEmpty()) S("Nothing yet. Read or listen and it shows here.", 15f, C.Meta, italic = true)
        }
    }
}

/** The field of small lamps: [cols] weeks across, seven days down, brightest with a glow. */
@Composable
internal fun LampField(levels: List<Int>, cols: Int, gap: Float, glow: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(gap.dp)) {
        repeat(7) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
                repeat(cols) { col ->
                    val lvl = levels.getOrElse(col * 7 + r) { 0 }
                    Box(Modifier.weight(1f).aspectRatio(1f).then(if (lvl == 3) Modifier.cssShadow(0f, 0f, glow, 0f, gold(.7f), CircleShape) else Modifier).clip(CircleShape).background(heatColors[lvl]))
                }
            }
        }
    }
}

@Composable
private fun Saved() {
    var filter by remember { mutableStateOf("all") }
    val shown = Prefs.saved.filter { filter == "all" || it.kind == filter }
    Row(Modifier.padding(top = 18.dp, bottom = 6.dp).offset(x = (-20).dp).width(LocalConfiguration.current.screenWidthDp.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("all" to "All", "quote" to "Quotes", "verse" to "Verses", "hl" to "Highlights").forEach { (id, label) -> Chip(label, filter == id, height = 34f, size = 12.5f) { filter = id } }
    }
    shown.forEachIndexed { j, s ->
        val (kind, col, hl) = savedKinds[s.kind] ?: savedKinds.getValue("quote")
        val t by oneShot(filter to j, 450, j * 50)
        Row(
            Modifier.rise({ t }, Ease.Swap).fillMaxWidth().height(IntrinsicSize.Min).drawBehind { drawLine(white(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().cssShadow(0f, 0f, 12f, 0f, col, RoundedCornerShape(2.dp)).clip(RoundedCornerShape(2.dp)).background(col))
            Column(Modifier.weight(1f)) {
                S(s.text, 18f, C.Ink, Modifier.background(hl, RoundedCornerShape(4.dp)).padding(horizontal = 2.dp), weight = 300, lineHeight = 1.45f)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { T(kind, 12f, col, weight = 600); T(s.source, 12f, C.Meta) }
            }
            Box(Modifier.size(34.dp).press({ Prefs.removeSaved(s.key) }, label = "Remove from saved"), contentAlignment = Alignment.Center) { Ico(Icons.BookmarkFilled, 17f, col) }
        }
    }
    if (shown.isEmpty()) S("Nothing here yet. Tap a verse or a quote to keep it.", 16f, C.Meta, Modifier.fillMaxWidth().padding(vertical = 40.dp), italic = true, align = TextAlign.Center)
}

@Composable
private fun LetterDialog(l: Letter, startLang: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var lang by remember { mutableStateOf(startLang) }
    val shown = (if (lang == startLang) l else letterIn(l, lang)) ?: l
    val t by oneShot(Unit, 700)
    val scrim by oneShot(Unit, 300)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().fade({ scrim }).background(c(Palette.ScrimUpdate)).press(onClose, role = null))
        Box(
            Modifier.padding(start = 14.dp, end = 14.dp, top = 70.dp, bottom = 40.dp).fillMaxSize().unfold({ t }, Ease.Swap)
                .cssShadow(0f, 40f, 80f, -20f, Color.Black.copy(alpha = .9f), RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(C.Paper).grain(.35f).clickable0()
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 30.dp)) { LetterBody(shown, lang, { lang = it }, LetterSpecs.PhoneLibrary) }
            Box(Modifier.align(Alignment.TopEnd).padding(14.dp).size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = .06f)).press(onClose, label = "Close the letter"), contentAlignment = Alignment.Center) {
                Ico(Icons.Close, 13f, C.PaperInk)
            }
        }
    }
}
