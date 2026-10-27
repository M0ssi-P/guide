package ui.screens.hymns

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.AsyncImageFromFile
import components.global.popIn
import components.global.rise
import db.controller.songbooks.BookMeta
import db.controller.songbooks.CustomBooks
import db.controller.songbooks.DraftSong
import db.controller.songbooks.SongBookPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.ISongDetails
import models.IProviderStats
import models.LyricType
import mvvm.SongBookViewModal
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WorkshopTab(val label: String) { LIBRARY("My books"), SHOP("Shop"), CREATE("Create"), TRANSFER("Export & import") }

/** Which Workshop tab and book to open on (set by Songs and the empty screen before navigating). */
object WorkshopState {
    var tab by mutableStateOf(WorkshopTab.LIBRARY)
    var bookId by mutableStateOf<String?>(null)
}

private val Ink = Color(0xFF2A1A0E)
private val Green = Color(0xFF7FD9A8)
private val Amber = Color(0xFFF2C46B)
private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private fun fmt(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFmt)

internal fun sectionColor(t: LyricType) = when (t) { LyricType.VERSE -> Color(0xFFA8A099); LyricType.CHORUS -> Color(0xFFE0813A); LyricType.BRIDGE -> Color(0xFF9C82E0); LyricType.OTHER -> Color(0xFF4FBFB0) }
private fun LyricType.label() = name.lowercase().replaceFirstChar { it.uppercase() }

/** Cover colours for books without a logo, picked from the name so they stay the same. */
internal fun coverFor(name: String): Brush {
    val pairs = listOf(Color(0xFF8A3A1A) to Color(0xFF2A120A), Color(0xFF3A5A8A) to Color(0xFF142034), Color(0xFF6A2A4A) to Color(0xFF200A16), Color(0xFF2A6A4A) to Color(0xFF0A2016), Color(0xFF5A4A6A) to Color(0xFF16101E))
    val (a, b) = pairs[(name.hashCode() and 0x7fffffff) % pairs.size]
    return Brush.linearGradient(listOf(a, b))
}
internal fun monoOf(name: String) = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "♪" }

private fun chooseFile(title: String, exts: Set<String>, save: Boolean = false, suggested: String? = null): File? {
    val d = java.awt.FileDialog(null as java.awt.Frame?, title, if (save) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD)
    if (!save) d.setFilenameFilter { _, n -> n.substringAfterLast('.').lowercase() in exts }
    suggested?.let { d.file = it }
    d.isVisible = true
    return d.file?.let { File(d.directory, it) }
}

private fun chooseFolder(): String? {
    val c = javax.swing.JFileChooser().apply { fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY; dialogTitle = "Choose a backup folder" }
    return if (c.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) c.selectedFile.absolutePath else null
}

@Composable
fun SongWorkshop() {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    val model = nav.viewModelStore.getOrCreate("SongBookVM-${nav.id}") { SongBookViewModal().apply { initilize() } }
    val books by model.songBooks.collectAsState()
    var toast by remember { mutableStateOf<Pair<String, (() -> Unit)?>?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(3200); toast = null } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 24.dp),
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("← Songs", color = theme.colors.text, fontSize = 12.5.sp, modifier = Modifier.clickable { nav.navigate(Screen.Library(navigation.LibraryScreen.Hymns())) }.hand())
                    Text("Song Workshop", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 38.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Get song books, make your own, and move them between computers.", color = theme.colors.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Row(Modifier.clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).padding(3.dp)) {
                    WorkshopTab.entries.forEach { t ->
                        val on = WorkshopState.tab == t
                        val bg by animateColorAsState(if (on) theme.colors.primary else Color.Transparent, tween(200))
                        Row(
                            Modifier.height(34.dp).clip(RoundedCornerShape(4.dp)).background(bg).clickable(role = Role.Tab) { WorkshopState.tab = t }.hand().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(t.label, color = if (on) Ink else theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (t == WorkshopTab.LIBRARY && books.isNotEmpty()) Text("${books.size}", color = (if (on) Ink else theme.colors.primaryText).copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
            AnimatedContent(WorkshopState.tab, Modifier.weight(1f), transitionSpec = { (fadeIn(tween(300)) + slideInVertically(tween(400)) { 24 }) togetherWith fadeOut(tween(150)) }) { t ->
                when (t) {
                    WorkshopTab.LIBRARY -> LibraryTab(model)
                    WorkshopTab.SHOP -> ShopTab(model)
                    WorkshopTab.CREATE -> CreateTab(model) { msg, undo -> toast = msg to undo }
                    WorkshopTab.TRANSFER -> TransferTab(model) { msg -> toast = msg to null }
                }
            }
        }
        AnimatedVisibility(toast != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
            val (msg, undo) = toast ?: ("" to null)
            Row(
                Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF6F1EC)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(msg, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (undo != null) Text("Undo", color = Color(0xFFB5561B), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { undo(); toast = null }.hand())
            }
        }
    }
}

@Composable
private fun Pad(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 22.dp)) { content() }
}

@Composable
internal fun BookCover(name: String, logo: String?, modifier: Modifier, mono: Dp = 44.dp, showName: Boolean = true) {
    Box(modifier.background(coverFor(name)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(14.dp)) {
            Box(Modifier.size(mono).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                if (logo != null && File(logo).isFile) AsyncImageFromFile(File(logo), true)
                else Text(monoOf(name), color = Color(0xFFFFF6E2), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = (mono.value * 0.36f).sp)
            }
            if (showName) Text(name, color = Color(0xFFFFF6E2), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun Lift(modifier: Modifier = Modifier, content: @Composable (Boolean) -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val y by animateDpAsState(if (hovered) (-4).dp else 0.dp, tween(300, easing = components.global.SoftEase))
    Box(modifier.offset(y = y).onHover { hovered = it }) { content(hovered) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryTab(model: SongBookViewModal) {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    val books by model.songBooks.collectAsState()
    val custom = remember(books) { CustomBooks.all().associateBy { it.bookId } }
    val counts = remember(books) { books.associate { it.id to runCatching { model.loadSongsFrom(it).size }.getOrDefault(0) } }
    Pad {
        BoxWithConstraints {
            val cols = (maxWidth / 240.dp).toInt().coerceIn(2, 5)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = cols) {
                books.forEachIndexed { i, b ->
                    val m = custom[b.id]
                    Lift(Modifier.weight(1f).rise(i * 50)) { hovered ->
                        Column(Modifier.clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, if (hovered) theme.colors.primary.copy(alpha = 0.4f) else theme.colors.border, RoundedCornerShape(4.dp))) {
                            BookCover(b.name, m?.logo, Modifier.fillMaxWidth().aspectRatio(4f / 3f))
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${b.language.uppercase()} · ${counts[b.id] ?: 0} songs", color = theme.colors.text, fontSize = 12.sp)
                                Text(
                                    if (m == null) "FROM THE SHOP" else if (m.backup) "YOUR BOOK · BACKED UP" else "YOUR BOOK",
                                    color = if (m?.backup == true) Green else theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp
                                )
                                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SmallBtn(if (m != null) "Edit" else "Open", Modifier.weight(1f)) {
                                        if (m != null) { WorkshopState.bookId = b.id; WorkshopState.tab = WorkshopTab.CREATE }
                                        else { model.selectBook(b); nav.navigate(Screen.Library(navigation.LibraryScreen.Hymns())) }
                                    }
                                    SmallBtn("PDF") { WorkshopState.bookId = b.id; WorkshopState.tab = WorkshopTab.TRANSFER }
                                }
                            }
                        }
                    }
                }
                Lift(Modifier.weight(1f).rise(books.size * 50)) { hovered ->
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 260.dp).clip(RoundedCornerShape(4.dp))
                            .border(1.5.dp, if (hovered) theme.colors.primary else theme.colors.border, RoundedCornerShape(4.dp))
                            .background(if (hovered) theme.colors.primary.copy(alpha = 0.05f) else Color.Transparent)
                            .clickable { WorkshopState.bookId = null; WorkshopState.tab = WorkshopTab.CREATE }.hand(),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        Text("+", color = theme.colors.primaryBright, fontSize = 30.sp)
                        Text("Create a song book", color = theme.colors.primaryText, fontWeight = FontWeight.SemiBold)
                        Text("Your own songs, your own cover", color = theme.colors.text, fontSize = 12.sp)
                    }
                }
                repeat((cols - (books.size + 1) % cols) % cols) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun SmallBtn(label: String, modifier: Modifier = Modifier, color: Color? = null, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    Box(
        modifier.height(30.dp).clip(RoundedCornerShape(4.dp)).background(if (hovered) theme.colors.menuHoverColor else theme.colors.surface)
            .border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).onHover { hovered = it }.clickable(role = Role.Button, onClick = onClick).hand().padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = color ?: theme.colors.primaryText, fontSize = 12.sp) }
}

@Composable
internal fun PrimaryBtn(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Box(
        modifier.height(38.dp).clip(RoundedCornerShape(4.dp)).background(if (enabled) theme.colors.primary else theme.colors.border)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).hand().padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun Field(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", lines: Int = 1, style: TextStyle? = null, height: Dp = 36.dp, border: Color? = null) {
    val theme = LocalTheme.current
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.then(if (lines == 1) Modifier.height(height) else Modifier.heightIn(min = (lines * 22 + 16).dp)).clip(RoundedCornerShape(4.dp)).background(theme.colors.surface)
            .border(1.dp, border ?: if (focused) theme.colors.primary else theme.colors.border, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = if (lines == 1) 0.dp else 8.dp),
        contentAlignment = if (lines == 1) Alignment.CenterStart else Alignment.TopStart
    ) {
        val st = style ?: TextStyle(color = theme.colors.primaryText, fontSize = 14.sp, fontFamily = Inter)
        if (value.isEmpty()) Text(placeholder, style = st.copy(color = theme.colors.text))
        BasicTextField(value, onChange, singleLine = lines == 1, textStyle = st.copy(color = st.color.takeIf { it != Color.Unspecified } ?: theme.colors.primaryText), cursorBrush = SolidColor(theme.colors.primary),
            modifier = Modifier.fillMaxWidth().androidxFocus { focused = it })
    }
}

private fun Modifier.androidxFocus(on: (Boolean) -> Unit) = this.onFocusChanged { on(it.isFocused) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopTab(model: SongBookViewModal) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()
    val shop by SongShop.books
    val err by SongShop.error
    var q by remember { mutableStateOf("") }
    val installed by model.songBooks.collectAsState()
    LaunchedEffect(Unit) { if (SongShop.books.value == null) SongShop.refresh() }
    Pad {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Field(q, { q = it }, Modifier.weight(1f), "Search song books or languages", height = 42.dp)
                SmallBtn("Check again") { scope.launch { SongShop.refresh() } }
            }
            val list = (shop ?: emptyList()).filter { q.isBlank() || it.name.contains(q, true) || it.id.contains(q, true) }
            when {
                shop == null -> Text("Looking for song books…", color = theme.colors.text, fontSize = 13.5.sp)
                list.isEmpty() -> Text(
                    err ?: if (q.isBlank()) "There are no song books on the server yet. Check again later, or create your own." else "No song book matches “$q”.",
                    color = theme.colors.text, fontSize = 13.5.sp
                )
            }
            BoxWithConstraints {
                val cols = (maxWidth / 320.dp).toInt().coerceIn(1, 3)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), maxItemsInEachRow = cols) {
                    list.forEachIndexed { i, b ->
                        val have = installed.any { it.saveName == b.id || it.name.equals(b.name, true) }
                        val p = SongShop.progress[b.id]
                        Lift(Modifier.weight(1f).rise(i * 50)) {
                            Row(Modifier.clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                BookCover(b.name, null, Modifier.width(64.dp).height(84.dp).clip(RoundedCornerShape(4.dp)), mono = 34.dp, showName = false)
                                Column(Modifier.weight(1f).height(84.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(b.name, color = theme.colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                    Text(b.language.ifBlank { "Song book" }, color = theme.colors.text, fontSize = 12.sp)
                                    Spacer(Modifier.weight(1f))
                                    if (p != null) Progress(p, if (p < 0.7f) "Downloading" else "Adding to your library")
                                    else Text(
                                        if (have) "✓ In your library" else "Download", color = if (have) Green else Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (have) theme.colors.surface else theme.colors.primary)
                                            .clickable(enabled = !have) { scope.launch { if (SongShop.download(b)) model.refreshBooks() } }.hand().padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                    repeat((cols - list.size % cols) % cols) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
internal fun Progress(p: Float, label: String) {
    val theme = LocalTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.border)) {
            Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(theme.colors.primary, theme.colors.primaryBright))))
        }
        Text("$label · ${(p * 100).toInt()}%", color = theme.colors.text, fontSize = 11.sp)
    }
}

/** A song being edited: a working copy saved to the database with Save. */
private data class Draft(val id: String?, val number: String, val title: String, val parts: List<Pair<LyricType, String>>)

private fun ISongDetails.draft() = Draft(id.ifBlank { null }, "$songNumber", titles.firstOrNull()?.title ?: "", lyrics.map { it.type to it.lines.joinToString("\n") })

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateTab(model: SongBookViewModal, onToast: (String, (() -> Unit)?) -> Unit) {
    val theme = LocalTheme.current
    // open the chosen book, else the newest of your own, else start one
    var meta by remember {
        mutableStateOf(
            WorkshopState.bookId?.let { CustomBooks.meta(it) } ?: CustomBooks.all().maxByOrNull { it.created }
            ?: CustomBooks.create(BookMeta("", "New song book")).also { model.refreshBooks() }
        )
    }
    var songs by remember(meta.bookId) { mutableStateOf(CustomBooks.songs(meta.bookId)) }
    var draft by remember(meta.bookId) { mutableStateOf(songs.firstOrNull()?.draft() ?: Draft(null, "1", "", listOf(LyricType.VERSE to ""))) }
    var saved by remember { mutableStateOf(true) }
    var paste by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(0) }

    // book details save themselves shortly after typing stops (and rewrite the backup PDF when it's on)
    LaunchedEffect(meta) { delay(700); withContext(Dispatchers.IO) { CustomBooks.saveMeta(meta) }; model.refreshBooks() }

    val sorted = songs.sortedBy { it.songNumber }
    val contentsPages = ((sorted.size + 29) / 30).coerceAtLeast(1)
    val firstSong = 3 + contentsPages
    fun pageOf(s: ISongDetails) = firstSong + sorted.indexOf(s)
    val num = draft.number.toIntOrNull() ?: 0
    val taken = songs.firstOrNull { it.id != draft.id && it.songNumber == num }

    fun save() {
        val before = CustomBooks.songs(meta.bookId)
        val (id, used) = CustomBooks.saveSong(meta.bookId, DraftSong(draft.id, num, draft.title, draft.parts.filter { it.second.isNotBlank() }.ifEmpty { draft.parts }))
        songs = CustomBooks.songs(meta.bookId)
        draft = draft.copy(id = id, number = "$used")
        saved = true
        model.refreshBooks(meta.bookId)
        onToast("Saved “${draft.title.ifBlank { "Untitled" }}” as No. $used" + if (meta.backup) " · backup PDF updated" else "") {
            CustomBooks.restore(meta.bookId, before); songs = CustomBooks.songs(meta.bookId); model.refreshBooks(meta.bookId)
        }
    }
    fun setParts(p: List<Pair<LyricType, String>>) { draft = draft.copy(parts = p); saved = false }

    Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        // book + songs
        Column(Modifier.width(310.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel {
                Label("THE BOOK")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(4.dp)).background(if (meta.logo == null) theme.colors.surface else Color.Transparent)
                            .border(1.5.dp, theme.colors.border, RoundedCornerShape(4.dp))
                            .clickable(role = Role.Button, onClickLabel = "Choose a logo image") { chooseFile("Choose a logo", setOf("png", "jpg", "jpeg"))?.let { meta = meta.copy(logo = it.absolutePath) } }.hand(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (meta.logo != null) AsyncImageFromFile(File(meta.logo!!), true) else Text("Add logo", color = theme.colors.text, fontSize = 11.sp)
                    }
                    Text("Logo for the cover.\nPNG or JPG, square is best.", color = theme.colors.text, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Small("Title"); Field(meta.title, { meta = meta.copy(title = it) }, Modifier.fillMaxWidth())
                Small("Author"); Field(meta.author, { meta = meta.copy(author = it) }, Modifier.fillMaxWidth())
                Text("Created ${fmt(meta.created)} · set automatically", color = theme.colors.text, fontSize = 12.sp)
                Label("THIS BOOK BELONGS TO", Modifier.padding(top = 6.dp))
                Field(meta.owner, { meta = meta.copy(owner = it) }, Modifier.fillMaxWidth(), "Name or church")
                Field(meta.address, { meta = meta.copy(address = it) }, Modifier.fillMaxWidth(), "Address", lines = 2, style = TextStyle(fontSize = 13.sp, fontFamily = Inter))
            }
            Panel(gap = 6.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Label("SONGS · SORTED BY NUMBER")
                    Text("+ Song", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(theme.colors.primary).clickable {
                            draft = Draft(null, "${(songs.maxOfOrNull { it.songNumber } ?: 0) + 1}", "", listOf(LyricType.VERSE to "")); saved = false
                        }.hand().padding(horizontal = 10.dp, vertical = 6.dp))
                }
                if (sorted.isEmpty()) Text("No songs yet. Write one on the right and press Save song.", color = theme.colors.text, fontSize = 12.5.sp, modifier = Modifier.padding(4.dp))
                sorted.forEachIndexed { i, s ->
                    val cur = s.id == draft.id
                    var hovered by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().rise(i * 30, 6.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (cur) theme.colors.primary.copy(alpha = 0.14f) else if (hovered) theme.colors.menuHoverColor else Color.Transparent)
                            .border(1.dp, if (cur) theme.colors.primary.copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(4.dp))
                            .onHover { hovered = it }.clickable { draft = s.draft(); saved = true; page = pageOf(s) - 1 }.hand().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(if (cur) theme.colors.primary else theme.colors.surface), contentAlignment = Alignment.Center) {
                            Text("${s.songNumber}", color = if (cur) Ink else theme.colors.primaryText.copy(alpha = 0.8f), fontFamily = Fraunces, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(s.titles.firstOrNull()?.title ?: "Untitled", color = theme.colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${s.lyrics.count { it.type == LyricType.VERSE }} verses" + if (s.lyrics.any { it.type == LyricType.CHORUS }) " · chorus" else "", color = theme.colors.text, fontSize = 11.5.sp)
                        }
                        if (hovered) Text("×", color = Color(0xFFE8708A), fontSize = 15.sp, modifier = Modifier.clickable(onClickLabel = "Delete song") {
                            val before = CustomBooks.songs(meta.bookId)
                            CustomBooks.deleteSong(meta.bookId, s.id); songs = CustomBooks.songs(meta.bookId); model.refreshBooks(meta.bookId)
                            onToast("Deleted No. ${s.songNumber}") { CustomBooks.restore(meta.bookId, before); songs = CustomBooks.songs(meta.bookId); model.refreshBooks(meta.bookId) }
                        }.hand())
                        else Text("p.${pageOf(s)}", color = theme.colors.text, fontSize = 11.sp)
                    }
                }
            }
        }

        // song editor
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.width(90.dp)) {
                    Small("Number")
                    Field(draft.number, { v -> draft = draft.copy(number = v.filter { it.isDigit() }.take(4)); saved = false }, Modifier.fillMaxWidth(),
                        style = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp), height = 46.dp, border = if (taken != null) Amber else null)
                }
                Column(Modifier.weight(1f)) {
                    Small("Song title")
                    Field(draft.title, { draft = draft.copy(title = it); saved = false }, Modifier.fillMaxWidth(), style = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp), height = 46.dp)
                }
                SmallBtn("Paste a whole song", Modifier.height(46.dp)) { paste = !paste }
            }
            if (taken != null) Text("Number $num is already “${taken.titles.firstOrNull()?.title ?: ""}”. Saving will move it to the next free number.", color = Amber, fontSize = 12.5.sp, modifier = Modifier.offset(y = (-6).dp))
            AnimatedVisibility(paste, enter = fadeIn() + androidx.compose.animation.expandVertically(), exit = fadeOut() + androidx.compose.animation.shrinkVertically()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(theme.colors.primary.copy(alpha = 0.06f)).border(1.dp, theme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Paste the lyrics. A blank line starts a new section, and lines starting with “Chorus” become the chorus.", color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 12.5.sp)
                    Field(pasteText, { pasteText = it }, Modifier.fillMaxWidth(), "1. Amazing grace! How sweet the sound…", lines = 5, style = TextStyle(fontSize = 13.5.sp, fontFamily = Inter))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        SmallBtn("Cancel") { paste = false }
                        PrimaryBtn("Split into sections") {
                            val blocks = pasteText.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
                            if (blocks.isNotEmpty()) {
                                setParts(blocks.map { b ->
                                    if (Regex("^chorus\\b", RegexOption.IGNORE_CASE).containsMatchIn(b)) LyricType.CHORUS to b.replace(Regex("^chorus[:.\\s]*", RegexOption.IGNORE_CASE), "")
                                    else LyricType.VERSE to b.replace(Regex("^\\d+[.)]\\s*"), "")
                                })
                                paste = false; pasteText = ""
                            }
                        }
                    }
                }
            }
            var vn = 0
            draft.parts.forEachIndexed { i, (t, text) ->
                if (t == LyricType.VERSE) vn++
                val label = if (t == LyricType.VERSE) "Verse $vn" else t.label()
                key(draft.id, i) {
                    Row(
                        Modifier.fillMaxWidth().rise(0, 8.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp))
                            .drawBehind { drawRect(sectionColor(t), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)) }
                            .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                LyricType.entries.forEach { ty ->
                                    val on = ty == t
                                    val bg by animateColorAsState(if (on) sectionColor(ty) else Color.Transparent, tween(200))
                                    Text(ty.label(), color = if (on) Color(0xFF1E1B1A) else theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.height(26.dp).clip(CircleShape).background(bg).border(1.dp, if (on) sectionColor(ty) else theme.colors.border, CircleShape)
                                            .clickable(role = Role.RadioButton) { setParts(draft.parts.mapIndexed { j, p -> if (j == i) ty to p.second else p }) }.hand().padding(horizontal = 10.dp, vertical = 5.dp))
                                }
                                Text(label, color = theme.colors.text, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                                Spacer(Modifier.weight(1f))
                                SmallBtn("↑") { if (i > 0) setParts(draft.parts.toMutableList().apply { add(i - 1, removeAt(i)) }) }
                                SmallBtn("↓") { if (i < draft.parts.size - 1) setParts(draft.parts.toMutableList().apply { add(i + 1, removeAt(i)) }) }
                                SmallBtn("×", color = Color(0xFFE8708A)) { setParts(draft.parts.filterIndexed { j, _ -> j != i }) }
                            }
                            Field(text, { v -> setParts(draft.parts.mapIndexed { j, p -> if (j == i) p.first to v else p }) }, Modifier.fillMaxWidth(), lines = 4,
                                style = TextStyle(fontFamily = Fraunces, fontSize = 16.sp, lineHeight = 25.sp, fontStyle = if (t == LyricType.CHORUS) FontStyle.Italic else FontStyle.Normal))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallBtn("+ Verse", Modifier.height(38.dp)) { setParts(draft.parts + (LyricType.VERSE to "")) }
                SmallBtn("+ Chorus", Modifier.height(38.dp), color = theme.colors.primaryBright) { setParts(draft.parts + (LyricType.CHORUS to "")) }
                SmallBtn("+ Bridge", Modifier.height(38.dp)) { setParts(draft.parts + (LyricType.BRIDGE to "")) }
                Spacer(Modifier.weight(1f))
                Text(if (saved) "All changes saved" else "Unsaved changes", color = theme.colors.text, fontSize = 12.sp)
                PrimaryBtn("Save song") { save() }
            }
        }

        // PDF preview
        Column(Modifier.width(340.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val pageCount = 3 + sorted.size
            val p = page.coerceIn(0, pageCount - 1)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Label("PDF PREVIEW · $pageCount PAGES")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallBtn("‹") { page = (p - 1).coerceAtLeast(0) }
                    SmallBtn("›") { page = (p + 1).coerceAtMost(pageCount - 1) }
                }
            }
            PdfPage(p, meta, sorted, firstSong)
            Text("The PDF is only written when you export, or when auto backup is on. Songs are kept in the app’s database for fast reading.", color = theme.colors.text, fontSize = 11.5.sp, lineHeight = 16.sp)
            SmallBtn("Export or back up this book →", Modifier.fillMaxWidth().height(38.dp), color = theme.colors.primaryBright) { WorkshopState.bookId = meta.bookId; WorkshopState.tab = WorkshopTab.TRANSFER }
        }
    }
}

/** The preview page that flips in, drawn like the exported PDF. */
@Composable
private fun PdfPage(p: Int, meta: BookMeta, sorted: List<ISongDetails>, firstSong: Int) {
    val paper = Color(0xFFFBF8F3); val ink = Color(0xFF2A1F15); val muted = Color(0xFF8A7660); val rust = Color(0xFFB5561B)
    Box(Modifier.fillMaxWidth().aspectRatio(1f / 1.414f).clip(RoundedCornerShape(4.dp)).background(paper)) {
        AnimatedContent(p, transitionSpec = { (fadeIn(tween(500)) togetherWith fadeOut(tween(200))) }, modifier = Modifier.fillMaxSize()) { page ->
            val flip = remember { androidx.compose.animation.core.Animatable(-14f) }
            LaunchedEffect(Unit) { flip.animateTo(0f, tween(500, easing = components.global.SoftEase)) }
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = flip.value; cameraDistance = 12f * density }) {
                when {
                    page == 0 -> Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                        BookCover(meta.title, meta.logo, Modifier.size(72.dp).clip(RoundedCornerShape(4.dp)), mono = 50.dp, showName = false)
                        Text(meta.title.ifBlank { "Untitled book" }, color = ink, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center, lineHeight = 27.sp)
                        Box(Modifier.width(40.dp).height(2.dp).background(Color(0xFFE0813A)))
                        if (meta.author.isNotBlank()) Text(meta.author, color = ink, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 14.sp)
                        Text("Created ${fmt(meta.created)}", color = muted, fontSize = 11.sp)
                    }
                    page == 1 -> Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                        Text("THIS BOOK BELONGS TO", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text(meta.owner.ifBlank { "______________" }, color = ink, fontFamily = Fraunces, fontSize = 20.sp)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDCCBB0)))
                        Text(meta.address, color = Color(0xFF4A3A2C), fontSize = 12.5.sp, lineHeight = 18.sp)
                    }
                    page == 2 -> Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Contents", color = ink, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 6.dp))
                        sorted.take(26).forEachIndexed { i, s ->
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${s.songNumber}", color = rust, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp))
                                Text(s.titles.firstOrNull()?.title ?: "Untitled", color = ink, fontSize = 11.5.sp, maxLines = 1)
                                Box(Modifier.weight(1f).padding(horizontal = 4.dp).height(1.dp).offset(y = (-4).dp).drawBehind {
                                    var x = 0f; while (x < size.width) { drawCircle(Color(0xFFC9B89C), 0.8f, Offset(x, 0f)); x += 4f }
                                })
                                Text("${firstSong + i}", color = ink, fontSize = 11.5.sp)
                            }
                        }
                    }
                    else -> sorted.getOrNull(page - 3)?.let { s ->
                        Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${s.songNumber}", color = rust, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Text(s.titles.firstOrNull()?.title ?: "", color = ink, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                            var v = 0
                            s.lyrics.forEach { l ->
                                if (l.type == LyricType.VERSE) v++
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (l.type == LyricType.VERSE) "$v" else l.type.name, color = muted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp).padding(top = 2.dp))
                                    Text(l.lines.joinToString("\n"), color = ink, fontFamily = Fraunces, fontSize = 11.sp, lineHeight = 16.sp, fontStyle = if (l.type == LyricType.CHORUS) FontStyle.Italic else FontStyle.Normal)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (p > 0) Text("${p + 1}", color = muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun Panel(gap: Dp = 10.dp, content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    Text(text, color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = modifier)
}

@Composable
private fun Small(text: String) {
    val theme = LocalTheme.current
    Text(text, color = theme.colors.text, fontSize = 12.sp)
}

@Composable
private fun TransferTab(model: SongBookViewModal, onToast: (String) -> Unit) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()
    val books by model.songBooks.collectAsState()
    var bookId by remember { mutableStateOf(WorkshopState.bookId ?: books.firstOrNull()?.id) }
    val book = books.firstOrNull { it.id == bookId }
    var exportP by remember { mutableStateOf<Float?>(null) }
    var exported by remember { mutableStateOf<File?>(null) }
    var importAt by remember { mutableStateOf(-1) }
    var importErr by remember { mutableStateOf<String?>(null) }
    var meta by remember(bookId) { mutableStateOf(bookId?.let { CustomBooks.meta(it) }) }
    val log = CustomBooks.backupLog

    fun metaFor(b: IProviderStats) = CustomBooks.meta(b.id) ?: BookMeta(b.id, b.name, language = b.language)

    Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card3(Modifier.weight(1f)) {
            H2("Export as PDF")
            Body("A printable book with the cover, owner page, contents with page numbers, then every song in number order.")
            Small("Book")
            var open by remember { mutableStateOf(false) }
            Box {
                SmallBtn((book?.name ?: "Choose a book") + "  ▾", Modifier.fillMaxWidth().height(36.dp)) { open = true }
                DropdownMenu(open, { open = false }, Modifier.background(theme.colors.popupLayer)) {
                    books.forEach { b -> DropdownMenuItem(onClick = { bookId = b.id; exported = null; open = false }) { Text(b.name, color = theme.colors.primaryText, fontSize = 13.sp) } }
                }
            }
            exportP?.let { Progress(it, if (it < 1f) "Writing pages" else "Saved") }
            exported?.let { Text("Saved to ${it.absolutePath}", color = theme.colors.text, fontSize = 11.5.sp) }
            Spacer(Modifier.weight(1f))
            PrimaryBtn(if (exported != null) "Show in folder" else "Export PDF", Modifier.fillMaxWidth(), enabled = book != null) {
                val b = book ?: return@PrimaryBtn
                exported?.let { f -> runCatching { java.awt.Desktop.getDesktop().open(f.parentFile) }; return@PrimaryBtn }
                val target = chooseFile("Export song book", setOf("pdf"), save = true, suggested = SongBookPdf.fileName(b.name)) ?: return@PrimaryBtn
                exportP = 0.1f
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { runCatching { SongBookPdf.write(metaFor(b), model.loadSongsFrom(b), if (target.extension.equals("pdf", true)) target else File(target.path + ".pdf")) }.isSuccess }
                    exportP = if (ok) 1f else null
                    if (ok) exported = target else onToast("Couldn't write the PDF.")
                }
            }
        }
        Card3(Modifier.weight(1f)) {
            H2("Import a PDF")
            Body("Bring in a book exported from another computer. It is read once and saved into your library.")
            var hovered by remember { mutableStateOf(false) }
            Column(
                Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(RoundedCornerShape(4.dp)).border(1.5.dp, if (hovered) theme.colors.primary else theme.colors.border, RoundedCornerShape(4.dp))
                    .background(if (hovered) theme.colors.primary.copy(alpha = 0.05f) else Color.Transparent).onHover { hovered = it }
                    .clickable {
                        val f = chooseFile("Import a song book PDF", setOf("pdf")) ?: return@clickable
                        importAt = 0; importErr = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { runCatching { SongBookPdf.read(f) { importAt = it } } }
                            result.onSuccess { (m, list) ->
                                importAt = 2
                                val id = withContext(Dispatchers.IO) { CustomBooks.importBook(m, list) }
                                importAt = 3; delay(400); importAt = 4
                                model.refreshBooks(id)
                                onToast("Imported “${m.title}” · ${list.size} songs")
                            }.onFailure { importErr = "Couldn't read that PDF."; importAt = -1 }
                        }
                    }.hand(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
            ) {
                Text("⇩", color = theme.colors.primaryBright, fontSize = 24.sp)
                Text("Choose a PDF file", color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.sp)
            }
            importErr?.let { Text(it, color = Color(0xFFF28B8B), fontSize = 12.5.sp) }
            listOf("Reading the PDF", "Finding songs, numbers and sections", "Saving to your library", "Ready in Songs").forEachIndexed { i, label ->
                val done = importAt > i; val run = importAt == i
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepDot(done, run)
                    Text(label, color = if (done) theme.colors.primaryText else if (run) theme.colors.primaryBright else theme.colors.text.copy(alpha = 0.6f), fontSize = 12.5.sp)
                }
            }
        }
        Card3(Modifier.weight(1f)) {
            val m = meta
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                H2("Auto backup")
                ui.screens.projection.Switch(m?.backup == true, theme.colors.primary) { on ->
                    val cur = m ?: return@Switch
                    val dir = cur.backupDir ?: if (on) chooseFolder() else null
                    if (on && dir == null) return@Switch
                    val next = cur.copy(backup = on, backupDir = dir); meta = next
                    scope.launch(Dispatchers.IO) { CustomBooks.saveMeta(next) }
                }
            }
            Body(if (m == null) "Auto backup is for books you made in Create. Pick one of yours above." else "Whenever you change a book, its PDF in this folder is rewritten, with songs sorted by number and the contents page updated.")
            Row(
                Modifier.fillMaxWidth().graphicsLayer { alpha = if (m?.backup == true) 1f else 0.45f }.clip(RoundedCornerShape(4.dp)).background(theme.colors.surface).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(m?.backupDir ?: "No folder chosen", color = theme.colors.primaryText, fontSize = 12.5.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                SmallBtn("Change") {
                    val cur = m ?: return@SmallBtn
                    chooseFolder()?.let { d -> val next = cur.copy(backupDir = d); meta = next; scope.launch(Dispatchers.IO) { CustomBooks.saveMeta(next) } }
                }
            }
            log.take(4).forEach { (what, at) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(what, color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 12.5.sp)
                    Text(ago(at), color = theme.colors.text, fontSize = 12.5.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Pick a USB stick or a synced folder to carry your books to another computer.", color = theme.colors.text, fontSize = 11.5.sp)
        }
    }
}

private fun ago(at: Long): String {
    val s = (System.currentTimeMillis() - at) / 1000
    return when { s < 60 -> "just now"; s < 3600 -> "${s / 60} min ago"; else -> "${s / 3600} h ago" }
}

@Composable
private fun StepDot(done: Boolean, running: Boolean) {
    val theme = LocalTheme.current
    val t = rememberInfiniteTransition()
    val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)))
    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        when {
            done -> Box(Modifier.size(18.dp).popIn().clip(CircleShape).background(Green), contentAlignment = Alignment.Center) { Text("✓", color = Color(0xFF1E1B1A), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            running -> Canvas(Modifier.size(18.dp).graphicsLayer { rotationZ = r }) { drawCircle(theme.colors.border, style = Stroke(2.5f)); drawArc(theme.colors.primary, 0f, 110f, false, style = Stroke(2.5f)) }
            else -> Box(Modifier.size(18.dp).clip(CircleShape).background(theme.colors.border))
        }
    }
}

@Composable
private fun Card3(modifier: Modifier, content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    Column(modifier.heightIn(min = 380.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

@Composable private fun H2(t: String) { Text(t, color = LocalTheme.current.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
@Composable private fun Body(t: String) { Text(t, color = LocalTheme.current.colors.text, fontSize = 13.sp, lineHeight = 19.sp) }
