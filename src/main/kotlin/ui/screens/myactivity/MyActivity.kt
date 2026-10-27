package ui.screens.myactivity

import activity.ActivityEvent
import activity.ActivityKind
import activity.ActivityLog
import activity.Badge
import activity.Range
import activity.shortDuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.checkIcon
import com.composables.flameIcon
import com.composables.headphonesIcon
import com.composables.play
import components.global.EqBars
import components.global.GlowBlob
import components.global.PingDot
import components.global.SoftEase
import components.global.animatedFill
import components.global.flicker
import components.global.growUp
import components.global.popIn
import components.global.rise
import components.global.shine
import live.LiveDiscovery
import navigation.LibraryScreen
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import player.LocalPlayer
import player.LocalPlayerState
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.LocalTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Fixed colours per kind of activity, as in the design (the accent colour still drives the rest). */
internal fun ActivityKind.color(accent: Color): Color = when (this) {
    ActivityKind.BIBLE -> Color(0xFF6BDCC3)
    ActivityKind.TABLE -> Color(0xFFC9A15F)
    ActivityKind.LISTENING -> Color(0xFF9FB6F5)
    ActivityKind.LIVE -> Color(0xFFEC8B9A)
    ActivityKind.HIGHLIGHTS -> Color(0xFFE5A4CB)
    ActivityKind.LETTERS -> accent
}

private val Ink = Color(0xFF2A1A0E)
private val headerDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

@Composable
fun MyActivity() {
    val theme = LocalTheme.current
    val scroll = rememberScrollState()
    var range by remember { mutableStateOf(Range.WEEK) }
    // read the state so every screen part recomposes when the log changes
    val events by ActivityLog.events
    val listening by ActivityLog.listening
    val paragraphs by ActivityLog.paragraphs
    val finished by ActivityLog.finished

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 48.dp, vertical = 36.dp), contentAlignment = Alignment.TopCenter) {
            val wide = maxWidth > 860.dp
            Column(Modifier.widthIn(max = 1120.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(Modifier.fillMaxWidth().rise(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(LocalDate.now().format(headerDate), color = theme.colors.text, fontSize = 13.sp, letterSpacing = 0.3.sp)
                        Text("My Activity", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Segmented(Range.entries.map { it.label }, range.ordinal) { range = Range.entries[it] }
                }

                key(events.size, listening.size, paragraphs.size, finished.size) {
                    if (wide) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StreakCard(Modifier.weight(2f).fillMaxHeight())
                            ListeningCard(range, Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        StreakCard(Modifier.fillMaxWidth())
                        ListeningCard(range, Modifier.fillMaxWidth())
                    }
                    Totals(range, wide)
                    LiveCard()
                    Badges(wide)
                    Milestones()
                    Feed(events)
                }
            }
        }
        VerticalScrollbar(scrollState = scroll, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

/** Pill-shaped segmented control (Week / Month / Year). */
@Composable
internal fun Segmented(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val theme = LocalTheme.current
    Row(
        Modifier.clip(CircleShape).background(theme.colors.surface).border(1.dp, theme.colors.border, CircleShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) theme.colors.primary else Color.Transparent, tween(250))
            val fg by animateColorAsState(if (on) Ink else theme.colors.primaryText.copy(alpha = 0.8f), tween(250))
            Text(
                label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(bg).clickable(role = Role.Tab) { onPick(i) }.hand().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
internal fun Panel(modifier: Modifier = Modifier, radius: Dp = 20.dp, padding: Dp = 20.dp, content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    val shape = RoundedCornerShape(radius)
    Box(modifier.clip(shape).background(theme.colors.menu).border(1.dp, theme.colors.border, shape).padding(padding)) { content() }
}

@Composable
internal fun SectionTitle(text: String, trailing: String? = null, trailingAccent: Boolean = false) {
    val theme = LocalTheme.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(text, color = theme.colors.primaryText, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, fontSize = 22.sp)
        if (trailing != null) Text(trailing, color = if (trailingAccent) theme.colors.primaryBright else theme.colors.text, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreakCard(modifier: Modifier) {
    val theme = LocalTheme.current
    val days = remember { ActivityLog.activeDays() }
    val streak = remember(days) { ActivityLog.currentStreak(days) }
    val longest = remember(days) { ActivityLog.longestStreak(days) }
    val heat = remember { ActivityLog.heat() }
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.rise(50).clip(shape)
            .background(Brush.linearGradient(0f to theme.colors.primary.copy(alpha = 0.22f), 0.55f to theme.colors.menu))
            .border(1.dp, theme.colors.primary.copy(alpha = 0.35f), shape)
    ) {
        GlowBlob(theme.colors.primary.copy(alpha = 0.35f), 240.dp, Modifier.align(Alignment.TopEnd).offset(60.dp, (-60).dp))
        FlowRow(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(theme.colors.primary.copy(alpha = 0.16f))
                        .border(1.dp, theme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(flameIcon(theme.colors.primary, Color(0xFFFFD9A8)), null, tint = Color.Unspecified, modifier = Modifier.size(38.dp).flicker())
                }
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$streak", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 44.sp)
                        Text(if (streak == 1) " day streak" else " day streak", color = theme.colors.primaryBright, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(
                        if (longest == 0) "Read, listen or highlight today to start one" else "Longest $longest day${if (longest == 1) "" else "s"}",
                        color = theme.colors.text, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                    WeekRow(days, Modifier.padding(top = 14.dp))
                }
            }
            Column(Modifier.widthIn(min = 260.dp, max = 380.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Last 15 weeks", color = theme.colors.text, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Less ", color = theme.colors.text, fontSize = 12.sp)
                        heatShades(theme.colors.primary).let { listOf(it[0], it[2], it[4]) }.forEach { Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(it)) }
                        Text(" More", color = theme.colors.text, fontSize = 12.sp)
                    }
                }
                HeatMap(heat)
            }
        }
    }
}

private fun heatShades(accent: Color) = listOf(Color(0xFF3A302A), accent.copy(alpha = 0.28f), accent.copy(alpha = 0.48f), accent.copy(alpha = 0.72f), accent)

@Composable
private fun WeekRow(days: Set<LocalDate>, modifier: Modifier) {
    val theme = LocalTheme.current
    val monday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value - 1L)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0..6).forEach { i ->
            val d = monday.plusDays(i.toLong())
            val done = d in days
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.size(28.dp).popIn(300 + i * 70).clip(CircleShape).background(if (done) theme.colors.primary else Color.Transparent)
                        .border(1.dp, if (done) theme.colors.primary else theme.colors.primary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { if (done) Icon(checkIcon(Ink), null, tint = Color.Unspecified, modifier = Modifier.size(13.dp)) }
                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), color = theme.colors.text, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HeatMap(heat: List<Pair<LocalDate, Int>>) {
    val theme = LocalTheme.current
    val shades = heatShades(theme.colors.primary)
    val max = heat.maxOf { it.second }.coerceAtLeast(1)
    val reveal = animatedFill(1f, 200, 900)
    Canvas(
        Modifier.fillMaxWidth().height(12.dp * 7 + 4.dp * 6)
            .semantics { contentDescription = "Daily activity heat map for the last 15 weeks" }
    ) {
        val gap = 4.dp.toPx()
        val cols = heat.size / 7
        val w = (size.width - gap * (cols - 1)) / cols
        val h = 12.dp.toPx()
        heat.forEachIndexed { i, (_, v) ->
            // cells fade in one after another, oldest first
            val a = ((reveal * heat.size - i) / 8f).coerceIn(0f, 1f)
            val lvl = if (v == 0) 0 else (1 + (v * 4 - 1) / max).coerceIn(1, 4)
            drawRoundRect(shades[lvl].copy(alpha = shades[lvl].alpha * a), Offset((i / 7) * (w + gap), (i % 7) * (h + gap)), Size(w, h), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        }
    }
}

@Composable
private fun ListeningCard(range: Range, modifier: Modifier) {
    val theme = LocalTheme.current
    val total = ActivityLog.listeningSeconds(range)
    val before = ActivityLog.listeningSeconds(range, offset = 1)
    val buckets = ActivityLog.listeningBuckets(range)
    val max = buckets.max().coerceAtLeast(1)
    Panel(modifier.rise(120)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Listening", color = theme.colors.text, fontSize = 13.sp)
                Icon(headphonesIcon(theme.colors.primaryBright), null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
            }
            Text(shortDuration(total), color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                buckets.forEachIndexed { i, v ->
                    key(range, i) {
                        Box(
                            Modifier.weight(1f).fillMaxHeight((v.toFloat() / max).coerceAtLeast(0.06f)).growUp(250 + i * 60)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(if (i == 6) theme.colors.primary else theme.colors.primary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
            val diff = total - before
            Text(
                if (diff >= 0) "+${shortDuration(diff)} vs last ${range.label.lowercase()}" else "−${shortDuration(-diff)} vs last ${range.label.lowercase()}",
                color = theme.colors.text, fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Totals(range: Range, wide: Boolean) {
    val theme = LocalTheme.current
    val stats = listOf(
        Triple(ActivityLog.count(ActivityKind.BIBLE, range).toString(), "Bible chapters read", ActivityKind.BIBLE),
        Triple(ActivityLog.paragraphsIn(range).toString(), "Table paragraphs read", ActivityKind.TABLE),
        Triple(ActivityLog.count(ActivityKind.HIGHLIGHTS, range).toString(), "Highlights", ActivityKind.HIGHLIGHTS),
        Triple(ActivityLog.count(ActivityKind.LIVE, range).toString(), "Live services joined", ActivityKind.LIVE),
    )
    val content: @Composable (Modifier, Int) -> Unit = { m, i ->
        val (value, label, kind) = stats[i]
        val c = kind.color(theme.colors.primary)
        Panel(m.rise(150 + i * 60), radius = 18.dp, padding = 18.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(c.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(12.dp).rotate(45f).clip(RoundedCornerShape(4.dp)).background(c))
                }
                Column {
                    Text(value, color = theme.colors.primaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(label, color = theme.colors.text, fontSize = 12.5.sp)
                }
            }
        }
    }
    if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) { stats.indices.forEach { content(Modifier.weight(1f), it) } }
    else FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { stats.indices.forEach { content(Modifier.width(220.dp), it) } }
}

/** Shown only while a host is sharing a live service on this network. */
@Composable
private fun LiveCard() {
    val hosts by LiveDiscovery.hosts.collectAsState()
    LaunchedEffect(Unit) { LiveDiscovery.start() }
    val host = hosts.firstOrNull() ?: return
    val nav = LocalTabs.current.current
    val rose = Color(0xFFEC8B9A)
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().rise(200).clip(shape).background(Color(0xFF2A2423)).border(1.dp, Color(0xFF4A3434), shape).padding(22.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(rose.copy(alpha = 0.14f)).border(1.dp, rose.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(bottom = 14.dp),
            contentAlignment = Alignment.BottomCenter
        ) { EqBars(rose, height = 26.dp, barWidth = 5.dp, gap = 4.dp) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PingDot(rose)
                Text("LIVE NOW · ON YOUR NETWORK", color = Color(0xFFF2A9B5), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            }
            Text(host.title.ifBlank { "Live service" }, color = Color(0xFFF6F1EC), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            Text(
                listOfNotNull(host.code.ifBlank { null }, "hosted by ${host.name}", "${host.listeners} listening").joinToString(" · "),
                color = Color(0xFFB8AEA6), fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp)
            )
        }
        Row(
            Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(rose).clickable(role = Role.Button) {
                ui.screens.projection.ListenSession.connect(host)
                nav.navigate(Screen.Listener)
            }.hand().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(play(Color(0xFF2A1014)), null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
            Text("Join live", color = Color(0xFF2A1014), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Badges(wide: Boolean) {
    val badges = remember { ActivityLog.badges() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Badges", "${badges.count { it.progress >= 1f }} of ${badges.size} earned")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), maxItemsInEachRow = if (wide) 6 else 3) {
            badges.forEachIndexed { i, b -> BadgeCard(b, i, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun BadgeCard(b: Badge, i: Int, modifier: Modifier) {
    val theme = LocalTheme.current
    val c = b.kind?.color(theme.colors.primary) ?: if (b.mark == "7") theme.colors.primaryBright else theme.colors.primary
    val earned = b.progress >= 1f
    var hovered by remember { mutableStateOf(false) }
    val lift by animateDpAsState(if (hovered) (-4).dp else 0.dp, tween(300, easing = SoftEase))
    val ring = animatedFill(b.progress.coerceIn(0f, 1f), 100 + i * 80, 1400)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.rise(100 + i * 80).offset(y = lift).clip(shape).background(theme.colors.menu)
            .border(1.dp, if (hovered) c.copy(alpha = 0.5f) else theme.colors.border, shape)
            .onHover { hovered = it }.shine(earned, 100 + i * 80).padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(74.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(74.dp)) {
                val sw = size.width * 3f / 40f
                val inset = size.width * 4f / 40f
                val arc = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(theme.colors.border, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(sw))
                drawArc(c, -90f, 360f * ring, false, Offset(inset, inset), arc, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Box(Modifier.size(50.dp).clip(CircleShape).background(c.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(b.mark, color = c, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        Text(b.name, color = theme.colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(b.desc, color = theme.colors.text, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
        Text(b.earnedOn?.let { "Earned ${ActivityLog.monthDay(it)}" } ?: b.status, color = c, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Milestones() {
    val theme = LocalTheme.current
    val ms = remember { ActivityLog.milestones() }
    val next = ms.firstOrNull { it.next }
    val doneCount = ms.count { it.done }
    // the line reaches the last finished dot, plus half a step towards the next one
    val target = if (ms.size < 2) 0f else ((doneCount - 1).coerceAtLeast(0) + if (next != null && doneCount > 0) 0.5f else 0f) / (ms.size - 1)
    val fill = animatedFill(target.coerceIn(0f, 1f), 0, 1600)
    Panel(Modifier.fillMaxWidth().rise(300), padding = 22.dp) {
        Column {
            SectionTitle("Milestones", next?.let { "Next: ${it.title} · ${it.whenLabel}" }, trailingAccent = true)
            Box(Modifier.fillMaxWidth().padding(top = 26.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                    val step = maxWidth / ms.size
                    Box(Modifier.padding(start = step / 2, end = step / 2, top = 13.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.colors.border))
                    Box(
                        Modifier.padding(start = step / 2, top = 13.dp).width((maxWidth - step) * fill).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(theme.colors.primary, theme.colors.primaryBright)))
                    )
                    Row(Modifier.fillMaxWidth()) {
                        ms.forEachIndexed { i, m ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier.size(30.dp).popIn(400 + i * 180).clip(CircleShape).background(if (m.done) theme.colors.primary else theme.colors.surface)
                                        .border(2.dp, if (m.done) theme.colors.primary else if (m.next) theme.colors.primaryBright else theme.colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { if (m.done) Icon(checkIcon(Ink), null, tint = Color.Unspecified, modifier = Modifier.size(13.dp)) }
                                Text(m.title, color = if (m.done || m.next) theme.colors.primaryText else theme.colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                Text(m.whenLabel, color = theme.colors.text, fontSize = 11.5.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Feed(events: List<ActivityEvent>) {
    val theme = LocalTheme.current
    var filter by remember { mutableStateOf<ActivityKind?>(null) }
    val visible = events.filter { filter == null || it.kind == filter }.take(60)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Recent")
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf<ActivityKind?>(null) + ActivityKind.entries).forEach { k ->
                FilterChip(k?.label ?: "All", filter == k) { filter = k }
            }
        }
        val shape = RoundedCornerShape(20.dp)
        Column(Modifier.fillMaxWidth().clip(shape).background(theme.colors.menu.copy(alpha = 0.85f)).border(1.dp, theme.colors.border, shape).padding(8.dp)) {
            if (visible.isEmpty()) {
                Text(
                    if (events.isEmpty()) "Nothing yet. Read a chapter, open a sermon or play a message and it will show up here."
                    else "Nothing here yet for this filter.",
                    color = theme.colors.text, fontSize = 13.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)
                )
            }
            visible.forEachIndexed { i, e ->
                if (i == 0 || visible[i - 1].day != e.day) {
                    Text(dayHeader(e.day), color = theme.colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp))
                }
                key(e.id, filter) { FeedRow(e, i) }
            }
        }
    }
}

private fun dayHeader(d: LocalDate): String = when (d) {
    LocalDate.now() -> "TODAY"
    LocalDate.now().minusDays(1) -> "YESTERDAY"
    else -> d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)).uppercase()
}

@Composable
internal fun FilterChip(label: String, on: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (on) theme.colors.primary else if (hovered) theme.colors.menuHoverColor else theme.colors.menu, tween(200))
    Text(
        label, color = if (on) Ink else theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.height(34.dp).clip(CircleShape).background(bg).border(1.dp, if (on) theme.colors.primary else theme.colors.border, CircleShape)
            .onHover { hovered = it }.clickable(role = Role.Tab, onClick = onClick).hand().padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun FeedRow(e: ActivityEvent, i: Int) {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    val engine = LocalPlayerState.current
    val nowPlaying by LocalPlayer.current.nowPlaying.collectAsState()
    val c = e.kind.color(theme.colors.primary)
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (hovered) theme.colors.menuHoverColor else Color.Transparent, tween(200))
    val shift by animateDpAsState(if (hovered) 3.dp else 0.dp, tween(250))
    val open: (() -> Unit)? = when {
        e.key.startsWith("letter:") -> { { nav.navigate(Screen.Letter(e.key.removePrefix("letter:"))) } }
        e.key.startsWith("bible:") || e.key.startsWith("hl:bible") -> { { nav.navigate(Screen.Library(LibraryScreen.Bible())) } }
        e.key.startsWith("listen:") && nowPlaying?.id == e.key.removePrefix("listen:") -> { { engine.setPause(false) } }
        e.key.startsWith("sermon:") || e.key.startsWith("finish:") || e.key.startsWith("hl:table") || e.key.startsWith("listen:") ->
            { { nav.navigate(Screen.Library(LibraryScreen.Table())) } }
        e.key.startsWith("live:") -> { { nav.navigate(Screen.Listener) } }
        e.key.startsWith("badge:") -> null
        else -> null
    }
    Row(
        Modifier.fillMaxWidth().rise(i.coerceAtMost(12) * 50).offset(x = shift).clip(RoundedCornerShape(14.dp)).background(bg).onHover { hovered = it }.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.10f)).border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(e.kind.tag, color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Text(e.title, color = theme.colors.primaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                Text(e.meta, color = c, fontSize = 12.sp)
            }
            e.quote?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it, color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.5.sp, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
            Text(
                java.time.Instant.ofEpochMilli(e.at).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(timeFormat),
                color = theme.colors.text, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (open != null) {
            Text(
                e.action, color = theme.colors.primaryBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = open).hand().padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
