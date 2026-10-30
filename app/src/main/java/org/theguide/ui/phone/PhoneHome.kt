package org.theguide.ui.phone

import androidx.compose.ui.text.withStyle
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.theguide.audio.Audio
import org.theguide.data.Daily
import org.theguide.data.DayQuote
import org.theguide.data.Prefs
import org.theguide.data.Tables
import org.theguide.data.Updates
import org.theguide.data.Updates.Stage
import org.theguide.live.LiveClient
import org.theguide.ui.*
import parsers.bt.Letter
import parsers.bt.Letters
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

internal fun monthName(d: LocalDate) = d.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

/** The latest letter in the reader's letter language (English when that language has none yet). */
@Composable
internal fun latestLetter(): Pair<Letter, String>? {
    val lang = Prefs.letterLang
    val mine by Letters.book(lang).all.collectAsState()
    val en by Letters.book("en").all.collectAsState()
    return mine.firstOrNull()?.let { it to lang } ?: en.firstOrNull()?.let { it to "en" }
}

/** Plays the sermon a daily quote comes from, at its paragraph, in the reading language. */
internal suspend fun hearQuote(q: DayQuote) {
    val iso3 = Prefs.readingLang.takeIf { Tables.has(it) } ?: Tables.installed().firstOrNull() ?: return
    val s = Tables.sermon(iso3, q.code) ?: return
    Audio.play(iso3, s)
    q.paragraph?.let { Audio.playParagraph(it) }
    Nav.player = true
}

internal fun share(ctx: android.content.Context, text: String) =
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))

/** PhoneHome board. */
@Composable
fun PhoneHome(haze: HazeState) {
    val today = LocalDate.now()
    var date by remember { mutableStateOf(today) }
    var qflip by remember { mutableIntStateOf(0) }
    var month by remember { mutableStateOf(false) }
    var letterOpen by remember { mutableStateOf(false) }
    var updOpen by remember { mutableStateOf(false) }
    var quote by remember { mutableStateOf<DayQuote?>(null) }
    LaunchedEffect(date) { quote = Daily.load(date) }
    LaunchedEffect(Updates.stage) { if (Updates.stage == Stage.Done) updOpen = true }
    LaunchedEffect(Unit) { Prefs.letterLangs.forEach { launch { Letters.book(it).sync() } } }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val screen by oneShot(Unit, 500)
    val pick = { d: LocalDate -> if (d != date) { date = d; qflip++ } }

    Box(Modifier.fillMaxSize().background(C.Night).screenGlow(gold(.16f))) {
        Column(Modifier.fillMaxSize().hazeSource(haze).padding(top = statusTop()).verticalScroll(rememberScrollState()).enter({ screen })) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 130.dp)) {
                // header
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        T(today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), 13.5f, C.Sub)
                        val part = LocalTime.now().hour.let { if (it < 12) "morning" else if (it < 17) "afternoon" else "evening" }
                        S("Good $part${Prefs.liveName.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}", 30f, C.Ink, Modifier.padding(top = 4.dp), weight = 300, tracking = -.4f)
                    }
                    UpdateButton { updOpen = true }
                }
                // streak lamp and the week
                Row(Modifier.padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StreakLamp()
                    Column(Modifier.weight(1f)) {
                        S("${Prefs.streak()} days in the Word", 21f, C.Ink)
                        T(if (today.toEpochDay() in Prefs.readDays) "You read today. The lamp stays lit." else "Read today to keep the lamp lit", 12.5f, C.Sub, Modifier.padding(top = 2.dp))
                    }
                    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(white(.06f)).press({ month = true }, label = "Open the calendar").padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        T(monthName(today), 12.5f, C.Soft)
                    }
                }
                Week(today, date, Modifier.padding(top = 14.dp), pick)
                QuoteBlock(quote, today == date, date, qflip, onHear = { quote?.let { q -> scope.launch { hearQuote(q) } } }, onShare = { quote?.let { share(ctx, "“${it.text}”\n${it.ref}") } })
                VerseBlock(quote)
                S("This week", 21f, C.Ink, Modifier.padding(top = 34.dp, bottom = 12.dp))
                latestLetter()?.let { (l, lang) -> LetterCard(l, lang) { letterOpen = true } }
                ContinueCard()
                if (LiveClient.hosts.isNotEmpty()) JoinPill()
            }
        }
        if (month) MonthSheet(haze, today, date, onPick = { pick(it); month = false }) { month = false }
        if (letterOpen) latestLetter()?.let { (l, lang) -> LetterUnfolded(l, lang) { letterOpen = false } }
        if (updOpen && Updates.stage != Stage.None) UpdateSheet(haze) { updOpen = false; if (Updates.stage == Stage.Done) Updates.dismissDone() }
    }
}

@Composable
private fun UpdateButton(onClick: () -> Unit) {
    val time by clock()
    Box(Modifier.size(42.dp).clip(CircleShape).background(white(.06f)).press(onClick, label = if (Updates.stage == Stage.None) "Updates" else "Update available"), contentAlignment = Alignment.Center) {
        Ico(Icons.Download, 19f, C.Text)
        if (Updates.stage != Stage.None && Updates.stage != Stage.Done) Box(
            Modifier.align(Alignment.TopEnd).offset((-6).dp, 6.dp).size(10.dp).beat({ time })
                .drawBehind { drawCircle(C.Night, radius = size.width / 2 + 3.dp.toPx()); drawCircle(C.Gold) }
        )
    }
}

private val flameShape = GenericShape { s, _ ->
    // border-radius: 50% 50% 45% 45% / 65% 65% 35% 35%
    val w = s.width; val h = s.height
    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h,
        androidx.compose.ui.geometry.CornerRadius(w * .5f, h * .65f), androidx.compose.ui.geometry.CornerRadius(w * .5f, h * .65f),
        androidx.compose.ui.geometry.CornerRadius(w * .45f, h * .35f), androidx.compose.ui.geometry.CornerRadius(w * .45f, h * .35f)))
}

@Composable
internal fun Week(today: LocalDate, sel: LocalDate, modifier: Modifier, pick: (LocalDate) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (6 downTo 0).map { today.minusDays(it.toLong()) }.forEach { d ->
            val on = d == sel
            val read = d.toEpochDay() in Prefs.readDays
            val bg by animateColorAsState(if (on) gold(.16f) else Color.Transparent, tw(350), label = "wk")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(bg).press({ pick(d) }, role = Role.Tab, label = "${monthName(d)} ${d.dayOfMonth}${if (read) ", read" else ", missed"}")
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                T(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), 11f, if (on) C.Gold else C.Faint)
                S("${d.dayOfMonth}", 18f, if (on) C.Ink else c(Palette.WeekNum))
                Box(
                    Modifier.size(8.dp, 11.dp)
                        .then(if (read) Modifier.cssShadow(0f, 0f, 8f, 0f, gold(.8f), flameShape) else Modifier)
                        .clip(flameShape)
                        .background(if (read) Brush.verticalGradient(listOf(C.Flame, C.Amber)) else Brush.linearGradient(listOf(white(.1f), white(.1f))))
                )
            }
        }
    }
}

@Composable
private fun QuoteBlock(q: DayQuote?, today: Boolean, date: LocalDate, flip: Int, onHear: () -> Unit, onShare: () -> Unit) {
    val swap by oneShot(flip to q, 600)
    val key = "quote:${q?.date}"
    val saved = Prefs.isSaved(key)
    Box(Modifier.padding(top = 28.dp).fillMaxWidth().heightIn(min = 250.dp)) {
        S("“", 140f, C.Gold.copy(alpha = .22f), Modifier.offset((-6).dp, (-36).dp), lineHeight = 1f)
        Column {
            T(if (today) "Quote for today" else "Quote for ${monthName(date)} ${date.dayOfMonth}", 13f, C.Brass)
            Column(Modifier.enter({ swap }, Ease.Swap)) {
                S(q?.text ?: "", 27f, C.Ink, Modifier.padding(top = 10.dp), weight = 300, lineHeight = 1.28f, tracking = -.2f)
                S(q?.ref ?: "", 15f, c(Palette.QuoteRef), Modifier.padding(top = 14.dp), italic = true)
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(C.Gold).press(onHear).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) { Ico(Icons.Play, 13f, C.OnGold); T("Hear it", 13.5f, C.OnGold, weight = 600) }
                val s = animate(if (saved) 1.15f else 1f, 350, Ease.O16)
                RoundButton({ q?.let { Prefs.toggleSaved(Prefs.Saved("quote", it.text, it.ref, key)) } }, "Save this quote", bg = white(.06f)) {
                    Ico(if (saved) Icons.BookmarkFilled else Icons.Bookmark, 18f, if (saved) C.Gold else C.Text, Modifier.graphicsLayer { scaleX = s; scaleY = s })
                }
                RoundButton(onShare, "Share", bg = white(.06f)) { Ico(Icons.Share, 18f, C.Text) }
            }
        }
    }
}

@Composable
private fun VerseBlock(q: DayQuote?) {
    val time by clock()
    Box(Modifier.padding(top = 30.dp).fillMaxWidth()) {
        Canvas(Modifier.offset(4.dp, (-10).dp).size(22.dp, 120.dp).swing({ time }, origin = TransformOrigin(.5f, 0f))) {
            val k = size.width / 22f
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(0f, 0f); lineTo(22f * k, 0f); lineTo(22f * k, 120f * k); lineTo(11f * k, 110f * k); lineTo(0f, 120f * k); close() }
            drawPath(p, Brush.horizontalGradient(0f to C.Wax, .5f to c(Palette.Ribbon), 1f to C.Wax))
        }
        Column(Modifier.padding(start = 40.dp, top = 4.dp, bottom = 4.dp)) {
            T("Verse for today", 13f, C.Brass)
            S(q?.verse ?: "", 19f, c(Palette.VerseText), Modifier.padding(top = 8.dp), weight = 300, italic = true, lineHeight = 1.5f)
            T(q?.verseRef ?: "", 13f, C.Sub, Modifier.padding(top = 8.dp))
        }
    }
}

/** The torn bottom edge of the letter card (the board's clip-path polygon). */
private val tornCard = GenericShape { s, _ ->
    val pts = listOf(0 to 0, 100 to 0, 100 to 92, 96 to 95, 91 to 92, 85 to 96, 79 to 93, 72 to 97, 65 to 93, 58 to 96, 51 to 93, 44 to 97, 37 to 93, 30 to 96, 23 to 92, 16 to 96, 9 to 93, 4 to 96, 0 to 93)
    pts.forEachIndexed { i, (x, y) -> val px = s.width * x / 100f; val py = s.height * y / 100f; if (i == 0) moveTo(px, py) else lineTo(px, py) }
    close()
}

@Composable
private fun LetterCard(l: Letter, lang: String, onOpen: () -> Unit) {
    val preview = l.sections.flatMap { it.blocks }.joinToString(" ") { it.text }.let { if (it.length > 110) it.take(110).trimEnd() + "…" else it }
    Box(
        Modifier.fillMaxWidth().graphicsLayer { rotationZ = -1.4f }.clip(tornCard).background(C.Paper).grain(.5f, multiply = true)
            .press(onOpen, label = "Open Brother Joseph's letter").padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 34.dp)
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(4.dp, (-6).dp).size(50.dp).graphicsLayer { rotationZ = 8f }.clip(CircleShape)
                .drawBehind {
                    drawCircle(Brush.radialGradient(0f to c(Palette.WaxBright), .7f to C.Wax, center = Offset(size.width * .35f, size.height * .3f), radius = size.width))
                    drawCircle(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .35f)), size.height * .7f, size.height))
                    drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = .25f), Color.Transparent), 0f, size.height * .12f))
                },
            contentAlignment = Alignment.Center,
        ) { S("BT", 17f, c(Palette.WaxText)) }
        Column {
            T(l.letterDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), 12f, c(Palette.PaperDate))
            S(l.salutation ?: "", 22f, c(Palette.PaperDeep), Modifier.padding(top = 10.dp), italic = true)
            S(preview, 15f, c(Palette.PaperPreview), Modifier.padding(top = 8.dp).widthIn(max = 270.dp), lineHeight = 1.55f)
            T("Read the letter", 12.5f, C.Wax, Modifier.padding(top = 12.dp), weight = 600)
        }
    }
}

/** Two small reels and where listening stopped. */
@Composable
private fun ContinueCard() {
    val r = Prefs.resume ?: return
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(22.dp)
    val a by spinAngle(3000f, true)
    Row(
        Modifier.padding(top = 30.dp).fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(c(Palette.HomeTapeTop), c(Palette.HomeTapeBottom)))).topHighlight(white(.06f), shape)
            .press({ scope.launch { Tables.sermon(r.iso3, r.code)?.let { Audio.play(r.iso3, it, r.seconds) }; Nav.player = true } })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MiniReel(7f, 9f, a); MiniReel(4f, 6f, a) }
        Column(Modifier.weight(1f)) {
            S("Keep listening to ${r.title}", 17f, C.Ink)
            T("${(r.seconds / 60).toInt()} minutes in, ${leftText(r.duration - r.seconds)}", 12.5f, C.Sub)
        }
    }
}

internal fun leftText(seconds: Double): String {
    val m = (seconds / 60).toInt()
    return when { m < 2 -> "nearly done"; m < 50 -> "$m minutes left"; m < 75 -> "about an hour left"; m < 105 -> "about an hour and a half left"; else -> "about ${Math.round(m / 60.0)} hours left" }
}

/** A 30 px cassette reel: copper ring of [ring] px, a dark band to [band] px, spokes turning. */
@Composable
internal fun MiniReel(ring: Float, band: Float, angle: Float, size: Float = 30f) {
    Canvas(Modifier.size(size.dp).graphicsLayer { rotationZ = angle }) {
        val stops = (0..17).flatMap { i -> val c1 = if (i % 2 == 0) c(Palette.CassetteDark) else c(Palette.CassetteLight); listOf(i / 18f to c1, (i + 1) / 18f - .0001f to c1) }
        drawCircle(Brush.sweepGradient(*stops.toTypedArray()))
        val d = density
        drawCircle(c(Palette.CassetteDark), radius = this.size.width / 2 - ring * d - (band - ring) * d / 2, style = Stroke((band - ring) * d))
        drawCircle(c(Palette.CassetteTape), radius = this.size.width / 2 - ring * d / 2, style = Stroke(ring * d))
    }
}

/** The way in to Join a service: on Home while a Live host is on the Wi-Fi, and always at the top of Library. */
@Composable
internal fun JoinPill(top: Float = 14f) {
    val time by clock()
    val host = LiveClient.hosts.firstOrNull()
    Row(
        Modifier.padding(top = top.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c(Palette.Card)).insetRing(1.dp, gold(.3f), RoundedCornerShape(22.dp))
            .press({ Nav.go(Nav.Tab.Live) }).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // the dot beats only while a service is live nearby
        Box(Modifier.size(10.dp).then(if (host != null) Modifier.beat({ time }, 1400) else Modifier).clip(CircleShape).background(if (host != null) C.Coral else white(.2f)))
        Column(Modifier.weight(1f)) {
            T("Join a service", 16f, C.Text, weight = 600)
            T(host?.let { "${it.name}, reading ${it.title}" } ?: "Listen along with your church", 13f, C.Sub)
        }
        Ico(Icons.HostArrow, 18f, C.Gold)
    }
}

@Composable
private fun BoxScope.MonthSheet(haze: HazeState, today: LocalDate, sel: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss, haze, scrim = c(Palette.ScrimHome), padBottom = 24f, handleBottom = 14f, shadow = false) {
        Row(verticalAlignment = Alignment.Bottom) {
            S(monthName(today), 26f, C.Ink, Modifier.weight(1f))
            T("Lit days are days you read", 13f, C.Sub)
        }
        Row(Modifier.padding(top = 14.dp).fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { T(it, 11f, C.Faint, Modifier.weight(1f), align = TextAlign.Center) } }
        val first = today.withDayOfMonth(1)
        val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
        val cells = 35.coerceAtLeast(((lead + today.lengthOfMonth() + 6) / 7) * 7)
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (0 until cells).chunked(7).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { i ->
                        val n = i - lead + 1
                        val inMonth = n in 1..today.lengthOfMonth()
                        val d = if (inMonth) first.withDayOfMonth(n) else null
                        val future = d != null && d.isAfter(today)
                        val isSel = d == sel; val isToday = d == today
                        val read = d != null && d.toEpochDay() in Prefs.readDays
                        val t by oneShot(Unit, 400, (i * 12))
                        Box(
                            Modifier.weight(1f).height(42.dp).pop({ t }, Ease.O12).clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) C.Gold else if (isToday) gold(.14f) else Color.Transparent)
                                .then(if (d != null && !future) Modifier.press({ onPick(d) }, label = "${monthName(d)} $n${if (read) ", read" else ""}") else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (inMonth) S("$n", 16f, if (isSel) C.OnGold else if (future) c(Palette.Future) else C.Text)
                            if (read && !isSel) Box(
                                Modifier.align(Alignment.BottomCenter).offset(y = (-4).dp).size(5.dp).cssShadow(0f, 0f, 6f, 0f, gold(.9f), CircleShape).clip(CircleShape).background(C.Gold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterUnfolded(letter: Letter, startLang: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var lang by remember { mutableStateOf(startLang) }
    val shown = (if (lang == startLang) letter else letterIn(letter, lang)) ?: letter
    val t by oneShot(Unit, 600)
    Box(
        Modifier.fillMaxSize().unfold({ t }, Ease.Sheet).background(c(Palette.PaperOpen)).grain(.55f, multiply = true)
            .clickable0()
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 30.dp, end = 30.dp, top = 70.dp, bottom = 60.dp)) {
            LetterBody(shown, lang, { lang = it; Prefs.letterLang = it }, LetterSpecs.PhoneHome)
            Box(
                Modifier.padding(top = 18.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Color(44, 34, 22).copy(alpha = .08f))
                    .press({ onClose(); Nav.libraryTab = "letters"; Nav.go(Nav.Tab.Library) }).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) { T("All letters", 14f, c(Palette.PaperDeep), weight = 600) }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 22.dp).size(40.dp).clip(CircleShape).background(Color(44, 34, 22).copy(alpha = .08f))
                .press(onClose, label = "Fold the letter away"),
            contentAlignment = Alignment.Center,
        ) { T("✕", 16f, c(Palette.PaperDeep)) }
    }
}

/** Swallows taps so the screen under an overlay doesn't get them. */
internal fun Modifier.clickable0() = this.then(Modifier.press({}, role = null))

@Composable
private fun BoxScope.UpdateSheet(haze: HazeState, onDismiss: () -> Unit) {
    val s = Updates.stage
    val time by clock()
    val (title, sub, btn, icon) = when (s) {
        Stage.Available -> listOf("Version ${Updates.version} is ready", "About ${Updates.sizeMb} MB, takes a minute", "Update now", "+")
        Stage.Downloading -> listOf("Downloading ${Updates.version}", "${Math.round(Updates.sizeMb * Updates.pct)} of ${Updates.sizeMb} MB", "Downloading", "↓")
        Stage.Ready -> listOf("Ready to install", "Android asks you once, then The Guide restarts on its own", "Restart and install", "✓")
        Stage.Installing -> listOf("Installing", "Your downloads and readings stay where they are", "Installing", "…")
        else -> listOf("You are on ${Updates.version}", "Here is what is new", "Got it", "✓")
    }
    val ring = when (s) { Stage.Downloading -> Updates.pct; Stage.Installing -> .5f; Stage.Ready, Stage.Done -> 1f; else -> 0f }
    val shownRing = animate(ring, 600, Ease.Css)
    val iconT by oneShot(s, 450)
    Sheet(onDismiss, haze, scrim = c(Palette.ScrimUpdate), scrimBlur = 4f, padTop = 14f, padX = 22f, padBottom = 26f, handleBottom = 18f, shadow = false) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(64.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = 28.dp.toPx(); val w = 5.dp.toPx()
                    drawCircle(white(.08f), r, style = Stroke(w))
                    drawArc(C.Gold, -90f, 360f * shownRing, false, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(w, cap = StrokeCap.Round))
                }
                Box(
                    Modifier.padding(10.dp).fillMaxSize()
                        .then(when (s) { Stage.Ready, Stage.Done -> Modifier.pop({ iconT }, Ease.O14); Stage.Installing -> Modifier.graphicsLayer { rotationZ = (time % 2000) / 2000f * 360f }; else -> Modifier })
                        .clip(CircleShape).drawBehind { drawCircle(Brush.radialGradient(0f to C.Flame, 1f to C.Amber, center = Offset(size.width * .35f, size.height * .3f), radius = size.width)) },
                    contentAlignment = Alignment.Center,
                ) { S(icon, 20f, C.OnGold) }
            }
            Column(Modifier.weight(1f)) {
                S(title, 24f, C.Ink, lineHeight = 1.1f)
                T(sub, 13f, C.Sub, Modifier.padding(top = 4.dp))
            }
        }
        if ((s == Stage.Available || s == Stage.Done) && Updates.notes.isNotEmpty()) Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Updates.notes.forEachIndexed { i, (t, d) ->
                val r by oneShot(Unit, 450, 50 + i * 70)
                Row(Modifier.rise({ r }, Ease.Swap), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(C.Gold))
                    androidx.compose.foundation.text.BasicText(
                        androidx.compose.ui.text.buildAnnotatedString {
                            if (t.isNotEmpty()) { withStyle(style(14.5f, weight = 600, color = C.Ink).toSpanStyle()) { append(t) }; append(" ") }
                            append(d)
                        }, style = style(14.5f, color = C.Soft, lineHeight = 21f),
                    )
                }
            }
        }
        T("Your downloads, highlights and reading streak stay exactly as they are.", 12.5f, C.Faint, Modifier.padding(top = 18.dp), lineHeight = 19f)
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (s == Stage.Available || s == Stage.Ready) Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(white(.07f)).press(onDismiss), contentAlignment = Alignment.Center,
            ) { T("Later", 15f, C.Text) }
            val busy = s == Stage.Downloading || s == Stage.Installing
            val bg by animateColorAsState(if (busy) gold(.25f) else C.Gold, tw(300), label = "upd")
            Box(
                Modifier.weight(2f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(bg)
                    .press({ when (s) { Stage.Available -> Updates.download(); Stage.Ready -> Updates.install(); Stage.Done -> onDismiss(); else -> {} } }, enabled = !busy),
                contentAlignment = Alignment.Center,
            ) { T(btn, 15f, C.OnGold, weight = 700) }
        }
    }
}

