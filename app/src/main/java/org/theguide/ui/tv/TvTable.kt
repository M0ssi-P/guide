package org.theguide.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Palette
import kotlinx.coroutines.launch
import org.theguide.audio.Audio
import org.theguide.data.Prefs
import org.theguide.data.Reader
import org.theguide.data.Tables
import org.theguide.ui.*
import org.theguide.ui.phone.BigPlay
import org.theguide.ui.phone.Reel
import org.theguide.ui.phone.Scrubber
import org.theguide.ui.phone.VuMeter
import org.theguide.ui.phone.clockText
import org.theguide.ui.phone.englishTitle
import org.theguide.ui.phone.langOptions
import org.theguide.ui.phone.mixBrush
import org.theguide.ui.phone.pack
import org.theguide.ui.phone.paragraphs
import org.theguide.ui.phone.split

/** The TV's small round-cornered button (46 tall, 15 px semibold, on the theme's chip). */
@Composable
internal fun TvChip(label: String, onClick: () -> Unit, k: Look, modifier: Modifier = Modifier, serif: Boolean = false, height: Float = 46f, size: Float = 15f, bg: Color = k.chip, fg: Color = k.tx, role: Role = Role.Button) {
    val shape = RoundedCornerShape((height / 2).dp)
    Box(modifier.height(height.dp).tvFocus(shape, onClick, role).clip(shape).background(bg).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
        T(label, if (serif) 18f else size, fg, face = if (serif) Face.Serif else Face.Sans, weight = if (serif) 400 else 600)
    }
}

/** TVTable board. */
@Composable
fun TvTable() {
    LaunchedEffect(Unit) { Reader.ensure() }
    val ls = rememberLook("tv-table", 30)
    val k = ls.look
    var sheet by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val s = Reader.sermon
    val items = Reader.items
    val paras = remember(items) { paragraphs(items) }
    val here = Reader.audioHere
    val playing = here && Audio.playing
    val p = if (here && Audio.duration > 0) (Audio.position / Audio.duration).toFloat() else 0f
    val angle by spinAngle(2400f, playing)
    val idx = if (here) Audio.currentIndex() else null
    val cur = Reader.current
    val ci = paras.indexOfFirst { it.n == cur }.coerceAtLeast(0)
    val screen by oneShot(Unit, 600)
    val body by oneShot(Reader.flip, 550)
    val play = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { play.requestFocus() } }

    Box(Modifier.fillMaxSize().background(k.bg).ambient(k.glow, .25f, .35f)) {
        Box(Modifier.fillMaxSize().enter({ screen })) {
            Column(Modifier.padding(start = 120.dp, top = 40.dp, bottom = 40.dp).width(410.dp).fillMaxHeight()) {
                val shape = RoundedCornerShape(34.dp)
                Column(
                    Modifier.fillMaxWidth().cssShadow(0f, 30f, 60f, -30f, Color.Black.copy(alpha = .8f), shape).clip(shape)
                        .background(Brush.verticalGradient(listOf(c(Palette.DeckTop), c(Palette.DeckBottom)))).grain(.25f).topHighlight(white(.08f), shape)
                        .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 20.dp)
                ) {
                    Box(Modifier.fillMaxWidth().height(194.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Reel(150f, pack(1 - p, 28f, 44f), angle, 26f, tabs = false); Reel(150f, pack(p, 28f, 44f), angle, 26f, tabs = false)
                        }
                        Canvas(Modifier.fillMaxWidth().height(30.dp).offset(y = 140.dp)) {
                            val sx = size.width / 370f; val sy = size.height / 30f
                            val path = Path().apply { moveTo(20 * sx, 0f); quadraticTo(30 * sx, 26 * sy, 150 * sx, 26 * sy); lineTo(220 * sx, 26 * sy); quadraticTo(340 * sx, 26 * sy, 350 * sx, 0f) }
                            drawPath(path, c(Palette.Tape), style = Stroke(3.dp.toPx()))
                        }
                        Box(Modifier.align(Alignment.TopCenter).offset(y = 166.dp)) { VuMeter(playing, 20, 170f) }
                    }
                    S(s?.title ?: "", 32f, c(Palette.Title), Modifier.fillMaxWidth().padding(top = 14.dp), weight = 300, align = TextAlign.Center, maxLines = 1, ellipsis = true)
                    T("${s?.dateCode ?: ""}, ${s?.location ?: ""}", 14.5f, C.Sub, Modifier.fillMaxWidth().padding(top = 2.dp), align = TextAlign.Center, maxLines = 1)
                }
                Column(Modifier.padding(top = 18.dp)) {
                    Scrubber(p, 60, 34f, 26f, k.line, null)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        T(clockText(if (here) Audio.position else 0.0), 14f, k.sub, tabular = true)
                        T("−" + clockText(if (here) Audio.duration - Audio.position else (s?.minutes ?: 0) * 60.0), 14f, k.sub, tabular = true)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(60.dp).tvFocus(CircleShape, { if (here) Audio.stepParagraph(-1) else Reader.paragraph = (cur - 1).coerceAtLeast(1) }).clip(CircleShape).background(k.chip).label("Previous paragraph"), contentAlignment = Alignment.Center) { Ico(Icons.PrevPara, 24f, k.tx) }
                    Box(Modifier.focusRequester(play)) { BigPlay(playing, 84f, 28f, tv = true) { scope.launch { Reader.toggle() } } }
                    Box(Modifier.size(60.dp).tvFocus(CircleShape, { if (here) Audio.stepParagraph(1) else Reader.paragraph = cur + 1 }).clip(CircleShape).background(k.chip).label("Next paragraph"), contentAlignment = Alignment.Center) { Ico(Icons.NextPara, 24f, k.tx) }
                }
            }
            Column(Modifier.padding(start = 590.dp, end = 60.dp, top = 40.dp, bottom = 30.dp).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    TvChip("Sermons", { sheet = "pick" }, k)
                    TvChip(Tables.localName(Reader.iso3), { sheet = "lang" }, k)
                    TvChip("Voices ${100 - Prefs.mix}/${Prefs.mix}", { sheet = "voices" }, k)
                    TvChip("Aa", { sheet = "set" }, k, serif = true)
                }
                // the three paragraphs centred as on the board; a paragraph too long for the room scrolls so the spoken sentence stays in view
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                val area = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = area).enter({ body }, Ease.Swap), verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
                    listOf(-1, 0, 1).forEach { d ->
                        val para = paras.getOrNull(ci + d) ?: return@forEach
                        val on = d == 0
                        val (before, now, after) = para.split(idx?.first, items)
                        val op = animate(if (d < 0) .4f else if (d > 0) .6f else 1f, 600)
                        val fs = animate(if (on) ls.size.toFloat() else Math.round(ls.size * .72f).toFloat(), 500, Ease.Sheet)
                        val numCol by animateColorAsState(if (on) k.acc else k.sub, tw(400), label = "n")
                        Row(Modifier.graphicsLayer { alpha = op }, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            S("${para.n}", 17f, numCol, Modifier.width(40.dp).padding(top = 8.dp), align = TextAlign.End)
                            LitText(before, now, after, if (on && now.isNotEmpty()) idx?.second ?: 0f else 0f, style(fs, if (ls.serif) Face.Serif else Face.Sans, color = k.tx, lineHeight = ls.lineHeight), k, Modifier.weight(1f), follow = on)
                        }
                    }
                }
                }
            }
        }
        when (sheet) {
            "pick" -> TvPanel(k.sheet, { sheet = "" }) { SermonsPanel(k) { sheet = "" } }
            "lang" -> TvPanel(k.sheet, { sheet = "" }) {
                S("Language", 32f, k.tx)
                T("Text and voice change together. Your place is kept.", 15f, k.sub, Modifier.padding(top = 6.dp, bottom = 16.dp))
                langOptions().forEach { (iso3, meta, have) ->
                    val sel = iso3 == Reader.iso3
                    val dot = animate(if (sel) 1f else 0f, 300, Ease.O14)
                    Row(
                        Modifier.padding(bottom = 6.dp).fillMaxWidth().tvFocus(RoundedCornerShape(20.dp), { if (have) scope.launch { Reader.switchLanguage(iso3); sheet = "" } }, Role.RadioButton).clip(RoundedCornerShape(20.dp)).background(if (sel) k.chip else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(Modifier.weight(1f)) { S(Tables.localName(iso3), 21f, k.tx); T(meta, 14f, k.sub) }
                        Box(Modifier.size(14.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(k.acc))
                    }
                }
            }
            "voices" -> TvPanel(k.sheet, { sheet = "" }) { VoicesPanel(k, Tables.localName(Reader.iso3)) }
            "set" -> TvPanel(k.sheet, { sheet = "" }, 500f) { ReadingSettings(ls, 22..40, tv = true) }
        }
    }
}

@Composable
private fun ColumnScope.SermonsPanel(k: Look, onPicked: () -> Unit) {
    val scope = rememberCoroutineScope()
    val all = remember(Reader.iso3) { Tables.sermons(Reader.iso3) }
    val years = remember(all) { all.mapNotNull { it.dateCode.take(2).toIntOrNull()?.let { y -> 1900 + y } }.distinct().sorted() }
    var year by remember { mutableStateOf(Reader.sermon?.dateCode?.take(2)?.toIntOrNull()?.let { 1900 + it } ?: years.firstOrNull() ?: 1962) }
    S("Sermons", 32f, k.tx)
    androidx.compose.foundation.lazy.LazyRow(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(years.size) { i ->
            val y = years[i]; val sel = y == year
            Box(Modifier.height(46.dp).tvFocus(RoundedCornerShape(23.dp), { year = y }, Role.Tab).clip(RoundedCornerShape(23.dp)).background(if (sel) k.acc else k.chip).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                S("$y", 20f, if (sel) k.onAcc else k.tx)
            }
        }
    }
    Column(Modifier.padding(top = 14.dp)) {
        all.filter { it.dateCode.take(2).toIntOrNull()?.let { 1900 + it } == year }.forEach { s ->
            Row(
                Modifier.padding(bottom = 6.dp).fillMaxWidth().tvFocus(RoundedCornerShape(20.dp), { scope.launch { Reader.open(Reader.iso3, s, 1); onPicked() } }).clip(RoundedCornerShape(20.dp)).background(if (s.dateCode == Reader.sermon?.dateCode) k.chip else Color.Transparent)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                T(s.dateCode, 14f, k.sub, Modifier.width(80.dp), tabular = true)
                Column(Modifier.weight(1f)) { S(s.title, 21f, k.tx); T(englishTitle(s)?.takeIf { it != s.title } ?: s.location, 14f, k.sub) }
            }
        }
    }
    T("Go to paragraph", 15f, k.sub, Modifier.padding(top = 18.dp, bottom = 8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TvChip("−", { Reader.paragraph = (Reader.current - 1).coerceAtLeast(1); if (Reader.audioHere) Audio.playParagraph(Reader.paragraph) }, k, Modifier.width(52.dp), size = 22f)
        S("${Reader.current}", 34f, k.tx, Modifier.width(70.dp), align = TextAlign.Center)
        TvChip("+", { Reader.paragraph = Reader.current + 1; if (Reader.audioHere) Audio.playParagraph(Reader.paragraph) }, k, Modifier.width(52.dp), size = 22f)
    }
}

/** Voices: the balance with Brother Branham's English, nudged left and right, and the left/right placement. */
@Composable
internal fun ColumnScope.VoicesPanel(k: Look, lang: String) {
    val mix = Prefs.mix
    S("Voices", 32f, k.tx)
    T("Keep Brother Branham’s own voice quietly underneath. Left and right move the balance.", 15f, k.sub, Modifier.padding(top = 6.dp, bottom = 22.dp), lineHeight = 22f)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { S("$lang ${100 - mix}%", 22f, k.acc); S("$mix% English", 22f, C.English) }
    Slider(mix.toFloat(), 10f, 90f, 5f, { Prefs.updateMix(it.toInt()) }, Modifier.padding(top = 6.dp).fillMaxWidth(), "Balance with the English voice", track = { mixBrush(mix) }, trackH = 8f, trackTop = 16f, height = 40f, tv = true)
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TvChip("More $lang", { Prefs.updateMix(mix - 5) }, k, Modifier.weight(1f))
        TvChip("More English", { Prefs.updateMix(mix + 5) }, k, Modifier.weight(1f))
    }
    Row(
        Modifier.padding(top = 22.dp).fillMaxWidth().tvFocus(RoundedCornerShape(16.dp), { Prefs.updateSplit(!Prefs.split) }, Role.Switch).clip(RoundedCornerShape(16.dp)).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) { T("Separate voices left and right", 16f, k.tx, Modifier.weight(1f)); Switch(Prefs.split, 52f, 30f, 22f, 4f, Color(127, 127, 127).copy(alpha = .3f)) }
}

