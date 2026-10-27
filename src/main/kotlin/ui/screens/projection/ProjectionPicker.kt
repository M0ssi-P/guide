package ui.screens.projection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.searchIcon
import components.global.AsyncImageFromFile
import components.global.rise
import db.controller.table.Sermons.getFullSermon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mvvm.BibleViewModel
import mvvm.SongBookViewModal
import navigation.Navigator
import org.jetbrains.jewel.foundation.modifier.onHover
import parsers.bible.models.IBibleVersion
import parsers.vgr.VgrViewModel
import perVerseMode
import presentation.DeckItem
import presentation.DeckSource
import presentation.DisplayView
import presentation.Displays
import presentation.LocalWindowController
import presentation.PictureLibrary
import presentation.ProjectionDeck
import presentation.ProjectionEntry
import presentation.Shows
import presentation.rememberScreens
import presentation.sentences
import ui.config.DB
import ui.screens.bible.chapterIn
import ui.screens.bible.plain
import ui.screens.hymns.deckFor
import ui.screens.hymns.sequence
import ui.screens.hymns.title
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import java.io.File

private val Gold = Color(0xFFC9A15F)

@Composable
private fun chip(on: Boolean): Triple<Color, Color, Color> =
    Triple(if (on) Gold else Color.Transparent, if (on) Color(0xFF1B1509) else Color(0xFFC7CDD8), if (on) Gold else Color(0xFF2A313E))

@Composable
private fun StageChip(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (bg, fg, border) = chip(on)
    val b by animateColorAsState(bg, tween(200))
    Box(
        modifier.height(28.dp).clip(RoundedCornerShape(8.dp)).background(b).border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(role = Role.Tab, onClick = onClick).hand().padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = fg, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

private val refRegex = Regex("""^(\d?\s?[a-z]+)\s*(\d+)?(?::(\d+))?$""")

/**
 * The card at the top of the left column: what is loaded, and a picker to change it
 * (Bible: translation, book, chapter; Songs: book and song; Table: any sermon).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SourcePicker(nav: Navigator, onNotice: (String?) -> Unit) {
    val deck = ProjectionDeck
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var query by remember(deck.source) { mutableStateOf("") }
    LaunchedEffect(deck.source) { open = deck.source == DeckSource.BIBLE || deck.source == DeckSource.SONGS }
    val images = deck.source == DeckSource.IMAGES

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(StagePanel).border(1.dp, if (open) Gold else StageBorder, RoundedCornerShape(12.dp))
                .clickable(enabled = !images) { open = !open; query = "" }.hand().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(deck.title.ifBlank { "Nothing loaded" }, color = StageText, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(deck.subtitle, color = StageMuted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 1.dp))
            }
            if (!images) Text(if (open) "Done" else "Change", color = Color(0xFFEFCB7E), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }
        AnimatedVisibility(open && !images, enter = expandVertically(tween(300)) + fadeIn(), exit = shrinkVertically(tween(250)) + fadeOut()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(StagePanel).border(1.dp, StageBorder, RoundedCornerShape(14.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FindField(
                    query, { query = it },
                    when (deck.source) { DeckSource.BIBLE -> "Book or reference, e.g. Rom 12:5"; DeckSource.SONGS -> "Song number or title"; else -> "Search sermons by title or date code" }
                )
                when (deck.source) {
                    DeckSource.BIBLE -> BiblePicker(nav, query) { open = false }
                    DeckSource.SONGS -> SongPicker(nav, query)
                    DeckSource.TABLE -> SermonPicker(nav, query) { s ->
                        scope.launch {
                            val full = withContext(Dispatchers.IO) { runCatching { DB.connection("main.db").getFullSermon(s) }.getOrNull() }
                            if (full == null) onNotice("Couldn't open ${s.title}.")
                            else { deck.load(DeckSource.TABLE, full.title, "${full.dateCode} · ${full.location}", full.deckItems(), 0, full.copy(sections = null)); onNotice(null); open = false }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun FindField(value: String, onChange: (String) -> Unit, hint: String) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)).background(StageBg).border(1.dp, if (focused) Gold else Color(0xFF2A313E), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(searchIcon(StageMuted), null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = StageMuted, fontSize = 13.sp, maxLines = 1)
            BasicTextField(
                value, onChange, singleLine = true, textStyle = TextStyle(color = StageText, fontSize = 13.sp, fontFamily = Inter),
                cursorBrush = SolidColor(Gold), modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BiblePicker(nav: Navigator, query: String, onPicked: () -> Unit) {
    val model = nav.viewModelStore.getOrCreate("BibleVM-${nav.id}") { BibleViewModel().apply { initilize() } }
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(model.currentVersion) }
    val books = version?.books ?: emptyList()
    // the book on screen, else the reader's
    var bookUsfm by remember { mutableStateOf(ProjectionDeck.items.firstOrNull()?.let { null } ?: model.currentBook?.usfm) }
    val q = query.trim().lowercase()
    val m = refRegex.find(q)
    val matchBook: (IBibleVersion.Book) -> Boolean = { b -> m == null || b.human.lowercase().replace(" ", "").startsWith(m.groupValues[1].replace(" ", "")) || b.usfm.lowercase().startsWith(m.groupValues[1].replace(" ", "")) }
    val shown = if (q.isEmpty()) books else books.filter(matchBook)
    val book = books.firstOrNull { it.usfm == bookUsfm } ?: shown.firstOrNull()

    fun load(v: IBibleVersion, b: IBibleVersion.Book, chapter: IBibleVersion.Chapter, verse: Int?) = scope.launch {
        val content = chapterIn(v, chapter.usfm) ?: return@launch
        val verses = perVerseMode(content.chapter)
        val name = "${b.human} ${chapter.human}"
        val items = verses.flatMap { DeckItem(it.number, "$name:${it.number}", it.plain()).sentences() }
        ProjectionDeck.load(DeckSource.BIBLE, name, "${v.localAbbreviation} · ${verses.size} verses", items, verse?.let { n -> items.indexOfFirst { it.number == n } }?.coerceAtLeast(0) ?: 0)
        onPicked()
    }
    // "rom 12:5" jumps straight there
    LaunchedEffect(q) {
        val v = version ?: return@LaunchedEffect
        val ch = m?.groupValues?.get(2)?.toIntOrNull() ?: return@LaunchedEffect
        val b = shown.firstOrNull() ?: return@LaunchedEffect
        b.chapters.firstOrNull { it.human == ch.toString() }?.let { load(v, b, it, m.groupValues[3].toIntOrNull()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val versions = (listOfNotNull(model.currentVersion) + model.versions).distinctBy { it.id }.take(4)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            versions.forEach { v -> StageChip(v.localAbbreviation, v.id == version?.id, Modifier.weight(1f)) { version = v } }
        }
        Text("BOOK", color = Color(0xFF6F7787), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        FlowRow(Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            shown.forEach { b -> StageChip(b.human, b.usfm == book?.usfm) { bookUsfm = b.usfm } }
        }
        if (q.isNotEmpty() && shown.isEmpty()) Text("Nothing matches “$query”.", color = StageMuted, fontSize = 12.sp)
        Text("CHAPTER", color = Color(0xFF6F7787), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        val chapters = book?.chapters ?: emptyList()
        Column(Modifier.heightIn(max = 110.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chapters.chunked(8).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { c ->
                        val on = ProjectionDeck.source == DeckSource.BIBLE && ProjectionDeck.title == "${book?.human} ${c.human}"
                        StageChip(c.human, on, Modifier.weight(1f)) { version?.let { v -> book?.let { load(v, it, c, null) } } }
                    }
                    repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SongPicker(nav: Navigator, query: String) {
    val model = nav.viewModelStore.getOrCreate("SongBookVM-${nav.id}") { SongBookViewModal().apply { initilize() } }
    val books by model.songBooks.collectAsState()
    var bookId by remember { mutableStateOf(model.currentBook.value?.id) }
    val book = books.firstOrNull { it.id == bookId } ?: books.firstOrNull()
    val songs = remember(book?.id) { book?.let { runCatching { model.loadSongsFrom(it) }.getOrNull() } ?: emptyList() }
    val q = query.trim().lowercase()
    val isNum = q.isNotEmpty() && q.all { it.isDigit() }
    val results = songs.filter { q.isEmpty() || if (isNum) it.songNumber.toString().startsWith(q) else it.title().lowercase().contains(q) }.sortedBy { it.songNumber }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            books.take(4).forEach { b -> StageChip(b.name, b.id == book?.id, Modifier.weight(1f)) { bookId = b.id } }
        }
        Column(Modifier.heightIn(max = 190.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            results.take(80).forEach { s ->
                val cur = ProjectionDeck.source == DeckSource.SONGS && ProjectionDeck.title == s.title()
                ResultRow(cur, {
                    Text("${s.songNumber}", color = if (cur) Color(0xFFEFCB7E) else StageMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                    Text(s.title(), color = StageText, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }) {
                    ProjectionDeck.load(DeckSource.SONGS, s.title(), "No. ${s.songNumber} · ${book?.name ?: ""}", deckFor(s, sequence(s, true)))
                }
            }
            if (results.isEmpty()) Text("Nothing matches “$query”.", color = StageMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SermonPicker(nav: Navigator, query: String, onPick: (parsers.vgr.models.Sermon) -> Unit) {
    val vgr = nav.viewModelStore.getOrCreate("HomeVM-${nav.id}") { VgrViewModel() }
    val sermons by vgr.sermons.collectAsState()
    val q = query.trim().lowercase()
    val results = sermons.filter { q.isEmpty() || (it.title + " " + it.dateCode).lowercase().contains(q) }.take(60)
    Column(Modifier.heightIn(max = 210.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        results.forEach { s ->
            val cur = ProjectionDeck.sermon?.stringId == s.stringId
            ResultRow(cur, {
                Column {
                    Text(s.title, color = StageText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("${s.dateCode} · ${s.location}", color = StageMuted, fontSize = 11.sp)
                }
            }) { onPick(s) }
        }
        if (results.isEmpty()) Text(if (sermons.isEmpty()) "No sermons yet. Open The Table once to load them." else "Nothing matches “$query”.", color = StageMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ResultRow(current: Boolean, content: @Composable () -> Unit, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val shift by animateDpAsState(if (hovered) 2.dp else 0.dp, tween(250))
    Row(
        Modifier.fillMaxWidth().offset(x = shift).clip(RoundedCornerShape(9.dp))
            .background(if (current) Gold.copy(alpha = 0.12f) else if (hovered) Color(0xFF121722) else Color.Transparent)
            .border(1.dp, if (current) Gold.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(9.dp))
            .onHover { hovered = it }.clickable(onClick = onClick).hand().padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/** The pictures library: add, show, delete. */
@Composable
internal fun PicturesGrid() {
    val pics by PictureLibrary.pictures
    val deck = ProjectionDeck
    LaunchedEffect(pics) {
        if (deck.source == DeckSource.IMAGES) deck.load(DeckSource.IMAGES, "Pictures", "${pics.size} in your library", PictureLibrary.items(), deck.index)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf<File?>(null) + pics).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { f ->
                    if (f == null) {
                        var hovered by remember { mutableStateOf(false) }
                        Column(
                            Modifier.weight(1f).aspectRatio(16f / 10f).clip(RoundedCornerShape(10.dp)).background(StagePanel)
                                .border(1.dp, if (hovered) Gold else Color(0xFF3A4150), RoundedCornerShape(10.dp)).onHover { hovered = it }
                                .clickable(role = Role.Button) { PictureLibrary.add(PictureLibrary.choose()) }.hand(),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                        ) {
                            Text("+", color = if (hovered) Color(0xFFEFCB7E) else StageSoft, fontSize = 20.sp)
                            Text("Add image", color = if (hovered) Color(0xFFEFCB7E) else StageSoft, fontSize = 12.sp)
                        }
                    } else PictureTile(f, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text("Press Add to bring in pictures. Click one to put it on screen. Hover to delete.", color = Color(0xFF6F7787), fontSize = 11.5.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun PictureTile(f: File, modifier: Modifier) {
    val deck = ProjectionDeck
    var hovered by remember { mutableStateOf(false) }
    val lift by animateDpAsState(if (hovered) (-2).dp else 0.dp, tween(250))
    val current = deck.source == DeckSource.IMAGES && deck.current?.image == f.absolutePath
    Box(
        modifier.rise(distance = 8.dp).offset(y = lift).aspectRatio(16f / 10f).clip(RoundedCornerShape(10.dp))
            .border(2.dp, if (current) Gold else StageBorder, RoundedCornerShape(10.dp)).onHover { hovered = it }
            .clickable(role = Role.Button, onClickLabel = "Show ${f.nameWithoutExtension}") {
                val items = PictureLibrary.items()
                deck.load(DeckSource.IMAGES, "Pictures", "${items.size} in your library", items, items.indexOfFirst { it.image == f.absolutePath }.coerceAtLeast(0))
            }.hand()
    ) {
        AsyncImageFromFile(f, true)
        Text(f.nameWithoutExtension, color = StageText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp))
        if (hovered) Box(
            Modifier.align(Alignment.TopEnd).padding(5.dp).size(22.dp).clip(CircleShape).background(Color(0xCC090B10))
                .clickable(role = Role.Button, onClickLabel = "Delete ${f.nameWithoutExtension}") { PictureLibrary.remove(f) }.hand(),
            contentAlignment = Alignment.Center
        ) { Text("×", color = Color.White, fontSize = 13.sp) }
    }
}

/**
 * A card per screen: a small live preview, what it shows (follow, next up, a picture, a held slide, blank),
 * and a button to put it on that screen or take it off.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DisplaysSection(accent: Color, lit: Color) {
    val windowController = LocalWindowController.current
    val screens = rememberScreens()
    val pics by PictureLibrary.pictures
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("DISPLAYS", color = StageSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text("Each screen can follow you, or hold its own picture, song or verse.", color = Color(0xFF6F7787), fontSize = 11.5.sp)
        }
        screens.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { dev ->
                    val i = screens.indexOf(dev)
                    val feed = Displays.feed(i)
                    val b = dev.defaultConfiguration.bounds
                    val onAir = windowController.presentation && windowController.availableProjection[i]?.viewModel === feed
                    val following = feed.shows == Shows.Follow
                    var hovered by remember { mutableStateOf(false) }
                    val lift by animateDpAsState(if (hovered) (-2).dp else 0.dp, tween(250))
                    var menu by remember { mutableStateOf(false) }
                    Column(
                        Modifier.weight(1f).offset(y = lift).clip(RoundedCornerShape(14.dp)).background(StagePanel)
                            .border(1.dp, if (following && onAir) Gold.copy(alpha = 0.55f) else StageBorder, RoundedCornerShape(14.dp)).onHover { hovered = it }.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
                            DisplayView(feed, accent, lit, Modifier.fillMaxSize())
                            Text(
                                feed.shows.tag, color = if (following) Color(0xFF1B1509) else Color(0xFFC7CDD8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(6.dp).clip(CircleShape).background(if (following) Gold else Color(0xBF090B10)).padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (i == 0) "Display 1 (this screen)" else "Display ${i + 1}", color = StageText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("${b.width} × ${b.height}", color = StageMuted, fontSize = 10.5.sp)
                            }
                            Text(
                                if (onAir) "On" else "Off", color = if (onAir) Color(0xFF1B1509) else StageSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(if (onAir) Gold else Color.Transparent).border(1.dp, if (onAir) Gold else Color(0xFF2A313E), CircleShape)
                                    .clickable(role = Role.Switch, onClickLabel = if (onAir) "Turn this screen off" else "Show on this screen") {
                                        if (onAir) windowController.removeScreen(i)
                                        else {
                                            val existed = windowController.containsKey(i)
                                            val entry = windowController.getOrCreate(i) { ProjectionEntry(device = dev, bounds = b, fullScreen = true, viewModel = feed) }
                                            if (existed && entry.viewModel !== feed) windowController.updateProjection(i, entry.copy(viewModel = feed))
                                            windowController.openPresentation()
                                        }
                                    }.hand().padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text("SHOWS", color = Color(0xFF6F7787), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Box {
                            Text(
                                when (val s = feed.shows) {
                                    Shows.Follow -> "Follow what I control"; Shows.Next -> "Next up (for the stage)"; Shows.Blank -> "Blank"
                                    is Shows.Picture -> "Picture: ${File(s.path).nameWithoutExtension}"; is Shows.Pinned -> "Pinned: ${s.item.label}"
                                } + "  ▾",
                                color = StageText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp)).background(StageBg).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(8.dp))
                                    .clickable(role = Role.DropdownList) { menu = true }.hand().padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                            DropdownMenu(menu, { menu = false }, Modifier.background(StagePanel).heightIn(max = 320.dp)) {
                                val options = listOf<Pair<String, Shows>>("Follow what I control" to Shows.Follow, "Next up (for the stage)" to Shows.Next) +
                                    pics.map { "Picture: ${it.nameWithoutExtension}" to Shows.Picture(it.absolutePath) } +
                                    listOfNotNull((feed.shows as? Shows.Pinned)?.let { "Pinned: ${it.item.label}" to it }) + ("Blank" to Shows.Blank)
                                options.forEach { (label, s) ->
                                    DropdownMenuItem(onClick = { feed.shows = s; menu = false }) { Text(label, color = StageText, fontSize = 12.5.sp) }
                                }
                            }
                        }
                        Text(
                            if (following) "Hold what is showing now" else "Follow me again", color = Color(0xFFC7CDD8), fontSize = 11.5.sp,
                            modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF2A313E), RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) {
                                    feed.shows = if (!following) Shows.Follow
                                    else ProjectionDeck.current?.let { cur -> cur.image?.let { Shows.Picture(it) } ?: Shows.Pinned(cur, ProjectionDeck.footer) } ?: Shows.Blank
                                }.hand().padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** "YOU ARE CONTROLLING · …": the screens that follow the deck. */
@Composable
internal fun controlledScreens(): String {
    val windowController = LocalWindowController.current
    val on = Displays.feeds.filter { (i, f) -> windowController.presentation && windowController.availableProjection[i]?.viewModel === f }
    if (on.isEmpty()) return "PREVIEW · NOT ON A SCREEN YET"
    val following = on.filter { it.value.shows == Shows.Follow }.keys.sorted()
    return "YOU ARE CONTROLLING · " + if (following.isEmpty()) "NO SCREEN (ALL HOLDING)" else following.joinToString(" + ") { "DISPLAY ${it + 1}" }
}
