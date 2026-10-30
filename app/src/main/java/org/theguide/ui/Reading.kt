package org.theguide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import design.ReadingThemes
import design.Tones
import org.theguide.data.Prefs

/** A screen's reading look, remembered per [surface]: theme, tone, text size, face and spacing. */
class LookState(private val surface: String, defSize: Int, defGap: String = "snug") {
    var theme by mutableStateOf(Prefs.theme(surface))
    var tone by mutableStateOf(Prefs.tone(surface))
    var size by mutableIntStateOf(Prefs.size(surface, defSize))
    var face by mutableStateOf(Prefs.face(surface))
    var gap by mutableStateOf(Prefs.gap(surface, defGap))
    val look get() = Look(ReadingThemes.byId(theme), Tones.byId(tone))
    val serif get() = face == "serif"
    val lineHeight get() = if (gap == "airy") 1.85f else 1.55f
    fun save() = Prefs.saveLook(surface, theme, tone, size, face, gap)
}

@Composable
fun rememberLook(surface: String, defSize: Int, defGap: String = "snug") = remember(surface) { LookState(surface, defSize, defGap) }

/**
 * Text with one part lit as it is spoken: the `.lit` span, a soft band of the tone behind the words that grows to
 * [fill] of each line it covers (box-decoration-break: clone), easing over .6s.
 */
@Composable
fun LitText(before: String, now: String, after: String, fill: Float, style: TextStyle, look: Look, modifier: Modifier = Modifier, follow: Boolean = false) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val f = animate(fill, 600, Ease.Linear, "lit")
    val start = before.length; val end = start + now.length
    // with [follow], the scrolling parent keeps the spoken sentence in view (long paragraphs on the TV)
    val bring = remember { BringIntoViewRequester() }
    LaunchedEffect(follow, now, layout) {
        val l = layout ?: return@LaunchedEffect
        if (!follow || now.isEmpty()) return@LaunchedEffect
        val top = l.getLineTop(l.getLineForOffset(start)); val bottom = l.getLineBottom(l.getLineForOffset((end - 1).coerceAtLeast(start)))
        bring.bringIntoView(androidx.compose.ui.geometry.Rect(0f, top, l.size.width.toFloat(), bottom))
    }
    BasicText(
        buildAnnotatedString { append(before); append(now); append(after) },
        modifier.bringIntoViewRequester(bring).drawBehind {
            val l = layout ?: return@drawBehind
            if (now.isEmpty()) return@drawBehind
            val r = 6.dp.toPx()
            for (line in l.getLineForOffset(start)..l.getLineForOffset((end - 1).coerceAtLeast(start))) {
                val s = maxOf(start, l.getLineStart(line)); val e = minOf(end, l.getLineEnd(line, visibleEnd = true))
                if (e <= s) continue
                val x0 = l.getHorizontalPosition(s, true); val x1 = l.getHorizontalPosition(e, true)
                val w = (x1 - x0) * f
                if (w <= 0) continue
                val top = l.getLineTop(line); val h = l.getLineBottom(line) - top
                drawRoundRect(Brush.horizontalGradient(listOf(look.litStart, look.litEnd), x0, x0 + w), Offset(x0, top), Size(w, h), CornerRadius(r))
            }
        },
        style = style, onTextLayout = { layout = it },
    )
}

/**
 * The "Reading" sheet's content: themes as little open books, tones as glowing orbs, text size, typeface and spacing.
 * [sizeRange] is the slider's range (phone 16–28, TV 22–40).
 */
@Composable
fun ColumnScope.ReadingSettings(s: LookState, sizeRange: IntRange = 16..28, tv: Boolean = false) {
    val k = s.look
    S("Reading", 24f, k.tx)
    T("Theme", 13f, k.sub, Modifier.padding(top = 16.dp, bottom = 10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ReadingThemes.all.forEach { t ->
            val on = s.theme == t.id
            val lift = animate(if (on) 1f else 0f, 350, Ease.O13)
            val ring by animateColorAsState(if (on) k.acc else Color.Transparent, tw(300), label = "ring")
            Column(
                Modifier.weight(1f).choiceOrFocus(tv, RoundedCornerShape(14.dp), { s.theme = t.id; s.save() }, Role.RadioButton),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    Modifier.size(74.dp, 56.dp).graphicsLayer { translationY = -4f * lift * density; rotationZ = -2f * lift }
                        .cssShadow(0f, 8f, 10f, 0f, Color.Black.copy(alpha = .28f), RoundedCornerShape(11.dp))
                        .drawBehind {
                            val w = 4.dp.toPx()
                            drawRoundRect(ring, Offset(-w, -w), Size(size.width + 2 * w, size.height + 2 * w), CornerRadius(14.dp.toPx()), style = Stroke(2.dp.toPx()))
                        }
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        val ink = c(t.ink); val acc = if (t.light) c(0xFF9A6417) else C.Gold
                        Page(Modifier.weight(1f), corners(10f, 3f, 3f, 12f), Brush.horizontalGradient(.7f to c(t.bg), 1f to c(t.fold)), 8f, 6f,
                            listOf(.9f to ink.copy(alpha = .7f), .7f to ink.copy(alpha = .45f), .8f to acc))
                        Page(Modifier.weight(1f), corners(3f, 10f, 12f, 3f), Brush.horizontalGradient(0f to c(t.fold), .3f to c(t.bg)), 6f, 8f,
                            listOf(.85f to ink.copy(alpha = .7f), .92f to ink.copy(alpha = .45f), .6f to ink.copy(alpha = .45f)))
                    }
                }
                T(t.name, 12f, if (on) k.tx else k.sub)
            }
        }
    }
    T("Tone", 13f, k.sub, Modifier.padding(top = 20.dp, bottom = 10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tones.all.forEach { n ->
            val on = s.tone == n.id
            val rgb = rgb(n.rgb)
            Column(
                Modifier.weight(1f).choiceOrFocus(tv, RoundedCornerShape(22.dp), { s.tone = n.id; s.save() }, Role.RadioButton).clip(RoundedCornerShape(22.dp)).background(if (on) k.toneSelected else Color.Transparent)
                    .padding(top = 10.dp, bottom = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(28.dp).then(if (on) Modifier.cssShadow(0f, 0f, 18f, 0f, rgb.copy(alpha = .8f), CircleShape) else Modifier)
                        .clip(CircleShape).drawBehind { drawCircle(Brush.radialGradient(0f to Color.White, .6f to rgb, center = Offset(size.width * .35f, size.height * .3f), radius = size.width * 1.1f)) }
                )
                T(n.name, 12.5f, k.tx)
            }
        }
    }
    T("Text size", 13f, k.sub, Modifier.padding(top = 20.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        S("A", 14f, k.tx)
        Slider(s.size.toFloat(), sizeRange.first.toFloat(), sizeRange.last.toFloat(), 1f, { s.size = it.toInt(); s.save() }, Modifier.weight(1f), "Text size",
            track = { f -> Brush.horizontalGradient(0f to k.acc, f to k.acc, f to k.track, 1f to k.track) }, trackH = 4f, trackTop = 16f, tv = tv)
        S("A", 24f, k.tx)
    }
    T("Typeface and spacing", 13f, k.sub, Modifier.padding(top = 12.dp, bottom = 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("serif" to "Serif", "sans" to "Sans").forEach { (id, name) ->
            Box(
                Modifier.weight(1f).height(48.dp).choiceOrFocus(tv, RoundedCornerShape(16.dp), { s.face = id; s.save() }, Role.RadioButton).clip(RoundedCornerShape(16.dp)).background(if (s.face == id) k.selected else Color.Transparent)
                    ,
                contentAlignment = Alignment.Center,
            ) { T(name, 17f, k.tx, face = if (id == "serif") Face.Serif else Face.Sans) }
        }
        listOf("snug" to 3f, "airy" to 6f).forEach { (id, gap) ->
            Column(
                Modifier.width(54.dp).height(48.dp).choiceOrFocus(tv, RoundedCornerShape(16.dp), { s.gap = id; s.save() }, Role.RadioButton).clip(RoundedCornerShape(16.dp)).background(if (s.gap == id) k.selected else Color.Transparent)
                    .label(if (id == "snug") "Snug lines" else "Airy lines"),
                verticalArrangement = Arrangement.spacedBy(gap.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listOf(22f, 22f, 16f).forEach { w -> Box(Modifier.size(w.dp, 2.dp).clip(RoundedCornerShape(1.dp)).background(k.tx)) }
            }
        }
    }
}

@Composable
private fun Page(modifier: Modifier, shape: androidx.compose.ui.graphics.Shape, bg: Brush, padL: Float, padR: Float, lines: List<Pair<Float, Color>>) {
    Column(modifier.fillMaxHeight().clip(shape).background(bg).padding(start = padL.dp, end = padR.dp, top = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { (w, col) -> Box(Modifier.fillMaxWidth(w).height(3.dp).clip(RoundedCornerShape(2.dp)).background(col)) }
    }
}

/** Tap on the phone, D-pad focus on the TV. */
fun Modifier.choiceOrFocus(tv: Boolean, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit, role: Role = Role.Button) =
    if (tv) tvFocus(shape, onClick, role) else press(onClick, role = role)

/**
 * The boards' range input: a [trackH] px track drawn by [track] (given the filled fraction) under a 26 px cream thumb
 * with a soft gold halo. Drag or tap on the phone; left/right on the TV.
 */
@Composable
fun Slider(
    value: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier, label: String,
    track: (Float) -> Brush, trackH: Float = 4f, trackTop: Float = 16f, height: Float = 36f, tv: Boolean = false,
) {
    val frac = ((value - min) / (max - min)).coerceIn(0f, 1f)
    var width by remember { mutableStateOf(1f) }
    fun at(x: Float) = (min + (x / width).coerceIn(0f, 1f) * (max - min)).let { Math.round(it / step) * step }.coerceIn(min, max)
    Box(
        modifier.height(height.dp).label(label)
            .then(if (tv) Modifier.tvSlider(value, min, max, step, onChange) else Modifier.pointerSlide({ onChange(at(it)) }))
            .drawBehind {
                width = size.width
                val th = trackH.dp.toPx(); val ty = trackTop.dp.toPx()
                drawRoundRect(track(frac), Offset(0f, ty), Size(size.width, th), CornerRadius(th / 2))
                val r = 13.dp.toPx(); val x = (r + frac * (size.width - 2 * r)); val cy = size.height / 2
                drawCircle(gold(.18f), r + 6.dp.toPx(), Offset(x, cy))
                drawCircle(Color.Black.copy(alpha = .25f), r + 2.dp.toPx(), Offset(x, cy + 6.dp.toPx()))
                drawCircle(C.Ink, r, Offset(x, cy))
            }
    )
}

private fun Modifier.pointerSlide(onX: (Float) -> Unit) = this.then(
    Modifier.pointerInputDrag(onX)
)

private fun Modifier.pointerInputDrag(onX: (Float) -> Unit) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val e = awaitPointerEvent()
            e.changes.forEach { if (it.pressed) { onX(it.position.x); it.consume() } }
        }
    }
}

/** Left / right on the remote move the slider by one step. */
private fun Modifier.tvSlider(value: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit) =
    tvFocus(RoundedCornerShape(18.dp), {}).then(
        Modifier.onPreviewKeyEventCompat { key ->
            when (key) {
                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> { onChange((value - step).coerceIn(min, max)); true }
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> { onChange((value + step).coerceIn(min, max)); true }
                else -> false
            }
        }
    )

/** Key-down handling by Android key code. */
fun Modifier.onPreviewKeyEventCompat(handle: (Int) -> Boolean) = onPreviewKeyEvent { e ->
    e.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && handle(e.nativeKeyEvent.keyCode)
}
