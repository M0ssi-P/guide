package ui.screens.reading

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.popIn
import loadData
import org.jetbrains.jewel.foundation.modifier.onHover
import saveData
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme
import ui.theme.ZillaSlab
import java.io.Serializable

enum class ReadStyle(val label: String, val desc: String) {
    BOOK("Book", "The page-turning view you know."),
    SCROLL("Scroll", "One long chapter, flows into the next."),
    PAGES("Pages", "A two-page book. Swipe to turn."),
    VERSE("Verse", "One verse at a time, large."),
}

enum class ReadFont(val label: String) { SERIF("Serif"), BOOK("Book"), SANS("Sans"), SLAB("Slab") }
enum class ReadSpacing(val label: String, val lh: Float) { COMPACT("Compact", 1.5f), NORMAL("Normal", 1.75f), AIRY("Airy", 2.05f) }
enum class ReadWidth(val label: String, val dp: Int) { NARROW("Narrow", 560), MEDIUM("Medium", 700), WIDE("Wide", 900) }

/** Page colours from the design. Night is the listener's darkest page. */
enum class ReadTheme(
    val label: String, val bg: Long, val paper: Long, val fg: Long, val head: Long, val muted: Long, val accent: Long, val rule: Long, val jesus: Long,
) {
    NIGHT("Night", 0xFF090B10, 0xFF0E1219, 0xFFE8E2D2, 0xFFFFF6E2, 0xFF8A93A6, 0xFFEFCB7E, 0xFF222834, 0xFFF2837A),
    DARK("Dark", 0xFF262220, 0xFF2B2724, 0xFFE6DED6, 0xFFF6F1EC, 0xFFA8A099, 0xFFF0A36B, 0xFF3A3532, 0xFFF2837A),
    WARM("Warm", 0xFF2B2118, 0xFF31261C, 0xFFF3E3CF, 0xFFFFF1DE, 0xFFB89E86, 0xFFF0A36B, 0xFF4A3A2C, 0xFFF2837A),
    SEPIA("Sepia", 0xFFF1E6D2, 0xFFF6ECDB, 0xFF3B2E22, 0xFF2A1F15, 0xFF8A7660, 0xFFB5561B, 0xFFDCCBB0, 0xFFB3261E),
    LIGHT("Light", 0xFFFBFAF7, 0xFFFFFFFF, 0xFF2A2522, 0xFF151210, 0xFF7D746D, 0xFFC2621F, 0xFFE6E0D8, 0xFFB3261E);

    val bgC get() = Color(bg); val paperC get() = Color(paper); val fgC get() = Color(fg); val headC get() = Color(head)
    val mutedC get() = Color(muted); val accentC get() = Color(accent); val ruleC get() = Color(rule); val jesusC get() = Color(jesus)
    val selC get() = accentC.copy(alpha = if (this == SEPIA || this == LIGHT) 0.16f else 0.22f)
}

/** Everything the reading settings modal changes. Bible, The Table and the live listener each keep their own copy. */
data class ReadingPrefs(
    val style: ReadStyle = ReadStyle.SCROLL,
    val font: ReadFont = ReadFont.SERIF,
    val size: Int = 20,
    val spacing: ReadSpacing = ReadSpacing.NORMAL,
    val theme: ReadTheme = ReadTheme.DARK,
    val width: ReadWidth = ReadWidth.MEDIUM,
    val numbers: Boolean = true,
    val red: Boolean = true,
    val headings: Boolean = true,
    val perLine: Boolean = false,
    val glow: Boolean = true,
    val showEn: Boolean = false,
    val awake: Boolean = true,
    val offsetMs: Int = 0,
) : Serializable {
    val family: FontFamily get() = when (font) {
        ReadFont.SERIF -> Fraunces
        // shortcut: Literata and Zilla Slab aren't bundled; Book uses the system serif and Slab a heavy Fraunces until those fonts are added
        ReadFont.BOOK -> FontFamily.Serif
        ReadFont.SANS -> Inter
        ReadFont.SLAB -> ZillaSlab
    }
    val weight: FontWeight get() = if (font == ReadFont.SLAB) FontWeight.SemiBold else FontWeight.Normal
}

/** One remembered set of prefs under [key]; [default] is what Reset goes back to. */
class ReadingPrefsStore(private val key: String, val default: ReadingPrefs) {
    val prefs = mutableStateOf(loadData<ReadingPrefs>(key) ?: default)
    fun set(p: ReadingPrefs) { prefs.value = p; saveData(key, p) }
}

object ReadingStores {
    val bible = ReadingPrefsStore("reading_bible", ReadingPrefs())
    val table = ReadingPrefsStore("reading_table", ReadingPrefs(style = ReadStyle.BOOK, size = 19))
    val listener = ReadingPrefsStore("reading_listener", ReadingPrefs(size = 22, theme = ReadTheme.NIGHT))
}

/** What the modal offers in each place; wording follows the place (verses vs paragraphs). */
data class SettingsOptions(
    val styles: List<ReadStyle>,
    val fonts: List<ReadFont> = ReadFont.entries,
    val themes: List<ReadTheme> = listOf(ReadTheme.DARK, ReadTheme.WARM, ReadTheme.SEPIA, ReadTheme.LIGHT),
    val sizeRange: IntRange = 15..30,
    val width: Boolean = true,
    val toggles: List<Pair<String, (ReadingPrefs) -> Boolean>>,
    val toggleSet: (ReadingPrefs, Int, Boolean) -> ReadingPrefs,
    val audioDelay: Boolean = false,
)

val BibleOptions = SettingsOptions(
    styles = listOf(ReadStyle.SCROLL, ReadStyle.PAGES, ReadStyle.VERSE),
    toggles = listOf("Verse numbers" to { it.numbers }, "Words of Jesus in red" to { it.red }, "Section headings" to { it.headings }, "Each verse on its own line" to { it.perLine }),
    toggleSet = { p, i, v -> when (i) { 0 -> p.copy(numbers = v); 1 -> p.copy(red = v); 2 -> p.copy(headings = v); else -> p.copy(perLine = v) } },
)

val TableOptions = SettingsOptions(
    styles = listOf(ReadStyle.BOOK, ReadStyle.SCROLL),
    toggles = listOf("Paragraph numbers" to { it.numbers }, "Each paragraph apart" to { it.perLine }),
    toggleSet = { p, i, v -> if (i == 0) p.copy(numbers = v) else p.copy(perLine = v) },
)

val ListenerOptions = SettingsOptions(
    styles = emptyList(),
    fonts = listOf(ReadFont.SERIF, ReadFont.BOOK, ReadFont.SANS),
    themes = listOf(ReadTheme.NIGHT, ReadTheme.DARK, ReadTheme.SEPIA, ReadTheme.LIGHT),
    sizeRange = 16..34,
    width = false,
    toggles = listOf("Glow on the words being spoken" to { it.glow }, "Paragraph numbers" to { it.numbers }, "Show the English under mine" to { it.showEn }, "Keep the screen awake" to { it.awake }),
    toggleSet = { p, i, v -> when (i) { 0 -> p.copy(glow = v); 1 -> p.copy(numbers = v); 2 -> p.copy(showEn = v); else -> p.copy(awake = v) } },
    audioDelay = true,
)

/**
 * The reading settings dialog from the Bible design: a live [preview] on the page colour beside the controls.
 * Every change applies at once; Reset goes back to the store's default.
 */
@Composable
fun ReadingSettingsModal(store: ReadingPrefsStore, options: SettingsOptions, onDismiss: () -> Unit, preview: @Composable ColumnScope.(ReadingPrefs) -> Unit) {
    val theme = LocalTheme.current
    val p by store.prefs
    val accent = theme.colors.primary
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.widthIn(max = 860.dp).fillMaxWidth(0.92f).clip(shape).background(theme.colors.popupLayer).border(1.dp, theme.colors.border, shape)
            .clickable(enabled = false) {}
    ) {
        // live preview
        Column(Modifier.weight(0.42f).background(p.theme.bgC).padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("PREVIEW", color = p.theme.mutedC, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            preview(p)
        }
        // controls
        Column(
            Modifier.weight(0.58f).heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Reading settings", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Box(
                    Modifier.size(32.dp).clip(CircleShape).border(1.dp, theme.colors.border, CircleShape).clickable(role = Role.Button, onClickLabel = "Close", onClick = onDismiss).hand(),
                    contentAlignment = Alignment.Center
                ) { Text("×", color = theme.colors.primaryText, fontSize = 16.sp) }
            }

            if (options.styles.isNotEmpty()) Group("READING STYLE") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.styles.forEach { st -> StyleCard(st, p.style == st, accent, Modifier.weight(1f)) { store.set(p.copy(style = st)) } }
                }
            }

            Group("TYPEFACE") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.fonts.forEach { f ->
                        val on = p.font == f
                        val probe = p.copy(font = f)
                        OptionBox(on, accent, Modifier.weight(1f), onClick = { store.set(p.copy(font = f)) }) {
                            Text("Aa", fontFamily = probe.family, fontWeight = probe.weight, fontSize = 24.sp, color = theme.colors.primaryText)
                            Text(f.label, color = theme.colors.text, fontSize = 11.5.sp)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Group("TEXT SIZE", Modifier.weight(1f), trailing = "${p.size} pt") {
                    Slider(
                        p.size.toFloat(), { store.set(p.copy(size = it.toInt())) },
                        valueRange = options.sizeRange.first.toFloat()..options.sizeRange.last.toFloat(), steps = options.sizeRange.last - options.sizeRange.first - 1,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = theme.colors.border, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent)
                    )
                }
                Group("LINE SPACING", Modifier.weight(1f)) {
                    Segmented(ReadSpacing.entries.map { it.label }, p.spacing.ordinal, accent) { store.set(p.copy(spacing = ReadSpacing.entries[it])) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Group(if (options.width) "PAGE COLOUR" else "PAGE", Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.themes.forEach { t ->
                            val on = p.theme == t
                            var hovered by remember { mutableStateOf(false) }
                            val s by androidx.compose.animation.core.animateFloatAsState(if (hovered) 1.08f else 1f, tween(200))
                            Box(
                                Modifier.size(34.dp * s).clip(CircleShape).background(t.bgC).border(2.dp, if (on) accent else theme.colors.border, CircleShape)
                                    .onHover { hovered = it }.clickable(role = Role.RadioButton, onClickLabel = t.label) { store.set(p.copy(theme = t)) }.hand(),
                                contentAlignment = Alignment.Center
                            ) { Text("A", color = t.fgC, fontFamily = Fraunces, fontSize = 13.sp) }
                        }
                    }
                }
                if (options.width) Group("TEXT WIDTH", Modifier.weight(1f)) {
                    Segmented(ReadWidth.entries.map { it.label }, p.width.ordinal, accent) { store.set(p.copy(width = ReadWidth.entries[it])) }
                }
            }

            Column {
                options.toggles.chunked(2).forEachIndexed { r, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        pair.forEachIndexed { c, (label, get) ->
                            val i = r * 2 + c
                            SwitchRow(label, get(p), accent, Modifier.weight(1f)) { store.set(options.toggleSet(p, i, it)) }
                        }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }

            if (options.audioDelay) Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.colors.border))
                Group("AUDIO DELAY", Modifier.padding(top = 12.dp), trailing = (if (p.offsetMs > 0) "+" else "") + "${p.offsetMs} ms") {
                    Slider(
                        p.offsetMs.toFloat(), { store.set(p.copy(offsetMs = (it / 20).toInt() * 20)) }, valueRange = -400f..400f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = theme.colors.border)
                    )
                    Text("Wireless headphones lag a little. Move this until the words and voice line up.", color = theme.colors.text, fontSize = 11.5.sp)
                }
            }

            if (options.audioDelay) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.colors.border))
                components.global.VoicesSettings()
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Reset to default", color = theme.colors.text, fontSize = 13.sp, modifier = Modifier.clickable { store.set(store.default) }.hand().padding(vertical = 10.dp))
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(accent).clickable(role = Role.Button, onClick = onDismiss).hand().padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Done", color = Color(0xFF2A1A0E), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}


@Composable
private fun Group(label: String, modifier: Modifier = Modifier, trailing: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalTheme.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            if (trailing != null) Text(trailing, color = theme.colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun OptionBox(on: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (on) accent.copy(alpha = 0.10f) else theme.colors.menu)
            .border(1.dp, if (on) accent else if (hovered) theme.colors.primaryText.copy(alpha = 0.3f) else theme.colors.border, RoundedCornerShape(12.dp))
            .onHover { hovered = it }.clickable(role = Role.RadioButton, onClick = onClick).hand().padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), content = content
    )
}

@Composable
private fun StyleCard(st: ReadStyle, on: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (on) accent.copy(alpha = 0.10f) else theme.colors.menu)
            .border(1.dp, if (on) accent else if (hovered) theme.colors.primaryText.copy(alpha = 0.3f) else theme.colors.border, RoundedCornerShape(14.dp))
            .onHover { hovered = it }.clickable(role = Role.RadioButton, onClick = onClick).hand().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(8.dp)).background(theme.colors.surface).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
        ) {
            val bar = Color(0xFF5A524D)
            when (st) {
                ReadStyle.SCROLL -> Box(Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.8f).clip(RoundedCornerShape(3.dp)).background(bar))
                ReadStyle.PAGES, ReadStyle.BOOK -> repeat(2) { Box(Modifier.width(30.dp).fillMaxHeight(0.9f).clip(RoundedCornerShape(3.dp)).background(bar)) }
                ReadStyle.VERSE -> Box(Modifier.fillMaxWidth(0.6f).fillMaxHeight(0.3f).clip(RoundedCornerShape(3.dp)).background(accent))
            }
        }
        Text(st.label, color = theme.colors.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        Text(st.desc, color = theme.colors.text, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
internal fun Segmented(labels: List<String>, selected: Int, accent: Color, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    val theme = LocalTheme.current
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(theme.colors.surface).border(1.dp, theme.colors.border, RoundedCornerShape(10.dp)).padding(3.dp)) {
        labels.forEachIndexed { i, l ->
            val on = i == selected
            val bg by animateColorAsState(if (on) accent else Color.Transparent, tween(200))
            Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable(role = Role.RadioButton) { onPick(i) }.hand(), contentAlignment = Alignment.Center) {
                Text(l, color = if (on) Color(0xFF2A1A0E) else theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun SwitchRow(label: String, on: Boolean, accent: Color, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val theme = LocalTheme.current
    val track by animateColorAsState(if (on) accent else theme.colors.border, tween(200))
    val x by animateDpAsState(if (on) 17.dp else 3.dp, tween(200))
    Row(
        modifier.clickable(role = Role.Switch) { onChange(!on) }.hand().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = theme.colors.primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(Modifier.width(34.dp).height(20.dp).clip(CircleShape).background(track)) {
            Box(Modifier.offset(x = x, y = 3.dp).size(14.dp).clip(CircleShape).background(Color.White))
        }
    }
}
