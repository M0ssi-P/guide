package ui.screens.projection

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import parsers.vgr.models.Paragraph
import parsers.vgr.models.Segment
import ui.screens.table.styleToSpanStyle
import ui.screens.table.toAnnotatedString
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.liveSignalIcon
import com.composables.pauseIcon
import com.composables.play
import com.composables.projectorIcon
import com.mossip.auraplayer.engine.PlayerState
import components.global.EqBars
import components.global.PingDot
import components.global.Ripple
import components.global.rise
import formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import live.LiveHost
import live.LiveState
import mvvm.BibleViewModel
import mvvm.SongBookViewModal
import models.LyricType
import navigation.LocalTabs
import org.jetbrains.jewel.foundation.modifier.onHover
import parsers.vgr.SermonMedia
import parsers.vgr.VgrViewModel
import parsers.vgr.models.Sermon
import perVerseMode
import player.GlobalPlayerType
import player.GlobalPlayerViewModel
import player.LocalPlayer
import player.LocalPlayerState
import presentation.DeckItem
import presentation.DeckSlide
import presentation.DeckSource
import presentation.DeckStyle
import presentation.LocalWindowController
import presentation.ProjectionDeck
import presentation.ProjectionEntry
import presentation.rememberScreens
import presentation.sentences
import ui.UiState
import parsers.vgr.toRecording
import parsers.vgr.toTimeline
import ui.screens.table.ContentBlock
import ui.screens.table.sectionsToBlocks
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.LocalTheme
import java.net.InetAddress

// The projection screens use their own dark stage palette in both themes; the accent still comes from settings.
internal val StageBg = Color(0xFF090B10)
internal val StagePanel = Color(0xFF0E1219)
internal val StageLine = Color(0xFF191E27)
internal val StageBorder = Color(0xFF222834)
internal val StageText = Color(0xFFECE5D3)
internal val StageMuted = Color(0xFF8A93A6)
internal val StageSoft = Color(0xFFA3ABBB)
internal val Rose = Color(0xFFEC8B9A)
internal val RoseText = Color(0xFFF2A9B5)

internal fun lagColor(lag: Double) = when { lag < 1 -> Color(0xFF7FD9A8); lag < 3 -> Color(0xFFF2C46B); else -> Color(0xFFF28B8B) }

/** A sermon's sentences, the unit the Table projects and shares live. */
internal fun Sermon.deckItems(): List<DeckItem> =
    (sections ?: emptyList()).flatMap { sec -> sec.content.filter { it.number != null && !it.isQuestion }.flatMap { it.sentenceItems() } }

/**
 * Consecutive segments with the same index ([section, block, sentence]) are one sentence in several styles.
 * Segments without an index stay with the sentence before them. Paragraphs with no indexes at all are split by punctuation.
 */
internal fun Paragraph.sentenceItems(): List<DeckItem> {
    val n = number ?: 0
    val label = "\u00B6 $n"
    // (starts a new line, segment) in reading order; lines join with a space like the reader
    val segs = content.flatMapIndexed { li, line -> line.segments.mapIndexed { si, seg -> (li > 0 && si == 0) to seg } }
    if (segs.none { !it.second.index.isNullOrEmpty() }) {
        val whole = toAnnotatedString()
        return DeckItem(n, label, whole.text, whole).sentences()
    }
    val groups = mutableListOf<Pair<List<Int>?, MutableList<Pair<Boolean, Segment>>>>()
    for (pair in segs) {
        val idx = pair.second.index?.takeIf { it.isNotEmpty() }
        val last = groups.lastOrNull()
        if (last != null && (idx == null || idx == last.first)) last.second += pair else groups += idx to mutableListOf(pair)
    }
    return groups.mapNotNull { (key, parts) ->
        val rich = buildAnnotatedString {
            parts.forEachIndexed { k, (newLine, seg) ->
                if (newLine && k > 0) append(" ")
                withStyle(seg.style.fold(SpanStyle()) { acc, s -> acc.merge(styleToSpanStyle(s)) }) { append(seg.content) }
            }
        }
        val lead = rich.text.length - rich.text.trimStart().length
        val tail = rich.text.trimEnd().length
        if (tail <= lead) null
        else rich.subSequence(lead, tail).let { t ->
            DeckItem(n, label, t.text, t, key, parts.mapNotNull { it.second.start }.minOrNull(), parts.mapNotNull { it.second.end }.maxOrNull())
        }
    }
}

/**
 * Where each item ends as a share of the whole text. The sermon audio has no per-paragraph timing,
 * so audio position maps to a paragraph by text length.
 * shortcut: proportional by characters; swap in real paragraph timings if the Table data ever has them.
 */
internal fun cumulativeShares(items: List<DeckItem>): List<Float> {
    val total = items.sumOf { it.text.length }.coerceAtLeast(1).toFloat()
    var run = 0
    return items.map { run += it.text.length; run / total }
}

@Composable
fun Projection() {
    val theme = LocalTheme.current
    val accent = theme.colors.primary
    val nav = LocalTabs.current.current
    val windowController = LocalWindowController.current
    val engine = LocalPlayerState.current
    val globalPlayer = LocalPlayer.current
    val nowPlaying by globalPlayer.nowPlaying.collectAsState()
    val playerState by engine.playerState.collectAsState()
    val currentTime by engine.currentTime.collectAsState()
    val duration by engine.duration.collectAsState()
    val scope = rememberCoroutineScope()
    val deck = ProjectionDeck
    val live by LiveHost.running
    var panel by remember { mutableStateOf(if (live) 1 else 0) }
    var notice by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    val vgr = nav.viewModelStore.getOrCreate("HomeVM-${nav.id}") { VgrViewModel() }
    val currentSermon by vgr.currentSermon.collectAsState()

    fun loadSource(src: DeckSource) {
        notice = null
        when (src) {
            DeckSource.TABLE -> (currentSermon as? UiState.Success)?.data?.let { s ->
                deck.load(DeckSource.TABLE, s.title, "${s.dateCode} · ${s.location}", s.deckItems(), 0, s.copy(sections = null))
            } ?: run { notice = "Open a sermon in The Table first." }
            DeckSource.BIBLE -> {
                val bible = nav.viewModelStore.getOrCreate("BibleVM-${nav.id}") { BibleViewModel().apply { initilize() } }
                val verses = perVerseMode(bible.chapterContent?.chapter)
                val name = "${bible.currentBook?.human ?: ""} ${bible.currentChapter?.human ?: ""}".trim()
                if (verses.isEmpty()) notice = "Open a chapter in the Bible first."
                else deck.load(
                    DeckSource.BIBLE, name, "${bible.currentVersion?.localAbbreviation ?: ""} · ${verses.size} verses",
                    verses.flatMap { v -> DeckItem(v.number, "$name:${v.number}", v.content.joinToString(" ") { it.content ?: "" }.replace(Regex("\\s+"), " ").trim()).sentences() }
                )
            }
            DeckSource.SONGS -> {
                val songs = nav.viewModelStore.getOrCreate("SongBookVM-${nav.id}") { SongBookViewModal() }
                val song = songs.currentSong.value ?: windowController.model.value.currentSong.value
                if (song == null) notice = "Pick a song in Song Books first."
                else deck.load(
                    DeckSource.SONGS, song.titles.firstOrNull()?.title ?: "Song", "Song Books · ${song.lyrics.size} verses",
                    song.lyrics.mapIndexed { i, l ->
                        DeckItem(l.verseNumber ?: (i + 1), if (l.type == LyricType.CHORUS) "Chorus" else "Verse ${l.verseNumber ?: (i + 1)}", l.lines.joinToString("\n"))
                    }
                )
            }
            DeckSource.IMAGES -> {
                val items = presentation.PictureLibrary.items()
                deck.load(DeckSource.IMAGES, "Pictures", "${items.size} in your library", items)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (deck.items.isEmpty()) loadSource(DeckSource.TABLE)
        focus.requestFocus()
    }

    val isThis = deck.source == DeckSource.TABLE && deck.sermon != null && nowPlaying?.id == deck.sermon?.stringId
    val playing = isThis && playerState == PlayerState.PLAYING
    val shares = remember(deck.items) { cumulativeShares(deck.items) }
    // Table segments carry start/end seconds; a few untimed sentences shouldn't throw the whole deck back to the estimate
    val timed = remember(deck.items) { presentation.isTimed(deck.items) }

    // the recording moves the slides while it plays
    LaunchedEffect(currentTime, playing, deck.followAudio) {
        if (!playing || !deck.followAudio || duration <= 0) return@LaunchedEffect
        if (timed) {
            // segment timings are on the Table timeline; this recording's map (if any) converts its time onto it
            val map = parsers.vgr.TimeMaps.get(globalPlayer.nowPlaying.value?.url)
            val (i, p) = presentation.locateTime(deck.items, map.toTimeline(currentTime), map.toTimeline(duration))
            deck.index = i
            deck.progress = p
            return@LaunchedEffect
        }
        val f = (currentTime / duration).toFloat()
        val i = shares.indexOfFirst { it >= f }
        if (i < 0) return@LaunchedEffect
        val start = shares.getOrNull(i - 1) ?: 0f
        deck.index = i
        deck.progress = ((f - start) / (shares[i] - start).coerceAtLeast(1e-6f)).coerceIn(0f, 1f)
    }

    // live listeners read this. It keeps working after leaving this screen, and while the sermon plays the place comes
    // from the host's own audio clock, so moving the slides by hand doesn't drag listeners along.
    LaunchedEffect(Unit) {
        LiveHost.source = {
            val audio = deck.sermon != null && globalPlayer.nowPlaying.value?.id == deck.sermon?.stringId
            val t = engine.currentTime.value
            val items = deck.items
            val map = if (audio) parsers.vgr.TimeMaps.get(globalPlayer.nowPlaying.value?.url) else null
            val tl = map.toTimeline(t)
            val (i, p) = if (audio && presentation.isTimed(items)) presentation.locateTime(items, tl, map.toTimeline(engine.duration.value)) else deck.index to deck.progress
            val cur = items.getOrNull(i)
            LiveState(
                title = deck.title, code = deck.sermon?.dateCode ?: "", location = deck.sermon?.location ?: "",
                paragraph = if (deck.source == DeckSource.TABLE) cur?.number else null,
                fraction = presentation.paragraphFraction(items, i, p),
                sentence = cur?.key,
                progress = p,
                // seconds this sentence takes in the host's own recording
                span = if (audio) cur?.let { c -> val st = c.start ?: return@let null; (c.end ?: items.getOrNull(i + 1)?.start)?.let { e -> map.toRecording(e) - map.toRecording(st) } } ?: 0.0 else 0.0,
                position = t,
                timeline = if (audio && presentation.isTimed(items)) tl else 0.0,
                playing = audio && engine.playerState.value == PlayerState.PLAYING,
                language = deck.sermon?.language ?: "",
                // the sermon's own label can be the language it was preached in, so listeners compare this instead
                tableLanguage = vgr.uiSettings.value.currentTableLanguage,
            )
        }
    }

    fun go(i: Int) {
        deck.go(i)
        // moving by hand takes over from the audio until Play is pressed
        if (playing && deck.followAudio) engine.setPause(true)
    }

    fun togglePlay() {
        val sermon = deck.sermon ?: return
        if (isThis) {
            if (playing) engine.setPause(true)
            else {
                if (deck.followAudio && duration > 0) engine.seek(deck.current?.start?.takeIf { timed }?.let { parsers.vgr.TimeMaps.get(globalPlayer.nowPlaying.value?.url).toRecording(it) } ?: ((shares.getOrNull(deck.index - 1) ?: 0f) * duration))
                engine.setPause(false)
            }
            return
        }
        scope.launch {
            val url = SermonMedia.findLocal(sermon)?.absolutePath ?: SermonMedia.audioUrl(sermon)
            if (url == null) { notice = "There's no recording of this sermon in its language yet."; return@launch }
            globalPlayer.loadNowPlaying(
                GlobalPlayerViewModel.NowPlaying(sermon.stringId, sermon.title, "${sermon.dateCode} - ${sermon.location}", GlobalPlayerType.SERMON, true, 0L, url.trim())
            )
            engine.setPause(false)
        }
    }

    Column(
        Modifier.fillMaxSize().background(StageBg).focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight, Key.DirectionDown, Key.PageDown -> { go(deck.index + 1); true }
                    Key.DirectionLeft, Key.DirectionUp, Key.PageUp -> { go(deck.index - 1); true }
                    else -> false
                }
            }
    ) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(projectorIcon(accent), null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                Text("PROJECTION", color = StageText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
            }
            StagePills(DeckSource.entries.map { it.label }, deck.source.ordinal, accent) { loadSource(DeckSource.entries[it]) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val people = LiveHost.listeners.size
                val bg by animateColorAsState(if (live) Rose.copy(alpha = 0.12f) else Color.Transparent, tween(300))
                Row(
                    Modifier.height(38.dp).clip(RoundedCornerShape(11.dp)).background(bg)
                        .border(1.dp, if (live) Rose.copy(alpha = 0.6f) else Color(0xFF2A313E), RoundedCornerShape(11.dp))
                        .clickable(role = Role.Button) { toggleLive(); panel = 1 }.hand().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PingDot(if (live) Rose else Color(0xFF6F7787), active = live)
                    Text(if (live) "Live · $people listening" else "Go live", color = if (live) RoseText else StageText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                StageGhost("Exit") { nav.back() }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(StageLine))

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.width(300.dp).fillMaxHeight().padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SourcePicker(nav) { notice = it }
                if (deck.source == DeckSource.IMAGES) Column(Modifier.verticalScroll(rememberScrollState())) { PicturesGrid() }
                else ItemList(deck, accent, Modifier.weight(1f)) { go(it) }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(StageLine))

            // main column
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(controlledScreens(), color = theme.colors.primaryBright, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(
                        when { deck.source == DeckSource.IMAGES -> "Picture"; !deck.focusAllowed -> "Lyrics · one verse per screen"; else -> deck.effectiveStyle.label },
                        color = StageMuted, fontSize = 11.5.sp
                    )
                }
                DeckSlide(
                    accent, theme.colors.primaryBright,
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).rise().clip(RoundedCornerShape(14.dp)).border(1.dp, Color(0xFF2A2F3A), RoundedCornerShape(14.dp))
                )
                notice?.let { Text(it, color = Color(0xFFF6D6BE), fontSize = 13.sp) }

                AnimatedVisibility(deck.source == DeckSource.TABLE && deck.followAudio && deck.sermon != null, enter = fadeIn(), exit = fadeOut()) {
                    AudioBar(accent, playing, currentTime, if (isThis) duration else 0.0, shares, deck.index, ::togglePlay) { f ->
                        if (isThis && duration > 0) engine.seek(f * duration)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StageGhost("← Previous", tall = true) { go(deck.index - 1) }
                        Text(
                            "${when (deck.source) { DeckSource.TABLE -> "Next sentence"; DeckSource.IMAGES -> "Next picture"; else -> "Next verse" }} →", color = Color(0xFF1B1509), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(accent).clickable(role = Role.Button) { go(deck.index + 1) }.hand()
                                .padding(horizontal = 20.dp, vertical = 13.dp)
                        )
                    }
                    Text(
                        if (deck.source == DeckSource.TABLE && deck.followAudio && playing) "Moving ahead takes over from the audio until you press Play" else "Use ← → or the list on the left",
                        color = StageMuted, fontSize = 12.sp
                    )
                }

                DisplaysSection(accent, theme.colors.primaryBright)
            }

            Box(Modifier.width(1.dp).fillMaxHeight().background(StageLine))
            Column(Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                StageTabs(listOf("Display", if (live) "Live · ${LiveHost.listeners.size}" else "Live"), panel) { panel = it }
                if (panel == 0) DisplayPanel(accent) else LivePanel(accent)
            }
        }
    }
}

internal fun toggleLive() {
    if (LiveHost.running.value) LiveHost.stop()
    else runCatching { LiveHost.start(System.getProperty("user.name") ?: "Host") }
}

@Composable
internal fun StagePills(labels: List<String>, selected: Int, accent: Color, onPick: (Int) -> Unit) {
    Row(Modifier.clip(CircleShape).background(StagePanel).border(1.dp, StageBorder, CircleShape).padding(3.dp)) {
        labels.forEachIndexed { i, l ->
            val on = i == selected
            val bg by animateColorAsState(if (on) accent else Color.Transparent, tween(250))
            Text(
                l, color = if (on) Color(0xFF1B1509) else StageSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(bg).clickable(role = Role.Tab) { onPick(i) }.hand().padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
internal fun StageGhost(label: String, tall: Boolean = false, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Text(
        label, color = Color(0xFFC7CDD8), fontSize = if (tall) 13.sp else 12.5.sp,
        modifier = Modifier.then(if (tall) Modifier.height(44.dp) else Modifier).clip(RoundedCornerShape(if (tall) 12.dp else 10.dp))
            .background(if (hovered) Color(0xFF161B24) else Color.Transparent).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(if (tall) 12.dp else 10.dp))
            .onHover { hovered = it }.clickable(role = Role.Button, onClick = onClick).hand()
            .padding(horizontal = if (tall) 16.dp else 12.dp, vertical = if (tall) 13.dp else 9.dp)
    )
}

@Composable
private fun StageTabs(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(11.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i, l ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Color(0xFF1A202A) else Color.Transparent, tween(200))
            Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable(role = Role.Tab) { onPick(i) }.hand(), contentAlignment = Alignment.Center) {
                Text(l, color = if (on) StageText else StageMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ItemList(deck: ProjectionDeck, accent: Color, modifier: Modifier, onPick: (Int) -> Unit) {
    val list = rememberLazyListState()
    LaunchedEffect(deck.index) { if (deck.index !in list.layoutInfo.visibleItemsInfo.map { it.index }.let { if (it.size > 2) it.drop(1).dropLast(1) else it }) list.animateScrollToItem((deck.index - 2).coerceAtLeast(0)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(state = list, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(deck.items) { i, item ->
                val cur = i == deck.index
                var hovered by remember { mutableStateOf(false) }
                val bg by animateColorAsState(if (cur) accent.copy(alpha = 0.12f) else if (hovered) Color(0xFF12161E) else Color.Transparent, tween(200))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(bg).border(1.dp, if (cur) accent.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(11.dp))
                        .onHover { hovered = it }.clickable(role = Role.Button) { onPick(i) }.hand().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("${item.number}", color = if (cur) accent else Color(0xFF6F7787), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(26.dp))
                    Text(item.text.replace("\n", " / "), color = if (cur) Color(0xFFF4EEDF) else StageSoft, fontSize = 12.5.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AudioBar(
    accent: Color, playing: Boolean, time: Double, duration: Double, shares: List<Float>, index: Int,
    onToggle: () -> Unit, onSeek: (Float) -> Unit,
) {
    val progress = if (duration > 0) (time / duration).toFloat().coerceIn(0f, 1f) else 0f
    Row(
        Modifier.fillMaxWidth().rise().clip(RoundedCornerShape(16.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(accent).clickable(role = Role.Button, onClickLabel = if (playing) "Pause" else "Play", onClick = onToggle).hand(), contentAlignment = Alignment.Center) {
            Icon(if (playing) pauseIcon(Color(0xFF1B1509)) else play(Color(0xFF1B1509)), null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
        }
        EqBars(accent, bars = 3, height = 18.dp, playing = playing)
        Text(formatTime(time), color = StageSoft, fontSize = 12.sp)
        Canvas(
            Modifier.weight(1f).height(22.dp).pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }.hand()
        ) {
            val y = size.height / 2
            val h = 5.dp.toPx()
            drawRoundRect(Color(0xFF1A202A), Offset(0f, y - h / 2), Size(size.width, h), CornerRadius(h / 2))
            drawRoundRect(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.7f))), Offset(0f, y - h / 2), Size(size.width * progress, h), CornerRadius(h / 2))
            // paragraph starts; only drawn when there are few enough to read
            if (shares.size in 2..80) shares.dropLast(1).forEachIndexed { i, s ->
                drawRoundRect(if (i < index) Color(0xFF1B1509) else Color(0xFF3A4150), Offset(size.width * s - 1f, y - 5.5.dp.toPx()), Size(2f, 11.dp.toPx()), CornerRadius(1f))
            }
            drawCircle(accent.copy(alpha = 0.18f), 10.dp.toPx(), Offset(size.width * progress, y))
            drawCircle(accent, 6.5.dp.toPx(), Offset(size.width * progress, y))
        }
        Text(if (duration > 0) formatTime(duration) else "--:--", color = StageSoft, fontSize = 12.sp)
        Text(
            if (duration > 0) "Audio is driving the slides" else "Press play to start the recording", color = accent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SectionHead(text: String) = Text(text, color = StageSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))

@Composable
private fun DisplayPanel(accent: Color) {
    val deck = ProjectionDeck
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (deck.focusAllowed) Column {
            SectionHead("STYLE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeckStyle.entries.forEach { st ->
                    val on = deck.style == st
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) accent.copy(alpha = 0.08f) else StagePanel)
                            .border(1.dp, if (on) accent else StageBorder, RoundedCornerShape(12.dp)).clickable(role = Role.RadioButton) { deck.style = st }.hand().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF05070A)).padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val focusStyle = st == DeckStyle.FOCUS
                            Box(Modifier.fillMaxWidth(if (focusStyle) 0.5f else 0.8f).height(2.dp).background(Color(0xFF4A4638)))
                            Box(Modifier.fillMaxWidth(0.8f).height(if (focusStyle) 6.dp else 3.dp).clip(RoundedCornerShape(2.dp)).background(accent))
                            Box(Modifier.fillMaxWidth(if (focusStyle) 0.5f else 0.8f).height(2.dp).background(Color(0xFF4A4638)))
                        }
                        Text(st.label, color = StageText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(st.desc, color = StageMuted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
        if (deck.source == DeckSource.TABLE) Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Audio drives slides", color = StageText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Follows the sermon recording. Table only.", color = StageMuted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Switch(deck.followAudio, accent) { deck.followAudio = it }
        }
        if (deck.source == DeckSource.IMAGES) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead("PICTURE")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Fill the screen" to true, "Show whole picture" to false).forEach { (label, fill) ->
                    val on = deck.fill == fill
                    Box(
                        Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(9.dp)).background(if (on) accent else Color.Transparent)
                            .border(1.dp, if (on) accent else Color(0xFF2A313E), RoundedCornerShape(9.dp)).clickable(role = Role.RadioButton) { deck.fill = fill }.hand(),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (on) Color(0xFF1B1509) else Color(0xFFC7CDD8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
            Text("Pictures fade in slowly. Use ← → to move through the library like slides.", color = StageMuted, fontSize = 11.5.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun Switch(on: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    val track by animateColorAsState(if (on) accent else Color(0xFF2A313E), tween(250))
    val x by androidx.compose.animation.core.animateDpAsState(if (on) 18.dp else 0.dp, tween(250))
    Box(Modifier.width(44.dp).height(26.dp).clip(CircleShape).background(track).clickable(role = Role.Switch) { onChange(!on) }.hand().padding(3.dp)) {
        Box(Modifier.padding(start = x).size(20.dp).clip(CircleShape).background(Color(0xFFF4EEDF)))
    }
}

@Composable
private fun LivePanel(accent: Color) {
    val live by LiveHost.running
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(live) { while (live) { delay(1000); tick++ } }
    if (!live) {
        Column(
            Modifier.fillMaxWidth().rise().clip(RoundedCornerShape(14.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(14.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Ripple(Rose, 64.dp)
                Icon(liveSignalIcon(Rose), null, tint = Color.Unspecified, modifier = Modifier.size(26.dp))
            }
            Text("Share this with people nearby", color = StageText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "People on the same network open the app and join. Each one hears the sermon in their own language, in time with you.",
                color = StageSoft, fontSize = 12.5.sp, lineHeight = 18.sp, textAlign = TextAlign.Center
            )
            Text(
                "Go live", color = Color(0xFF2A1014), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.height(42.dp).clip(RoundedCornerShape(11.dp)).background(Rose).clickable(role = Role.Button) { toggleLive() }.hand().padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
        return
    }
    val people = remember(tick, LiveHost.listeners.size) { LiveHost.active() }
    val code by LiveHost.joinCode
    val hostName = remember { runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("this computer") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth().rise().clip(RoundedCornerShape(14.dp)).background(Rose.copy(alpha = 0.06f)).border(1.dp, Rose.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(Rose.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(liveSignalIcon(Rose), null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
            }
            Column {
                Text("JOIN CODE", color = RoseText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Text(code, color = StageText, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("On $hostName · local network", color = StageSoft, fontSize = 11.5.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "${people.size}" to "listening",
                "${people.map { it.report.language }.distinct().size}" to "languages",
                (if (people.isEmpty()) "–" else "%.1fs".format(people.map { it.report.lag }.average())) to "avg delay",
            ).forEachIndexed { i, (v, l) ->
                Column(Modifier.weight(1f).rise(i * 60).clip(RoundedCornerShape(12.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(12.dp)).padding(10.dp)) {
                    Text(v, color = StageText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(l, color = StageMuted, fontSize = 11.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CONNECTED", color = StageSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            StageGhost("Resync everyone") { LiveHost.resyncEveryone() }
        }
        if (people.isEmpty()) Text("Waiting for people to join…", color = StageMuted, fontSize = 12.5.sp)
        val avatars = listOf(Color(0xFFEFCB7E), Color(0xFF9FB6F5), Color(0xFF6BDCC3), Color(0xFFE5A4CB), Color(0xFFF0A36B), Color(0xFFB5A9F0))
        people.forEachIndexed { i, p ->
            val r = p.report
            val lc = lagColor(r.lag)
            Row(Modifier.fillMaxWidth().rise(i * 50).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(avatars[i % avatars.size]), contentAlignment = Alignment.Center) {
                    Text(r.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""), color = StagePanel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(r.name, color = StageText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${r.language} · ${if (r.audio) r.mode else "no audio in this language, following text"}",
                        color = if (r.audio) StageMuted else Color(0xFFF0A36B), fontSize = 11.sp
                    )
                }
                Text(
                    "+%.1fs".format(r.lag), color = lc, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(lc.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
