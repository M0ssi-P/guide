package org.theguide.ui.phone

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.theguide.audio.Audio
import org.theguide.data.Prefs
import org.theguide.data.Reader
import org.theguide.data.Sentence
import org.theguide.data.Tables
import org.theguide.ui.*
import parsers.vgr.VgrMedia
import parsers.vgr.models.Sermon
import store.FileStore

/** "1:07:12" / "41:05". */
internal fun clockText(sec: Double): String {
    val x = sec.toLong().coerceAtLeast(0); val h = x / 3600; val m = x % 3600 / 60; val s = x % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** A paragraph's text split around the sentence being spoken, and how far into it. */
internal data class Para(val n: Int, val sentences: List<Sentence>)

internal fun paragraphs(items: List<Sentence>) = items.groupBy { it.number }.map { (n, s) -> Para(n, s) }

internal fun Para.split(curIndex: Int?, items: List<Sentence>): Triple<String, String, String> {
    val i = curIndex?.let { items.getOrNull(it) }?.takeIf { it.number == n } ?: return Triple(sentences.joinToString(" ") { it.text }, "", "")
    val k = sentences.indexOf(i)
    return Triple(sentences.take(k).joinToString(" ") { it.text }.let { if (it.isEmpty()) it else "$it " }, i.text, sentences.drop(k + 1).joinToString(" ") { it.text }.let { if (it.isEmpty()) it else " $it" })
}

/** Two short letters for a language chip ("Sw"). */
internal fun shortLang(iso3: String) = Tables.tag(iso3).take(2).replaceFirstChar { it.uppercase() }

/** PhoneTable board. */
@Composable
fun PhoneTable(haze: HazeState) {
    LaunchedEffect(Unit) { Reader.ensure() }
    val ls = rememberLook("table", 19)
    val k = ls.look
    var sheet by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val sermon = Reader.sermon
    val items = Reader.items
    val paras = remember(items) { paragraphs(items) }
    val idx = if (Reader.audioHere) Audio.currentIndex() else null
    val cur = Reader.current
    val list = rememberLazyListState()
    LaunchedEffect(cur, paras) { paras.indexOfFirst { it.n == cur }.takeIf { it >= 0 }?.let { list.animateScrollToItem(it + 1, -120) } }
    val screen by oneShot(Unit, 500)
    val body by oneShot(Reader.flip, 550)
    val playing = Reader.audioHere && Audio.playing

    Box(Modifier.fillMaxSize().background(k.bg).screenGlow(k.glow)) {
        Column(Modifier.fillMaxSize().hazeSource(haze).padding(top = statusTop()).enter({ screen })) {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundButton({ Nav.go(Nav.Tab.Home) }, "Back", bg = k.chip) { Ico(Icons.Back, 16f, k.tx) }
                Column(Modifier.weight(1f).press({ sheet = "pick" }, label = "Choose a sermon or paragraph"), horizontalAlignment = Alignment.CenterHorizontally) {
                    S(sermon?.title ?: "", 18f, k.tx, maxLines = 1, ellipsis = true)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        T("${sermon?.dateCode ?: ""}, paragraph $cur", 12f, k.sub)
                        Ico(Icons.Down, 10f, k.sub)
                    }
                }
                RoundButton({ sheet = "lang" }, "Language, ${Tables.localName(Reader.iso3)}", bg = k.chip) { T(shortLang(Reader.iso3), 12.5f, k.tx, weight = 600) }
                RoundButton({ sheet = "set" }, "Reading settings", bg = k.chip) { S("Aa", 16f, k.tx) }
            }
            LazyColumn(Modifier.fillMaxSize().enter({ body }, Ease.Swap), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 150.dp)) {
                item {
                    S(sermon?.title ?: "", 34f, k.tx, weight = 300, lineHeight = 1.1f, tracking = -.4f)
                    S(listOfNotNull(englishTitle(sermon)?.takeIf { it != sermon?.title }, sermon?.location).joinToString(", "), 14.5f, k.sub,
                        Modifier.padding(top = 8.dp, bottom = 24.dp), italic = true)
                }
                itemsIndexed(paras, key = { _, p -> p.n }) { _, p ->
                    val on = p.n == cur
                    val op by animateFloatAsState(if (p.n < cur) .55f else 1f, tw(500), label = "op")
                    val (before, now, after) = p.split(idx?.first, items)
                    Row(Modifier.padding(bottom = 18.dp).graphicsLayer { alpha = op }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val numColor by androidx.compose.animation.animateColorAsState(if (on) k.acc else k.sub, tw(400), label = "n")
                        S("${p.n}", 13f, numColor, Modifier.width(30.dp).padding(top = 4.dp).press({ scope.launch { Reader.playFrom(p.n) } }, label = "Play from paragraph ${p.n}"), align = TextAlign.End)
                        LitText(before, now, after, if (on && now.isNotEmpty()) idx?.second ?: 0f else 0f,
                            style(ls.size.toFloat(), if (ls.serif) Face.Serif else Face.Sans, color = k.tx, lineHeight = ls.lineHeight), k, Modifier.weight(1f))
                    }
                }
            }
        }
        TableDeck(haze, k, cur, playing) { scope.launch { Reader.toggle() } }
        when (sheet) {
            "pick" -> PickSheet(haze, k) { sheet = "" }
            "lang" -> LangSheet(haze, k) { sheet = "" }
            "set" -> Sheet({ sheet = "" }, haze, k.sheet, k.line, base = k.bg) { ReadingSettings(ls) }
        }
    }
}

/** The English title of a translated sermon, from the English Table when it is on the device. */
@Composable
internal fun englishTitle(s: Sermon?): String? {
    val code = s?.dateCode ?: return null
    return remember(code) { if (Tables.has("eng")) Tables.sermon("eng", code)?.title else null }
}

/** Two small spinning reels (22 px), the deck's. */
@Composable
internal fun DeckReels(running: Boolean, size: Float = 22f) {
    val a by spinAngle(2400f, running)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) {
            androidx.compose.foundation.Canvas(Modifier.size(size.dp).graphicsLayer { rotationZ = a }) {
                val k = this.size.width / 24f; val col = c(Palette.ReelSpoke)
                drawCircle(col, 10 * k, style = Stroke(2 * k)); drawCircle(col, 3 * k)
                listOf(Offset(12f, 2f) to Offset(12f, 7f), Offset(12f, 17f) to Offset(12f, 22f), Offset(2f, 12f) to Offset(7f, 12f), Offset(17f, 12f) to Offset(22f, 12f))
                    .forEach { (a1, b1) -> drawLine(col, a1 * k, b1 * k, 2 * k) }
            }
        }
    }
}

/** The gold play button with the warm gradient (radial at 35% 30%). */
@Composable
internal fun GoldPlay(playing: Boolean, size: Float, icon: Float, onClick: () -> Unit, modifier: Modifier = Modifier, tv: Boolean = false) {
    Box(
        modifier.size(size.dp).clip(CircleShape)
            .drawBehind { drawCircle(Brush.radialGradient(0f to C.Flame, .45f to C.Gold, 1f to C.Honey, center = Offset(this.size.width * .35f, this.size.height * .3f), radius = this.size.width * .95f)) }
            .choiceOrFocus(tv, CircleShape, onClick).label(if (playing) "Pause" else "Play"),
        contentAlignment = Alignment.Center,
    ) { Ico(if (playing) Icons.Pause else Icons.Play, icon, C.OnGold) }
}

@Composable
private fun BoxScope.TableDeck(haze: HazeState, k: Look, cur: Int, playing: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(36.dp)
    val total = Reader.items.lastOrNull()?.number ?: 1
    val progress = animate(if (Reader.audioHere && Audio.duration > 0) (Audio.position / Audio.duration).toFloat() else cur / total.toFloat(), 450, Ease.Linear)
    val meta = Tables.localName(Reader.iso3) + if (Reader.audioHere && Audio.duration > 0) ", ${clockText(Audio.duration - Audio.position)} left" else Reader.sermon?.minutes?.let { ", $it min" } ?: ""
    Row(
        Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 24.dp + (bottomInset() - 8.dp).coerceAtLeast(0.dp)).fillMaxWidth().height(72.dp)
            .cssShadow(0f, 24f, 40f, -20f, Color.Black.copy(alpha = .8f), shape).clip(shape)
            .hazeEffect(haze, HazeStyle(k.bg, HazeTint(k.deck), 18.dp, noiseFactor = 0f)).topHighlight(white(.1f), shape)
            .drawBehind { drawRect(k.acc, Offset(0f, size.height - 2.dp.toPx()), androidx.compose.ui.geometry.Size(size.width * progress, 2.dp.toPx())) }
            .press({ if (Reader.audioHere) Nav.player = true }).padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeckReels(playing)
        Column(Modifier.weight(1f)) {
            S("Paragraph $cur", 15.5f, C.Ink)
            T(meta, 12f, C.Sub)
        }
        GoldPlay(playing, 52f, 20f, onToggle)
    }
}

@Composable
private fun BoxScope.PickSheet(haze: HazeState, k: Look, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val all = remember(Reader.iso3) { Tables.sermons(Reader.iso3) }
    val english = remember { if (Tables.has("eng")) Tables.sermons("eng").associateBy { it.dateCode } else emptyMap() }
    val years = remember(all) { all.mapNotNull { it.dateCode.take(2).toIntOrNull()?.let { y -> 1900 + y } }.distinct().sorted() }
    var year by remember { mutableStateOf(Reader.sermon?.dateCode?.take(2)?.toIntOrNull()?.let { 1900 + it } ?: years.firstOrNull() ?: 1962) }
    val q = query.trim().lowercase()
    val found = all.filter { s ->
        if (q.isNotEmpty()) "${s.dateCode} ${s.title} ${english[s.dateCode]?.title ?: ""}".lowercase().contains(q)
        else s.dateCode.take(2).toIntOrNull()?.let { 1900 + it } == year
    }.take(200)
    val local = remember { mutableStateMapOf<String, Boolean>() }
    Sheet(onDismiss, haze, k.sheet, k.line, base = k.bg) {
        S("Find a sermon", 24f, k.tx)
        SearchField(query, { query = it }, "Title, date code or words", Modifier.padding(top = 14.dp), height = 48f, bg = k.chip, ink = k.tx, iconColor = k.tx, iconSize = 17f)
        val yearScroll = rememberScrollState()
        LaunchedEffect(Unit) { yearScroll.scrollTo((years.indexOf(year) - 2).coerceAtLeast(0) * 70) }
        Row(Modifier.padding(top = 14.dp).offset(x = (-20).dp).width(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp).horizontalScroll(yearScroll).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            years.forEach { y ->
                val sel = q.isEmpty() && y == year
                val dot = animate(if (sel) 1f else 0f, 350, Ease.O14)
                Box(Modifier.press({ year = y; query = "" }, role = Role.Tab).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
                    S("$y", 20f, if (sel) k.tx else k.sub)
                    Box(Modifier.align(Alignment.BottomCenter).offset(y = 8.dp).size(6.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(k.acc))
                }
            }
        }
        Column(Modifier.padding(top = 6.dp).heightIn(min = 250.dp)) {
            found.forEachIndexed { j, s ->
                val t by oneShot(found.size to j, 450, j * 50)
                LaunchedEffect(s.dateCode) { local[s.dateCode] = Audio.localFile(Reader.iso3, s) != null }
                Row(
                    Modifier.rise({ t }, Ease.Swap).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (s.dateCode == Reader.sermon?.dateCode) k.chip else Color.Transparent)
                        .press({ scope.launch { Reader.open(Reader.iso3, s, 1); onDismiss() } }).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    T(s.dateCode, 12f, k.sub, Modifier.width(66.dp), tabular = true)
                    Column(Modifier.weight(1f)) {
                        S(s.title, 17f, k.tx)
                        T(english[s.dateCode]?.title?.takeIf { it != s.title } ?: s.location, 12f, k.sub)
                    }
                    if (local[s.dateCode] == true) Ico(Icons.TickThin, 16f, k.acc)
                }
            }
            if (found.isEmpty()) S("Nothing by that name in ${if (q.isNotEmpty()) "any year" else "$year"}.", 16f, k.sub, Modifier.fillMaxWidth().padding(vertical = 30.dp), italic = true, align = TextAlign.Center)
        }
        Row(
            Modifier.padding(top = 14.dp).fillMaxWidth().drawBehind { drawLine(k.hair, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            T("Go to paragraph", 14f, k.tx, Modifier.weight(1f))
            RoundButton({ Reader.paragraph = (Reader.current - 1).coerceAtLeast(1); if (Reader.audioHere) Audio.playParagraph(Reader.paragraph) }, "Earlier paragraph", bg = k.chip) { T("−", 16f, k.tx) }
            S("${Reader.current}", 24f, k.tx, Modifier.width(44.dp), align = TextAlign.Center)
            RoundButton({ Reader.paragraph = Reader.current + 1; if (Reader.audioHere) Audio.playParagraph(Reader.paragraph) }, "Later paragraph", bg = k.chip) { T("+", 16f, k.tx) }
        }
    }
}

/** Every Table language: those on the device (with or without this sermon's voice) and those that can be downloaded. */
@Composable
internal fun langOptions(): List<Triple<String, String, Boolean>> {
    val code = Reader.sermon?.dateCode
    val offers by produceState(emptyList<String>()) { value = runCatching { FileStore.list("table") }.getOrDefault(Tables.installed()) }
    val metas = remember(code) { mutableStateMapOf<String, String>() }
    val all = (Tables.installed() + offers).distinct()
    all.forEach { iso3 ->
        LaunchedEffect(iso3, code) {
            metas[iso3] = when {
                !Tables.has(iso3) -> "Download text and voice, ${FileStore.size(FileStore.tableUrl(iso3))?.let { Math.round(it / 1e6) } ?: "?"} MB"
                Reader.sermon == null -> "Text"
                else -> {
                    val s = Tables.sermon(iso3, code ?: "")
                    when {
                        s == null -> "This sermon isn’t in this language yet"
                        Audio.localFile(iso3, s) != null -> "Text and voice, downloaded"
                        else -> runCatching { VgrMedia.catalogFiles(s.i, iso3).firstOrNull()?.size }.getOrNull()
                            ?.let { "Text only, voice is ${Math.round(it / 1e6)} MB" } ?: "Text only"
                    }
                }
            }
        }
    }
    return all.map { Triple(it, metas[it] ?: "", Tables.has(it)) }
}

@Composable
private fun BoxScope.LangSheet(haze: HazeState, k: Look, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val dl = remember { mutableStateMapOf<String, Int>() }
    Sheet(onDismiss, haze, k.sheet, k.line, base = k.bg) {
        S("Language", 24f, k.tx)
        T("The text and the voice change together. Your place is kept.", 13f, k.sub, Modifier.padding(top = 4.dp, bottom = 14.dp))
        langOptions().forEach { (iso3, meta, have) ->
            val sel = iso3 == Reader.iso3
            val dot = animate(if (sel) 1f else 0f, 300, Ease.O14)
            Row(
                Modifier.padding(bottom = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (sel) k.chip else Color.Transparent)
                    .press({
                        if (have) scope.launch { Reader.switchLanguage(iso3); onDismiss() }
                        else if (dl[iso3] == null) scope.launch {
                            dl[iso3] = 0
                            runCatching { Tables.download(iso3) { r, t -> if (t > 0) dl[iso3] = (r * 100 / t).toInt() } }
                            dl.remove(iso3)
                            Prefs.tableLangs = Prefs.tableLangs + iso3
                        }
                    }, role = Role.RadioButton).padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    S(Tables.localName(iso3), 18f, k.tx)
                    T(dl[iso3]?.let { "Downloading $it%" } ?: meta, 12f, k.sub)
                }
                Box(Modifier.size(20.dp).drawBehind { drawCircle(if (sel) k.acc else k.line, size.width / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx())) }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(10.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(k.acc))
                }
            }
        }
    }
}

