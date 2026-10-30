package org.theguide.ui.tv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import design.Palette
import kotlinx.coroutines.delay
import live.LiveHostInfo
import org.theguide.audio.Audio
import org.theguide.data.Prefs
import org.theguide.data.Tables
import org.theguide.live.LiveClient
import org.theguide.ui.*
import org.theguide.ui.phone.hostMeta
import org.theguide.ui.phone.hostOrbs
import org.theguide.ui.phone.initials
import org.theguide.ui.phone.liveLangMeta
import org.theguide.ui.phone.liveParagraph
import org.theguide.ui.phone.mixBrush

/** TVJoin board: the TV joins a host (it never hosts). */
@Composable
fun TvJoin() {
    var step by remember { mutableStateOf(if (LiveClient.listening) "listen" else "find") }
    var flip by remember { mutableStateOf(0) }
    var ctrl by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf("") }
    var textSize by remember { mutableStateOf(1) }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) { delay(2200); toast = "" } }
    LaunchedEffect(Unit) { LiveClient.discover() }
    val go = { s: String -> step = s; flip++ }
    val order = listOf("find", "name", "lang"); val ix = order.indexOf(step)
    val swapA by oneShot(flip, 550)
    val host = LiveClient.host?.name ?: "the host"

    Box(Modifier.fillMaxSize().background(C.Backdrop).ambient(gold(.14f), if (step == "listen") .5f else .7f, if (step == "listen") .5f else .4f)) {
        if (ix >= 0) StepBars(3, ix, Modifier.offset(130.dp, 40.dp).width(300.dp), 5f, 8f)
        Box(Modifier.fillMaxSize().enter({ swapA }, Ease.Css)) {
            when (step) {
                "find" -> Find { h -> LiveClient.connect(h); go("name") }
                "name" -> Name { go("lang") }
                "lang" -> Lang { go("listen"); ctrl = false }
                else -> Listen(ctrl, textSize) { ctrl = !ctrl }
            }
        }
        if (step == "listen" && ctrl) Controls({ id ->
            when (id) {
                "audio" -> { LiveClient.textOnly = !LiveClient.textOnly; toast = if (LiveClient.textOnly) "Following the words only" else "Voice is back, in step" }
                "sync" -> { LiveClient.resync(); toast = "Back in step with $host" }
                else -> sheet = id
            }
        }, toast)
        val k = Look(design.ReadingThemes.Night, design.Tones.Lamp)
        when (sheet) {
            "lang" -> TvPanel(c(Palette.TvPanel), { sheet = "" }) {
                S("Change language", 32f, C.Text)
                T("You stay in step with $host. The new voice picks up at the same sentence.", 15f, C.Sub, Modifier.padding(top = 6.dp, bottom = 16.dp), lineHeight = 22f)
                Tables.installed().forEach { iso3 ->
                    val sel = iso3 == LiveClient.lang
                    Column(
                        Modifier.padding(bottom = 8.dp).fillMaxWidth().tvFocus(RoundedCornerShape(20.dp), { LiveClient.switchLanguage(iso3); sheet = ""; toast = "${Tables.localName(iso3)}, same sentence" }, Role.RadioButton).clip(RoundedCornerShape(20.dp)).background(if (sel) gold(.12f) else Color.Transparent).insetRing(2.dp, if (sel) gold(.6f) else Color.Transparent, RoundedCornerShape(20.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) { S(Tables.localName(iso3), 22f, c(Palette.Title)); T(liveLangMeta(iso3, LiveClient.state?.code ?: ""), 14f, C.Sub) }
                }
            }
            "set" -> TvPanel(c(Palette.TvPanel), { sheet = "" }) {
                S("Settings", 32f, C.Text)
                T("Text size", 15f, C.Sub, Modifier.padding(top = 22.dp, bottom = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(34f, 44f, 54f).forEachIndexed { i, fs ->
                        Box(Modifier.weight(1f).height(64.dp).tvFocus(RoundedCornerShape(20.dp), { textSize = i }, Role.RadioButton).clip(RoundedCornerShape(20.dp)).background(if (textSize == i) gold(.16f) else white(.06f)), contentAlignment = Alignment.Center) {
                            S("Aa", Math.round(fs * .6f).toFloat(), c(Palette.Title))
                        }
                    }
                }
                T("Voices", 15f, C.Sub, Modifier.padding(top = 26.dp, bottom = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { S("${Tables.localName(LiveClient.lang)} ${100 - Prefs.mix}%", 20f, C.Gold); S("${Prefs.mix}% English", 20f, C.English) }
                val pos = animate((Prefs.mix - 10) / 80f, 300, Ease.O12)
                Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(mixBrush(Prefs.mix))) {}
                Box(Modifier.fillMaxWidth().height(0.dp)) { Box(Modifier.offset(y = (-26).dp).fillMaxWidth().drawBehind { drawCircle(gold(.2f), 19.dp.toPx(), Offset(size.width * pos, 0f)); drawCircle(C.Ink, 13.dp.toPx(), Offset(size.width * pos, 0f)) }) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvChip("More ${Tables.localName(LiveClient.lang)}", { Prefs.updateMix(Prefs.mix - 5) }, k, Modifier.weight(1f), bg = white(.08f), fg = c(Palette.Title))
                    TvChip("More English", { Prefs.updateMix(Prefs.mix + 5) }, k, Modifier.weight(1f), bg = white(.08f), fg = c(Palette.Title))
                }
                Row(Modifier.padding(top = 22.dp).fillMaxWidth().tvFocus(RoundedCornerShape(16.dp), { Prefs.updateSplit(!Prefs.split) }, Role.Switch).clip(RoundedCornerShape(16.dp)).padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    T("Separate voices left and right", 16f, c(Palette.Title), Modifier.weight(1f)); Switch(Prefs.split, 52f, 30f, 22f, 4f, Color(127, 127, 127).copy(alpha = .3f))
                }
            }
            "leave" -> LeaveScreen(host, { sheet = "" }, { keep -> LiveClient.leave(keep); sheet = ""; ctrl = false; go("find"); if (keep) Nav.go(Nav.Tab.Table) })
        }
    }
}

@Composable
private fun Find(onHost: (LiveHostInfo) -> Unit) {
    val time by clock()
    var code by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }
    LaunchedEffect(LiveClient.hosts.isNotEmpty()) { runCatching { first.requestFocus() } }
    Row(Modifier.fillMaxSize().padding(start = 130.dp, end = 90.dp, top = 80.dp)) {
        Column(Modifier.weight(1.1f)) {
            S("Join a service", 66f, c(Palette.Title), weight = 300, tracking = -1.2f)
            T("Live on this Wi-Fi right now", 20f, C.Sub, Modifier.padding(top = 8.dp, bottom = 30.dp))
            LiveClient.hosts.forEachIndexed { i, h ->
                val (o1, o2) = hostOrbs[i % 2]
                Row(
                    Modifier.padding(bottom = 16.dp).fillMaxWidth().tvFocus(RoundedCornerShape(26.dp), { onHost(h) }).clip(RoundedCornerShape(26.dp)).background(c(Palette.Card)).then(if (i == 0) Modifier.sheen({ time }, 3200) else Modifier)
                        .insetRing(1.dp, if (i == 0) gold(.3f) else white(.06f), RoundedCornerShape(26.dp))
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier).padding(horizontal = 26.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).drawBehind { drawRect(Brush.radialGradient(0f to o1, 1f to o2, center = Offset(size.width * .3f, size.height * .3f), radius = size.width)) }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(12.dp).beat({ time }, 1400).clip(CircleShape).background(Color.White))
                    }
                    Column(Modifier.weight(1f)) { T(h.name, 26f, c(Palette.Title), weight = 600); T(hostMeta(h), 18f, C.Sub, Modifier.padding(top = 2.dp)) }
                }
            }
            if (LiveClient.hosts.isEmpty()) T("No one is sharing on this Wi-Fi yet. On the computer, open Live and start sharing.", 18f, C.Sub, lineHeight = 27f)
        }
        Box(
            Modifier.weight(.9f).fillMaxHeight().padding(start = 50.dp, bottom = 80.dp)
                .onPreviewKeyEventCompat { key ->
                    when (key) {
                        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> { code = (code + (key - KeyEvent.KEYCODE_0)).take(4); if (code.length == 4) LiveClient.byCode(code)?.let(onHost); true }
                        KeyEvent.KEYCODE_DEL -> { code = code.dropLast(1); true }
                        else -> false
                    }
                }.focusable(),
            contentAlignment = Alignment.Center,
        ) {
            repeat(3) { i -> Box(Modifier.requiredSize(180.dp).ripple({ time }, 3000, -i * 1000).drawBehind { drawCircle(gold(.45f), style = Stroke(2.dp.toPx())) }) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                T("or type the live ID", 18f, C.Sub)
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { i ->
                        Box(Modifier.size(64.dp, 80.dp).clip(RoundedCornerShape(18.dp)).background(white(.06f)).insetRing(2.dp, if (i == code.length) gold(.7f) else white(.08f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                            S(code.getOrNull(i)?.toString() ?: "", 40f, c(Palette.Title))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Name(onNext: () -> Unit) {
    var name by remember { mutableStateOf(Prefs.liveName.ifBlank { "" }) }
    val time by clock()
    val init = initials(name)
    val t by oneShot(init, 350)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val keys = "QWERTYUIOPASDFGHJKL".map { "$it" } + "del" + "ZXCVBNM".map { "$it" } + listOf("space", "clear")
    Row(Modifier.fillMaxSize().padding(start = 130.dp, end = 90.dp, top = 90.dp), horizontalArrangement = Arrangement.spacedBy(60.dp)) {
        Column(Modifier.width(420.dp)) {
            S("What should we call this TV?", 54f, c(Palette.Title), weight = 300, lineHeight = 1.05f, tracking = -1f)
            T("${LiveClient.host?.name ?: "The host"} sees who is listening. A family name works well for a room.", 18f, C.Sub, Modifier.padding(top = 14.dp), lineHeight = 27f)
            Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.requiredSize(190.dp).halo({ time }, 3000).background(Brush.radialGradient(0f to gold(.35f), .65f to Color.Transparent), CircleShape))
                Box(Modifier.size(130.dp).then(if (init.length % 2 == 1) Modifier.pop({ t }, Ease.O13) else Modifier.enter({ t }, Ease.O13)).clip(CircleShape)
                    .drawBehind { drawCircle(Brush.radialGradient(0f to C.Flame, .6f to C.Amber, 1f to c(Palette.Copper), center = Offset(size.width * .35f, size.height * .3f), radius = size.width)) }, contentAlignment = Alignment.Center) {
                    S(init, 52f, c(Palette.Initials))
                }
            }
        }
        Column(Modifier.weight(1f).padding(top = 10.dp)) {
            Row(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(24.dp)).background(white(.07f)).insetRing(2.dp, gold(.45f), RoundedCornerShape(24.dp)).padding(horizontal = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                S(name, 34f, c(Palette.Title))
                Box(Modifier.padding(start = 3.dp).size(2.dp, 36.dp).graphicsLayer { alpha = if (phase(time, 1000) < .5f) 1f else 0f }.background(C.Gold))
            }
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.chunked(10).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { j, key ->
                            val wide = key == "space"
                            Box(
                                Modifier.weight(if (wide) 2f else 1f).height(54.dp).tvFocus(RoundedCornerShape(16.dp), {
                                        name = when (key) { "del" -> name.dropLast(1); "clear" -> ""; "space" -> "$name "; else -> (name + if (name.isNotEmpty() && !name.endsWith(' ')) key.lowercase() else key).take(22) }
                                    }).clip(RoundedCornerShape(16.dp)).background(if (key.length > 1) white(.1f) else white(.05f))
                                    .then(if (r == 0 && j == 0) Modifier.focusRequester(first) else Modifier).label(key),
                                contentAlignment = Alignment.Center,
                            ) { T(when (key) { "del" -> "⌫"; "space" -> "Space"; "clear" -> "Clear"; else -> key }, if (key.length > 1) 15f else 20f, c(Palette.Title), weight = 500) }
                        }
                        if (row.size < 10) Box(Modifier.weight((10 - row.size - row.count { it == "space" }).toFloat().coerceAtLeast(.01f)))
                    }
                }
            }
            TvGold("Continue as ${name.trim().ifBlank { "Friend" }}", { Prefs.liveName = name.trim().ifBlank { "Friend" }; onNext() }, Modifier.padding(top = 22.dp), height = 60f)
        }
    }
}

@Composable
private fun Lang(onJoined: () -> Unit) {
    var lang by remember { mutableStateOf(Prefs.readingLang.takeIf { Tables.has(it) } ?: Tables.installed().firstOrNull() ?: "eng") }
    var joining by remember { mutableStateOf(false) }
    LaunchedEffect(joining) { if (joining) { delay(1400); LiveClient.join(lang); joining = false; onJoined() } }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(Modifier.fillMaxSize().padding(start = 130.dp, end = 90.dp, top = 90.dp)) {
        S("Hear it in", 60f, c(Palette.Title), weight = 300, tracking = -1f)
        T("${LiveClient.host?.name ?: "The host"} is reading ${LiveClient.state?.title ?: LiveClient.host?.title ?: "a sermon"}. You can change this any time.", 19f, C.Sub, Modifier.padding(top = 8.dp, bottom = 34.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Tables.installed().take(4).forEachIndexed { i, iso3 ->
                val sel = iso3 == lang
                Box(
                    Modifier.weight(1f).height(200.dp).tvFocus(RoundedCornerShape(30.dp), { lang = iso3 }, Role.RadioButton).clip(RoundedCornerShape(30.dp)).background(if (sel) gold(.12f) else c(Palette.Card)).insetRing(2.dp, if (sel) gold(.6f) else Color.Transparent, RoundedCornerShape(30.dp))
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier).padding(24.dp)
                ) {
                    S(Tables.tag(iso3).take(2).replaceFirstChar { it.uppercase() }, 130f, if (sel) gold(.25f) else white(.05f), Modifier.align(Alignment.TopEnd).offset(34.dp, (-50).dp), weight = 300, lineHeight = 1f)
                    Column(Modifier.align(Alignment.BottomStart)) { S(Tables.localName(iso3), 30f, c(Palette.Title)); T(liveLangMeta(iso3, LiveClient.state?.code ?: LiveClient.host?.code ?: ""), 15f, C.Sub) }
                }
            }
            repeat((4 - Tables.installed().size).coerceAtLeast(0)) { Box(Modifier.weight(1f)) }
        }
        val w = animate(if (joining) 60f else 300f, 450, Ease.Sheet)
        Box(
            Modifier.padding(top = 40.dp).width(w.dp).height(60.dp).cssShadow(0f, 18f, 40f, -14f, gold(.7f), RoundedCornerShape(30.dp)).tvFocus(RoundedCornerShape(30.dp), { if (!joining) joining = true }).clip(RoundedCornerShape(30.dp)).background(C.Gold)
                ,
            contentAlignment = Alignment.Center,
        ) {
            if (joining) { val a by spinAngle(800f, true); Box(Modifier.size(26.dp).graphicsLayer { rotationZ = a }.drawBehind { drawCircle(C.OnGold.copy(alpha = .25f), style = Stroke(3.dp.toPx())); drawArc(C.OnGold, -90f, 90f, false, style = Stroke(3.dp.toPx())) }) }
            else T("Join as ${Prefs.liveName.ifBlank { "Friend" }}", 19f, C.OnGold, weight = 700, maxLines = 1)
        }
    }
}

@Composable
private fun Listen(ctrl: Boolean, size: Int, onOk: () -> Unit) {
    val time by clock()
    val s = LiveClient.state
    val items = LiveClient.items
    val (i, p) = if (s != null && items.isNotEmpty()) LiveClient.locateHost(items, s) else 0 to 0f
    val sz = listOf(34f, 44f, 54f)[size]
    val focus = remember { FocusRequester() }
    LaunchedEffect(ctrl) { if (!ctrl) runCatching { focus.requestFocus() } }
    val k = Look(design.ReadingThemes.Night, design.Tones.Lamp)
    Box(Modifier.fillMaxSize().focusRequester(focus).tvFocusless(onOk)) {
        Row(Modifier.padding(start = 130.dp, end = 70.dp, top = 44.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(12.dp)) { Box(Modifier.fillMaxSize().clip(CircleShape).background(C.Coral)); Box(Modifier.fillMaxSize().ripple({ time }, 1800).clip(CircleShape).background(C.Coral)) }
            T("With ${LiveClient.host?.name ?: "the host"}, in ${Tables.localName(LiveClient.lang)}", 19f, C.Soft)
            if (!LiveClient.textOnly) Row(Modifier.padding(start = 8.dp).height(20.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(0 to C.Gold, 200 to C.Gold, 400 to C.English).forEach { (off, col) -> Box(Modifier.size(3.dp, 20.dp).wave({ time }, 1000, off).clip(RoundedCornerShape(2.dp)).background(col)) }
            } else T("Words only", 16f, C.Gold, Modifier.padding(start = 8.dp))
            Box(Modifier.weight(1f))
            if (!ctrl) T("Press OK for controls", 17f, C.Faint, Modifier.hint({ time }))
        }
        Column(Modifier.fillMaxSize().padding(start = 180.dp, end = 180.dp, bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterVertically)) {
            if (items.isEmpty()) S(if (s == null) "Waiting for the host…" else "This sermon isn’t in ${Tables.localName(LiveClient.lang)} on this TV.", 34f, C.Sub, italic = true, weight = 300)
            listOf(-1, 0, 1).forEach { d ->
                val it = items.getOrNull(i + d) ?: return@forEach
                val now = d == 0
                val fs = animate(if (now) sz else Math.round(sz * .72f).toFloat(), 700, Ease.Sheet)
                val op = animate(if (d < 0) .4f else if (d > 0) .6f else 1f, 700)
                LitText("", it.text, "", if (d < 0) 1f else if (now) p else 0f, style(fs, Face.Serif, if (now) 400 else 300, color = if (now) c(Palette.Title) else C.Sub, lineHeight = 1.32f, tracking = -.3f), k, Modifier.graphicsLayer { alpha = op })
            }
        }
    }
}

/** The listening page takes OK (and the centre button) to bring the controls up or down. */
private fun Modifier.tvFocusless(onOk: () -> Unit) = onPreviewKeyEventCompat { key ->
    if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) { onOk(); true } else false
}.focusable()

@Composable
private fun Controls(onTool: (String) -> Unit, toast: String) {
    val t by oneShot(Unit, 500)
    val f by oneShot(Unit, 400)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    var syncing by remember { mutableStateOf(false) }
    LaunchedEffect(syncing) { if (syncing) { delay(1200); syncing = false } }
    val textOnly = LiveClient.textOnly
    Box(Modifier.fillMaxSize()) {
        if (toast.isNotEmpty()) {
            val pt by oneShot(toast, 450)
            Row(Modifier.align(Alignment.TopCenter).padding(top = 100.dp).pop({ pt }, Ease.O13).height(50.dp).cssShadow(0f, 20f, 40f, -14f, Color.Black.copy(alpha = .9f), RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp)).background(c(Palette.TvToast)).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Ico(Icons.Tick, 18f, C.Gold); T(toast, 17f, c(Palette.Title), maxLines = 1)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp).fade({ f }).background(Brush.verticalGradient(0f to Color.Transparent, .6f to Color(5, 8, 16).copy(alpha = .92f))))
        Row(Modifier.align(Alignment.BottomStart).padding(start = 130.dp, end = 70.dp, bottom = 34.dp).fillMaxWidth().rise({ t }, Ease.Swap), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple("audio", if (textOnly) "Back to audio" else "Words only", if (textOnly) Icons.AudioBack else Icons.WordsOnly),
                Triple("lang", "Language", Icons.Language), Triple("sync", "Resync", Icons.ResyncTv), Triple("set", "Settings", Icons.Settings),
            ).forEachIndexed { i, (id, label, icon) ->
                val on = id == "audio" && textOnly
                val a by spinAngle(800f, id == "sync" && syncing)
                Row(
                    Modifier.height(64.dp).tvFocus(RoundedCornerShape(32.dp), { if (id == "sync") syncing = true; onTool(id) }).clip(RoundedCornerShape(32.dp)).background(if (on) gold(.16f) else white(.08f))
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) { Ico(icon, 22f, if (on) C.Gold else c(Palette.Title), Modifier.graphicsLayer { rotationZ = a }); T(label, 17f, if (on) C.Gold else c(Palette.Title), weight = 600) }
            }
            Box(Modifier.weight(1f))
            if (!textOnly) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val vol = Math.round(Audio.volume * 10)
                Box(Modifier.size(52.dp).tvFocus(RoundedCornerShape(23.dp), { Audio.volume = ((vol - 1) / 10f).coerceIn(0f, 1f) }).clip(RoundedCornerShape(23.dp)).background(white(.08f)).label("Quieter"), contentAlignment = Alignment.Center) { T("−", 22f, c(Palette.Title), weight = 600) }
                Row(Modifier.height(34.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    repeat(10) { b -> Box(Modifier.size(6.dp, (10 + b * 2.4f).dp).clip(RoundedCornerShape(3.dp)).background(if (b < vol) C.Gold else white(.15f))) }
                }
                Box(Modifier.size(52.dp).tvFocus(RoundedCornerShape(23.dp), { Audio.volume = ((vol + 1) / 10f).coerceIn(0f, 1f) }).clip(RoundedCornerShape(23.dp)).background(white(.08f)).label("Louder"), contentAlignment = Alignment.Center) { T("+", 22f, c(Palette.Title), weight = 600) }
            }
            Box(Modifier.height(64.dp).tvFocus(RoundedCornerShape(32.dp), { onTool("leave") }).clip(RoundedCornerShape(32.dp)).background(Color(255, 122, 102).copy(alpha = .14f)).padding(horizontal = 26.dp), contentAlignment = Alignment.Center) {
                T("Leave", 17f, C.CoralSoft, weight = 600)
            }
        }
    }
}

@Composable
private fun LeaveScreen(host: String, onStay: () -> Unit, onLeave: (Boolean) -> Unit) {
    BackHandler(onBack = onStay)
    val t by oneShot(Unit, 350); val p by oneShot(Unit, 500)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().fade({ t }).background(c(Palette.TvLeave)), contentAlignment = Alignment.Center) {
        Column(Modifier.pop({ p }, Ease.O12), horizontalAlignment = Alignment.CenterHorizontally) {
            S("Leave the service?", 54f, c(Palette.Title), weight = 300)
            T("$host will see that ${Prefs.liveName.ifBlank { "this TV" }} left. You can carry on alone from paragraph ${liveParagraph()}.", 19f, C.Sub, Modifier.padding(top = 10.dp, bottom = 30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvGhost("Stay", onStay, Modifier.focusRequester(first), height = 60f, size = 18f)
                TvGold("Keep reading here", { onLeave(true) }, height = 60f)
                TvGhost("Leave", { onLeave(false) }, height = 60f, size = 18f, fg = C.CoralSoft, bg = Color(255, 122, 102).copy(alpha = .16f))
            }
        }
    }
}
