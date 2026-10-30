package org.theguide.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.theguide.audio.Audio
import org.theguide.data.Prefs
import org.theguide.data.Reader
import org.theguide.data.Tables
import org.theguide.ui.*
import parsers.bt.Letters
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** "evening" for an E code, "morning" for M… */
internal fun partOfDay(code: String) = when (code.lastOrNull()) { 'E' -> "evening"; 'M' -> "morning"; 'A' -> "afternoon"; 'B' -> "evening"; else -> null }

/** A reel's tape pack radius for share [q] of the tape: 26 + 40√q px (the phone); the TV's is 28 + 44√q. */
internal fun pack(q: Float, base: Float = 26f, span: Float = 40f) = base + span * sqrt(q.coerceIn(0f, 1f))

/**
 * One tape reel: the face, the wound tape out to [packPx], and the turning spokes ([angle]); drawn in a
 * [size] px circle from the board's 138×138 viewBox.
 */
@Composable
internal fun Reel(size: Float, packPx: Float, angle: Float, innerPx: Float = 24f, tabs: Boolean = true) {
    val p = animate(packPx, 1000)
    Canvas(Modifier.size(size.dp)) {
        val d = density; val c0 = center
        drawCircle(c(Palette.ReelFace), size.dp.toPx() / 2)
        drawCircle(c(Palette.ReelTape), p * d)
        drawCircle(c(Palette.ReelTapeEdge), p * d - .5f * d, style = Stroke(1f * d))
        drawCircle(c(Palette.ReelHub), innerPx * d)
        drawCircle(white(.06f), this.size.width / 2 - d, style = Stroke(2 * d))
        val k = this.size.width / 138f
        rotate(angle, c0) {
            val col = c(Palette.ReelSpoke)
            drawCircle(col.copy(alpha = .9f), 22 * k, c0, style = Stroke(2.5f * k))
            val spokes = listOf(69f to 47f, 69f to 41f, 69f to 91f, 69f to 97f, 47f to 69f, 41f to 69f, 91f to 69f, 97f to 69f)
            spokes.chunked(2).forEach { (a, b) -> drawLine(col.copy(alpha = .9f), Offset(a.first * k, a.second * k), Offset(b.first * k, b.second * k), 2.5f * k) }
            listOf(Offset(53.4f, 53.4f) to Offset(49.2f, 49.2f), Offset(84.6f, 84.6f) to Offset(88.8f, 88.8f), Offset(53.4f, 84.6f) to Offset(49.2f, 88.8f), Offset(84.6f, 53.4f) to Offset(88.8f, 49.2f))
                .forEach { (a, b) -> drawLine(col.copy(alpha = .9f), a * k, b * k, 2.5f * k) }
            drawCircle(col, 9 * k, c0); drawCircle(c(Palette.ReelHub), 4 * k, c0)
            if (tabs) listOf(Offset(66f, 49f) to Size(6f, 7f), Offset(66f, 82f) to Size(6f, 7f), Offset(49f, 66f) to Size(7f, 6f), Offset(82f, 66f) to Size(7f, 6f))
                .forEach { (o, s) -> drawRoundRect(col, o * k, s * k, androidx.compose.ui.geometry.CornerRadius(k)) }
        }
    }
}

/** The 18 VU bars (20 on TV): the board's swinging heights while playing, gold with every fifth one blue. */
@Composable
internal fun VuMeter(playing: Boolean, bars: Int = 18, w: Float = 150f) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(playing) { while (playing) { delay(450); tick++ } }
    Row(
        Modifier.width(w.dp).height(34.dp).clip(RoundedCornerShape(10.dp)).background(c(Palette.VuWell)).insetRing(1.dp, white(.06f), RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(bars) { k ->
            val h = animate(if (playing) 6f + Math.round(20 * abs(sin((tick + k) * 1.3) * cos(k * .7))) else 4f, 150)
            Box(Modifier.size(4.dp, h.dp).clip(RoundedCornerShape(2.dp)).background(if (k % 5 == 4) C.English else C.Gold))
        }
    }
}

/** The waveform scrubber: [bars] bars of the board's fixed shape, gold up to the position; drag or tap to seek. */
@Composable
internal fun Scrubber(p: Float, bars: Int, height: Float, amp: Float, idle: Color, onSeek: ((Float) -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().height(height.dp).then(if (onSeek != null) Modifier
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) { detectDragGestures { ch, _ -> onSeek((ch.position.x / size.width).coerceIn(0f, 1f)) } } else Modifier)
            .label("Position in the sermon"),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(bars) { k ->
            val col by animateColorAsState(if (k / bars.toFloat() < p) C.Gold else idle, tw(300), label = "bar")
            Box(Modifier.weight(1f).height((6 + Math.round(amp * abs(sin(k * 1.7) * cos(k * .45)).toFloat())).dp).clip(RoundedCornerShape(2.dp)).background(col))
        }
    }
}

/** PhonePlayer board. */
@Composable
fun PhonePlayer(@Suppress("UNUSED_PARAMETER") shell: HazeState) {
    // its own blur source: it sits over the tab screen, which has the shell's
    val haze = remember { HazeState() }
    BackHandler { Nav.player = false }
    val t = Audio.track ?: run { Nav.player = false; return }
    var tool by remember { mutableStateOf("") }
    var queue by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val p = if (Audio.duration > 0) (Audio.position / Audio.duration).toFloat() else 0f
    val angle by spinAngle(2400f / Audio.speed, Audio.playing)
    val idx = Audio.currentIndex()
    val sentence = idx?.let { t.items.getOrNull(it.first) }
    val screen by oneShot(Unit, 500)
    val line by oneShot(idx?.first, 500)
    val mix = Prefs.mix
    val iso = t.iso3

    Box(Modifier.fillMaxSize().background(C.Night).screenGlow(gold(.14f)).clickable0()) {
        Column(Modifier.fillMaxSize().hazeSource(haze).padding(top = statusTop()).verticalScroll(rememberScrollState()).enter({ screen })) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 40.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundButton({ Nav.player = false }, "Close the player") { Ico(Icons.DownRound, 16f, C.Text) }
                    T(if (t.local) "From your downloads" else "Playing from the internet", 13f, C.Sub, Modifier.weight(1f), align = TextAlign.Center)
                    RoundButton({ queue = true }, "Up next") { Ico(Icons.Queue, 17f, C.Text) }
                }
                // the tape deck
                val deckShape = RoundedCornerShape(30.dp)
                Column(
                    Modifier.padding(top = 22.dp).fillMaxWidth().clip(deckShape)
                        .background(Brush.verticalGradient(listOf(c(Palette.DeckTop), c(Palette.DeckBottom)))).grain(.25f, shape = deckShape)
                        .drawBehind { drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .25f)), size.height - 40.dp.toPx(), size.height)) }
                        .topHighlight(white(.08f), deckShape).padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 18.dp),
                ) {
                    // reels 138, the tape 24 px up into them, the VU 6 px up into the tape (the board's negative margins)
                    Box(Modifier.fillMaxWidth().height(182.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Reel(138f, pack(1 - p), angle); Reel(138f, pack(p), angle)
                        }
                        Canvas(Modifier.fillMaxWidth().height(40.dp).offset(y = 114.dp)) {
                            val sx = size.width / 330f; val sy = size.height / 40f
                            val path = Path().apply { moveTo((40 + 34 * (1 - p)) * sx, 0f); lineTo(140 * sx, 30 * sy); lineTo(190 * sx, 30 * sy); lineTo((290 - 34 * p) * sx, 0f) }
                            drawPath(path, c(Palette.Tape), style = Stroke(3.dp.toPx()))
                        }
                        Box(Modifier.align(Alignment.TopCenter).offset(y = 148.dp)) { VuMeter(Audio.playing) }
                    }
                    S(t.sermon.title, 28f, C.Ink, Modifier.fillMaxWidth().padding(top = 14.dp), weight = 300, tracking = -.3f, align = TextAlign.Center)
                    T(listOfNotNull(englishTitle(t.sermon)?.takeIf { it != t.sermon.title }, t.sermon.date.ifBlank { null }, partOfDay(t.sermon.dateCode)).joinToString(", "),
                        12.5f, C.Sub, Modifier.fillMaxWidth().padding(top = 2.dp), align = TextAlign.Center)
                }
                // the sentence being spoken
                Column(Modifier.padding(top = 22.dp).fillMaxWidth().heightIn(min = 92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LitText("", sentence?.text ?: "", "", idx?.second ?: 0f,
                        style(20f, Face.Serif, color = c(Palette.LineGold), lineHeight = 1.45f, align = TextAlign.Center),
                        Look(design.ReadingThemes.Night, design.Tones.Lamp), Modifier.enter({ line }, Ease.Swap))
                    if (mix >= 40 && Audio.bedActive) Audio.englishLine()?.let { T(it, 13f, C.English.copy(alpha = .8f), Modifier.padding(top = 8.dp), lineHeight = 19f, align = TextAlign.Center) }
                }
                // scrubber
                Column(Modifier.padding(top = 10.dp)) {
                    Scrubber(p, 48, 38f, 30f, white(.14f)) { Audio.seek(it * Audio.duration) }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        T(clockText(Audio.position), 12f, C.Sub, tabular = true)
                        T("paragraph ${sentence?.number ?: 1}", 12f, C.Sub, tabular = true)
                        T("−" + clockText(Audio.duration - Audio.position), 12f, C.Sub, tabular = true)
                    }
                }
                // transport
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton({ Audio.stepParagraph(-1) }, "Previous paragraph", size = 48f, bg = Color.Transparent) { Ico(Icons.PrevPara, 24f, C.Text) }
                    BigPlay(Audio.playing, 80f, 26f) { Audio.toggle() }
                    RoundButton({ Audio.stepParagraph(1) }, "Next paragraph", size = 48f, bg = Color.Transparent) { Ico(Icons.NextPara, 24f, C.Text) }
                }
                // tools
                val tools = listOf(
                    Triple("voices", "Voices", "${100 - mix}/$mix"), Triple("speed", "Speed", speedLabel(Audio.speed)),
                    Triple("sleep", "Sleep", if (Audio.sleep > 0) "${Audio.sleep}m" else if (Audio.sleep < 0) "¶" else "☾"), Triple("lang", "Language", shortLang(iso)),
                )
                Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tools.forEach { (id, label, big) ->
                        val on = tool == id
                        val bg by animateColorAsState(if (on) gold(.16f) else white(.05f), tw(300), label = "tool")
                        val fg by animateColorAsState(if (on) C.Gold else C.Soft, tw(300), label = "toolFg")
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).press({ tool = if (on) "" else id }).padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) { S(big, 17f, fg, Modifier.height(18.dp), lineHeight = 1f); T(label, 11.5f, fg, weight = 600) }
                    }
                }
                val panelH = animate(when (tool) { "voices" -> 150f; "speed", "sleep" -> 56f; "lang" -> 170f; else -> 0f }, 450, Ease.Sheet)
                val op = animate(if (tool.isEmpty()) 0f else 1f, 300)
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(panelH.dp).graphicsLayer { alpha = op; clip = true }) {
                    when (tool) {
                        "voices" -> VoicesPanel(Tables.localName(iso))
                        "speed" -> Pills(listOf(.75f, 1f, 1.15f, 1.25f, 1.5f, 2f).map { speedLabel(it) to (Audio.speed == it) }, serif = true) { i -> Audio.setRate(listOf(.75f, 1f, 1.15f, 1.25f, 1.5f, 2f)[i]) }
                        "sleep" -> Pills(listOf(0 to "Off", 15 to "15 min", 30 to "30 min", -1 to "End of paragraph").map { (v, l) -> l to (Audio.sleep == v) }, serif = false) { i -> Audio.sleepFor(listOf(0, 15, 30, -1)[i]) }
                        "lang" -> Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tables.installed().forEach { lang ->
                                val sel = lang == iso
                                Row(
                                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(if (sel) gold(.14f) else white(.04f))
                                        .press({ scope.launch { if (!sel) { Reader.open(lang, Tables.sermon(lang, t.sermon.dateCode) ?: return@launch, sentence?.number ?: 1); val at = Audio.timeline(); Tables.sermon(lang, t.sermon.dateCode)?.let { Audio.play(lang, it) }; Audio.seek(Audio.recordingTime(at)) } } })
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    S(Tables.englishName(lang), 16f, C.Ink, Modifier.weight(1f))
                                    T(when { lang == "eng" -> "Brother Branham alone"; sel && Audio.bedActive -> "Playing, with English underneath"; sel -> "Playing"; else -> "Same place" }, 12.5f, if (sel) C.Gold else C.Meta)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (queue) QueueSheet(haze) { queue = false }
    }
}

internal fun speedLabel(v: Float) = (if (v % 1f == 0f) "${v.toInt()}" else "$v".trimEnd('0')) + "×"

/** The big gold play button with its ripple ring while playing. */
@Composable
internal fun BigPlay(playing: Boolean, size: Float, icon: Float, tv: Boolean = false, onClick: () -> Unit) {
    val time by clock()
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        if (playing) Box(Modifier.size((size + 16).dp).ripple({ time }, 2400).drawBehind { drawCircle(gold(.35f), style = Stroke(1.5.dp.toPx())) })
        Box(Modifier.cssShadow(0f, 18f, 44f, -10f, gold(.75f), CircleShape)) { GoldPlay(playing, size, icon, onClick, tv = tv) }
    }
}

@Composable
private fun Pills(items: List<Pair<String, Boolean>>, serif: Boolean, onPick: (Int) -> Unit) {
    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, (label, sel) ->
            Box(
                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(if (sel) C.Gold else white(.05f)).press({ onPick(i) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) { if (serif) S(label, 16f, if (sel) C.OnGold else C.Soft) else T(label, 13f, if (sel) C.OnGold else C.Soft, weight = 600, maxLines = 1) }
        }
    }
}

/** The mix track: gold to amber up to the balance point, then English blue. */
internal fun mixBrush(mix: Int): Brush {
    val pos = (mix - 10) / 80f
    return Brush.horizontalGradient(0f to C.Gold, pos to C.Amber, pos to C.English, 1f to C.EnglishDeep)
}

@Composable
internal fun VoicesPanel(langName: String) {
    val mix = Prefs.mix
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(white(.04f)).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            S("$langName ${100 - mix}%", 16f, C.Gold); S("$mix% English", 16f, C.English)
        }
        Slider(mix.toFloat(), 10f, 90f, 5f, { Prefs.updateMix(it.toInt()) }, Modifier.fillMaxWidth(), "Balance between $langName and English",
            track = { mixBrush(mix) }, trackH = 6f, trackTop = 15f)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).press({ Prefs.updateSplit(!Prefs.split) }, role = Role.Switch), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            T("$langName in the right ear, English in the left", 13f, C.Soft, Modifier.weight(1f))
            Switch(Prefs.split, 40f, 24f, 18f)
        }
    }
}

@Composable
private fun BoxScope.QueueSheet(haze: HazeState, onDismiss: () -> Unit) {
    val t = Audio.track ?: return
    val scope = rememberCoroutineScope()
    val list = remember(t) {
        val all = Tables.sermons(t.iso3)
        val i = all.indexOfFirst { it.dateCode == t.sermon.dateCode }
        val next = all.drop(i + 1).take(2).map { it to "${englishTitleOf(it.dateCode) ?: it.location}${it.minutes?.let { m -> ", ${minutesText(m)}" } ?: ""}" }
        val letter = Letters.book(Prefs.letterLang).all.value.firstOrNull() ?: Letters.book("en").all.value.firstOrNull()
        val sunday = letter?.let { l -> Tables.sermon(t.iso3, l.code)?.let { it to "For Sunday, from this week’s letter" } }
        next + listOfNotNull(sunday)
    }
    Sheet(onDismiss, haze, scrim = c(Palette.ScrimHome), padBottom = 24f, handleBottom = 14f, highlight = false, shadow = false) {
        S("Up next", 24f, C.Ink, Modifier.padding(bottom = 10.dp))
        list.forEach { (s, meta) ->
            Row(
                Modifier.fillMaxWidth().drawBehind { drawLine(white(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                    .press({ scope.launch { Audio.play(t.iso3, s); onDismiss() } }).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                T("≡", 16f, C.Faint)
                Column(Modifier.weight(1f)) { S(s.title, 17f, C.Ink); T(meta, 12.5f, C.Meta) }
            }
        }
    }
}

internal fun englishTitleOf(code: String) = if (Tables.has("eng")) Tables.sermon("eng", code)?.title else null
internal fun minutesText(m: Int) = if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"

