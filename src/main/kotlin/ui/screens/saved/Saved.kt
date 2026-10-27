package ui.screens.saved

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.style.TextOverflow
import activity.SavedBookmark
import activity.SavedHighlight
import activity.SavedNote
import activity.SavedSource
import activity.SavedStore
import activity.Tone
import activity.openTarget
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.closeIcon
import com.composables.openExternalIcon
import com.composables.play
import com.composables.projectorIcon
import com.composables.searchIcon
import com.composables.shareUpIcon
import components.global.animatedFill
import components.global.rise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import loadData
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import parsers.vgr.SermonMedia
import player.GlobalPlayerType
import player.GlobalPlayerViewModel
import player.LocalPlayer
import player.LocalPlayerState
import presentation.DeckItem
import presentation.DeckSource
import presentation.ProjectionDeck
import ui.screens.myactivity.FilterChip
import ui.screens.myactivity.Panel
import ui.screens.thisweek.ActionButton
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class SavedTab { Highlights, Bookmarks, Notes, Downloads }
private enum class Sort(val label: String) { NEWEST("Newest"), OLDEST("Oldest"), BOOK("Book order") }

private val Ink = Color(0xFF2A1A0E)
private val BibleTeal = Color(0xFF6BDCC3)
private val TableGold = Color(0xFFE0B872)
private val OtherBlue = Color(0xFF9FB6F5)

internal fun whenLabel(at: Long): String {
    val d = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (d) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> activity.ActivityLog.monthDay(d)
    }
}

/** An audio file found in one of the sermon folders. */
private data class Download(val file: File, val code: String, val title: String, val language: String, val ours: Boolean)

private val fileName = Regex("""^(\d{2}-\d{4}[A-Z]?)\s+(.*?)(?:\s+VGR)?$""")
private val audioExt = setOf("opus", "m4a", "mp3", "ogg", "aac", "wav")

private fun scanDownloads(): List<Download> {
    val ours = loadData<HashSet<String>>("guide_downloads") ?: hashSetOf()
    return SermonMedia.mediaRoots().flatMap { root ->
        val lang = if (root.name == "Sermons") root.parentFile.name else "—"
        root.walkTopDown().maxDepth(2).filter { it.isFile && it.extension.lowercase() in audioExt }.map { f ->
            val m = fileName.find(f.nameWithoutExtension)
            Download(f, m?.groupValues?.get(1) ?: "", m?.groupValues?.get(2) ?: f.nameWithoutExtension, lang, f.absolutePath in ours)
        }.toList()
    }.sortedByDescending { it.file.lastModified() }
}

internal fun sizeLabel(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "${bytes shr 20} MB"
    else -> "${bytes shr 10} KB"
}

@Composable
fun Saved() {
    val theme = LocalTheme.current
    val scroll = rememberScrollState()
    val highlights by SavedStore.highlights
    val bookmarks by SavedStore.bookmarks
    val notes by SavedStore.notes
    var tab by remember { mutableStateOf(SavedTab.Highlights) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(Sort.NEWEST) }
    var downloads by remember { mutableStateOf<List<Download>?>(null) }
    LaunchedEffect(Unit) { downloads = withContext(Dispatchers.IO) { scanDownloads() } }

    fun matches(vararg s: String?) = query.isBlank() || s.any { it?.contains(query.trim(), ignoreCase = true) == true }
    fun <T> sorted(list: List<T>, at: (T) -> Long, book: (T) -> String): List<T> = when (sort) {
        Sort.NEWEST -> list.sortedByDescending(at)
        Sort.OLDEST -> list.sortedBy(at)
        Sort.BOOK -> list.sortedBy(book)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 48.dp, vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 1120.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(Modifier.fillMaxWidth().rise(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Everything you kept, in one place", color = theme.colors.text, fontSize = 13.sp)
                        Text("Saved", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SearchBox(query) { query = it }
                        Text("Sort", color = theme.colors.text, fontSize = 13.sp)
                        SortPicker(sort) { sort = it }
                    }
                }

                val counts = mapOf(
                    SavedTab.Highlights to highlights.size, SavedTab.Bookmarks to bookmarks.size,
                    SavedTab.Notes to notes.size, SavedTab.Downloads to (downloads?.size ?: 0)
                )
                Tabs(tab, counts) { tab = it }

                key(tab) {
                    when (tab) {
                        SavedTab.Highlights -> HighlightsTab(sorted(highlights.filter { matches(it.text, it.ref) }, { it.at }, { it.ref }))
                        SavedTab.Bookmarks -> BookmarksTab(sorted(bookmarks.filter { matches(it.title, it.meta) }, { it.at }, { it.title }))
                        SavedTab.Notes -> NotesTab(sorted(notes.filter { matches(it.title, it.body, it.ref) }, { it.at }, { it.ref }))
                        SavedTab.Downloads -> DownloadsTab(downloads?.filter { matches(it.title, it.code, it.language) })
                    }
                }
            }
        }
        VerticalScrollbar(scrollState = scroll, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit) {
    val theme = LocalTheme.current
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) theme.colors.primary else theme.colors.border, tween(200))
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.width(260.dp).height(42.dp).clip(shape).background(theme.colors.surface).border(1.dp, border, shape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(searchIcon(theme.colors.text), null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search highlights, notes, sermons…", color = theme.colors.text, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = theme.colors.primaryText, fontSize = 13.5.sp, fontFamily = Inter),
                cursorBrush = SolidColor(theme.colors.primary),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            )
        }
    }
}

@Composable
private fun Tabs(selected: SavedTab, counts: Map<SavedTab, Int>, onPick: (SavedTab) -> Unit) {
    val theme = LocalTheme.current
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SavedTab.entries.forEach { t ->
                val on = t == selected
                val fg by animateColorAsState(if (on) theme.colors.primaryText else theme.colors.text, tween(200))
                val bar by animateFloatAsState(if (on) 1f else 0f, tween(350, easing = components.global.SoftEase))
                // IntrinsicSize.Max keeps the underline as wide as its label instead of the whole row
                Column(Modifier.width(IntrinsicSize.Max).clickable(role = Role.Tab) { onPick(t) }.hand().padding(horizontal = 16.dp).padding(top = 12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(t.name, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("  ${counts[t] ?: 0}", color = theme.colors.text, fontSize = 12.sp, maxLines = 1)
                    }
                    Box(Modifier.padding(top = 11.dp).fillMaxWidth().height(2.dp).graphicsLayer { scaleX = bar }.clip(RoundedCornerShape(2.dp)).background(theme.colors.primary))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.colors.border))
    }
}

@Composable
private fun Empty(text: String) {
    val theme = LocalTheme.current
    Panel(Modifier.fillMaxWidth().rise()) {
        Text(text, color = theme.colors.text, fontSize = 13.5.sp, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightsTab(all: List<SavedHighlight>) {
    val theme = LocalTheme.current
    var tone by remember { mutableStateOf<Tone?>(null) }
    val list = all.filter { tone == null || it.tone == tone }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Color", color = theme.colors.text, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
            (listOf<Tone?>(null) + Tone.entries).forEach { t ->
                val on = tone == t
                val bg by animateColorAsState(if (on) theme.colors.menuHoverColor else Color.Transparent, tween(200))
                Row(
                    Modifier.height(32.dp).clip(CircleShape).background(bg).border(1.dp, if (on) theme.colors.primary else theme.colors.border, CircleShape)
                        .clickable(role = Role.Tab) { tone = t }.hand().padding(start = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        Modifier.size(14.dp).clip(CircleShape).background(
                            if (t == null) Brush.sweepGradient(Tone.entries.map { it.color } + Tone.GOLD.color) else SolidColor(t.color)
                        )
                    )
                    Text(t?.label ?: "All", color = theme.colors.primaryText, fontSize = 12.5.sp)
                }
            }
        }
        if (list.isEmpty()) Empty(
            if (all.isEmpty()) "No highlights yet. Hover a paragraph in The Table, or select a verse in the Bible, and pick a colour."
            else "No highlights in this colour."
        )
        BoxWithConstraints {
            val cols = (maxWidth / 316.dp).toInt().coerceIn(1, 4)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = cols) {
                list.forEachIndexed { i, h -> key(h.id) { HighlightCard(h, i, Modifier.weight(1f)) } }
                repeat((cols - list.size % cols) % cols) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HighlightCard(h: SavedHighlight, i: Int, modifier: Modifier) {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    val clipboard = LocalClipboardManager.current
    val notes by SavedStore.notes
    val note = notes.firstOrNull { it.target.key == h.target.key }
    var hovered by remember { mutableStateOf(false) }
    val lift by animateDpAsState(if (hovered) (-3).dp else 0.dp, tween(250))
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.rise(i.coerceAtMost(12) * 60).offset(y = lift).clip(shape).background(theme.colors.menu)
            .border(1.dp, if (hovered) h.tone.color.copy(alpha = 0.6f) else theme.colors.border, shape).onHover { hovered = it }
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(h.tone.color))
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(h.target.source.label, color = if (h.target.source == SavedSource.BIBLE) BibleTeal else TableGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                Text(whenLabel(h.at), color = theme.colors.text, fontSize = 12.sp)
            }
            Text(
                buildAnnotatedString { withStyle(SpanStyle(background = h.tone.color.copy(alpha = 0.32f))) { append(h.text) } },
                fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 26.sp, color = theme.colors.primaryText, maxLines = 8
            )
            Text(h.ref, color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            note?.let {
                Text(
                    it.body, color = theme.colors.primaryText.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(theme.colors.surface)
                        .border(1.dp, theme.colors.border, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                CardIcon(::openExternalIcon, "Open") { nav.openTarget(h.target) }
                CardIcon(::projectorIcon, "Project to screen") {
                    ProjectionDeck.load(
                        if (h.target.source == SavedSource.BIBLE) DeckSource.BIBLE else DeckSource.TABLE,
                        h.ref.substringBefore(" · "), h.ref, listOf(DeckItem(h.target.number ?: 1, h.ref.substringBefore(" · "), h.text)), 0, h.target.sermon
                    )
                    nav.navigate(Screen.Projection)
                }
                CardIcon(::shareUpIcon, "Copy") { clipboard.setText(AnnotatedString("“${h.text}”\n${h.ref}")) }
                Spacer(Modifier.weight(1f))
                CardIcon(::closeIcon, "Remove") { SavedStore.remove(h) }
            }
        }
    }
}

@Composable
private fun CardIcon(icon: (Color) -> ImageVector, label: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (hovered) theme.colors.menuHoverColor else Color.Transparent, tween(150))
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg).border(1.dp, theme.colors.border, RoundedCornerShape(10.dp))
            .onHover { hovered = it }.clickable(role = Role.Button, onClickLabel = label, onClick = onClick).hand(),
        contentAlignment = Alignment.Center
    ) { Icon(icon(theme.colors.primaryText), label, tint = Color.Unspecified, modifier = Modifier.size(16.dp)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookmarksTab(list: List<SavedBookmark>) {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    if (list.isEmpty()) return Empty("No bookmarks yet. Use the bookmark button on a verse or paragraph to keep your place.")
    BoxWithConstraints {
        val cols = (maxWidth / 336.dp).toInt().coerceIn(1, 3)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), maxItemsInEachRow = cols) {
            list.forEachIndexed { i, b ->
                key(b.id) {
                    val c = if (b.target.source == SavedSource.BIBLE) BibleTeal else Color(0xFFC9A15F)
                    val fill = animatedFill(b.progress.coerceIn(0f, 1f), i * 60)
                    var hovered by remember { mutableStateOf(false) }
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.weight(1f).rise(i * 60).clip(shape).background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu)
                            .border(1.dp, theme.colors.border, shape).onHover { hovered = it }.clickable { nav.openTarget(b.target) }.hand().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(c.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text(if (b.target.source == SavedSource.BIBLE) b.title.take(2) else "T", color = c, fontFamily = Fraunces, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(b.title, color = theme.colors.primaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                            Text(b.meta, color = theme.colors.text, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
                            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.colors.border)) {
                                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(c))
                            }
                        }
                        if (hovered) CardIcon(::closeIcon, "Remove") { SavedStore.remove(b) }
                    }
                }
            }
            repeat((cols - list.size % cols) % cols) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun NotesTab(list: List<SavedNote>) {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    if (list.isEmpty()) return Empty("No notes yet. Use the note button on a verse or paragraph to write one.")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        list.forEachIndexed { i, n ->
            key(n.id) {
                var hovered by remember { mutableStateOf(false) }
                Panel(Modifier.fillMaxWidth().rise(i * 60).onHover { hovered = it }.clickable { nav.openTarget(n.target) }.hand(), radius = 16.dp, padding = 18.dp) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(n.title, color = theme.colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${n.ref} · ${whenLabel(n.at)}", color = theme.colors.text, fontSize = 12.sp)
                            if (hovered) Box(Modifier.padding(start = 10.dp)) { CardIcon(::closeIcon, "Delete note") { SavedStore.remove(n) } }
                        }
                        Text(n.body, color = theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsTab(list: List<Download>?) {
    val theme = LocalTheme.current
    val player = LocalPlayer.current
    val engine = LocalPlayerState.current
    if (list == null) return Empty("Looking for downloaded sermons…")
    val ours = list.filter { it.ours }.sumOf { it.file.length() }
    val other = list.filterNot { it.ours }.sumOf { it.file.length() }
    val root = SermonMedia.mediaRoots().firstOrNull()
    val free = root?.usableSpace ?: 0L
    val total = (ours + other + free).coerceAtLeast(1)
    // tiny slices still show as a sliver
    val oursFill = animatedFill(if (ours > 0) maxOf(ours.toFloat() / total, 0.01f) else 0f, 0, 1200)
    val otherFill = animatedFill(if (other > 0) maxOf(other.toFloat() / total, 0.01f) else 0f, 150, 1200)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Panel(Modifier.fillMaxWidth().rise(), radius = 18.dp, padding = 18.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Offline audio", color = theme.colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${sizeLabel(ours + other)} used · ${sizeLabel(free)} free", color = theme.colors.text, fontSize = 13.sp)
                    }
                    Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.border)) {
                        if (oursFill > 0f) Box(Modifier.fillMaxHeight().weight(oursFill).background(theme.colors.primary))
                        if (otherFill > 0f) Box(Modifier.fillMaxHeight().weight(otherFill).background(OtherBlue))
                        Spacer(Modifier.weight((1f - oursFill - otherFill).coerceAtLeast(0.0001f)))
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Legend(theme.colors.primary, "Downloaded here")
                        Legend(OtherBlue, "Found from other apps")
                    }
                }
                if (root != null) ActionButton("Open folder") { runCatching { java.awt.Desktop.getDesktop().open(root) } }
            }
        }
        if (list.isEmpty()) return Empty("No sermon audio on this computer yet. Downloads from The Table land here.")
        val shape = RoundedCornerShape(18.dp)
        Column(Modifier.fillMaxWidth().clip(shape).background(theme.colors.menu.copy(alpha = 0.85f)).border(1.dp, theme.colors.border, shape).padding(6.dp)) {
            list.forEachIndexed { i, d ->
                key(d.file.absolutePath) {
                    var hovered by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().rise(i.coerceAtMost(12) * 60).clip(RoundedCornerShape(12.dp))
                            .background(if (hovered) theme.colors.menuHoverColor else Color.Transparent).onHover { hovered = it }.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(theme.colors.primary).clickable(role = Role.Button, onClickLabel = "Play ${d.title}") {
                                player.loadNowPlaying(
                                    GlobalPlayerViewModel.NowPlaying(
                                        id = d.file.absolutePath, title = d.title, subtitle = listOf(d.code, d.language).filter { it.isNotBlank() }.joinToString(" · "),
                                        type = GlobalPlayerType.SERMON, isPlaying = true, currentPosition = 0L, url = d.file.absolutePath
                                    )
                                )
                                engine.setPause(false)
                            }.hand(),
                            contentAlignment = Alignment.Center
                        ) { Icon(play(Ink), null, tint = Color.Unspecified, modifier = Modifier.size(13.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(d.title, color = theme.colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOf(d.code, d.language).filter { it.isNotBlank() }.joinToString(" · "), color = theme.colors.text, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        val c = if (d.ours) theme.colors.primaryBright else Color(0xFFB5C6F7)
                        Text(
                            if (d.ours) "This app" else "Other app", color = c, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(CircleShape).background(c.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Text(sizeLabel(d.file.length()), color = theme.colors.text, fontSize = 12.5.sp, modifier = Modifier.width(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    val theme = LocalTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, color = theme.colors.text, fontSize = 12.sp)
    }
}

@Composable
private fun SortPicker(sort: Sort, onPick: (Sort) -> Unit) {
    val theme = LocalTheme.current
    var open by remember { mutableStateOf(false) }
    Box {
        Text(
            "${sort.label}  ▾", color = theme.colors.primaryText, fontSize = 13.sp, maxLines = 1,
            modifier = Modifier.height(42.dp).clip(RoundedCornerShape(12.dp)).background(theme.colors.surface).border(1.dp, theme.colors.border, RoundedCornerShape(12.dp))
                .clickable(role = Role.DropdownList) { open = true }.hand().padding(horizontal = 14.dp, vertical = 12.dp)
        )
        androidx.compose.material.DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.background(theme.colors.popupLayer)) {
            Sort.entries.forEach { s ->
                androidx.compose.material.DropdownMenuItem(onClick = { onPick(s); open = false }) {
                    Text(s.label, color = if (s == sort) theme.colors.primary else theme.colors.primaryText, fontSize = 13.sp)
                }
            }
        }
    }
}
