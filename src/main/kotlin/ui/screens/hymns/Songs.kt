package ui.screens.hymns

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.heartIcon
import com.composables.plusIcon
import com.composables.projectorIcon
import com.composables.searchIcon
import components.global.PingDot
import components.global.rise
import loadData
import models.ISongDetails
import models.ISongLyricLine
import models.LyricType
import mvvm.SongBookViewModal
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import presentation.DeckItem
import presentation.DeckSource
import presentation.ProjectionDeck
import saveData
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val Ink = Color(0xFF2A1A0E)
private val Rose = Color(0xFFE8708A)

data class SungSong(val songId: String, val bookId: String, val at: Long) : Serializable

/** Favourites, the service set (in order) and recently sung songs, kept on this computer. */
object SongPrefs {
    val favs = mutableStateOf(loadData<ArrayList<String>>("songs_favs")?.toList() ?: emptyList())
    val set = mutableStateOf(loadData<ArrayList<String>>("songs_set")?.toList() ?: emptyList())
    val recent = mutableStateOf(loadData<ArrayList<SungSong>>("songs_recent")?.toList() ?: emptyList())

    fun toggleFav(id: String) { favs.value = if (id in favs.value) favs.value - id else favs.value + id; saveData("songs_favs", ArrayList(favs.value)) }
    fun toggleSet(id: String) { set.value = if (id in set.value) set.value - id else set.value + id; saveData("songs_set", ArrayList(set.value)) }
    fun move(from: Int, to: Int) {
        if (from == to || from !in set.value.indices) return
        set.value = set.value.toMutableList().apply { add(to.coerceIn(0, size - 1), removeAt(from)) }
        saveData("songs_set", ArrayList(set.value))
    }
    fun sung(song: ISongDetails, bookId: String) {
        recent.value = (listOf(SungSong(song.id, bookId, System.currentTimeMillis())) + recent.value.filterNot { it.songId == song.id }).take(20)
        saveData("songs_recent", ArrayList(recent.value))
    }
}

private enum class SongFilter(val label: String) { ALL("All"), FAVS("Favourites"), RECENT("Recent"), SET("In set") }

internal fun ISongDetails.title(lang: String? = null): String =
    (lang?.let { l -> titles.firstOrNull { it.languageCode.equals(l, true) } } ?: titles.firstOrNull())?.title ?: "Song $songNumber"

private fun ISongLyricLine.text() = lines.joinToString("\n")
private fun ISongLyricLine.isChorus() = type == LyricType.CHORUS
private fun ISongLyricLine.short() = when (type) { LyricType.CHORUS -> "C"; LyricType.BRIDGE -> "B"; else -> "V${verseNumber ?: ""}" }
private fun ISongLyricLine.name() = when (type) { LyricType.CHORUS -> "Chorus"; LyricType.BRIDGE -> "Bridge"; else -> "Verse ${verseNumber ?: ""}" }

/** The order the song is sung in: each verse, followed by the chorus when [chorusAfter] is on. */
internal fun sequence(song: ISongDetails, chorusAfter: Boolean): List<ISongLyricLine> {
    val chorus = song.lyrics.firstOrNull { it.isChorus() }
    val rest = song.lyrics.filterNot { it.isChorus() }
    return when {
        rest.isEmpty() -> listOfNotNull(chorus)
        chorus == null -> rest
        chorusAfter -> rest.flatMap { listOf(it, chorus) }
        else -> rest + chorus
    }
}

internal fun deckFor(song: ISongDetails, seq: List<ISongLyricLine>) =
    seq.mapIndexed { i, l -> DeckItem(i + 1, l.name(), l.text()) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Songs() {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    val model = nav.viewModelStore.getOrCreate("SongBookVM-${nav.id}") { SongBookViewModal().apply { initilize() } }
    val books by model.songBooks.collectAsState()
    val book by model.currentBook.collectAsState()
    val songs by model.songs.collectAsState()
    val current by model.currentSong.collectAsState()
    val favs by SongPrefs.favs
    val set by SongPrefs.set
    val recent by SongPrefs.recent
    if (books.isEmpty()) { SongsEmpty(); return }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SongFilter.ALL) }
    var chorusAfter by remember { mutableStateOf(true) }
    var stanza by remember { mutableStateOf(0) }
    var size by remember { mutableStateOf(1) }
    var lang by remember { mutableStateOf<String?>(null) }
    var searchFocused by remember { mutableStateOf(false) }
    val search = remember { FocusRequester() }
    val root = remember { FocusRequester() }
    // song counts per book for the chips; loading every book once is cheap next to a page of lyrics
    val counts = remember(books) { books.associate { it.id to runCatching { model.loadSongsFrom(it).size }.getOrDefault(0) } }

    val q = query.trim().lowercase()
    val isNumber = q.isNotEmpty() && q.all { it.isDigit() }
    val list = songs.asSequence()
        .filter {
            when (filter) {
                SongFilter.ALL -> true
                SongFilter.FAVS -> it.id in favs
                SongFilter.RECENT -> recent.any { r -> r.songId == it.id }
                SongFilter.SET -> it.id in set
            }
        }
        .filter { s ->
            q.isEmpty() || if (isNumber) s.songNumber.toString().startsWith(q)
            else (s.titles.joinToString(" ") { it.title } + " " + s.lyrics.joinToString(" ") { it.text() }).lowercase().contains(q)
        }
        .sortedBy { it.songNumber }.toList()

    val song = current
    val seq = remember(song, chorusAfter) { song?.let { sequence(it, chorusAfter) } ?: emptyList() }
    val at = stanza.coerceIn(0, (seq.size - 1).coerceAtLeast(0))
    val onScreen = song != null && ProjectionDeck.source == DeckSource.SONGS && ProjectionDeck.title == song.title()

    fun pick(s: ISongDetails) { model.setCurrentSong(s); stanza = 0; lang = null }
    fun show(i: Int) {
        stanza = i
        if (onScreen) ProjectionDeck.go(i)
    }
    fun project() {
        val s = song ?: return
        ProjectionDeck.load(DeckSource.SONGS, s.title(), "No. ${s.songNumber} · ${book?.name ?: ""}", deckFor(s, seq), at)
        SongPrefs.sung(s, book?.id ?: "")
        nav.navigate(Screen.Projection)
    }
    fun nextInSet() {
        if (set.isEmpty()) return
        val nextId = set[((set.indexOf(song?.id) + 1) % set.size)]
        songs.firstOrNull { it.id == nextId }?.let { pick(it) }
    }

    LaunchedEffect(Unit) { root.requestFocus() }
    // keep the projector in step when the order changes under it
    LaunchedEffect(seq, onScreen) { if (onScreen && song != null) ProjectionDeck.items = deckFor(song, seq) }

    Row(
        Modifier.fillMaxSize().focusRequester(root).focusable().onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (e.isCtrlPressed && e.key == Key.K) { search.requestFocus(); return@onPreviewKeyEvent true }
            if (searchFocused) return@onPreviewKeyEvent false
            when (e.key) {
                Key.DirectionDown -> { show((at + 1).coerceAtMost(seq.size - 1)); true }
                Key.DirectionUp -> { show((at - 1).coerceAtLeast(0)); true }
                Key.C -> { seq.indexOfFirst { it.isChorus() }.takeIf { it >= 0 }?.let { show(it) }; true }
                Key.B -> { ProjectionDeck.blank = !ProjectionDeck.blank; true }
                Key.N -> { nextInSet(); true }
                else -> {
                    val n = (e.key.keyCode - Key.One.keyCode).toInt()
                    if (n in 0..8) {
                        seq.indexOfFirst { !it.isChorus() && it.verseNumber == n + 1 }.takeIf { it >= 0 }?.let { show(it) }
                        true
                    } else false
                }
            }
        }
    ) {
        // song list
        Column(
            Modifier.width(380.dp).fillMaxHeight().background(theme.colors.surface).padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Songs", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                Ghost("Workshop") { WorkshopState.tab = WorkshopTab.LIBRARY; nav.navigate(Screen.SongWorkshop) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                books.forEach { b ->
                    val on = b.id == book?.id
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) theme.colors.primary.copy(alpha = 0.16f) else theme.colors.menu)
                            .border(1.dp, if (on) theme.colors.primary else theme.colors.border, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Tab) { model.selectBook(b); query = "" }.hand().padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(b.name, color = if (on) theme.colors.primaryBright else theme.colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${counts[b.id] ?: 0} songs · ${b.language.uppercase()}", color = theme.colors.text, fontSize = 11.sp)
                    }
                }
            }
            SearchBox(query, search, { query = it }, { searchFocused = it }) {
                if (isNumber) list.firstOrNull { it.songNumber.toString() == q }?.let { pick(it); query = ""; root.requestFocus() }
            }
            if (isNumber && list.isNotEmpty()) Text("Press Enter to open song $q", color = theme.colors.primaryBright, fontSize = 12.5.sp, modifier = Modifier.offset(y = (-6).dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SongFilter.entries.forEach { f -> Pill(f.label, filter == f) { filter = f } }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                items(list, key = { it.id }) { s -> SongRow(s, s.id == song?.id, s.id in favs, s.id in set, onPick = { pick(s) }) }
                if (list.isEmpty()) item {
                    Text(
                        if (q.isNotEmpty()) "No song matches “$query”. Try a number or a line from the chorus." else "No songs here yet.",
                        color = theme.colors.text, fontSize = 13.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 24.dp)
                    )
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(theme.colors.border))

        // song view
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (song == null) {
                Text("Pick a song on the left.", color = theme.colors.text, fontSize = 14.sp)
                return@Column
            }
            key(song.id) {
                Row(Modifier.fillMaxWidth().rise(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(theme.colors.primary, theme.colors.primary.copy(alpha = 0.7f)))),
                        contentAlignment = Alignment.Center
                    ) { Text("${song.songNumber}", color = Ink, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 32.sp) }
                    Column(Modifier.weight(1f)) {
                        val verses = song.lyrics.count { !it.isChorus() }
                        Text("${book?.name ?: ""} · $verses verses${if (song.lyrics.any { it.isChorus() }) " · chorus" else ""}", color = theme.colors.text, fontSize = 12.5.sp)
                        Text(song.title(lang), color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 38.sp)
                        if (song.titles.size > 1) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            song.titles.forEach { t ->
                                Pill(t.languageCode.uppercase(), (lang ?: song.titles.first().languageCode).equals(t.languageCode, true), small = true) { lang = t.languageCode }
                            }
                            Text(
                                if (lang != null && !lang.equals(song.titles.first().languageCode, true)) "Original: ${song.title()}"
                                else song.titles.drop(1).joinToString(" · ") { it.title },
                                color = theme.colors.text, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val fav = song.id in favs
                        Ghost(if (fav) "Favourite" else "Add to favourites", icon = { Icon(heartIcon(Rose, fav), null, tint = Color.Unspecified, modifier = Modifier.size(15.dp)) }) { SongPrefs.toggleFav(song.id) }
                        Ghost(if (song.id in set) "✓ In service set" else "+ Add to set") { SongPrefs.toggleSet(song.id) }
                        Row(
                            Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(theme.colors.primary).clickable(role = Role.Button) { project() }.hand().padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(projectorIcon(Ink), null, tint = Color.Unspecified, modifier = Modifier.size(15.dp))
                            Text("Project", color = Ink, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // order strip
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ORDER", color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(top = 8.dp, end = 4.dp))
                        seq.forEachIndexed { i, l -> OrderChip(l, i == at) { show(i) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (song.lyrics.any { it.isChorus() } && song.lyrics.any { !it.isChorus() }) Row(
                            Modifier.clickable(role = Role.Switch) { chorusAfter = !chorusAfter; stanza = 0 }.hand(),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniSwitch(chorusAfter)
                            Text("Chorus after each verse", color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 12.5.sp)
                        }
                        Box(Modifier.width(1.dp).height(18.dp).background(theme.colors.border))
                        SquareButton("A−") { size = (size - 1).coerceAtLeast(0) }
                        SquareButton("A+") { size = (size + 1).coerceAtMost(3) }
                    }
                }

                // stanza cards: two columns, the chorus across both
                val fontSize = listOf(17, 19, 22, 25)[size]
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    var row = mutableListOf<ISongLyricLine>()
                    val rows = mutableListOf<List<ISongLyricLine>>()
                    song.lyrics.forEach { l ->
                        if (l.isChorus()) { if (row.isNotEmpty()) rows += row; rows += listOf(l); row = mutableListOf() }
                        else { row += l; if (row.size == 2) { rows += row; row = mutableListOf() } }
                    }
                    if (row.isNotEmpty()) rows += row
                    rows.forEachIndexed { r, items ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items.forEach { l ->
                                val first = seq.indexOf(l)
                                val isCur = seq.getOrNull(at) == l
                                StanzaCard(l, isCur, fontSize, Modifier.weight(1f).rise(60 * r)) { show(first.coerceAtLeast(0)) }
                            }
                            if (items.size == 1 && !items[0].isChorus()) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("↑ ↓" to "previous / next", "1–9" to "jump to verse", "C" to "chorus", "B" to "blank screen", "N" to "next song in set").forEach { (k, v) ->
                        Row {
                            Text(k, color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(" $v", color = theme.colors.text, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(theme.colors.border))

        // service set + screen
        Column(
            Modifier.width(320.dp).fillMaxHeight().background(theme.colors.surface).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val live = onScreen && !ProjectionDeck.blank
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PingDot(if (live) theme.colors.primary else theme.colors.text.copy(alpha = 0.5f), active = live)
                    Text(
                        if (!onScreen) "NOT PROJECTING" else if (ProjectionDeck.blank) "SCREEN BLANK" else "ON SCREEN",
                        color = if (live) theme.colors.primaryBright else theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
                    )
                }
                SquareButton(if (ProjectionDeck.blank) "Show" else "Blank", wide = true) { ProjectionDeck.blank = !ProjectionDeck.blank }
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF3A2A1F), Color(0xFF120F0E)), radius = 600f)).border(1.dp, theme.colors.border, RoundedCornerShape(12.dp)).padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                val stanzaNow = seq.getOrNull(at)
                if (stanzaNow != null && !(onScreen && ProjectionDeck.blank)) key(song?.id, at) {
                    Text(stanzaNow.text(), color = Color(0xFFFFF6E2), fontFamily = Fraunces, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center, modifier = Modifier.rise(distance = 4.dp))
                }
                Text(
                    "${song?.title() ?: ""} · ${stanzaNow?.name() ?: ""}".uppercase(), color = theme.colors.text, fontSize = 9.sp, letterSpacing = 0.8.sp,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(theme.colors.border))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Service set", color = theme.colors.primaryText, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 20.sp)
                Text("${set.size} song${if (set.size == 1) "" else "s"}", color = theme.colors.text, fontSize = 12.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                set.forEachIndexed { i, id ->
                    val s = songs.firstOrNull { it.id == id }
                    key(id) { SetRow(i, s?.title() ?: "Song from another book", s?.let { "No. ${it.songNumber} · ${it.lyrics.size} parts" } ?: "", id == song?.id,
                        onPick = { s?.let { pick(it) } }, onRemove = { SongPrefs.toggleSet(id) }, onMove = { to -> SongPrefs.move(i, to) }) }
                }
                if (set.isEmpty()) Text(
                    "Add songs with + to plan the service. They project in order.", color = theme.colors.text, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 18.dp)
                )
            }
            Box(
                Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(12.dp)).background(theme.colors.menu).border(1.dp, theme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { nextInSet() }.hand(),
                contentAlignment = Alignment.Center
            ) { Text("Next song in set  →", color = theme.colors.primaryBright, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("RECENTLY SUNG", color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 4.dp))
                if (recent.isEmpty()) Text("Songs you project show up here.", color = theme.colors.text, fontSize = 12.5.sp)
                recent.take(5).forEach { r ->
                    val s = songs.firstOrNull { it.id == r.songId }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                            books.firstOrNull { it.id == r.bookId }?.takeIf { it.id != book?.id }?.let { model.selectBook(it) }
                            model.songs.value.firstOrNull { it.id == r.songId }?.let { pick(it) }
                        }.hand().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(s?.let { "${it.songNumber} · ${it.title()}" } ?: "Song from another book", color = theme.colors.primaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(sungWhen(r.at), color = theme.colors.text, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun sungWhen(at: Long): String {
    val d = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(d, LocalDate.now())
    return when {
        days == 0L -> "Today"
        days < 7 -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
        else -> d.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }
}

@Composable
private fun SearchBox(value: String, focus: FocusRequester, onChange: (String) -> Unit, onFocus: (Boolean) -> Unit, onEnter: () -> Unit) {
    val theme = LocalTheme.current
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) theme.colors.primary else theme.colors.border, tween(200))
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(theme.colors.menu).border(1.dp, border, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(searchIcon(theme.colors.text), null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Number, title or any line…", color = theme.colors.text, fontSize = 14.sp, maxLines = 1)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = theme.colors.primaryText, fontSize = 14.sp, fontFamily = Inter),
                cursorBrush = SolidColor(theme.colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onEnter() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }
                    .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.Enter) { onEnter(); true } else false }
            )
        }
        Text(
            "Ctrl K", color = theme.colors.text, fontSize = 11.sp,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun Pill(label: String, on: Boolean, small: Boolean = false, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val bg by animateColorAsState(if (on) theme.colors.primary else theme.colors.menu, tween(200))
    Text(
        label, color = if (on) Ink else theme.colors.primaryText.copy(alpha = 0.85f), fontSize = if (small) 11.5.sp else 12.5.sp,
        fontWeight = if (small) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.height(if (small) 26.dp else 30.dp).clip(CircleShape).background(bg)
            .border(1.dp, if (on) theme.colors.primary else theme.colors.border, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick).hand().padding(horizontal = if (small) 10.dp else 12.dp, vertical = if (small) 5.dp else 7.dp)
    )
}

@Composable
private fun SongRow(s: ISongDetails, current: Boolean, fav: Boolean, inSet: Boolean, onPick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val shift by animateDpAsState(if (hovered) 3.dp else 0.dp, tween(250, easing = components.global.SoftEase))
    val bg by animateColorAsState(if (current) theme.colors.primary.copy(alpha = 0.14f) else if (hovered) theme.colors.menuHoverColor else Color.Transparent, tween(200))
    Row(
        Modifier.fillMaxWidth().offset(x = shift).clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.dp, if (current) theme.colors.primary.copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(12.dp))
            .onHover { hovered = it }.clickable(onClick = onPick).hand().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (current) theme.colors.primary else theme.colors.menu),
            contentAlignment = Alignment.Center
        ) { Text("${s.songNumber}", color = if (current) Ink else theme.colors.primaryText.copy(alpha = 0.8f), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        Column(Modifier.weight(1f)) {
            Text(s.title(), color = theme.colors.primaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.lyrics.firstOrNull()?.lines?.firstOrNull() ?: "", color = theme.colors.text, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (fav) Icon(heartIcon(Rose), "Favourite", tint = Color.Unspecified, modifier = Modifier.size(14.dp))
        if (hovered || inSet) Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClickLabel = if (inSet) "Remove from service set" else "Add to service set") { SongPrefs.toggleSet(s.id) }.hand(),
            contentAlignment = Alignment.Center
        ) {
            if (inSet) Text("✓", color = theme.colors.primaryBright, fontSize = 13.sp)
            else Icon(plusIcon(theme.colors.primaryText), null, tint = Color.Unspecified, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun OrderChip(l: ISongLyricLine, current: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val lift by animateDpAsState(if (hovered) (-2).dp else 0.dp, tween(200))
    val bg by animateColorAsState(if (current) theme.colors.primary else if (l.isChorus()) theme.colors.primary.copy(alpha = 0.10f) else theme.colors.menuHoverColor, tween(200))
    Box(
        Modifier.offset(y = lift).widthIn(min = 36.dp).height(30.dp).clip(RoundedCornerShape(8.dp)).background(bg)
            .border(1.dp, if (current) theme.colors.primary else theme.colors.border, RoundedCornerShape(8.dp))
            .onHover { hovered = it }.clickable(role = Role.Tab, onClickLabel = l.name(), onClick = onClick).hand().padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(l.short(), color = if (current) Ink else if (l.isChorus()) theme.colors.primaryBright else theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StanzaCard(l: ISongLyricLine, current: Boolean, fontSize: Int, modifier: Modifier, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (current) theme.colors.primary else if (hovered) theme.colors.primary.copy(alpha = 0.4f) else theme.colors.border, tween(300))
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape)
            .background(if (current) Brush.linearGradient(listOf(theme.colors.primary.copy(alpha = 0.18f), theme.colors.menu)) else SolidColor(theme.colors.menu))
            .border(1.dp, border, shape).onHover { hovered = it }.clickable(onClick = onClick).hand()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(l.name().uppercase(), color = if (l.isChorus()) theme.colors.primaryBright else theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            if (current) Text(
                "ON SCREEN", color = Ink, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(theme.colors.primary).padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(if (l.isChorus()) "C" else "${l.verseNumber ?: ""}", color = theme.colors.text.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Text(
            l.text(), fontFamily = Fraunces, fontSize = fontSize.sp, lineHeight = (fontSize * 1.55f).sp,
            color = if (current) Color(0xFFFFF6E2) else theme.colors.primaryText.copy(alpha = 0.85f),
            fontStyle = if (l.isChorus()) FontStyle.Italic else FontStyle.Normal
        )
    }
}

@Composable
private fun SetRow(i: Int, title: String, meta: String, current: Boolean, onPick: () -> Unit, onRemove: () -> Unit, onMove: (Int) -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf(0f) }
    Row(
        Modifier.fillMaxWidth().rise(distance = 0.dp).offset(y = with(androidx.compose.ui.platform.LocalDensity.current) { drag.toDp() })
            .clip(RoundedCornerShape(10.dp)).background(if (current) theme.colors.primary.copy(alpha = 0.14f) else if (hovered) theme.colors.menuHoverColor else Color.Transparent)
            .onHover { hovered = it }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // drag the grip to reorder: every row height moved is one place
        Text(
            "⋮⋮", color = theme.colors.text.copy(alpha = 0.6f), fontSize = 13.sp,
            modifier = Modifier.pointerInput(i) {
                detectDragGestures(onDragEnd = { onMove(i + (drag / 52.dp.toPx()).roundToInt()); drag = 0f }, onDragCancel = { drag = 0f }) { c, d -> c.consume(); drag += d.y }
            }
        )
        Text("${i + 1}", color = theme.colors.text, fontSize = 12.sp, modifier = Modifier.width(14.dp))
        Column(Modifier.weight(1f).clickable(onClick = onPick).hand()) {
            Text(title, color = if (current) theme.colors.primaryBright else theme.colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, color = theme.colors.text, fontSize = 11.5.sp)
        }
        if (hovered) Text("×", color = theme.colors.text, fontSize = 16.sp, modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Remove from set", onClick = onRemove).hand().padding(horizontal = 4.dp))
    }
}

@Composable
private fun MiniSwitch(on: Boolean) {
    val theme = LocalTheme.current
    val track by animateColorAsState(if (on) theme.colors.primary else theme.colors.border, tween(200))
    val x by animateDpAsState(if (on) 16.dp else 2.dp, tween(200))
    Box(Modifier.width(32.dp).height(18.dp).clip(CircleShape).background(track)) {
        Box(Modifier.offset(x = x, y = 2.dp).size(14.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun SquareButton(label: String, wide: Boolean = false, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    Box(
        Modifier.then(if (wide) Modifier.height(28.dp) else Modifier.size(30.dp)).clip(RoundedCornerShape(8.dp))
            .background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(8.dp))
            .onHover { hovered = it }.clickable(role = Role.Button, onClick = onClick).hand().padding(horizontal = if (wide) 10.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = theme.colors.primaryText, fontSize = 12.sp) }
}

@Composable
private fun Ghost(label: String, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu)
            .border(1.dp, theme.colors.border, RoundedCornerShape(12.dp)).onHover { hovered = it }.clickable(role = Role.Button, onClick = onClick).hand().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.invoke()
        Text(label, color = theme.colors.primaryText, fontSize = 13.sp)
    }
}
