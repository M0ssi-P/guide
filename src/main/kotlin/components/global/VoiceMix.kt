package components.global

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.modifier.onHover
import parsers.vgr.SermonMedia
import player.EnglishBed
import player.MIX_MAX
import player.MIX_MIN
import ui.screens.thisweek.hand
import ui.theme.LocalTheme
import ui.theme.ZillaSlab

private val R4 = RoundedCornerShape(4.dp)
private val EnglishBlue = Color(0xFF9FC3F0)

/** Three little level bars that bob; [level] 0..1 sets their height. */
@Composable
private fun Meter(color: Color, level: Float, height: Dp = 16.dp, alpha: Float = 1f) {
    val t = rememberInfiniteTransition()
    val durations = listOf(800, 1050, 700)
    Row(Modifier.height(height), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        durations.forEach { d ->
            val s by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(d / 2, easing = LinearEasing), RepeatMode.Reverse))
            val h = height * (0.38f + 0.62f * level.coerceIn(0f, 1f))
            Box(Modifier.width(3.dp).height(h).graphicsLayer { scaleY = s; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f); this.alpha = alpha }.clip(RoundedCornerShape(1.dp)).background(color))
        }
    }
}

/** Two orange bars (the translation) and a blue one (English), brighter for the louder voice. */
@Composable
private fun ChipMeter(orange: Color, orangeAlpha: Float, blueAlpha: Float) {
    val t = rememberInfiniteTransition()
    Row(Modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(Triple(800, orange, orangeAlpha), Triple(1100, orange, orangeAlpha), Triple(700, EnglishBlue, blueAlpha)).forEach { (d, c, a) ->
            val s by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(d / 2, easing = LinearEasing), RepeatMode.Reverse))
            Box(Modifier.width(3.dp).height(14.dp).graphicsLayer { scaleY = s; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f); alpha = a }.clip(RoundedCornerShape(1.dp)).background(c))
        }
    }
}

/** The float player chip: level bars and "85 · 15". Opens the Voices popover above it. Hidden when there's no English to mix. */
@Composable
fun VoicesChip() {
    if (!EnglishBed.active) return
    val theme = LocalTheme.current
    var open by remember { mutableStateOf(false) }
    val eng = EnglishBed.mix
    val swa = 100 - eng
    var hovered by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(32.dp).clip(R4).background(if (hovered) theme.colors.menuHoverColor else theme.colors.surface)
                .border(1.dp, theme.colors.border, R4)
                .semantics { contentDescription = "Voices: ${EnglishBed.languageName} $swa percent, English $eng percent" }
                .onHover { hovered = it }
                .clickable(role = Role.Button) { open = !open }.hand().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ChipMeter(theme.colors.primaryBright, 0.35f + swa / 150f, 0.35f + eng / 150f)
            Text("$swa · $eng", color = theme.colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
        if (open) Popup(popupPositionProvider = AboveEnd, onDismissRequest = { open = false }, properties = PopupProperties(focusable = true)) {
            Column(
                Modifier.width(340.dp).rise(0, 8.dp).clip(R4).background(theme.colors.popupLayer).border(1.dp, theme.colors.border, R4).padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Voices", color = theme.colors.primaryText, fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("✕", color = theme.colors.text, fontSize = 16.sp, modifier = Modifier.clickable(onClickLabel = "Close") { open = false }.hand())
                }
                Spacer(Modifier.height(12.dp))
                VoicesMixer()
            }
        }
    }
}

/** Places a popup above its anchor, right edges lined up. */
private object AboveEnd : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
        IntOffset((anchorBounds.right - popupContentSize.width).coerceAtLeast(8), (anchorBounds.top - popupContentSize.height - 12).coerceAtLeast(8))
}

/** The balance itself: both voices with their percent, the two-colour slider (10–90, steps of 5), presets and the note. */
@Composable
fun VoicesMixer(language: String = EnglishBed.languageName) {
    val theme = LocalTheme.current
    val eng = EnglishBed.mix
    val swa = 100 - eng
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Meter(theme.colors.primaryBright, swa / 100f)
                Text(language, color = theme.colors.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Text("$swa%", color = theme.colors.primaryBright, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$eng%", color = EnglishBlue, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Text("English", color = theme.colors.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Meter(EnglishBlue, eng / 100f)
            }
        }
        BalanceSlider(eng, theme.colors.primary, "Voice balance, left more $language, right more English") { EnglishBed.updateMix(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15 to "Mostly $language", 50 to "Even", 85 to "Mostly English").forEach { (v, label) ->
                val on = eng == v
                val bg by animateColorAsState(if (on) theme.colors.primary.copy(alpha = 0.12f) else Color.Transparent, tween(200))
                Box(
                    Modifier.weight(1f).height(30.dp).clip(R4).background(bg).border(1.dp, if (on) theme.colors.primary else theme.colors.border, R4)
                        .clickable(role = Role.Button) { EnglishBed.updateMix(v) }.hand(),
                    contentAlignment = Alignment.Center
                ) { Text(label, color = if (on) theme.colors.primaryBright else theme.colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
            }
        }
        Text("English stays underneath so you hear him sing and shout. Neither voice goes below 10%.", color = theme.colors.text, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

/** The track is orange up to the thumb and blue after it, with end stops at 10 and 90; the thumb grows while dragged. */
@Composable
private fun BalanceSlider(value: Int, accent: Color, label: String, onChange: (Int) -> Unit) {
    val theme = LocalTheme.current
    var dragging by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(dragging) { scale.animateTo(if (dragging) 1.15f else 1f, tween(150)) }
    BoxWithConstraints(Modifier.fillMaxWidth().height(28.dp).semantics { contentDescription = "$label: English $value percent" }) {
        val w = constraints.maxWidth.toFloat()
        val f = (value - MIX_MIN) / (MIX_MAX - MIX_MIN).toFloat()
        fun pick(x: Float) = onChange((MIX_MIN + (x / w).coerceIn(0f, 1f) * (MIX_MAX - MIX_MIN)).toInt())
        Box(
            Modifier.fillMaxSize().pointerInput(w) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.first()
                        if (c.pressed) { dragging = true; pick(c.position.x) } else dragging = false
                    }
                }
            }
        ) {
            Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(6.dp).clip(R4)
                .background(Brush.horizontalGradient(0f to accent, f to accent, f to EnglishBlue, 1f to EnglishBlue)))
            Box(Modifier.align(Alignment.CenterStart).width(2.dp).height(10.dp).background(theme.colors.text.copy(alpha = 0.5f)))
            Box(Modifier.align(Alignment.CenterEnd).width(2.dp).height(10.dp).background(theme.colors.text.copy(alpha = 0.5f)))
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { (f * w).toDp() } - 10.dp)
                    .size(20.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }.clip(CircleShape).background(Color(0xFFF6F1EC)).border(3.dp, accent, CircleShape)
            )
        }
    }
}

/** The Reading settings section: the same mixer and "Always download English with other languages". */
@Composable
fun VoicesSettings() {
    val theme = LocalTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("VOICES", color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        VoicesMixer()
        Row(Modifier.clickable { EnglishBed.setAlways(EnglishBed.alwaysDownload != true) }.hand(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Checkbox(EnglishBed.alwaysDownload == true, { EnglishBed.setAlways(it) }, Modifier.size(16.dp), colors = CheckboxDefaults.colors(checkedColor = theme.colors.primary))
            Text("Always download English with other languages", color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}

/** The first-time question after downloading a translated sermon. Lives over the whole window (like the update overlay). */
@Composable
fun EnglishAskOverlay() {
    val sermon = EnglishBed.asking ?: return
    val theme = LocalTheme.current
    var remember by remember { mutableStateOf(true) }
    val language = remember(sermon) { mutableStateOf("Kiswahili") }
    LaunchedEffect(sermon) { live.TableInstaller.installed().firstOrNull { it.tag == sermon.language }?.let { language.value = it.localName.ifBlank { it.name } } }
    fun answer(yes: Boolean) {
        if (remember) EnglishBed.setAlways(yes)
        if (yes) SermonMedia.downloadEnglishCompanionLater(sermon)
        EnglishBed.asking = null
    }
    Box(Modifier.fillMaxSize().background(Color(0x8C0C0A09)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}, contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.padding(top = 140.dp).widthIn(max = 460.dp).fillMaxWidth(0.9f).rise(0, 8.dp).clip(R4).background(theme.colors.popupLayer).border(1.dp, theme.colors.border, R4).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp).clip(R4).background(EnglishBlue.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Text("♪", color = EnglishBlue, fontSize = 20.sp) }
                Column {
                    Text("Also get Brother Branham's voice?", color = theme.colors.primaryText, fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Text("The English recording plays softly under ${language.value}, so you hear him sing and shout where the interpreter is quiet. Adds about 12 MB per sermon.",
                        color = theme.colors.text, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Row(Modifier.clickable { remember = !remember }.hand(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Checkbox(remember, { remember = it }, Modifier.size(16.dp), colors = CheckboxDefaults.colors(checkedColor = theme.colors.primary))
                Text("Remember my choice", color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                Box(Modifier.height(38.dp).clip(R4).border(1.dp, theme.colors.border, R4).clickable { answer(false) }.hand().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text("Only ${language.value}", color = theme.colors.text, fontSize = 13.sp)
                }
                Box(Modifier.height(38.dp).clip(R4).background(theme.colors.primary).clickable { answer(true) }.hand().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Download both", color = Color(0xFF2A1A0E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
