package ui.screens.thisweek

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.skiko.Cursor
import loadData
import saveData
import parsers.bt.Letter
import parsers.bt.LetterBlock
import parsers.bt.LetterPart
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

internal fun Modifier.hand() = pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))

internal val longDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
internal val chipDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd.yy", Locale.ENGLISH)

/** The tear-off calendar tile used on the This Week cards and the letter header. */
@Composable
internal fun DateTile(date: LocalDate, width: Dp = 104.dp) {
    val theme = LocalTheme.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(width).clip(shape).background(Color(0xFFF6F1EC)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(),
            color = Color(0xFF7A4A28), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            date.dayOfMonth.toString(),
            color = Color(0xFF2A1A0E), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = (width.value * 0.44f).sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(Modifier.fillMaxWidth().background(theme.colors.primary).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(),
                color = Color(0xFF2A1A0E), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp
            )
        }
    }
}

/** Small rounded toggle used for filters and related services. */
@Composable
internal fun Chip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = null,
    mono: Boolean = false,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        when {
            selected -> theme.colors.primary
            hovered -> theme.colors.menuHoverColor
            else -> theme.colors.menu
        }, tween(160)
    )
    val fg = if (selected) Color(0xFF2A1A0E) else theme.colors.primaryText
    val shape = RoundedCornerShape(10.dp)
    val lift by androidx.compose.animation.core.animateDpAsState(if (hovered) (-2).dp else 0.dp, tween(250))
    Row(
        modifier
            .offset(y = lift)
            .height(36.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) theme.colors.primary else theme.colors.border, shape)
            .onHover { hovered = it }
            .clickable(role = Role.Tab, onClick = onClick)
            .hand()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label, color = fg, fontSize = 12.5.sp,
            fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else Inter,
            fontWeight = FontWeight.SemiBold
        )
        if (tag != null) {
            Text(
                tag, color = if (selected) fg else theme.colors.primaryBright, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    .background(if (selected) Color(0x2E2A1A0E) else theme.colors.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/** Icon + label button. [primary] fills it with the accent colour. */
@Composable
internal fun ActionButton(
    label: String?,
    icon: ((Color) -> ImageVector)? = null,
    primary: Boolean = false,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val fg = if (primary) Color(0xFF2A1A0E) else if (active) theme.colors.primary else theme.colors.primaryText
    val shape = RoundedCornerShape(11.dp)
    Row(
        Modifier
            .height(40.dp)
            .clip(shape)
            .background(if (primary) theme.colors.primary else if (hovered) theme.colors.menuHoverColor else Color.Transparent)
            .then(if (primary) Modifier else Modifier.border(1.dp, theme.colors.border, shape))
            .onHover { hovered = it }
            .clickable(role = Role.Button, onClick = onClick)
            .hand()
            .padding(horizontal = if (label == null) 11.dp else 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) Icon(icon(fg), contentDescription = label, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
        if (label != null) Text(label, color = fg, fontSize = 13.sp, fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    Text(text.uppercase(), color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = modifier)
}

@Composable
internal fun Card(modifier: Modifier = Modifier, padding: Dp = 20.dp, content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.clip(shape).background(theme.colors.menu).border(1.dp, theme.colors.border, shape).padding(padding)) { content() }
}

internal fun partColor(part: LetterPart, accent: Color): Color = if (part == LetterPart.LETTER) accent else Color(0xFF7FB8D9)

/** One paragraph, or a sermon quote with an accent bar and hover tools. */
@Composable
internal fun LetterBlockView(block: LetterBlock, fontSize: Int = 18, onCopy: (String) -> Unit) {
    val theme = LocalTheme.current
    if (!block.isQuote) {
        Text(block.text, fontFamily = Fraunces, fontSize = fontSize.sp, lineHeight = (fontSize * 1.72f).sp, color = theme.colors.primaryText.copy(alpha = 0.88f))
        return
    }
    var hovered by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape)
            .background(theme.colors.primary.copy(alpha = 0.08f))
            .border(1.dp, theme.colors.primary.copy(alpha = 0.28f), shape)
            .onHover { hovered = it }
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(theme.colors.primary))
        Column(Modifier.padding(start = 18.dp, top = 20.dp, end = 22.dp, bottom = 14.dp)) {
            Text(
                block.text, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = (fontSize + 1).sp,
                lineHeight = ((fontSize + 1) * 1.65f).sp, color = theme.colors.primaryBright
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Rev. William Marrion Branham" + (block.cite?.let { " · $it" } ?: ""),
                    color = theme.colors.text, fontSize = 12.sp
                )
                if (hovered) Text(
                    "Copy", color = theme.colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(7.dp))
                        .clickable { onCopy(block.text) }.hand().padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Bold-highlights [ranges] of [text] for search results. */
internal fun highlighted(text: String, ranges: List<IntRange>, mark: Color, ink: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    for (r in ranges.sortedBy { it.first }) {
        if (r.first < i || r.last >= text.length) continue
        append(text.substring(i, r.first))
        withStyle(SpanStyle(background = mark, color = ink, fontWeight = FontWeight.SemiBold)) { append(text.substring(r.first, r.last + 1)) }
        i = r.last + 1
    }
    append(text.substring(i))
}

/**
 * Waveform scrubber. The bar heights are decorative (seeded by the sermon code), not real
 * audio levels; [progress] and [onSeek] use 0..1.
 */
@Composable
internal fun Waveform(seed: String, progress: Float, modifier: Modifier = Modifier, onSeek: (Float) -> Unit) {
    val theme = LocalTheme.current
    val bars = remember(seed) {
        val s = seed.hashCode() % 97 / 97f
        List(110) { i -> 0.18f + 0.82f * abs(sin(i * 0.37f + s * 6f) * 0.6f + sin(i * 1.7f + s * 3f) * 0.4f) }
    }
    val played = theme.colors.primaryBright
    val rest = theme.colors.border.copy(alpha = 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .hand()
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> onSeek((change.position.x / size.width).coerceIn(0f, 1f)) } }
    ) {
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (bars.size - 1)) / bars.size
        bars.forEachIndexed { i, h ->
            val bh = size.height * h
            drawRoundRect(
                color = if ((i + 0.5f) / bars.size <= progress) played else rest,
                topLeft = Offset(i * (w + gap), (size.height - bh) / 2),
                size = Size(w, bh),
                cornerRadius = CornerRadius(w / 2)
            )
        }
    }
}

internal fun Letter.copyText(): String = buildString {
    appendLine("${code} ${title}")
    serviceDate?.let { appendLine(it.format(longDateFormat)) }
    appendLine()
    sections.forEach { s ->
        if (sections.size > 1) appendLine(s.part.label.uppercase()).also { appendLine() }
        if (s.part == LetterPart.LETTER && salutation != null) appendLine(salutation).also { appendLine() }
        s.blocks.forEach { appendLine(it.text); appendLine() }
    }
    appendLine("Bro. Joseph Branham")
}

private val scriptureRegex = Regex("""\b((?:[1-3]\s)?(?:Genesis|Exodus|Leviticus|Numbers|Deuteronomy|Joshua|Judges|Ruth|Samuel|Kings|Chronicles|Ezra|Nehemiah|Esther|Job|Psalms?|Proverbs|Ecclesiastes|Song of Solomon|Isaiah|Jeremiah|Lamentations|Ezekiel|Daniel|Hosea|Joel|Amos|Obadiah|Jonah|Micah|Nahum|Habakkuk|Zephaniah|Haggai|Zechariah|Malachi|Matthew|Mark|Luke|John|Acts|Romans|Corinthians|Galatians|Ephesians|Philippians|Colossians|Thessalonians|Timothy|Titus|Philemon|Hebrews|James|Peter|Jude|Revelation)\s\d{1,3}:\d{1,3}(?:[-–]\d{1,3})?)""")

/** Scripture references the letter itself mentions, in order. */
internal fun Letter.scriptures(): List<String> = scriptureRegex.findAll(plainText).map { it.value }.distinct().toList()

/** What the reader did with letters on this machine: saved, read, scripture ticked off. */
internal object LetterPrefs {
    val saved = mutableStateOf(loadData<HashSet<String>>("bt_saved") ?: hashSetOf())
    val read = mutableStateOf(loadData<HashSet<String>>("bt_read") ?: hashSetOf())
    val scripture = mutableStateOf(loadData<HashMap<String, HashSet<String>>>("bt_scripture") ?: hashMapOf())

    fun toggleSaved(id: String) {
        val next = HashSet(saved.value).apply { if (!add(id)) remove(id) }
        saved.value = next
        saveData("bt_saved", next)
    }

    fun markRead(id: String) {
        if (id in read.value) return
        parsers.bt.Letters.byId(id)?.let {
            activity.ActivityLog.record(activity.ActivityKind.LETTERS, "Read the letter for ${it.title}", "${it.code} · ${it.letterDate.format(longDateFormat)}", "letter:$id", "Open", quote = it.salutation)
        }
        val next = HashSet(read.value).apply { add(id) }
        read.value = next
        saveData("bt_read", next)
    }

    fun toggleScripture(id: String, ref: String) {
        val next = HashMap(scripture.value)
        next[id] = HashSet(next[id] ?: emptySet()).apply { if (!add(ref)) remove(ref) }
        scripture.value = next
        saveData("bt_scripture", next)
    }
}
