package ui.screens.projection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.PingDot
import components.global.popIn
import components.global.rise
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import live.LiveDiscovery
import live.TableInstaller
import live.TableLang
import org.jetbrains.jewel.foundation.modifier.onHover
import parsers.bible.models.ILanguage
import parsers.vgr.SermonMedia
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import java.net.InetAddress

private enum class Step(val label: String) { FIND("Find"), NAME("Your name"), LANG("Language"), READY("Get ready") }

private val Green = Color(0xFF7FD9A8)
private val Amber = Color(0xFFF2C46B)

internal fun initials(name: String) = name.split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }

/** The four steps to follow a live service: find the host, your name, your language, get in time. */
@Composable
fun JoinLive() {
    var step by remember { mutableStateOf(if (ListenSession.host != null && ListenSession.name.isNotBlank()) Step.LANG else Step.FIND) }
    LaunchedEffect(Unit) { LiveDiscovery.start() }
    Column(
        Modifier.fillMaxSize().background(StageBg).background(Brush.radialGradient(listOf(Rose.copy(alpha = 0.08f), Color.Transparent), radius = 900f))
            .verticalScroll(rememberScrollState()).padding(horizontal = 56.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        Stepper(step) { step = it }
        AnimatedContent(step, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(150)) }) { s ->
            when (s) {
                Step.FIND -> FindStep { step = Step.NAME }
                Step.NAME -> NameStep { step = Step.LANG }
                Step.LANG -> LangStep { step = Step.READY }
                Step.READY -> ReadyStep()
            }
        }
        if (step != Step.FIND) Text(
            "← Back", color = StageMuted, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { step = Step.entries[step.ordinal - 1] }.hand().padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun Stepper(at: Step, onPick: (Step) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Step.entries.forEach { st ->
            val done = st.ordinal < at.ordinal
            val cur = st == at
            val bg by animateColorAsState(if (cur) Rose.copy(alpha = 0.12f) else Color.Transparent, tween(250))
            Row(
                Modifier.height(34.dp).clip(CircleShape).background(bg).border(1.dp, if (cur) Rose else StageBorder, CircleShape)
                    .clickable(enabled = st.ordinal <= at.ordinal, role = Role.Tab) { onPick(st) }.hand().padding(start = 6.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(if (done) Green else if (cur) Rose else Color(0xFF1A202A)), contentAlignment = Alignment.Center) {
                    Text(if (done) "✓" else "${st.ordinal + 1}", color = if (done || cur) StagePanel else Color(0xFF6F7787), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(st.label, color = if (cur || done) StageText else Color(0xFF6F7787), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            if (st != Step.READY) Box(Modifier.width(18.dp).height(1.dp).background(Color(0xFF2A313E)))
        }
    }
}

@Composable
private fun FindStep(onFound: () -> Unit) {
    val hosts by LiveDiscovery.hosts.collectAsState()
    var code by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(bad) {
        if (bad) shake.animateTo(0f, keyframes { durationMillis = 350; -6f at 87; 6f at 262 })
    }
    fun join(h: live.LiveHostInfo) { ListenSession.connect(h); onFound() }
    val network = remember { runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("this network") }

    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().rise(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Join a live service", color = Color(0xFFF6F1EC), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            Text("Follow the sermon in your own language, in time with the host.", color = StageSoft, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Column(
            Modifier.fillMaxWidth().offset(x = shake.value.dp).clip(RoundedCornerShape(20.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(20.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ENTER THE LIVE ID", color = StageMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(StageBg).border(1.dp, if (code.isNotEmpty() && !bad) Color(0xFFC9A15F) else Color(0xFF2A313E), RoundedCornerShape(16.dp)).padding(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (0..3).forEach { i ->
                        val ch = code.getOrNull(i)?.toString() ?: ""
                        Box(
                            Modifier.width(54.dp).height(64.dp).clip(RoundedCornerShape(12.dp)).background(if (ch.isNotEmpty()) Color(0xFF141A24) else StagePanel)
                                .border(1.dp, if (i == code.length) Color(0xFFC9A15F) else StageBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { if (ch.isNotEmpty()) Text(ch, color = Color(0xFFFFF6E2), fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.popIn()) }
                    }
                }
                // the boxes are drawn above; the real (invisible) field takes the typing
                BasicTextField(
                    code, { v ->
                        val digits = v.filter { it.isDigit() }.take(4)
                        code = digits
                        bad = false
                        if (digits.length == 4) {
                            val h = hosts.firstOrNull { it.joinCode == digits }
                            if (h != null) join(h) else bad = true
                        }
                    },
                    singleLine = true, textStyle = TextStyle(color = Color.Transparent), cursorBrush = SolidColor(Color.Transparent),
                    modifier = Modifier.matchParentSize().focusRequester(focus)
                )
            }
            Text(
                if (bad) "No live service with ID $code on this network. Check the number on the host’s screen." else "Or pick one below. Both of you need to be on the same Wi-Fi.",
                color = if (bad) Color(0xFFF28B8B) else Color(0xFF6F7787), fontSize = 12.5.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Radar()
                Text("Live on this network", color = StageText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("$network · searching…", color = Color(0xFF6F7787), fontSize = 12.sp)
            }
            hosts.forEach { h -> HostCard(h) { join(h) } }
            if (hosts.isEmpty()) Text("No one is live here yet. When the host presses Go live, it shows up here.", color = StageMuted, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun Radar() {
    val t = rememberInfiniteTransition()
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)))
    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp)) {
            drawCircle(Rose.copy(alpha = 0.6f * (1 - p)), radius = size.minDimension / 2 * (0.3f + 1.3f * p), style = Stroke(2.dp.toPx()))
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(Rose))
    }
}

@Composable
private fun HostCard(h: live.LiveHostInfo, onJoin: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val lift by animateDpAsState(if (hovered) (-2).dp else 0.dp, tween(250))
    val mins = if (h.startedAt > 0) ((System.currentTimeMillis() - h.startedAt) / 60000) else null
    Row(
        Modifier.fillMaxWidth().offset(y = lift).clip(RoundedCornerShape(16.dp)).background(StagePanel)
            .border(1.dp, if (hovered) Color(0xFF3A4252) else StageBorder, RoundedCornerShape(16.dp)).onHover { hovered = it }
            .clickable(role = Role.Button, onClick = onJoin).hand().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF3A1E26), Color(0xFF1A0E12)))), contentAlignment = Alignment.Center) {
            Text(h.code.take(2).ifBlank { "•" }, color = Rose, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(h.title.ifBlank { "Live service" }, color = StageText, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 18.sp)
            Text(
                listOfNotNull(h.code.ifBlank { null }, "hosted by ${h.name}", "${h.listeners} listening", mins?.let { "started $it min ago" }).joinToString(" · "),
                color = StageMuted, fontSize = 12.5.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ID ", color = StageSoft, fontSize = 12.sp)
            Text(h.joinCode, color = StageText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Text("Join", color = Color(0xFF2A1014), fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).background(Rose).padding(horizontal = 16.dp, vertical = 9.dp))
    }
}

@Composable
private fun NameStep(onNext: () -> Unit) {
    var editing by remember { mutableStateOf(ListenSession.name.isBlank()) }
    var draft by remember { mutableStateOf(ListenSession.name) }
    var keep by remember { mutableStateOf(true) }
    val shown = if (editing) draft else ListenSession.name
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().rise(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(Modifier.size(84.dp).popIn().clip(CircleShape).background(Color(0xFFEFCB7E)), contentAlignment = Alignment.Center) {
            Text(initials(shown), color = StagePanel, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        if (!editing) {
            Text("Welcome back, ${ListenSession.name}", color = Color(0xFFF6F1EC), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp, textAlign = TextAlign.Center)
            Text("The host will see you as ${ListenSession.name}.", color = StageSoft, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("Continue as ${ListenSession.name}", true, onClick = onNext)
                BigButton("Change name", false) { editing = true; draft = ListenSession.name }
            }
        } else {
            Text("What should we call you?", color = Color(0xFFF6F1EC), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(StagePanel).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                if (draft.isEmpty()) Text("Your name", color = StageMuted, fontSize = 17.sp)
                BasicTextField(draft, { draft = it.take(40) }, singleLine = true, textStyle = TextStyle(color = Color(0xFFFFF6E2), fontSize = 17.sp, textAlign = TextAlign.Center, fontFamily = Inter), cursorBrush = SolidColor(Rose), modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.clickable { keep = !keep }.hand(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(if (keep) Rose else Color.Transparent).border(1.dp, Rose, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    if (keep) Text("✓", color = StagePanel, fontSize = 11.sp)
                }
                Text("Remember me on this computer", color = StageSoft, fontSize = 12.5.sp)
            }
            BigButton("Continue", draft.isNotBlank(), Modifier.fillMaxWidth()) {
                if (draft.isNotBlank()) { ListenSession.rememberName(draft.trim(), keep); editing = false; onNext() }
            }
        }
        val s = ListenSession.state
        val h = ListenSession.host
        if (h != null) Text("Joining ${s?.title ?: h.title} · ${s?.code ?: h.code} · ID ${h.joinCode}", color = Color(0xFF6F7787), fontSize = 12.sp)
    }
}

@Composable
private fun BigButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        modifier.height(46.dp).clip(RoundedCornerShape(12.dp)).background(if (primary) Rose else if (hovered) Color(0xFF161B24) else Color.Transparent)
            .then(if (primary) Modifier else Modifier.border(1.dp, Color(0xFF2A313E), RoundedCornerShape(12.dp)))
            .onHover { hovered = it }.clickable(role = Role.Button, onClick = onClick).hand().padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (primary) Color(0xFF2A1014) else StageText, fontSize = 14.sp, fontWeight = if (primary) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun LangStep(onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(TableInstaller.installed()) }
    val more = remember(installed) { TableInstaller.available() }
    val code = ListenSession.state?.code ?: ListenSession.host?.code ?: ""
    val status = remember { mutableStateMapOf<String, LangStatus>() }
    val audioProgress = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(installed, code) {
        installed.forEach { l -> if (status[l.tag] == null && code.isNotBlank()) { status[l.tag] = LangStatus.Checking; scope.launch { status[l.tag] = prepareLanguage(l, code)?.let { LangStatus.Ready(it) } ?: LangStatus.NoSermon } } }
    }
    val selected = ListenSession.lang
    val ok = selected != null && status[selected.tag] is LangStatus.Ready

    Column(Modifier.widthIn(max = 820.dp).fillMaxWidth().rise(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Choose your language", color = Color(0xFFF6F1EC), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Text("You need The Table in your language so the words can follow the host. Audio is optional.", color = StageSoft, fontSize = 13.5.sp, modifier = Modifier.padding(top = 8.dp))
        }
        installed.forEach { l ->
            val st = status[l.tag]
            LangRow(l.name, l.localName.ifBlank { l.name }, selected?.tag == l.tag, enabled = st is LangStatus.Ready, onPick = {
                ListenSession.lang = l
                (st as? LangStatus.Ready)?.let { ListenSession.prepared = it.prepared }
            }) {
                StatusLine("The Table") { Text("✓ Installed", color = Green, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
                StatusLine("Audio") {
                    when (st) {
                        null, LangStatus.Checking -> Text("Checking…", color = StageMuted, fontSize = 12.5.sp)
                        LangStatus.NoSermon -> Text("This sermon isn’t in your ${l.name} Table yet.", color = Amber, fontSize = 12.sp)
                        is LangStatus.Ready -> {
                            val p = st.prepared
                            val prog = audioProgress[l.tag]
                            when {
                                prog != null -> ProgressBar(prog, Rose)
                                p.local -> Text("✓ Downloaded", color = Green, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                p.audio != null -> {
                                    SmallButton("Download") {
                                        val sermon = p.sermon ?: return@SmallButton
                                        audioProgress[l.tag] = 0.01f
                                        scope.launch {
                                            val target = SermonMedia.audioFile(sermon, p.audio)
                                            val ok2 = runCatching { SermonMedia.download(p.audio, target) { audioProgress[l.tag] = it.coerceAtLeast(0.01f) } }.isSuccess
                                            audioProgress.remove(l.tag)
                                            if (ok2) status[l.tag] = LangStatus.Ready(p.copy(audio = target.absolutePath, local = true)).also { if (ListenSession.lang?.tag == l.tag) ListenSession.prepared = it.prepared }
                                        }
                                    }
                                    Text("or stream it", color = Color(0xFF6F7787), fontSize = 11.5.sp)
                                }
                                else -> {
                                    Text("Not recorded in this language.", color = Color(0xFFF0A36B), fontSize = 12.5.sp)
                                    val on = ListenSession.useEnglishAudio && ListenSession.lang?.tag == l.tag
                                    SmallButton(if (on) "✓ Hear English audio" else "Text only · or hear English audio", on) {
                                        ListenSession.lang = l; ListenSession.prepared = p
                                        ListenSession.useEnglishAudio = !on
                                        if (!on && ListenSession.english == null) scope.launch {
                                            installed.firstOrNull { it.tag == "en" }?.let { ListenSession.english = prepareLanguage(it, code) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        more.take(12).forEach { lang -> InstallRow(lang) { scope.launch { if (TableInstaller.install(lang)) installed = TableInstaller.installed() } } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("The host is preaching ${ListenSession.state?.let { "${it.title} · ${it.code}" } ?: code}", color = StageMuted, fontSize = 12.5.sp)
            BigButton(if (selected != null) "Continue in ${selected.name}" else "Pick a language", ok) { if (ok) onNext() }
        }
    }
}

@Composable
private fun LangRow(name: String, native: String, selected: Boolean, enabled: Boolean, onPick: () -> Unit, status: @Composable () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) Rose.copy(alpha = 0.06f) else StagePanel)
            .border(1.dp, if (selected) Rose else if (hovered) Color(0xFF3A4252) else StageBorder, RoundedCornerShape(16.dp)).onHover { hovered = it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.weight(1f).clickable(enabled = enabled, role = Role.RadioButton, onClick = onPick).hand(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) Rose else if (enabled) Color(0xFF4A5262) else Color(0xFF2A313E), CircleShape), contentAlignment = Alignment.Center) {
                if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(Rose))
            }
            Column {
                Text(name, color = StageText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(native, color = StageMuted, fontSize = 12.sp)
            }
        }
        Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(6.dp)) { status() }
    }
}

@Composable
private fun InstallRow(lang: ILanguage, onInstall: () -> Unit) {
    val prog = TableInstaller.progress[lang.tag]
    LangRow(lang.name, lang.localName, false, enabled = false, onPick = {}) {
        StatusLine("The Table") {
            if (prog != null) ProgressBar(prog, Color(0xFFC9A15F))
            else {
                SmallButton("Download", gold = true, onClick = onInstall)
                Text("install this Table first", color = Amber, fontSize = 11.5.sp)
            }
        }
        StatusLine("Audio") { Text("after The Table", color = Color(0xFF6F7787), fontSize = 12.sp) }
    }
}

@Composable
private fun StatusLine(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = StageMuted, fontSize = 12.5.sp, modifier = Modifier.width(80.dp))
        content()
    }
}

@Composable
private fun ProgressBar(p: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(160.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF1A202A))) {
            Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f)))))
        }
        Text("${(p * 100).toInt()}%", color = StageSoft, fontSize = 12.sp)
    }
}

@Composable
private fun SmallButton(label: String, on: Boolean = false, gold: Boolean = false, onClick: () -> Unit) {
    Text(
        label, color = if (gold) Color(0xFFEFCB7E) else if (on) RoseText else StageText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.height(28.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, if (gold) Color(0xFFC9A15F) else if (on) Rose else Color(0xFF2A313E), RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick).hand().padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun ReadyStep() {
    var tick by remember { mutableStateOf(0) }
    var phones by remember { mutableStateOf(true) }
    val s = ListenSession.state
    val h = ListenSession.host
    val p = ListenSession.prepared
    val lang = ListenSession.lang?.name ?: ""
    LaunchedEffect(Unit) { while (tick < 5) { ListenSession.ping(); delay(450); tick++ } }
    val matched = if (s != null && p != null) ListenSession.locateIn(p.items, s).let { (i, _) -> p.items.getOrNull(i) } else null
    val checks = listOf(
        Triple("Connected to the host", ListenSession.connected, "Live ID ${h?.joinCode ?: ""} · ${h?.name ?: ""}’s computer"),
        Triple("Fast link", ListenSession.rtt != null, "WebSocket to the host · ${ListenSession.rtt ?: "…"} ms round trip"),
        Triple("Text matched", matched != null, if (matched != null) "${s?.title} ${s?.code} · your $lang Table lines up with the host at ¶ ${matched.number}${matched.key?.let { ", sentence ${it.last()}" } ?: ""}" else "Waiting for the host’s place"),
        Triple("Audio ready", true, when {
            ListenSession.textOnly -> "Text only"
            ListenSession.useEnglishAudio -> "English audio with $lang text"
            p?.local == true -> "$lang audio downloaded · matched by sentence"
            p?.audio != null -> "Streaming $lang audio · matched by sentence"
            else -> "No $lang audio · following the text only"
        }),
    )
    val ready = tick >= 4 && checks.take(3).all { it.second }
    Row(Modifier.widthIn(max = 820.dp).fillMaxWidth().rise(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            Modifier.weight(1.4f).clip(RoundedCornerShape(20.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(20.dp)).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (ready) "You’re in time with the host" else "Getting you in time…", color = Color(0xFFF6F1EC), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            checks.forEachIndexed { i, (label, ok, detail) ->
                val done = tick > i && ok
                val running = !done && tick >= i
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CheckMark(done, running)
                    Column {
                        Text(label, color = if (done) StageText else Color(0xFF6F7787), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (done) detail else if (running) "Checking…" else "Waiting", color = StageMuted, fontSize = 12.5.sp)
                    }
                }
            }
            Row(Modifier.clickable { phones = !phones }.hand(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(if (phones) Rose else Color.Transparent).border(1.dp, Rose, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    if (phones) Text("✓", color = StagePanel, fontSize = 11.sp)
                }
                Text("I'm using headphones (keeps the host's speaker from echoing)", color = StageSoft, fontSize = 12.5.sp)
            }
            val bg by animateColorAsState(if (ready) Rose else Color(0xFF3A3236), tween(300))
            Box(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                    .clickable(enabled = ready, role = Role.Button) { ListenSession.listening = true; ListenSession.logJoin() }.hand(),
                contentAlignment = Alignment.Center
            ) { Text(if (ready) "Start listening →" else "Almost ready…", color = Color(0xFF2A1014), fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
        Column(
            Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("HOW IT STAYS IN TIME", color = StageMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            listOf(
                "The host sends which sentence it is on (its section, paragraph and sentence number) and how far through it, a few times a second over the local network.",
                "Your recording in another language has its own timing, so your computer finds the same sentence in your Table and starts your audio from that sentence's own time.",
                "Small drift is fixed by playing a touch faster or slower (0.97× to 1.03×), which you won't hear. Only a gap of more than about 2 seconds jumps.",
                "If the connection drops, you keep listening and catch up as soon as it's back.",
            ).forEach { Text(it, color = StageSoft, fontSize = 12.5.sp, lineHeight = 19.sp) }
        }
    }
}

@Composable
private fun CheckMark(done: Boolean, running: Boolean) {
    val t = rememberInfiniteTransition()
    val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)))
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        when {
            done -> Box(Modifier.size(24.dp).popIn().clip(CircleShape).background(Green), contentAlignment = Alignment.Center) { Text("✓", color = StagePanel, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            running -> Canvas(Modifier.size(24.dp).graphicsLayer { rotationZ = r }) {
                drawCircle(Color(0xFF2A313E), style = Stroke(3.dp.toPx()))
                drawArc(Rose, 0f, 90f, false, style = Stroke(3.dp.toPx()))
            }
            else -> Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF1A202A)))
        }
    }
}
