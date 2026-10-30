package org.theguide.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Palette
import kotlinx.coroutines.launch
import org.theguide.audio.Audio
import org.theguide.data.Daily
import org.theguide.data.DayQuote
import org.theguide.data.Prefs
import org.theguide.data.Tables
import org.theguide.data.Updates
import org.theguide.data.Updates.Stage
import org.theguide.ui.*
import org.theguide.ui.phone.hearQuote
import org.theguide.ui.phone.latestLetter
import org.theguide.ui.phone.monthName
import parsers.bt.Letter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The TV letter's torn paper (clip-path polygon, top and bottom edges). */
internal val tvTorn = GenericShape { s, _ ->
    listOf(0f to 3f, 8f to 0f, 20f to 2f, 34f to .5f, 50f to 2.2f, 66f to .4f, 82f to 2f, 100f to .6f, 100f to 97f, 86f to 100f, 70f to 98f, 52f to 99.6f, 34f to 98f, 16f to 99.8f, 0f to 97.5f)
        .forEachIndexed { i, (x, y) -> val px = s.width * x / 100; val py = s.height * y / 100; if (i == 0) moveTo(px, py) else lineTo(px, py) }
    close()
}

/** TVHome board. */
@Composable
fun TvHome() {
    val today = LocalDate.now()
    var date by remember { mutableStateOf(today) }
    var sheet by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf<DayQuote?>(null) }
    var toastHidden by remember { mutableStateOf(false) }
    LaunchedEffect(date) { quote = Daily.load(date) }
    val scope = rememberCoroutineScope()
    val time by clock()
    val screen by oneShot(Unit, 600)
    val qSwap by oneShot(date to quote, 700)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val saved = Prefs.isSaved("quote:${quote?.date}")

    Box(Modifier.fillMaxSize().background(C.Backdrop).ambient(gold(.14f), .3f, if (sheet == "month") .4f else .3f)) {
        Box(Modifier.fillMaxSize().enter({ screen })) {
            Column(Modifier.offset(130.dp, 54.dp).width(640.dp)) {
                T(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), 18f, C.Sub)
                Box {
                    S("“", 220f, C.Gold.copy(alpha = .16f), Modifier.offset((-24).dp, (-26).dp), lineHeight = 1f)
                    Column {
                        T(if (date == today) "Quote for today" else "Quote for ${monthName(date)} ${date.dayOfMonth}", 16f, C.Brass, Modifier.padding(top = 26.dp))
                        Column(Modifier.enter({ qSwap }, Ease.Swap)) {
                            // the board's 50 px suits its short sample; real quotes step down so they keep to the space above the cards
                            val qt = quote?.text ?: ""
                            val qs = when { qt.length <= 70 -> 50f; qt.length <= 120 -> 42f; qt.length <= 180 -> 36f; else -> 32f }
                            S(qt, qs, c(Palette.Title), Modifier.padding(top = 10.dp), weight = 300, lineHeight = 1.16f, tracking = -.8f * qs / 50f, maxLines = 4, ellipsis = true)
                            S(quote?.ref ?: "", 21f, c(Palette.QuoteRef), Modifier.padding(top = 16.dp), italic = true)
                        }
                        Row(Modifier.padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                Modifier.height(58.dp).tvFocus(RoundedCornerShape(29.dp), { quote?.let { q -> scope.launch { hearQuote(q); Nav.go(Nav.Tab.Table) } } }).clip(RoundedCornerShape(29.dp)).background(C.Gold).sheen({ time })
                                    .focusRequester(first).padding(horizontal = 28.dp),
                                contentAlignment = Alignment.Center,
                            ) { T("Hear it", 19f, C.OnGold, weight = 600) }
                            val pop by oneShot(saved, 450)
                            Row(
                                Modifier.height(58.dp).tvFocus(RoundedCornerShape(29.dp), { quote?.let { Prefs.toggleSaved(Prefs.Saved("quote", it.text, it.ref, "quote:${it.date}")) } }).clip(RoundedCornerShape(29.dp)).background(white(.08f))
                                    .padding(horizontal = 26.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Ico(if (saved) Icons.BookmarkFilled else Icons.Bookmark, 20f, C.Gold, if (saved) Modifier.pop({ pop }) else Modifier)
                                T(if (saved) "Saved" else "Save", 19f, c(Palette.Title), weight = 600)
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(start = 850.dp, end = 60.dp, top = 110.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StreakLamp(64f, 74f, 80f, 0f)
                    Column {
                        S("${Prefs.streak()} days in the Word", 28f, c(Palette.Title))
                        T(if (today.toEpochDay() in Prefs.readDays) "The lamp is lit for today" else "Read today to keep the lamp lit", 15f, C.Sub)
                    }
                }
                TvWeek(today, date, Modifier.padding(top = 16.dp)) { date = it }
                Box(Modifier.padding(top = 30.dp).fillMaxWidth()) {
                    Canvas(Modifier.offset(y = (-6).dp).size(22.dp, 70.dp).swing({ time }, 4000, TransformOrigin(.5f, 0f))) {
                        val k = size.width / 22f
                        drawPath(Path().apply { moveTo(2 * k, 0f); lineTo(20 * k, 0f); lineTo(20 * k, 62 * k); lineTo(11 * k, 54 * k); lineTo(2 * k, 62 * k); close() }, c(Palette.RibbonTv))
                        drawRect(c(Palette.RibbonTvTop), Offset(2 * k, 0f), Size(18 * k, 6 * k))
                    }
                    Column(Modifier.padding(start = 40.dp)) {
                        T("Verse for today", 15f, C.Brass)
                        S(quote?.verse ?: "", 22f, C.Text, Modifier.padding(top = 6.dp), weight = 300, italic = true, lineHeight = 1.4f, maxLines = 4, ellipsis = true)
                        T(quote?.verseRef ?: "", 15f, C.Sub, Modifier.padding(top = 8.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomStart).padding(start = 130.dp, end = 60.dp, bottom = 44.dp).fillMaxWidth().height(196.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                val letter = latestLetter()
                LetterCard(letter?.first, Modifier.weight(1.15f)) { sheet = "letter" }
                ContinueCard(Modifier.weight(1f))
                MonthCard(today, Modifier.weight(1f)) { sheet = "month" }
            }
        }
        if (sheet == "month") MonthOverlay(today, date, quote, { date = it }) { sheet = "" }
        if (sheet == "letter") latestLetter()?.let { (l, lang) -> TvLetterDialog(l, lang, LetterSpecs.TvHome) { sheet = "" } }
        if (Updates.stage == Stage.Available && sheet.isEmpty() && !toastHidden) UpdateToast { toastHidden = true }
        if (Updates.stage in setOf(Stage.Downloading, Stage.Ready, Stage.Installing, Stage.Done)) InstallModal()
    }
}

/** The light sweeping across the gold button (`@keyframes sheen`, 3.4s). */
internal fun Modifier.sheen(time: () -> Long, ms: Int = 3400) = drawBehind {
    val p = keyframes(phase(time(), ms), Ease.InOut, 0f to 0f, 1f to 1f)
    val w = size.width * .4f
    val x = -1.2f * w + p * 3.8f * w
    val sk = size.height * kotlin.math.tan(Math.toRadians(18.0)).toFloat()
    val path = Path().apply { moveTo(x + sk, 0f); lineTo(x + w + sk, 0f); lineTo(x + w, size.height); lineTo(x, size.height); close() }
    drawPath(path, Brush.horizontalGradient(listOf(Color.Transparent, white(.4f), Color.Transparent), x, x + w + sk))
}

@Composable
internal fun TvWeek(today: LocalDate, sel: LocalDate, modifier: Modifier, pick: (LocalDate) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (6 downTo 0).map { today.minusDays(it.toLong()) }.forEach { d ->
            val on = d == sel; val read = d.toEpochDay() in Prefs.readDays
            Column(
                Modifier.weight(1f).tvFocus(RoundedCornerShape(18.dp), { pick(d) }).clip(RoundedCornerShape(18.dp)).background(if (on) gold(.16f) else Color.Transparent)
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                T(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), 13f, if (on) C.Gold else C.Faint)
                S("${d.dayOfMonth}", 22f, if (on) c(Palette.Title) else c(Palette.Stone))
                val flame = GenericShape { s, _ -> addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, s.width, s.height,
                    androidx.compose.ui.geometry.CornerRadius(s.width * .5f, s.height * .65f), androidx.compose.ui.geometry.CornerRadius(s.width * .5f, s.height * .65f),
                    androidx.compose.ui.geometry.CornerRadius(s.width * .45f, s.height * .35f), androidx.compose.ui.geometry.CornerRadius(s.width * .45f, s.height * .35f))) }
                Box(Modifier.size(9.dp, 13.dp).then(if (read) Modifier.cssShadow(0f, 0f, 10f, 0f, gold(.7f), flame) else Modifier).clip(flame)
                    .drawBehind { if (read) drawRect(Brush.radialGradient(0f to C.Flame, 1f to C.Amber, center = Offset(size.width / 2, size.height * .7f), radius = size.height)) else drawRect(white(.12f)) })
            }
        }
    }
}

@Composable
private fun LetterCard(letter: Letter?, modifier: Modifier, onOpen: () -> Unit) {
    Box(modifier.fillMaxHeight().tvFocus(tvTorn, onOpen).clip(tvTorn).background(C.Paper).grain(.35f).padding(horizontal = 26.dp, vertical = 22.dp)) {
        Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).graphicsLayer { rotationZ = -8f }) { org.theguide.ui.phone.WaxSeal(48f, 16f, shadow = false) }
        Column {
            T(letter?.letterDate?.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)) ?: "", 15f, C.PaperMuted, face = Face.Serif, italic = true)
            S(letter?.salutation ?: "", 27f, C.PaperInk, Modifier.padding(top = 4.dp, end = 50.dp), lineHeight = 1.12f, maxLines = 2)
            T("${letter?.code ?: ""} ${letter?.title ?: ""}, and Sunday’s service to play", 14.5f, C.PaperSign, Modifier.padding(top = 12.dp), maxLines = 2)
        }
    }
}

@Composable
private fun ContinueCard(modifier: Modifier) {
    val r = Prefs.resume
    val scope = rememberCoroutineScope()
    val a by spinAngle(3000f, true)
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier.fillMaxHeight().tvFocus(shape, { r?.let { scope.launch { Tables.sermon(it.iso3, it.code)?.let { s -> Audio.play(it.iso3, s, it.seconds); org.theguide.data.Reader.open(it.iso3, s, it.paragraph) }; Nav.go(Nav.Tab.Table) } } })
            .clip(shape).background(Brush.verticalGradient(listOf(c(Palette.DeckTop), c(Palette.DeckBottom)))).grain(.2f).topHighlight(white(.08f), shape).padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { TinyReel(11f, a); TinyReel(8f, a) }
            S(r?.title ?: "Nothing playing yet", 24f, c(Palette.Title), Modifier.padding(top = 16.dp), maxLines = 1, ellipsis = true)
            T(r?.let { "${(it.seconds / 60).toInt()} minutes in, paragraph ${it.paragraph}" } ?: "Open the Table to begin", 14.5f, C.Sub)
        }
        Box(Modifier.align(Alignment.BottomCenter).offset(y = 0.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(white(.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(r?.let { (it.seconds / it.duration.coerceAtLeast(1.0)).toFloat() } ?: 0f).background(C.Gold))
        }
    }
}

/** The small reel of the TV continue card (viewBox 24): tape out to [tape], hub, ring and four marks. */
@Composable
internal fun TinyReel(tape: Float, angle: Float, size: Float = 52f) = Canvas(Modifier.size(size.dp).graphicsLayer { rotationZ = angle }) {
    val k = this.size.width / 24f
    drawCircle(c(Palette.ReelTape), tape * k); drawCircle(c(Palette.ReelHub), 6 * k); drawCircle(c(Palette.ReelSpoke), 5 * k, style = Stroke(1.2f * k))
    listOf(Offset(12f, 7f) to Offset(12f, 9f), Offset(12f, 15f) to Offset(12f, 17f), Offset(7f, 12f) to Offset(9f, 12f), Offset(15f, 12f) to Offset(17f, 12f))
        .forEach { (a, b) -> drawLine(c(Palette.ReelSpoke), a * k, b * k, 1.4f * k) }
}

@Composable
private fun MonthCard(today: LocalDate, modifier: Modifier, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val first = today.withDayOfMonth(1); val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    Column(modifier.fillMaxHeight().tvFocus(shape, onOpen).clip(shape).background(white(.05f)).insetRing(1.dp, white(.06f), shape).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { S(monthName(today), 24f, c(Palette.Title)); T("Every quote", 14f, C.Sub) }
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            (0 until 35).chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { i ->
                        val n = i - lead + 1
                        val d = if (n in 1..today.lengthOfMonth()) first.withDayOfMonth(n) else null
                        val lit = d != null && !d.isAfter(today) && d.toEpochDay() in Prefs.readDays
                        Box(Modifier.size(9.dp).then(if (d == today) Modifier.cssShadow(0f, 0f, 8f, 0f, C.Gold, CircleShape) else Modifier).clip(CircleShape)
                            .background(if (d == null) Color.Transparent else if (lit) C.Gold else white(.12f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthOverlay(today: LocalDate, sel: LocalDate, q: DayQuote?, onPick: (LocalDate) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val t by oneShot(Unit, 400)
    val qSwap by oneShot(sel to q, 600)
    val back = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { back.requestFocus() } }
    val first = today.withDayOfMonth(1); val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    Row(Modifier.fillMaxSize().fade({ t }).background(c(Palette.TvOverlay)).padding(start = 130.dp, end = 80.dp, top = 70.dp), horizontalArrangement = Arrangement.spacedBy(70.dp)) {
        Column(Modifier.width(560.dp)) {
            S(monthName(today), 54f, c(Palette.Title), weight = 300)
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { T(it, 14f, C.Faint, Modifier.weight(1f), align = TextAlign.Center) } }
                (0 until 35).chunked(7).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { i ->
                            val n = i - lead + 1
                            val ok = n in 1..today.lengthOfMonth()
                            val d = if (ok) first.withDayOfMonth(n) else null
                            val past = d != null && !d.isAfter(today)
                            val on = d == sel
                            Box(
                                Modifier.weight(1f).height(62.dp).graphicsLayer { alpha = if (!ok) 0f else if (past) 1f else .35f }
                                    .clip(RoundedCornerShape(20.dp)).background(if (on) C.Gold else white(.04f))
                                    .then(if (past) Modifier.tvFocus(RoundedCornerShape(20.dp), { onPick(d!!) }) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (ok) S("$n", 22f, if (on) C.OnGold else c(Palette.Title))
                                if (past && !on && d!!.toEpochDay() in Prefs.readDays) Box(Modifier.align(Alignment.BottomCenter).offset(y = (-8).dp).size(6.dp).clip(CircleShape).background(C.Gold))
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.weight(1f).padding(top = 110.dp)) {
            S("“", 200f, C.Gold.copy(alpha = .15f), Modifier.offset((-20).dp, (-70).dp), lineHeight = 1f)
            Column {
                T(if (sel == today) "Quote for today" else "Quote for ${monthName(sel)} ${sel.dayOfMonth}", 17f, C.Brass)
                Column(Modifier.enter({ qSwap }, Ease.Swap)) {
                    S(q?.text?.let { if (it.length > 200) it.take(200).substringBeforeLast(' ') + "…" else it } ?: "", 42f, c(Palette.Title), Modifier.padding(top = 12.dp), weight = 300, lineHeight = 1.2f)
                    S(q?.ref ?: "", 20f, c(Palette.QuoteRef), Modifier.padding(top = 16.dp), italic = true)
                }
                TvGhost("Back to home", onClose, Modifier.padding(top = 36.dp).focusRequester(back), height = 54f, padX = 26f)
            }
        }
    }
}

/** A letter over the room: a 720 px sheet of paper unfolding from the top. */
@Composable
internal fun TvLetterDialog(letter: Letter, startLang: String, spec: LetterSpec, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var lang by remember { mutableStateOf(startLang) }
    val shown = (if (lang == startLang) letter else letterIn(letter, lang)) ?: letter
    val t by oneShot(Unit, 800)
    val s by oneShot(Unit, 350)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().fade({ s }).background(c(Palette.ScrimTv)))
        Box(
            Modifier.align(Alignment.TopCenter).padding(vertical = 40.dp).width(720.dp).fillMaxHeight().unfold({ t }, Ease.Swap)
                .cssShadow(0f, 50f, 100f, -30f, Color.Black.copy(alpha = .9f), RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(C.Paper).grain(.35f)
        ) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 40.dp).graphicsLayer { rotationZ = -8f }) { org.theguide.ui.phone.WaxSeal(70f, 22f, shadow = false) }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).focusRequester(focus).padding(horizontal = 56.dp, vertical = 44.dp)) {
                LetterBody(shown, lang, { lang = it }, spec, tv = true)
            }
        }
    }
}

@Composable
private fun BoxScope.UpdateToast(onLater: () -> Unit) {
    val t by oneShot(Unit, 700)
    val time by clock()
    val shape = RoundedCornerShape(40.dp)
    Row(
        Modifier.align(Alignment.TopCenter).offset(x = 60.dp, y = 16.dp).width(640.dp).sheetDown({ t }, Ease.Sheet)
            .cssShadow(0f, 30f, 60f, -20f, Color.Black.copy(alpha = .9f), shape).clip(shape).background(c(Palette.TvToast)).topHighlight(white(.08f), shape)
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.requiredSize(60.dp).ripple({ time }, 2400).drawBehind { drawCircle(gold(.5f), style = Stroke(1.5.dp.toPx())) })
            Box(Modifier.fillMaxSize().clip(CircleShape).drawBehind { drawCircle(Brush.radialGradient(0f to C.Flame, 1f to C.Amber, center = Offset(size.width * .35f, size.height * .3f), radius = size.width)) }, contentAlignment = Alignment.Center) {
                Ico(Icons.DownloadTv, 20f, C.OnGold)
            }
        }
        Column(Modifier.weight(1f)) {
            S("The Guide ${Updates.version} is here", 19f, c(Palette.Title))
            T(Updates.notes.firstOrNull()?.let { (a, b) -> listOf(a, b).filter { it.isNotBlank() }.joinToString(": ") } ?: "About ${Updates.sizeMb} MB", 13.5f, C.Sub, maxLines = 1, ellipsis = true)
        }
        TvGhost("Later", onLater, height = 46f, size = 15f, padX = 18f)
        TvGold("Update now", { Updates.download() }, height = 46f, size = 15f, padX = 20f, weight = 600)
    }
}

@Composable
private fun InstallModal() {
    val s = Updates.stage
    val time by clock()
    val pct = when (s) { Stage.Downloading -> Updates.pct; Stage.Done -> 1f; else -> 1f }
    val ring = animate(pct, 400, Ease.Linear)
    val btn = remember { FocusRequester() }
    LaunchedEffect(s) { if (s == Stage.Ready || s == Stage.Done) runCatching { btn.requestFocus() } }
    Box(Modifier.fillMaxSize().background(c(Palette.TvModal)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.padding(20.dp).fillMaxSize().halo({ time }).background(Brush.radialGradient(0f to gold(.28f), .7f to Color.Transparent), CircleShape))
                Canvas(Modifier.fillMaxSize()) {
                    val r = 84.dp.toPx(); val w = 6.dp.toPx()
                    drawCircle(white(.08f), r, style = Stroke(w))
                    drawArc(C.Gold, -90f, 360f * ring, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(w, cap = StrokeCap.Round))
                }
                S(if (s == Stage.Done || s == Stage.Ready) "✓" else "${Math.round(pct * 100)}%", 46f, c(Palette.Title), weight = 300)
            }
            S(when (s) { Stage.Done -> "You’re on ${Updates.version}"; Stage.Ready -> "Ready to install"; Stage.Installing -> "Putting it in place"; else -> "Downloading the update" }, 40f, c(Palette.Title), Modifier.padding(top = 26.dp), weight = 300)
            T(when (s) { Stage.Done -> "Your place, downloads and saved quotes are all here."; Stage.Ready -> "Android asks once, then The Guide reopens on its own."; else -> "The Guide will reopen by itself. Keep the TV on." }, 18f, C.Sub, Modifier.padding(top = 8.dp))
            if (s == Stage.Ready) TvGold("Install", { Updates.install() }, Modifier.padding(top = 26.dp).focusRequester(btn), height = 56f, size = 18f, padX = 30f, weight = 600)
            if (s == Stage.Done) TvGold("Back to The Guide", { Updates.dismissDone() }, Modifier.padding(top = 26.dp).focusRequester(btn), height = 56f, size = 18f, padX = 30f, weight = 600)
        }
    }
}
