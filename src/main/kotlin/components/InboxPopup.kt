package components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.inboxIcon
import components.global.popIn
import components.global.rise
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import parsers.bt.Letter
import parsers.bt.Letters
import ui.screens.thisweek.LetterPrefs
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.LocalTheme
import ui.theme.ZillaSlab
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val INBOX_SIZE = 6
private val Badge = Color(0xFFE0546B)

/** The newest letters, which is what the inbox shows and counts. */
private fun inboxLetters(all: List<Letter>) = all.take(INBOX_SIZE)

private fun ago(d: LocalDate): String = when (val days = ChronoUnit.DAYS.between(d, LocalDate.now())) {
    in Long.MIN_VALUE..0 -> "today"; 1L -> "yesterday"; in 2..13 -> "$days days ago"; else -> "${days / 7} weeks ago"
}

/** Title-bar button: tray icon with an unread badge that pops in, and a ring when new letters arrive. */
@Composable
fun InboxButton(open: Boolean) {
    val theme = LocalTheme.current
    val all by Letters.all.collectAsState()
    val read by LetterPrefs.read
    val unread = inboxLetters(all).count { it.id !in read }
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (open || hovered) theme.colors.menuHoverColor else Color.Transparent, tween(200))
    val ring = remember { Animatable(0f) }
    var lastUnread by remember { mutableStateOf(unread) }

    // pick up this week's letter without opening the letters pages; a first run (empty cache) is left to them
    LaunchedEffect(Unit) { if (Letters.all.value.isNotEmpty()) Letters.sync() }
    LaunchedEffect(unread) {
        if (unread > lastUnread && !open) {
            repeat(2) { ring.animateTo(0f, keyframes { durationMillis = 1200; -12f at 240; 10f at 480; -6f at 720; 3f at 960 }) }
        }
        lastUnread = unread
    }

    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(bg).onHover { hovered = it }.hand()
            .semantics { contentDescription = "Inbox, $unread unread" },
        contentAlignment = Alignment.Center
    ) {
        Icon(inboxIcon(if (unread > 0) theme.colors.primaryBright else theme.colors.text), null, tint = Color.Unspecified,
            modifier = Modifier.size(19.dp).graphicsLayer { rotationZ = ring.value })
        if (unread > 0) Box(
            Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-1).dp).popIn().heightIn(min = 14.dp).widthIn(min = 14.dp).clip(CircleShape)
                .background(Badge).padding(horizontal = 3.dp),
            contentAlignment = Alignment.Center
        ) { Text("$unread", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }
}

/** The popup: newest letters, All / Unread, Mark all as read; opening one marks it read. */
@Composable
fun InboxPopup(state: MutableState<Boolean>) {
    val theme = LocalTheme.current
    val tab = LocalTabs.current.current
    val all by Letters.all.collectAsState()
    val read by LetterPrefs.read
    var unreadOnly by remember { mutableStateOf(false) }
    val letters = inboxLetters(all)
    val unread = letters.count { it.id !in read }
    val shown = letters.filter { !unreadOnly || it.id !in read }

    Column(Modifier.width(380.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.popupLayer).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp))) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Inbox", color = theme.colors.primaryText, fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Mark all as read", color = if (unread > 0) theme.colors.primaryBright else theme.colors.text.copy(alpha = 0.5f), fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = unread > 0, role = Role.Button) { letters.forEach { LetterPrefs.markRead(it.id) } }.hand())
        }
        Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(false to "All", true to "Unread").forEach { (v, label) ->
                val on = unreadOnly == v
                val chip by animateColorAsState(if (on) theme.colors.primary else theme.colors.menu, tween(200))
                Text(label, color = if (on) Color(0xFF2A1A0E) else theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.height(28.dp).clip(CircleShape).background(chip).clickable(role = Role.Tab) { unreadOnly = v }.hand().padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()).border(1.dp, theme.colors.border)) {
            shown.forEachIndexed { i, l ->
                InboxRow(l, l.id !in read, i) {
                    LetterPrefs.markRead(l.id)
                    state.value = false
                    tab.navigate(Screen.Letter(l.id))
                }
            }
            if (shown.isEmpty()) Text(
                if (letters.isEmpty()) "No letters yet. Open This Week to fetch them." else "You're all caught up. New letters show up here each week.",
                color = theme.colors.text, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 30.dp)
            )
        }
        Text("See every letter in the archive →", color = theme.colors.primaryBright, fontSize = 12.5.sp,
            modifier = Modifier.fillMaxWidth().background(theme.colors.surface).clickable { state.value = false; tab.navigate(Screen.LetterArchive) }.hand().padding(horizontal = 16.dp, vertical = 11.dp))
    }
}

@Composable
private fun InboxRow(l: Letter, unread: Boolean, i: Int, onOpen: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (hovered) theme.colors.menuHoverColor else Color.Transparent, tween(200))
    val dot by animateColorAsState(if (unread) Badge else Color.Transparent, tween(300))
    val go = remember { Animatable(0f) }
    LaunchedEffect(hovered) { go.animateTo(if (hovered) 1f else 0f, tween(200)) }
    Row(
        Modifier.fillMaxWidth().rise(i * 50, 6.dp).background(bg).onHover { hovered = it }.clickable(role = Role.Button, onClick = onOpen).hand()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.width(44.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF6F1EC)).padding(top = 4.dp, bottom = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(l.letterDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(), color = Color(0xFF7A4A28), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text("${l.letterDate.dayOfMonth}", color = Color(0xFF2A1A0E), fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (unread) "NEW LETTER" else "LETTER", color = if (unread) theme.colors.primaryBright else theme.colors.text, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Text(" · ${ago(l.letterDate)}", color = theme.colors.text, fontSize = 11.sp)
            }
            Text("${l.title} · ${l.code}", color = if (unread) theme.colors.primaryText else theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 14.sp,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            l.salutation?.let { Text(it, color = theme.colors.text, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dot).semantics { contentDescription = if (unread) "Unread" else "Read" })
            Text("Open →", color = theme.colors.primaryBright, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp).graphicsLayer { alpha = go.value; translationX = (go.value - 1f) * 4 * density })
        }
    }
}
