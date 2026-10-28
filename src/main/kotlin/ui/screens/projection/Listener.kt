package ui.screens.projection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mossip.auraplayer.engine.PlayerState
import components.global.EqBars
import components.global.PingDot
import components.global.popIn
import components.global.rise
import formatTime
import kotlinx.coroutines.launch
import live.TableInstaller
import live.TableLang
import navigation.LocalModal
import navigation.dismiss
import navigation.show
import org.jetbrains.jewel.foundation.modifier.onHover
import player.LocalPlayer
import player.LocalPlayerState
import presentation.DeckItem
import presentation.FocusReader
import presentation.glowText
import ui.screens.reading.ListenerOptions
import ui.screens.reading.ReadingPrefs
import ui.screens.reading.ReadingSettingsModal
import ui.screens.reading.ReadingStores
import ui.screens.thisweek.hand
import ui.theme.Fraunces

/** Join live and, once joined, the listening page. */
@Composable
fun Listener() {
    if (!ListenSession.listening) JoinLive() else ListenerPage()
}

private fun ReadingPrefs.lit() = theme.headC

private suspend fun loadEnglish(code: String?) {
    if (ListenSession.english != null || code == null) return
    TableInstaller.installed().firstOrNull { it.tag == "en" }?.let { ListenSession.english = prepareLanguage(it, code) }
}

@Composable
private fun ListenerPage() {
    val engine = LocalPlayerState.current
    val nowPlaying by LocalPlayer.current.nowPlaying.collectAsState()
    val playerState by engine.playerState.collectAsState()
    val currentTime by engine.currentTime.collectAsState()
    val duration by engine.duration.collectAsState()
    val volume by engine.volume.collectAsState()
    val modal = LocalModal.current
    val scope = rememberCoroutineScope()
    val p by ReadingStores.listener.prefs
    val t = p.theme
    val s = ListenSession.state
    val prepared = ListenSession.prepared
    val items = prepared?.items ?: emptyList()
    val src = ListenSession.audioSource()
    val noAudio = prepared != null && prepared.audio == null
    var langMenu by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val accent = Color(0xFFC9A15F)

    ListenSync()

    // the sentence on screen: from the audio being heard when it is this text's own recording, else from the host
    val ownAudio = src != null && src === prepared && nowPlaying?.id?.startsWith("live:") == true && playerState == PlayerState.PLAYING
    val (idx, prog) = when {
        ownAudio && presentation.isTimed(items) -> presentation.locateTime(items, currentTime - p.offsetMs / 1000.0, duration)
        s != null -> ListenSession.locateIn(items, s)
        else -> 0 to 0f
    }
    val curPara = items.getOrNull(idx)?.number
    val english = ListenSession.english?.items
    fun englishFor(item: DeckItem?): String? =
        if (!p.showEn || english == null || ListenSession.lang?.tag == "en" || item == null) null
        else english.firstOrNull { it.key != null && it.key == item.key }?.text

    LaunchedEffect(p.showEn) { if (p.showEn) loadEnglish(s?.code) }

    Box(Modifier.fillMaxSize().background(t.bgC)) {
        Column(Modifier.fillMaxSize()) {
            // header
            val drop = !ListenSession.connected
            Row(
                Modifier.fillMaxWidth().background(StageBg).padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PingDot(if (drop) Color(0xFFF2C46B) else Rose, 10.dp, active = !drop)
                    Column {
                        val hostName = ListenSession.host?.name?.uppercase() ?: "HOST"
                        Text(
                            if (drop) "RECONNECTING TO $hostName…" else "LIVE · CONNECTED TO $hostName",
                            color = if (drop) Color(0xFFF2C46B) else RoseText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(s?.title ?: ListenSession.host?.title ?: "", color = StageText, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 18.sp)
                            Text("  ${s?.code ?: ""}${curPara?.let { " · ¶ $it" } ?: ""}", color = StageMuted, fontSize = 12.5.sp)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.height(34.dp).clip(CircleShape).background(StagePanel).border(1.dp, StageBorder, CircleShape).padding(start = 4.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFEFCB7E)), contentAlignment = Alignment.Center) {
                            Text(initials(ListenSession.name), color = StagePanel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(ListenSession.name, color = StageText, fontSize = 12.5.sp)
                    }
                    val lag = ListenSession.lag
                    val lc = if (drop) Color(0xFFF28B8B) else lagColor(lag)
                    Row(
                        Modifier.height(34.dp).clip(CircleShape).background(lc.copy(alpha = 0.12f)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(lc))
                        Text(
                            if (drop) "Holding position" else if (!ListenSession.following) "Not following · press Resync" else if (src == null) "Following the text" else if (lag < 1) "In sync · %.1fs behind".format(lag) else "%.1fs behind host".format(lag),
                            color = lc, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (ListenSession.debug) Text(ListenSession.debugLine, color = StageMuted, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    StageGhost("Resync") { ListenSession.resync() }
                    Box {
                        StageGhost("${ListenSession.lang?.name ?: "Language"} ▾") { langMenu = !langMenu }
                        DropdownMenu(langMenu, { langMenu = false }, Modifier.width(280.dp).background(StagePanel).heightIn(max = 420.dp)) {
                            LangMenu { l ->
                                langMenu = false
                                scope.launch {
                                    val code = s?.code ?: return@launch
                                    val prep = prepareLanguage(l, code)
                                    if (prep == null) notice = "${s.title} isn’t in your ${l.name} Table yet."
                                    else { ListenSession.lang = l; ListenSession.prepared = prep; ListenSession.useEnglishAudio = false; notice = null }
                                }
                            }
                        }
                    }
                    Box(
                        Modifier.height(34.dp).clip(RoundedCornerShape(10.dp)).background(StagePanel).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClickLabel = "Reading settings") {
                                modal.show { ReadingSettingsModal(ReadingStores.listener, ListenerOptions, onDismiss = { modal.dismiss() }) { ListenerPreview(it) } }
                            }.hand().padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("A", color = StageText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("a", color = StageText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    StageGhost("Leave") { leaving = true }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(StageLine))

            AnimatedVisibility(drop, enter = fadeIn() + slideInVertically { -it / 3 }, exit = fadeOut()) {
                Banner(Color(0xFFF2C46B), spinner = true, text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Lost the host for a moment. ") }
                    append("Your audio keeps playing; we'll catch up the second it's back.")
                }, action = "Try now") { ListenSession.reconnect() }
            }
            AnimatedVisibility(noAudio, enter = fadeIn() + slideInVertically { -it / 3 }, exit = fadeOut()) {
                val name = ListenSession.lang?.name ?: ""
                Banner(Color(0xFFF0A36B), spinner = false, text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("No $name audio for this sermon. ") }
                    append("You're following the $name text in time with the host.")
                }, action = if (ListenSession.useEnglishAudio) "Back to text only" else "Hear English audio instead") {
                    ListenSession.useEnglishAudio = !ListenSession.useEnglishAudio
                    if (ListenSession.useEnglishAudio) scope.launch { loadEnglish(s?.code) }
                }
            }
            notice?.let { Text(it, color = Color(0xFFF6D6BE), fontSize = 13.sp, modifier = Modifier.padding(start = 22.dp, top = 12.dp)) }

            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) { ModeSwitch() }

            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = if (p.size > 26) 980.dp else 860.dp).fillMaxSize()) {
                    when {
                        prepared == null -> Text("Getting the sermon ready…", color = t.mutedC, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                        ListenSession.mode == "Center" -> {
                            FocusReader(
                                items, idx, prog, p.lit(), Modifier.fillMaxSize().padding(vertical = 30.dp),
                                family = p.family, sizes = Triple(p.size * 1.3f, p.size * 1.3f, p.size * 1.3f), fg = t.fgC, glow = p.glow, rest = p.lit(), grow = false,
                                under = { englishFor(items.getOrNull(it)) }
                            )
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to t.bgC, 0.25f to Color.Transparent, 0.75f to Color.Transparent, 1f to t.bgC)))
                        }
                        else -> Paragraphs(items, idx, prog, follow = ListenSession.mode == "Read", p, english) { ListenSession.mode = "Read" }
                    }
                }
            }

            // footer
            Box(Modifier.fillMaxWidth().height(1.dp).background(StageLine))
            Row(
                Modifier.fillMaxWidth().background(StageBg.copy(alpha = 0.94f)).padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val audioOn = src != null
                EqBars(if (audioOn) Color(0xFFEFCB7E) else Color(0xFF4A5263), playing = audioOn && playerState == PlayerState.PLAYING)
                Column {
                    val name = ListenSession.lang?.name ?: ""
                    Text(
                        when { !audioOn -> "Text only · following the host"; ListenSession.useEnglishAudio -> "English audio · $name text"; else -> "Listening in $name" },
                        color = StageText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (audioOn && duration > 0) "${formatTime(currentTime)} of ${formatTime(duration)} · your recording, matched by sentence" else "Following the host’s place",
                        color = StageMuted, fontSize = 11.5.sp
                    )
                }
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF1A202A))) {
                    Box(
                        Modifier.fillMaxWidth(if (duration > 0 && audioOn) (currentTime / duration).toFloat().coerceIn(0f, 1f) else 0f).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(accent, Color(0xFFEFCB7E))))
                    )
                }
                StageGhost(if (ListenSession.textOnly) "Turn audio on" else "Text only") { ListenSession.textOnly = !ListenSession.textOnly }
                Text("Volume", color = StageSoft, fontSize = 12.sp)
                Slider(
                    volume.toFloat() / 100f, { engine.setVolume(it * 100.0) }, Modifier.width(110.dp),
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF1A202A))
                )
            }
        }

        if (leaving) Box(Modifier.fillMaxSize().background(Color(0x99050608)).clickable { leaving = false }, contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 380.dp).popIn().clip(RoundedCornerShape(20.dp)).background(StagePanel).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Leave the live service?", color = StageText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    "You can rejoin any time with ID ${ListenSession.host?.joinCode ?: ""} while the host is live. Your place is saved in My Activity.",
                    color = StageSoft, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(12.dp)).clickable { leaving = false }.hand(), contentAlignment = Alignment.Center) {
                        Text("Stay", color = StageText, fontSize = 13.5.sp)
                    }
                    Box(
                        Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(Rose).clickable {
                            if (nowPlaying?.id?.startsWith("live:") == true) engine.setPause(true)
                            engine.setSpeed(1.0)
                            ListenSession.leave()
                            leaving = false
                        }.hand(),
                        contentAlignment = Alignment.Center
                    ) { Text("Leave", color = Color(0xFF2A1014), fontSize = 13.5.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ListenerPreview(p: ReadingPrefs) {
    val t = rememberInfiniteTransition()
    val g by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4500, easing = LinearEasing)))
    val item = DeckItem(14, "", "What is the body of Christ? It’s the body of believers that’s associated with the Holy Spirit.")
    Text(
        if (p.glow) glowText(item, g, p.lit(), p.theme.fgC.copy(alpha = 0.6f)) else AnnotatedString(item.text),
        fontFamily = p.family, fontSize = p.size.sp, lineHeight = (p.size * p.spacing.lh).sp, color = p.theme.fgC
    )
}

@Composable
private fun Banner(color: Color, spinner: Boolean, text: AnnotatedString, action: String, onAction: () -> Unit) {
    val t = rememberInfiniteTransition()
    val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)))
    Row(
        Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (spinner) Canvas(Modifier.size(16.dp).graphicsLayer { rotationZ = r }) { drawArc(color, 0f, 270f, false, style = Stroke(2.dp.toPx())) }
        Text(text, color = Color(0xFFF6E3B8), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            action, color = Color(0xFFF6E3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.height(32.dp).clip(RoundedCornerShape(9.dp)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                .clickable(role = Role.Button, onClick = onAction).hand().padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ModeSwitch() {
    val accent = Color(0xFFC9A15F)
    Row(Modifier.clip(CircleShape).background(StagePanel).border(1.dp, StageBorder, CircleShape).padding(3.dp)) {
        listOf("Center" to "Auto-center", "Read" to "Read along", "Scroll" to "Scroll").forEach { (key, label) ->
            val on = ListenSession.mode == key
            val bg by animateColorAsState(if (on) accent else Color.Transparent, tween(250))
            Text(
                label, color = if (on) Color(0xFF1B1509) else StageSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(bg).clickable(role = Role.RadioButton) { ListenSession.mode = key }.hand().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LangMenu(onPick: (TableLang) -> Unit) {
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(TableInstaller.installed()) }
    val locked = remember(installed) { TableInstaller.available() }
    Text("TABLE INSTALLED", color = Color(0xFF6F7787), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
    installed.forEach { l ->
        val on = ListenSession.lang?.tag == l.tag
        var hovered by remember { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(9.dp)).background(if (hovered) Color(0xFF161B24) else Color.Transparent)
                .onHover { hovered = it }.clickable(role = Role.RadioButton) { onPick(l) }.hand().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (on) "✓" else "", color = Color(0xFFEFCB7E), fontSize = 13.sp, modifier = Modifier.width(14.dp))
            Text(l.name, color = StageText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
    }
    if (locked.isNotEmpty()) {
        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(StageLine))
        Text("NEEDS THE TABLE FIRST", color = Color(0xFF6F7787), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp))
        locked.take(10).forEach { lang ->
            val prog = TableInstaller.progress[lang.tag]
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(lang.name, color = Color(0xFF6F7787), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    if (prog != null) "${(prog * 100).toInt()}%" else "Install", color = Color(0xFFEFCB7E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, Color(0xFFC9A15F), RoundedCornerShape(7.dp))
                        .clickable(enabled = prog == null) { scope.launch { if (TableInstaller.install(lang)) installed = TableInstaller.installed() } }.hand()
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/** Read along (follows the host, the live sentence glowing) and Scroll (free, the host's paragraph marked). */
@Composable
private fun Paragraphs(items: List<DeckItem>, index: Int, progress: Float, follow: Boolean, p: ReadingPrefs, english: List<DeckItem>?, onJump: () -> Unit) {
    val t = p.theme
    val list = rememberLazyListState()
    val paragraphs = remember(items) { items.indices.groupBy { items[it].number }.values.toList() }
    val current = paragraphs.indexOfFirst { index in it }.coerceAtLeast(0)
    LaunchedEffect(current, follow) { if (follow) list.animateScrollToItem((current - 1).coerceAtLeast(0)) }
    val shown by animateFloatAsState(progress, tween(400, easing = LinearEasing))
    Box(Modifier.fillMaxSize().rise()) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (p.spacing.name == "AIRY") 30.dp else 22.dp)) {
            item { Box(Modifier.height(30.dp)) }
            itemsIndexed(paragraphs) { pi, idx ->
                val cur = pi == current
                val alpha by animateFloatAsState(if (follow && pi < current) 0.5f else 1f, tween(400))
                Row(Modifier.fillMaxWidth().padding(end = 8.dp).graphicsLayer { this.alpha = alpha }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (p.numbers) Text(
                        "${items[idx.first()].number}", color = if (cur) Color(0xFFEFCB7E) else Color(0xFF6F7787), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp).padding(top = 6.dp)
                    )
                    Column(
                        Modifier.weight(1f).then(
                            if (!follow) Modifier.drawBehind { if (cur) drawLine(Rose, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx()) }.padding(start = 12.dp) else Modifier
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (follow) buildAnnotatedString {
                                idx.forEachIndexed { k, i ->
                                    if (k > 0) append(" ")
                                    when {
                                        i == index -> append(if (p.glow) glowText(items[i], shown, p.lit(), t.fgC.copy(alpha = 0.38f)) else AnnotatedString(items[i].text))
                                        i < index -> append(items[i].text)
                                        else -> withStyle(SpanStyle(color = t.fgC.copy(alpha = 0.38f))) { append(items[i].text) }
                                    }
                                }
                            } else AnnotatedString(idx.joinToString(" ") { items[it].text }),
                            fontFamily = p.family, fontWeight = p.weight, fontSize = p.size.sp, lineHeight = (p.size * p.spacing.lh).sp, color = t.fgC
                        )
                        if (p.showEn && english != null) {
                            val en = idx.mapNotNull { i -> english.firstOrNull { it.key != null && it.key == items[i].key }?.text }.joinToString(" ")
                            if (en.isNotBlank()) Text(en, color = Color(0xFF8A93A6), fontSize = 14.sp, lineHeight = 22.sp, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
            item { Box(Modifier.heightIn(min = 140.dp)) }
        }
        if (!follow) Text(
            "↓ Back to where the host is (¶ ${items.getOrNull(index)?.number ?: ""})", color = Color(0xFF2A1014), fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).rise().clip(CircleShape).background(Rose)
                .clickable(role = Role.Button, onClick = onJump).hand().padding(horizontal = 16.dp, vertical = 11.dp)
        )
    }
}
