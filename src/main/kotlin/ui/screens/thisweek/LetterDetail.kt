package ui.screens.thisweek

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import androidx.compose.ui.text.style.TextAlign
import com.composables.projectorIcon
import components.global.EqBars
import components.global.PingDot
import components.global.SoftEase
import components.global.popIn
import components.global.rise
import presentation.DeckItem
import presentation.DeckSource
import presentation.ProjectionDeck
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.checkIcon
import com.composables.downloadArrowIcon
import com.composables.pauseIcon
import com.composables.play
import com.composables.searchIcon
import com.mossip.auraplayer.engine.PlayerState
import formatTime
import icons.bookmarkIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import navigation.LibraryScreen
import navigation.LocalModal
import navigation.LocalTabs
import navigation.Screen
import navigation.dismiss
import navigation.show
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import parsers.bt.Letter
import parsers.bt.LetterPart
import parsers.bt.Letters
import parsers.vgr.SermonMedia
import parsers.vgr.VgrViewModel
import parsers.vgr.models.Sermon
import player.GlobalPlayerType
import player.GlobalPlayerViewModel
import player.LocalPlayer
import player.LocalPlayerState
import ui.screens.table.ListenModal
import ui.theme.Fraunces
import ui.theme.LocalTheme
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterDetail(id: String) {
    val theme = LocalTheme.current
    val tab = LocalTabs.current.current
    val modal = LocalModal.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val letters by Letters.all.collectAsState()
    val letter = letters.firstOrNull { it.id == id }
    var loading by remember(id) { mutableStateOf(true) }
    LaunchedEffect(id) {
        if (Letters.byId(id) == null) Letters.sync()
        Letters.detail(id)
        loading = false
    }

    val vgrVM = tab.viewModelStore.getOrCreate("HomeVM-${tab.id}") { VgrViewModel() }
    val sermons by vgrVM.sermons.collectAsState()
    val sermon = letter?.let { l -> sermons.firstOrNull { it.dateCode.equals(l.code, ignoreCase = true) } }

    val saved by LetterPrefs.saved
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(1600); toast = null } }
    val copy = { text: String -> clipboard.setText(AnnotatedString(text)); toast = "Copied" }

    var containerTop by remember { mutableStateOf(0f) }
    val sectionTops = remember(id) { mutableStateMapOf<LetterPart, Float>() }

    Box(Modifier.fillMaxSize().onGloballyPositioned { containerTop = it.positionInRoot().y }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 56.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 1160.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ActionButton("← This Week") { tab.navigate(Screen.ThisWeek) }
                    ActionButton("Search the letter archive", ::searchIcon) { tab.navigate(Screen.LetterArchive) }
                }

                if (letter == null) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(if (loading) "Loading the letter…" else "Couldn't load this letter.", color = theme.colors.primaryText, fontSize = 14.sp)
                    }
                } else key(letter.id) {

                // reaching the end of the letter counts as reading it
                LaunchedEffect(scroll.value >= scroll.maxValue - 40 && scroll.maxValue > 0) {
                    if (scroll.maxValue > 0 && scroll.value >= scroll.maxValue - 40) { delay(1500); LetterPrefs.markRead(letter.id) }
                }

                Header(
                    letter,
                    isSaved = letter.id in saved,
                    onFocus = { modal.show { LetterFocus(letter, onDismiss = { modal.dismiss() }) } },
                    onOpenTable = sermon?.let { s -> { vgrVM.setCurrentSermon(s); tab.navigate(Screen.Library(LibraryScreen.Table())) } },
                    onSave = { LetterPrefs.toggleSaved(letter.id) },
                    onCopy = { copy(letter.copyText()) },
                    onWeb = { browse(letter.url) },
                    onProject = {
                        ProjectionDeck.load(
                            DeckSource.TABLE, letter.title, "Letter · ${letter.code}",
                            letter.sections.flatMap { it.blocks }.mapIndexed { i, b -> DeckItem(i + 1, if (b.isQuote) "Quote" else "Letter", b.text) }
                        )
                        tab.navigate(Screen.Projection)
                    },
                )

                Box(Modifier.rise(60)) { RelatedServices(letter, letters) { tab.navigate(Screen.Letter(it)) } }

                Box(Modifier.rise(120)) { SermonPlayer(letter, sermon) }

                Row(Modifier.rise(180), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        letter.sections.forEach { section ->
                            val isLetter = section.part == LetterPart.LETTER
                            val shape = RoundedCornerShape(22.dp)
                            Column(
                                Modifier.fillMaxWidth()
                                    .onGloballyPositioned { sectionTops[section.part] = it.positionInRoot().y }
                                    .clip(shape)
                                    .background(if (isLetter) theme.colors.menu else Color.Transparent)
                                    .border(1.dp, theme.colors.border, shape)
                                    .padding(horizontal = 36.dp, vertical = 30.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val c = partColor(section.part, theme.colors.primary)
                                    Text(
                                        section.part.label.uppercase(), color = c, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
                                        modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(c.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                    Box(Modifier.weight(1f).height(1.dp).background(theme.colors.border))
                                }
                                if (isLetter) letter.salutation?.let {
                                    Text(it, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 23.sp, color = theme.colors.primaryBright)
                                }
                                section.blocks.forEach { LetterBlockView(it, onCopy = copy) }
                                if (isLetter) Text("Bro. Joseph Branham", fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = theme.colors.primaryBright)
                            }
                        }
                    }

                    Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (letter.sections.size > 1) Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                            Column {
                                SectionLabel("In this service", Modifier.padding(start = 6.dp, bottom = 8.dp))
                                letter.sections.forEach { s ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                val top = sectionTops[s.part] ?: return@clickable
                                                scope.launch { scroll.animateScrollTo((scroll.value + top - containerTop - 24).toInt().coerceAtLeast(0)) }
                                            }.hand()
                                            .padding(horizontal = 8.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(partColor(s.part, theme.colors.primary)))
                                        Text(s.part.label, color = theme.colors.primaryText, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        ScriptureCard(letter)
                        YourWeek(letter, sermon)
                    }
                }
                }
            }
        }

        VerticalScrollbar(scrollState = scroll, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())

        toast?.let {
            Text(
                it, color = Color(0xFF2A1A0E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(10.dp)).background(theme.colors.primary).padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }
    }
}

private fun browse(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(
    letter: Letter,
    isSaved: Boolean,
    onFocus: () -> Unit,
    onOpenTable: (() -> Unit)?,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onWeb: () -> Unit,
    onProject: () -> Unit,
) {
    val theme = LocalTheme.current
    Row(Modifier.rise(), horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.Bottom) {
        DateTile(letter.serviceDate ?: letter.letterDate, width = 108.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((letter.serviceDate ?: letter.letterDate).format(longDateFormat), color = theme.colors.text, fontSize = 13.5.sp)
                Row(
                    Modifier.height(26.dp).clip(RoundedCornerShape(50))
                        .background(theme.colors.primary.copy(alpha = 0.12f))
                        .border(1.dp, theme.colors.primary.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .clickable(role = Role.Button) { browse(letter.hookupUrl ?: "${Letters.HOST}/en/streaming") }.hand()
                        .padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(Modifier.size(15.dp), contentAlignment = Alignment.Center) {
                        PingDot(theme.colors.primary, 15.dp)
                        Icon(play(Color(0xFF2A1A0E)), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(7.dp))
                    }
                    Text("THE HOOKUP", color = theme.colors.primary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }
            Text(letter.code, color = theme.colors.primaryBright, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text(letter.title, color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 50.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Focus", primary = true, onClick = onFocus)
            if (onOpenTable != null) ActionButton("Open in The Table", onClick = onOpenTable)
            ActionButton("Project", ::projectorIcon, onClick = onProject)
            ActionButton(null, ::bookmarkIcon, active = isSaved, onClick = onSave)
            ActionButton("Copy", onClick = onCopy)
            ActionButton("Website", onClick = onWeb)
        }
    }
}

/** Other services that played this same sermon, newest first; each has its own letter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedServices(letter: Letter, all: List<Letter>, onPick: (String) -> Unit) {
    val theme = LocalTheme.current
    val sameSermon = (all.filter { it.code == letter.code } + listOf(letter)).distinctBy { it.id }.sortedByDescending { it.serviceDate ?: it.letterDate }
    // the service page may list services we haven't cached yet
    val fromPage = letter.related.filter { r -> r.label.contains(letter.code) && sameSermon.none { it.id == r.id } }
    if (sameSermon.size + fromPage.size < 2) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Related services")
            Text("${letter.code} was played at ${sameSermon.size + fromPage.size} services. Each has its own letter.", color = theme.colors.text, fontSize = 12.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sameSermon.forEach { l ->
                Chip((l.serviceDate ?: l.letterDate).format(chipDateFormat), selected = l.id == letter.id, tag = l.kind, mono = true) { onPick(l.id) }
            }
            fromPage.forEach { r ->
                Chip(r.date?.format(chipDateFormat) ?: r.label, selected = false, tag = r.kind, mono = true) { onPick(r.id) }
            }
        }
    }
}

@Composable
private fun SermonPlayer(letter: Letter, sermon: Sermon?) {
    val theme = LocalTheme.current
    val modal = LocalModal.current
    val scope = rememberCoroutineScope()
    val globalPlayer = LocalPlayer.current
    val engine = LocalPlayerState.current
    val nowPlaying by globalPlayer.nowPlaying.collectAsState()
    val playerState by engine.playerState.collectAsState()
    val position by engine.currentTime.collectAsState()
    val duration by engine.duration.collectAsState()
    val speed by engine.speed.collectAsState()

    val isThis = sermon != null && nowPlaying?.id == sermon.stringId
    val playing = isThis && playerState == PlayerState.PLAYING
    val progress = if (isThis && duration > 0) (position / duration).toFloat() else 0f
    val onDevice = remember(sermon) { sermon?.let { SermonMedia.audioFile(it).exists() } ?: false }

    fun start(s: Sermon, url: String) {
        globalPlayer.loadNowPlaying(
            GlobalPlayerViewModel.NowPlaying(
                id = s.stringId, title = s.title, subtitle = "${s.dateCode} - ${s.location}", type = GlobalPlayerType.SERMON,
                isPlaying = true, currentPosition = 0L, url = url.trim(),
            )
        )
        engine.setPause(false)
    }

    fun listen(s: Sermon) = scope.launch {
        // same order as The Table: a copy already on disk, otherwise ask download or stream
        val local = SermonMedia.findLocal(s)
        if (local != null) start(s, local.absolutePath)
        else modal.show { ListenModal(sermon = s, onPlay = { start(s, it); modal.dismiss() }, onDismiss = { modal.dismiss() }) }
    }

    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(theme.colors.primary.copy(alpha = 0.16f), theme.colors.menu)))
            .border(1.dp, theme.colors.primary.copy(alpha = 0.3f), shape)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (playing) EqBars(theme.colors.primaryBright, Modifier.padding(end = 12.dp), height = 18.dp)
            Column(Modifier.weight(1f)) {
                Text(letter.title, color = theme.colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    if (sermon == null) "This sermon isn't in your Table library for this language." else "${sermon.dateCode} · ${sermon.location}",
                    color = theme.colors.text, fontSize = 12.sp
                )
            }
            if (sermon != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("${if (speed % 1.0 == 0.0) speed.toInt() else speed}×") {
                    val speeds = listOf(1.0, 1.25, 1.5, 0.75)
                    engine.setSpeed(speeds[(speeds.indexOf(speed) + 1) % speeds.size])
                }
                if (onDevice) ActionButton("On this device", ::checkIcon) {}
                else ActionButton("Download", ::downloadArrowIcon) { listen(sermon) }
            }
        }

        Waveform(letter.code, progress) { f -> if (isThis && duration > 0) engine.seek(f * duration) }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${formatTime(if (isThis) position else 0.0)} / ${if (isThis && duration > 0) formatTime(duration) else "--:--"}",
                color = theme.colors.text, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton("−15") { if (isThis) engine.seek((position - 15).coerceAtLeast(0.0)) }
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(if (sermon == null) theme.colors.border else theme.colors.primary)
                        .clickable(enabled = sermon != null, role = Role.Button) {
                            val s = sermon ?: return@clickable
                            if (isThis) engine.setPause(playing) else listen(s)
                        }.hand(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playing) pauseIcon(Color(0xFF2A1A0E)) else play(Color(0xFF2A1A0E)),
                        contentDescription = if (playing) "Pause" else "Play", tint = Color.Unspecified, modifier = Modifier.size(18.dp)
                    )
                }
                RoundButton("+30") { if (isThis) engine.seek((position + 30).coerceAtMost(duration)) }
            }
            Text(if (isThis) "Resumes where you left off" else "", color = theme.colors.text, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RoundButton(label: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Box(
        Modifier.size(40.dp).clip(CircleShape).border(1.dp, theme.colors.border, CircleShape).clickable(role = Role.Button, onClick = onClick).hand(),
        contentAlignment = Alignment.Center
    ) { Text(label, color = theme.colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ScriptureCard(letter: Letter) {
    val theme = LocalTheme.current
    val refs = remember(letter) { letter.scriptures() }
    if (refs.isEmpty()) return
    val done = LetterPrefs.scripture.value[letter.id] ?: emptySet()
    val fill by animateFloatAsState(refs.count { it in done } / refs.size.toFloat(), tween(600, easing = SoftEase))
    Card(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Scripture to read", fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 19.sp, color = theme.colors.primaryText)
                Text("${refs.count { it in done }} / ${refs.size}", color = theme.colors.primary, fontSize = 12.sp)
            }
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.colors.border)) {
                Box(Modifier.fillMaxWidth(fill).height(4.dp).background(theme.colors.primary))
            }
            refs.forEach { ref ->
                val checked = ref in done
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Checkbox) { LetterPrefs.toggleScripture(letter.id, ref) }.hand()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
                            .background(if (checked) theme.colors.primary else Color.Transparent)
                            .border(2.dp, if (checked) theme.colors.primary else theme.colors.border, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (checked) Icon(checkIcon(Color(0xFF2A1A0E)), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(12.dp).popIn())
                    }
                    Text(
                        ref, color = if (checked) theme.colors.text else theme.colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (checked) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

/** Letter read, how far into the sermon, and scripture ticked off: this week at a glance. */
@Composable
private fun YourWeek(letter: Letter, sermon: Sermon?) {
    val theme = LocalTheme.current
    val read by LetterPrefs.read
    val engine = LocalPlayerState.current
    val nowPlaying by LocalPlayer.current.nowPlaying.collectAsState()
    val position by engine.currentTime.collectAsState()
    val duration by engine.duration.collectAsState()
    val refs = remember(letter) { letter.scriptures() }
    val done = LetterPrefs.scripture.value[letter.id] ?: emptySet()
    val listened = activity.ActivityLog.events.value.filter { it.key == "listen:${sermon?.stringId}" }.sumOf { it.seconds }
    Card(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your week", fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 19.sp, color = theme.colors.primaryText)
            listOf(
                "Letter" to if (letter.id in read) "Read" else "Not yet",
                "Sermon" to when {
                    sermon != null && nowPlaying?.id == sermon.stringId && duration > 0 -> "${formatTime(position)} of ${formatTime(duration)}"
                    listened > 0 -> "${activity.shortDuration(listened)} heard"
                    else -> "Not started"
                },
                "Scripture" to "${refs.count { it in done }} of ${refs.size}",
            ).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k, color = theme.colors.text, fontSize = 13.sp)
                    Text(v, color = if (v == "Read") Color(0xFF7FD9A8) else theme.colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
