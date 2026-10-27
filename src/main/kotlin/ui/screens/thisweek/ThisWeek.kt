package ui.screens.thisweek

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import com.composables.play
import components.global.GlowBlob
import components.global.SoftEase
import components.global.animatedFill
import components.global.bob
import components.global.rise
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.searchIcon
import kotlinx.coroutines.launch
import navigation.LocalModal
import navigation.LocalTabs
import navigation.Screen
import navigation.dismiss
import navigation.show
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import parsers.bt.Letter
import parsers.bt.LetterPart
import parsers.bt.Letters
import ui.theme.Fraunces
import ui.theme.LocalTheme

internal fun Letter.excerpt(): String? =
    sections.firstOrNull { it.part == LetterPart.LETTER }?.blocks?.firstOrNull { !it.isQuote }?.text

@Composable
fun ThisWeek() {
    val theme = LocalTheme.current
    val globalPlayer = player.LocalPlayer.current
    val engine = player.LocalPlayerState.current
    val playScope = rememberCoroutineScope()
    val playModal = LocalModal.current
    fun playSermon(s: parsers.vgr.models.Sermon) {
        val start = { url: String ->
            globalPlayer.loadNowPlaying(player.GlobalPlayerViewModel.NowPlaying(s.stringId, s.title, "${s.dateCode} - ${s.location}", player.GlobalPlayerType.SERMON, true, 0L, url.trim()))
            engine.setPause(false)
        }
        playScope.launch {
            val local = parsers.vgr.SermonMedia.findLocal(s)
            if (local != null) start(local.absolutePath)
            else playModal.show { ui.screens.table.ListenModal(sermon = s, onPlay = { start(it); playModal.dismiss() }, onDismiss = { playModal.dismiss() }) }
        }
    }
    val nav = LocalTabs.current.current
    val modal = LocalModal.current
    val letters by Letters.all.collectAsState()
    val sync by Letters.sync.collectAsState()
    val read by LetterPrefs.read
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) { Letters.sync() }
    val vgrVM = nav.viewModelStore.getOrCreate("HomeVM-${nav.id}") { parsers.vgr.VgrViewModel() }
    val sermons by vgrVM.sermons.collectAsState()
    val events by activity.ActivityLog.events
    fun sermonFor(l: Letter) = sermons.firstOrNull { it.dateCode.equals(l.code, ignoreCase = true) }
    /** 0..1: half for reading the letter, half for hearing the sermon. */
    fun progressOf(l: Letter): Pair<Float, String> {
        val s = sermonFor(l)
        val heard = events.filter { it.key == "listen:${s?.stringId}" }.sumOf { it.seconds }
        val total = (s?.minutes ?: 0) * 60L
        val sermonPart = if (total > 0) (heard.toFloat() / total).coerceIn(0f, 1f) else if (heard > 0) 1f else 0f
        val isRead = l.id in read
        val label = when {
            !isRead && heard == 0L -> "Not started"
            sermonPart >= 0.98f -> if (isRead) "Letter read · sermon heard" else "Sermon heard"
            heard > 0 -> (if (isRead) "Letter read · " else "") + "${activity.shortDuration(heard)}" + (if (total > 0) " of ${activity.shortDuration(total)}" else " heard")
            else -> "Letter read"
        }
        return ((if (isRead) 0.5f else 0f) + sermonPart / 2) to label
    }
    var month by remember { mutableStateOf<java.time.YearMonth?>(java.time.YearMonth.now()) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 56.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 1040.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.rise()) {
                        Text("The weekly sermon and letter from Brother Joseph Branham", color = theme.colors.text, fontSize = 13.sp)
                        Text("This Week", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            when {
                                sync.running -> "Checking for new letters…"
                                sync.error != null -> sync.error!!
                                else -> ""
                            },
                            color = if (sync.error != null) theme.colors.crimson else theme.colors.text, fontSize = 12.sp
                        )
                        ActionButton("Letter archive", ::searchIcon) { nav.navigate(Screen.LetterArchive) }
                    }
                }

                val latest = letters.firstOrNull()
                if (latest == null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (sync.running) "Getting this week's letter…" else "No letters yet. Check your internet connection and try again.",
                                color = theme.colors.primaryText, fontSize = 14.sp
                            )
                            if (!sync.running) ActionButton("Try again", primary = true) { scope.launch { Letters.sync() } }
                        }
                    }
                } else {
                    HeroCard(
                        latest, isRead = latest.id in read,
                        onOpen = { nav.navigate(Screen.Letter(latest.id)) },
                        onPlay = sermonFor(latest)?.let { s -> { playSermon(s) } },
                        modifier = Modifier.rise(50)
                    )

                    val earlier = letters.drop(1)
                    if (earlier.isNotEmpty()) {
                        val months = earlier.map { java.time.YearMonth.from(it.letterDate) }.distinct().take(2)
                        if (month != null && month !in months) month = months.firstOrNull()
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Earlier weeks", color = theme.colors.primaryText, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, fontSize = 22.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (months.map<java.time.YearMonth, java.time.YearMonth?> { it } + listOf(null)).forEach { m ->
                                    ui.screens.myactivity.FilterChip(m?.month?.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) ?: "All", month == m) { month = m }
                                }
                            }
                        }
                        val shown = earlier.filter { month == null || java.time.YearMonth.from(it.letterDate) == month }.take(if (month == null) 12 else 6)
                        androidx.compose.runtime.key(month) {
                            shown.forEachIndexed { i, l ->
                                val (pct, label) = progressOf(l)
                                LetterRow(l, pct, label, Modifier.rise(100 + i * 70)) { nav.navigate(Screen.Letter(l.id)) }
                            }
                        }
                        if (earlier.size > shown.size) ActionButton("See all ${letters.size} letters in the archive") { nav.navigate(Screen.LetterArchive) }
                    }
                }
            }
        }

        VerticalScrollbar(scrollState = scroll, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(letter: Letter, isRead: Boolean, onOpen: () -> Unit, onPlay: (() -> Unit)?, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(0f to theme.colors.primary.copy(alpha = 0.22f), 0.6f to theme.colors.menu))
            .border(1.dp, theme.colors.primary.copy(alpha = 0.35f), shape)
            .clickable(onClick = onOpen).hand()
    ) {
        GlowBlob(theme.colors.primary.copy(alpha = 0.32f), 300.dp, Modifier.align(Alignment.TopEnd).offset(80.dp, (-80).dp), periodMs = 3400)
        Row(Modifier.padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Box(Modifier.bob()) { DateTile(letter.letterDate, width = 112.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "THIS WEEK", color = Color(0xFF2A1A0E), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(theme.colors.primary).padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Text(listOfNotNull(letter.code, letter.kind).joinToString(" · "), color = theme.colors.text, fontSize = 12.5.sp)
                    if (isRead) Text("Read", color = Color(0xFF7FD9A8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(letter.title, color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp)
                letter.salutation?.let {
                    Text(it, color = theme.colors.primaryText.copy(alpha = 0.85f), fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 17.sp)
                }
                letter.excerpt()?.let {
                    Text(it, color = theme.colors.text, fontSize = 15.sp, lineHeight = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 640.dp))
                }
                FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Read the letter →", primary = true, onClick = onOpen)
                    if (onPlay != null) ActionButton("Play sermon", ::play, onClick = onPlay)
                }
            }
        }
    }
}

@Composable
private fun LetterRow(letter: Letter, progress: Float, progressLabel: String, modifier: Modifier, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val lift by animateDpAsState(if (hovered) (-4).dp else 0.dp, tween(350, easing = SoftEase))
    val tilt by animateFloatAsState(if (hovered) 1f else 0f, tween(400, easing = SoftEase))
    val fill = animatedFill(progress, 0, 1000)
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.fillMaxWidth().offset(y = lift).clip(shape)
            .background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu)
            .border(1.dp, if (hovered) theme.colors.primary.copy(alpha = 0.35f) else theme.colors.border, shape)
            .onHover { hovered = it }
            .clickable(onClick = onClick).hand()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Box(Modifier.graphicsLayer { rotationZ = -4f * tilt; scaleX = 1f + 0.04f * tilt; scaleY = 1f + 0.04f * tilt }) {
            DateTile(letter.letterDate, width = 78.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Text(letter.code, color = theme.colors.primaryBright, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Text(letter.title, color = theme.colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            letter.salutation?.let { Text(it, color = theme.colors.primaryText.copy(alpha = 0.85f), fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 15.sp) }
            letter.excerpt()?.let { Text(it, color = theme.colors.text, fontSize = 14.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.colors.border)) {
                    Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(theme.colors.primary))
                }
                Text(progressLabel, color = theme.colors.text, fontSize = 12.sp)
            }
        }
        Text("›", color = theme.colors.text, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
    }
}
