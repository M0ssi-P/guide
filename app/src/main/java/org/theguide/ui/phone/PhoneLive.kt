package org.theguide.ui.phone

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlinx.coroutines.delay
import live.LiveHostInfo
import org.theguide.audio.Audio
import org.theguide.data.Prefs
import org.theguide.data.Tables
import org.theguide.live.LiveClient
import org.theguide.ui.*
import parsers.vgr.VgrMedia

internal val hostOrbs = listOf(c(Palette.HostWarm) to c(Palette.HostWarmDeep), c(Palette.HostCool) to c(Palette.HostCoolDeep))
internal fun initials(name: String) = name.trim().split(Regex("\\s+")).map { it.firstOrNull() ?: ' ' }.joinToString("").trim().take(2).uppercase().ifEmpty { "?" }
internal fun hostMeta(h: LiveHostInfo) = "Reading ${h.title}, ${h.listeners} listening"

/** A language's line on the language step: this sermon's title in it and whether it has a voice. */
@Composable
internal fun liveLangMeta(iso3: String, code: String): String {
    val meta by produceState("", iso3, code) {
        val s = Tables.sermon(iso3, code)
        value = when {
            iso3 == "eng" -> "Brother Branham himself"
            s == null -> "Not in this language yet"
            Audio.localFile(iso3, s) != null || runCatching { VgrMedia.catalogFiles(s.i, iso3).isNotEmpty() }.getOrDefault(false) -> "${s.title}, recorded voice"
            else -> "Text only for this sermon"
        }
    }
    return meta
}

/** PhoneLive board: join a desktop host (phones only join). */
@Composable
fun PhoneLive(haze: HazeState) {
    val ls = rememberLook("live", 24)
    val k = ls.look
    var step by remember { mutableStateOf(if (LiveClient.listening) "listen" else "find") }
    var flip by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf("") }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) { delay(2200); toast = "" } }
    LaunchedEffect(Unit) { LiveClient.discover() }
    val go = { s: String -> step = s; flip++ }
    val order = listOf("find", "name", "lang")
    val ix = order.indexOf(step)
    val swapA by oneShot(flip, 500)
    val listen = step == "listen"

    Box(Modifier.fillMaxSize().background(k.bg).screenGlow(if (listen) k.glow else gold(.12f))) {
        BoxWithConstraints(Modifier.fillMaxSize().hazeSource(haze).padding(top = statusTop())) {
            val minH = maxOf(780.dp, maxHeight)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = !listen)) {
                Column(Modifier.fillMaxWidth().heightIn(min = minH).padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 40.dp)) {
                    if (ix >= 0) HRow(gap = 12f) {
                        RoundButton({ if (ix > 0) go(order[ix - 1]) else Nav.go(Nav.Tab.Home) }, "Back", bg = k.chip) { Ico(Icons.Back, 16f, k.tx) }
                        StepBars(3, ix, Modifier.weight(1f))
                    }
                    Column(Modifier.weight(1f).enter({ swapA }, Ease.Swap)) {
                        when (step) {
                            "find" -> FindStep(k) { h -> LiveClient.connect(h); go("name") }
                            "name" -> NameStep(k) { go("lang") }
                            "lang" -> LangStep(k) { go("listen") }
                            else -> ListenStep(k, ls) { sheet = "leave" }
                        }
                    }
                }
            }
        }
        if (listen) Dock(haze, k, onTool = { id ->
            when (id) {
                "audio" -> { LiveClient.textOnly = !LiveClient.textOnly; toast = if (LiveClient.textOnly) "Following the words only" else "Voice is back, in step" }
                else -> sheet = id
            }
        }, onResync = { LiveClient.resync(); toast = "Back in step with ${LiveClient.host?.name ?: "the host"}" })
        if (toast.isNotEmpty() && listen) {
            val t by oneShot(toast, 450)
            Row(
                Modifier.align(Alignment.TopCenter).padding(top = statusTop() + 12.dp).pop({ t }, Ease.O13).height(38.dp)
                    .cssShadow(0f, 14f, 30f, -12f, Color.Black.copy(alpha = .8f), RoundedCornerShape(19.dp)).clip(RoundedCornerShape(19.dp)).background(C.Sheet).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { Ico(Icons.Tick, 14f, C.Gold); T(toast, 13f, C.Ink, maxLines = 1) }
        }
        val host = LiveClient.host?.name ?: "The host"
        when (sheet) {
            "lang" -> Sheet({ sheet = "" }, haze, k.sheet, k.line, base = k.bg) {
                S("Change language", 24f, k.tx)
                T("You stay in step with $host. The new voice picks up at the same sentence.", 13f, k.sub, Modifier.padding(top = 4.dp, bottom = 14.dp))
                Tables.installed().forEach { iso3 ->
                    LiveLangRow(iso3, iso3 == LiveClient.lang, k) {
                        LiveClient.switchLanguage(iso3); sheet = ""; toast = "${Tables.localName(iso3)}, same sentence"
                    }
                }
            }
            "set" -> Sheet({ sheet = "" }, haze, k.sheet, k.line, base = k.bg) {
                ReadingSettings(ls)
                T("Voices", 13f, k.sub, Modifier.padding(top = 20.dp, bottom = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    S("${Tables.localName(LiveClient.lang)} ${100 - Prefs.mix}%", 15f, k.acc); S("${Prefs.mix}% English", 15f, C.English)
                }
                Slider(Prefs.mix.toFloat(), 10f, 90f, 5f, { Prefs.updateMix(it.toInt()) }, Modifier.fillMaxWidth(), "Balance with the English voice", track = { mixBrush(Prefs.mix) }, trackH = 6f, trackTop = 15f)
            }
            "leave" -> Sheet({ sheet = "" }, haze, k.sheet, k.line, base = k.bg) {
                S("Leave the service?", 24f, k.tx)
                T("$host will see that you left. You can carry on alone from paragraph ${liveParagraph()}, or come back any time while it is live.", 14f, k.sub, Modifier.padding(top = 6.dp, bottom = 18.dp), lineHeight = 21f)
                GoldButton("Leave and keep reading here", { LiveClient.leave(keep = true); sheet = ""; go("find"); Nav.player = true }, Modifier.fillMaxWidth())
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(52.dp).press({ LiveClient.leave(keep = false); sheet = ""; go("find") }), contentAlignment = Alignment.Center) { T("Leave", 15f, C.Coral, weight = 600) }
                Box(Modifier.fillMaxWidth().height(44.dp).press({ sheet = "" }), contentAlignment = Alignment.Center) { T("Stay", 14f, k.sub) }
            }
        }
    }
}

internal fun liveParagraph(): Int {
    val s = LiveClient.state ?: return 1
    val items = LiveClient.items
    return items.getOrNull(LiveClient.locateHost(items, s).first)?.number ?: s.paragraph ?: 1
}

@Composable
private fun ColumnScope.FindStep(k: Look, onHost: (LiveHostInfo) -> Unit) {
    var code by remember { mutableStateOf("") }
    val time by clock()
    S("Join a service", 36f, k.tx, Modifier.padding(top = 22.dp), weight = 300, tracking = -.4f)
    T("Looking on your Wi-Fi", 14f, k.sub, Modifier.padding(top = 6.dp))
    Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
        repeat(3) { i -> Box(Modifier.requiredSize(110.dp).ripple({ time }, 2600, -i * 850).drawBehind { drawCircle(gold(.55f), style = Stroke(1.5.dp.toPx())) }) }
        Box(Modifier.size(64.dp).cssShadow(0f, 0f, 50f, 0f, gold(.55f), CircleShape).clip(CircleShape)
            .drawBehind { drawCircle(Brush.radialGradient(0f to C.Flame, 1f to C.Amber, center = Offset(size.width * .35f, size.height * .3f), radius = size.width)) })
    }
    LiveClient.hosts.forEachIndexed { i, h ->
        val t by oneShot(h.address, 600, i * 250)
        val (o1, o2) = hostOrbs[i % 2]
        Row(
            Modifier.padding(bottom = 8.dp).pop({ t }, Ease.O10).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c(Palette.Card))
                .insetRing(1.dp, if (i == 0) gold(.3f) else white(.06f), RoundedCornerShape(24.dp)).press({ onHost(h) }).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).drawBehind { drawRect(Brush.radialGradient(0f to o1, 1f to o2, center = Offset(size.width * .3f, size.height * .3f), radius = size.width)) }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).beat({ time }, 1400).clip(CircleShape).background(Color.White))
            }
            Column(Modifier.weight(1f)) { T(h.name, 16f, C.Text, weight = 600); T(hostMeta(h), 13f, C.Sub) }
            Ico(Icons.HostArrow, 18f, C.Gold)
        }
    }
    if (LiveClient.hosts.isEmpty()) T("No one is sharing on this Wi-Fi yet. On the computer, open Live and start sharing.", 13f, k.sub, Modifier.fillMaxWidth().padding(vertical = 8.dp), align = TextAlign.Center, lineHeight = 19f)
    T("Or type the live ID from the screen", 13f, k.sub, Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), align = TextAlign.Center)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
        repeat(4) { i ->
            val ring by animateColorAsState(if (i == code.length) gold(.7f) else white(.08f), tw(300), label = "id")
            Box(Modifier.size(52.dp, 62.dp).clip(RoundedCornerShape(16.dp)).background(white(.06f)).insetRing(1.5.dp, ring, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                S(code.getOrNull(i)?.toString() ?: "", 28f, C.Ink)
            }
        }
    }
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del").chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (key.isEmpty()) Color.Transparent else white(.05f))
                            .then(if (key.isEmpty()) Modifier else Modifier.press({
                                code = if (key == "del") code.dropLast(1) else (code + key).take(4)
                                if (code.length == 4) LiveClient.byCode(code)?.let(onHost)
                            }, label = if (key == "del") "Delete" else key)),
                        contentAlignment = Alignment.Center,
                    ) { S(if (key == "del") "⌫" else key, 21f, C.Ink) }
                }
            }
        }
    }
    if (code.length == 4 && LiveClient.byCode(code) == null) T("No service with that ID on this Wi-Fi.", 13f, C.CoralSoft, Modifier.fillMaxWidth().padding(top = 10.dp), align = TextAlign.Center)
}

@Composable
private fun ColumnScope.NameStep(k: Look, onNext: () -> Unit) {
    var name by remember { mutableStateOf(Prefs.liveName) }
    val time by clock()
    val init = initials(name)
    val t by oneShot(init, 350)
    val host = LiveClient.host
    S("What should we call you?", 36f, k.tx, Modifier.padding(top = 22.dp), weight = 300, tracking = -.4f)
    T("${host?.name ?: "The host"} sees who is listening, so they know the room is with them.", 14f, k.sub, Modifier.padding(top = 6.dp), lineHeight = 20f)
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.requiredSize(150.dp).halo({ time }, 3000).background(Brush.radialGradient(0f to gold(.35f), .65f to Color.Transparent), CircleShape))
        Box(
            Modifier.size(104.dp).then(if (init.length % 2 == 1) Modifier.pop({ t }, Ease.O13) else Modifier.enter({ t }, Ease.O13))
                .cssShadow(0f, 20f, 40f, -14f, gold(.7f), CircleShape).clip(CircleShape)
                .drawBehind { drawCircle(Brush.radialGradient(0f to C.Flame, .6f to C.Amber, 1f to c(Palette.Copper), center = Offset(size.width * .35f, size.height * .3f), radius = size.width)) },
            contentAlignment = Alignment.Center,
        ) { S(init, 42f, c(Palette.Initials)) }
    }
    Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(white(.07f)).insetRing(1.5.dp, gold(.4f), RoundedCornerShape(20.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        if (name.isEmpty()) S("Your name", 22f, Color(0xFF757575))
        BasicTextField(name, { name = it.take(24) }, Modifier.fillMaxWidth(), textStyle = style(22f, Face.Serif, color = C.Ink, align = TextAlign.Center), singleLine = true, cursorBrush = SolidColor(C.Ink))
    }
    host?.takeIf { it.listeners > 0 }?.let { h ->
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Row { listOf(c(Palette.HostWarm), c(Palette.HostCool), c(Palette.Sage), C.Gold).take(minOf(4, h.listeners)).forEach { col ->
                Box(Modifier.offset(x = (-8).dp).size(28.dp).drawBehind { drawCircle(k.bg, size.width / 2 + 2.dp.toPx()) }.clip(CircleShape).background(col))
            } }
            T("${h.listeners} ${if (h.listeners == 1) "person is" else "people are"} in", 13f, k.sub)
        }
    }
    Box(Modifier.weight(1f))
    GoldButton("Continue", { Prefs.liveName = name.trim().ifBlank { "Friend" }; onNext() }, Modifier.padding(top = 24.dp).fillMaxWidth())
}

@Composable
private fun LiveLangRow(iso3: String, sel: Boolean, k: Look, big: Boolean = true, onClick: () -> Unit) {
    val code = LiveClient.state?.code ?: LiveClient.host?.code ?: ""
    val dot = animate(if (sel) 1f else 0f, 350, Ease.O14)
    val bg by animateColorAsState(if (sel) gold(.12f) else k.chip, tw(300), label = "lg")
    val ring by animateColorAsState(if (sel) gold(.6f) else Color.Transparent, tw(300), label = "lgr")
    Row(
        Modifier.padding(bottom = if (big) 8.dp else 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).insetRing(1.5.dp, ring, RoundedCornerShape(20.dp))
            .press(onClick, role = Role.RadioButton).padding(horizontal = 16.dp, vertical = if (big) 14.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) { S(Tables.localName(iso3), 18f, if (big) C.Ink else k.tx); T(liveLangMeta(iso3, code), 12.5f, if (big) C.Meta else k.sub) }
        Box(Modifier.size(22.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(C.Gold), contentAlignment = Alignment.Center) { Ico(Icons.Tick, 12f, C.OnGold) }
    }
}

@Composable
private fun ColumnScope.LangStep(k: Look, onJoined: () -> Unit) {
    var lang by remember { mutableStateOf(Prefs.readingLang.takeIf { Tables.has(it) } ?: Tables.installed().firstOrNull() ?: "eng") }
    var joining by remember { mutableStateOf(false) }
    val host = LiveClient.host
    LaunchedEffect(joining) { if (joining) { delay(1400); LiveClient.join(lang); joining = false; onJoined() } }
    S("Hear it in", 36f, k.tx, Modifier.padding(top = 22.dp), weight = 300, tracking = -.4f)
    T("${host?.name ?: "The host"} is reading ${LiveClient.state?.title ?: host?.title ?: "a sermon"}. You can change this later.", 14f, k.sub, Modifier.padding(top = 6.dp, bottom = 18.dp))
    Tables.installed().forEach { iso3 -> LiveLangRow(iso3, iso3 == lang, k) { lang = iso3 } }
    Box(Modifier.weight(1f))
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
        val w = animate(if (joining) 56f else maxWidth.value, 450, Ease.Sheet)
        Box(
            Modifier.width(w.dp).height(56.dp).cssShadow(0f, 14f, 36f, -12f, gold(.7f), RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).background(C.Gold)
                .press({ if (!joining) joining = true }),
            contentAlignment = Alignment.Center,
        ) {
            if (joining) { val a by spinAngle(800f, true); Box(Modifier.size(22.dp).graphicsLayer { rotationZ = a }.drawBehind {
                drawCircle(C.OnGold.copy(alpha = .25f), style = Stroke(3.dp.toPx())); drawArc(C.OnGold, -90f, 90f, false, style = Stroke(3.dp.toPx()))
            }) } else T("Join as ${Prefs.liveName.ifBlank { "Friend" }}", 16f, C.OnGold, weight = 700, maxLines = 1)
        }
    }
}

@Composable
private fun ColumnScope.ListenStep(k: Look, ls: LookState, onLeave: () -> Unit) {
    val time by clock()
    val host = LiveClient.host
    val s = LiveClient.state
    val items = LiveClient.items
    val (i, p) = if (s != null && items.isNotEmpty()) LiveClient.locateHost(items, s) else 0 to 0f
    HRow(Modifier.padding(top = 6.dp), gap = 10f) {
        Box(Modifier.size(9.dp)) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(C.Coral))
            Box(Modifier.fillMaxSize().ripple({ time }, 1800).clip(CircleShape).background(C.Coral))
        }
        T("With ${host?.name ?: "the host"}, ${host?.listeners ?: 0} listening", 13.5f, k.sub, Modifier.weight(1f))
        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(k.chip).press(onLeave).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { T("Leave", 13f, k.tx, weight = 600) }
    }
    S("${Tables.sermon(LiveClient.lang, s?.code ?: "")?.title ?: s?.title ?: ""}, paragraph ${items.getOrNull(i)?.number ?: s?.paragraph ?: 1}", 14f, k.sub, Modifier.padding(top = 18.dp), italic = true)
    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp, bottom = 210.dp), verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
        if (items.isEmpty()) S(if (s == null) "Waiting for ${host?.name ?: "the host"}…" else "This sermon isn’t in ${Tables.localName(LiveClient.lang)} on this phone.", 20f, k.sub, italic = true)
        listOf(-1, 0, 1).forEach { d ->
            val it = items.getOrNull(i + d) ?: return@forEach
            val now = d == 0
            val fs = if (now) ls.size.toFloat() else Math.round(ls.size * .8f).toFloat()
            val col by animateColorAsState(if (now) k.tx else k.sub, tw(700), label = "ln")
            val op = animate(if (d < 0) .45f else if (d > 0) .6f else 1f, 700)
            LitText("", it.text, "", if (d < 0) 1f else if (now) p else 0f,
                style(fs, if (ls.serif) Face.Serif else Face.Sans, if (now) 400 else 300, color = col, lineHeight = 1.4f), k, Modifier.graphicsLayer { alpha = op })
        }
    }
}

@Composable
private fun BoxScope.Dock(haze: HazeState, k: Look, onTool: (String) -> Unit, onResync: () -> Unit) {
    val shape = RoundedCornerShape(34.dp)
    val time by clock()
    var syncing by remember { mutableStateOf(false) }
    LaunchedEffect(syncing) { if (syncing) { delay(1200); syncing = false } }
    val textOnly = LiveClient.textOnly
    val host = LiveClient.host?.name ?: "the host"
    Column(
        Modifier.align(Alignment.BottomCenter).padding(start = 10.dp, end = 10.dp, bottom = 22.dp + (bottomInset() - 8.dp).coerceAtLeast(0.dp)).fillMaxWidth()
            .cssShadow(0f, 24f, 40f, -20f, Color.Black.copy(alpha = .9f), shape).clip(shape)
            .hazeEffect(haze, HazeStyle(k.bg, HazeTint(c(Palette.LiveDock)), 20.dp, noiseFactor = 0f)).topHighlight(white(.1f), shape)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
    ) {
        if (!textOnly) {
            HRow(gap = 12f) {
                Row(Modifier.width(44.dp).height(26.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(0 to C.Gold, -300 to C.Gold, -600 to C.Gold, -150 to C.English, -450 to C.Gold).forEach { (off, col) ->
                        Box(Modifier.size(3.dp, 26.dp).wave({ time }, 1000, off).clip(RoundedCornerShape(2.dp)).background(col))
                    }
                }
                Column(Modifier.weight(1f)) {
                    S(Tables.localName(LiveClient.lang), 15.5f, C.Ink)
                    T(if (syncing) "Finding your place" else "In step with the host, ${Math.round(Audio.volume * 100)}%", 12f, C.Sub)
                }
                val a by spinAngle(800f, syncing)
                Row(
                    Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(white(.08f)).press({ syncing = true; onResync() }, label = "Get back in step with the host").padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) { Ico(Icons.Resync, 14f, C.Text, Modifier.graphicsLayer { rotationZ = if (syncing) a else 0f }); T("Resync", 12.5f, C.Text, weight = 600) }
            }
            HRow(Modifier.padding(top = 6.dp), gap = 10f) {
                Ico(Icons.VolumeLow, 18f, C.Sub)
                Slider(Audio.volume * 100, 0f, 100f, 1f, { Audio.volume = it / 100f }, Modifier.weight(1f), "Volume",
                    track = { f -> Brush.horizontalGradient(0f to C.Gold, f to C.Gold, f to white(.14f), 1f to white(.14f)) })
                Ico(Icons.VolumeHigh, 18f, C.Sub)
            }
        } else HRow(Modifier.padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 8.dp), gap = 12f) {
            S("¶", 24f, C.Gold)
            Column(Modifier.weight(1f)) { S("Following the words", 15.5f, C.Ink); T("The text keeps pace with $host, no sound", 12f, C.Sub) }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("audio", if (textOnly) "Audio back" else "Words only", if (textOnly) Icons.AudioBack else Icons.WordsOnly),
                Triple("lang", "Language", Icons.Language), Triple("set", "Settings", Icons.Settings), Triple("leave", "Leave", Icons.Leave),
            ).forEach { (id, label, icon) ->
                val on = id == "audio" && textOnly
                val fg = if (id == "leave") C.CoralSoft else if (on) C.Gold else C.Soft
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (on) gold(.16f) else white(.05f)).press({ onTool(id) }).padding(top = 9.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) { Ico(icon, 19f, fg); T(label, 11f, fg, weight = 600) }
            }
        }
    }
}
