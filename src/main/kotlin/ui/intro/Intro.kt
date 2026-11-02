package ui.intro

// The same file is compiled by the desktop app (src/main/kotlin/ui/intro/Intro.kt): keep the two identical apart
// from the package line.

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * The opening sting: a spark falls onto the wick, the flame catches with a flash and a warm halo, the lamp draws
 * itself in, "The Guide" rises letter by letter under a sheen, then everything blooms away into the app (drawn
 * underneath from the start, so nothing waits on it). All vector, sized by the screen, so it stays sharp on 4K.
 * [wide] sizes it for TVs and desktops (by the height) rather than phones (by the short side). [at] freezes the
 * timeline at a moment (ms), for checking frames.
 */
@Composable
fun Intro(serif: FontFamily, wide: Boolean, reducedMotion: Boolean, onSound: () -> Unit, onDone: () -> Unit, at: Long? = null) {
    var t by remember { mutableLongStateOf(at ?: 0L) }
    var done by remember { mutableStateOf(false) }
    val end = if (reducedMotion) REDUCED_END else END
    LaunchedEffect(Unit) {
        if (at != null) return@LaunchedEffect
        if (!reducedMotion) onSound()
        val start = withFrameMillis { it }
        while (t < end) t = withFrameMillis { it } - start
        done = true; onDone()
    }
    if (done) return
    val skip = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { skip.requestFocus() } }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = 1f - prog(t.toFloat(), end - 500f, 500f, LinearEasing) }
            .background(Night)
            .pointerInput(Unit) { detectTapGestures { done = true; onDone() } }
            .onKeyEvent { done = true; onDone(); true }
            .focusRequester(skip).focusable(),
    ) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val lampW = if (wide) .10f * h else .135f * min(w, h)
        val k = lampW / 54f
        val lampH = 62f * k
        val lampX = (w - lampW) / 2f
        val lampY = h * .40f - lampH / 2f
        val word = if (wide) .096f * h else .055f * w
        val f = t.toFloat()

        if (reducedMotion) {
            val a = prog(f, 0f, 600f, LinearEasing)
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = a }) {
                halo(lampX, lampY, k, 1f, 1f)
                translate(lampX, lampY) { scale(k, k, Offset.Zero) { lamp(1f, 1f, 1f, 1f, 1f, f) } }
            }
            Title(serif, word, lampY + lampH + .45f * lampW, density.density, a, letters = { 1f }, spacing = -.015f * word, sheen = -1f, rule = 1f)
            return@BoxWithConstraints
        }

        // the whole group leaves together: fading, growing a little and blurring
        val exit = prog(f, 3300f, 800f, CubicBezierEasing(.6f, 0f, .8f, .2f))
        val group = Modifier.fillMaxSize().graphicsLayer {
            alpha = 1f - exit; scaleX = 1f + .07f * exit; scaleY = scaleX
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, (lampY + lampH / 2) / h)
        }.then(if (exit > 0f) Modifier.blur((6 * exit).dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier)

        Box(group) {
            // the grain: one batched draw of fixed specks, placed once per screen size
            Box(Modifier.fillMaxSize().drawWithCache {
                val specks = Specks.map { (x, y) -> Offset(x * size.width, y * size.height) }
                onDrawBehind { drawPoints(specks, androidx.compose.ui.graphics.PointMode.Points, Color.White.copy(alpha = .022f), 2.2f, androidx.compose.ui.graphics.StrokeCap.Round) }
            })
            Canvas(Modifier.fillMaxSize()) {
                // the room warms: an ellipse 60% x 50% of the screen at the lamp's height
                val warm = prog(f, 650f, 1400f, LinearEasing)
                if (warm > 0f) withTransform({ scale(1f, (.5f * h) / (.6f * w), Offset(w / 2, lampY + 24f * k)) }) {
                    drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .20f * warm), Color.Transparent), Offset(w / 2, lampY + 24f * k), .3f * w), .3f * w, Offset(w / 2, lampY + 24f * k))
                }
                val haloT = prog(f, 650f, 1300f, CubicBezierEasing(.1f, .7f, .2f, 1f))
                halo(lampX, lampY, k, haloT, .3f + .7f * haloT)

                // the flash as the flame catches
                val flashT = prog(f, 650f, 700f, CubicBezierEasing(0f, 0f, .58f, 1f))
                if (flashT in 0.0001f..0.9999f) {
                    val a = if (flashT < .25f) flashT / .25f else 1f - (flashT - .25f) / .75f
                    val r = .6f * lampW * (.2f + 2.2f * flashT)
                    val c = Offset(lampX + 27f * k, lampY + 22f * k)
                    drawCircle(Brush.radialGradient(0f to Color(0xFFFFFBEA).copy(alpha = a), .3f to Color(255, 231, 168).copy(alpha = .6f * a), .65f to Color.Transparent, center = c, radius = r), r, c)
                }

                // the lamp: flame from the catch, then the body's outline, its fill, the handle and the wick base
                val flame = keys(prog(f, 650f, 650f, LinearEasing), Catch, 0f to 0f, .55f to 1.18f, .75f to .94f, 1f to 1f)
                val outline = prog(f, 950f, 650f, CubicBezierEasing(.45f, 0f, .2f, 1f))
                val fill = prog(f, 1370f, 450f, LinearEasing)
                val handle = prog(f, 1330f, 380f, CubicBezierEasing(.45f, 0f, .2f, 1f))
                val wick = prog(f, 1100f, 300f, LinearEasing)
                translate(lampX, lampY) { scale(k, k, Offset.Zero) { lamp(flame, outline, fill, handle, wick, f) } }

                spark(f, lampX + 27f * k, lampY + 8f * k, lampW)

                // the bloom the app comes out of
                val bloomT = prog(f, 3200f, 1000f, LinearEasing)
                if (bloomT in 0.0001f..0.9999f) {
                    val a = if (bloomT < .45f) bloomT / .45f else 1f - (bloomT - .45f) / .55f
                    val r = lampW * (.2f + 4.8f * bloomT)
                    val c = Offset(lampX + 27f * k, lampY + 30f * k)
                    drawCircle(Brush.radialGradient(0f to Color(255, 231, 168).copy(alpha = .55f * a), .4f to Gold.copy(alpha = .18f * a), .7f to Color.Transparent, center = c, radius = r), r, c)
                }
            }
            val ease = CubicBezierEasing(.2f, .8f, .2f, 1f)
            Title(
                serif, word, lampY + lampH + .45f * lampW, density.density, 1f,
                letters = { i -> prog(f, 1700f + i * 45f, 700f, ease) },
                spacing = (.25f - .265f * prog(f, 1700f, 1100f, ease)) * word,
                sheen = prog(f, 2350f, 1100f, CubicBezierEasing(.42f, 0f, .58f, 1f)).let { if (f < 2350f) -1f else it },
                rule = prog(f, 2200f, 800f, ease),
            )
        }
    }
}

/** "The Guide" centred at [top]: letters rising out of a blur, the spacing closing up, a sheen across, a rule under it. */
@Composable
private fun Title(serif: FontFamily, textPx: Float, top: Float, density: Float, alpha: Float, letters: (Int) -> Float, spacing: Float, sheen: Float, rule: Float) {
    val style = TextStyle(fontFamily = serif, fontWeight = FontWeight.Light, fontSize = (textPx / density).sp, color = Ink)
    Box(Modifier.fillMaxSize().offset { IntOffset(0, top.roundToInt()) }.graphicsLayer { this.alpha = alpha }, contentAlignment = Alignment.TopCenter) {
        // real spacing (not offsets), so the row's size, and the layer the sheen is drawn in, always holds every letter
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy((spacing / density).dp),
            modifier = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
                drawContent()
                if (sheen in 0f..1f) {
                    // a narrow light band crossing at a slight slant, kept to the letters
                    val x = size.width * (-1.6f + 4.2f * sheen)
                    val band = size.width * .06f
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Transparent, .48f to Color(255, 231, 168).copy(alpha = .9f), .5f to Color.White, .52f to Color(255, 231, 168).copy(alpha = .9f), 1f to Color.Transparent,
                            start = Offset(x - band * 8, -size.height * .2f), end = Offset(x + band * 8, size.height * 1.2f),
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                }
            },
        ) {
            WORD.forEachIndexed { i, ch ->
                val a = letters(i)
                BasicText(
                    ch.toString(), style = style,
                    modifier = Modifier.offset { IntOffset(0, ((1 - a) * textPx * .25f).roundToInt()) }
                        .graphicsLayer { this.alpha = a }
                        .then(if (a < 1f) Modifier.blur((textPx * .18f * (1 - a) / density).dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier),
                )
            }
        }
        if (rule > 0f) Canvas(Modifier.offset { IntOffset(0, (textPx * 1.45f).roundToInt()) }.fillMaxSize()) {
            val half = textPx * 2.6f * rule
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = .7f), Color.Transparent), this.size.width / 2 - half, this.size.width / 2 + half),
                Offset(this.size.width / 2 - half, 0f), Size(half * 2, max(1f, density)),
            )
        }
    }
}

// ---------- drawing ----------

private fun DrawScope.halo(lampX: Float, lampY: Float, k: Float, alpha: Float, grow: Float) {
    if (alpha <= 0f) return
    val r = 54f * k * 2.8f / 2 * grow
    val c = Offset(lampX + 27f * k, lampY + 24f * k)
    drawCircle(Brush.radialGradient(0f to Gold.copy(alpha = .55f * alpha), .45f to Gold.copy(alpha = .12f * alpha), .68f to Color.Transparent, center = c, radius = r), r, c)
}

/** The board's lamp in its 54 x 62 box; each part from 0 (not there) to 1. The flame flickers on [ms]. */
private fun DrawScope.lamp(flame: Float, outline: Float, fill: Float, handle: Float, wick: Float, ms: Float) {
    if (wick > 0f) drawRoundRect(LampBottom.copy(alpha = wick), Offset(22f, 33f), Size(10f, 7f), androidx.compose.ui.geometry.CornerRadius(2f))
    if (fill > 0f) drawPath(BodyPath, Brush.verticalGradient(listOf(LampTop, LampBottom), 37.75f, 55f), alpha = fill)
    if (outline in 0.0001f..0.9999f || (outline >= 1f && fill < 1f)) drawPath(trim(BodyPath, outline), LampTop, style = Stroke(.9f))
    if (handle > 0f) drawPath(trim(HandlePath, handle), LampHandle, style = Stroke(2.5f))
    if (flame > 0f) {
        // the boards' flicker: 1.8 s, scaleY and a little skew about (27, 30)
        val ph = (ms % 1800f) / 1800f
        val sy = keys(ph, Flicker, 0f to 1f, .25f to 1.06f, .5f to .95f, .75f to 1.03f, 1f to 1f)
        val sk = keys(ph, Flicker, 0f to 0f, .25f to -2f, .5f to 1.5f, .75f to -1f, 1f to 0f)
        withTransform({
            scale(flame, flame, Offset(27f, 30f))
            transform(androidx.compose.ui.graphics.Matrix().apply {
                translate(27f, 30f)
                timesAssign(androidx.compose.ui.graphics.Matrix(floatArrayOf(1f, 0f, 0f, 0f, tan(Math.toRadians(sk.toDouble())).toFloat(), sy, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)))
                translate(-27f, -30f)
            })
        }) {
            drawPath(FlameOuter, Brush.verticalGradient(listOf(Flame, Amber), 8f, 32f))
            drawPath(FlameInner, FlameCore)
        }
    }
}

/** The spark, 200-650 ms: appears well above the wick, swells, then falls onto it trailing gold, and goes out. */
private fun DrawScope.spark(f: Float, tipX: Float, tipY: Float, lampW: Float) {
    if (f < 200f || f > 770f) return
    val seg = prog(f, 200f, 450f, LinearEasing)
    val fall = CubicBezierEasing(.55f, 0f, .85f, .45f).transform(((seg - .12f) / .88f).coerceIn(0f, 1f))
    val s = if (seg < .12f) .4f + .6f * (seg / .12f) else 1f - .4f * fall
    // the brief: 150 px above the tip, a 120 px trail and an 8 px spark for a ~72 px lamp
    val y = tipY - 2.08f * lampW * (1f - fall)
    val a = 1f - prog(f, 650f, 120f, LinearEasing)
    val dot = lampW * .11f * s / 2
    // the trail it leaves, growing then fading
    val trail = 1.67f * lampW * fall
    if (trail > 0f) drawRect(Brush.verticalGradient(listOf(Color.Transparent, Gold.copy(alpha = .8f * a * (1 - fall * .6f))), y - trail, y), Offset(tipX - lampW * .02f, y - trail), Size(lampW * .04f, trail))
    val c = Offset(tipX, y)
    drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .5f * a), Color.Transparent), c, dot * 4.2f), dot * 4.2f, c)
    drawCircle(Brush.radialGradient(listOf(Color(255, 231, 168).copy(alpha = .9f * a), Color.Transparent), c, dot * 2f), dot * 2f, c)
    drawCircle(FlameCore.copy(alpha = a), dot, c)
}

/** A faint grain over the dark: fixed scattered specks. */
private val Specks = kotlin.random.Random(7).let { r -> List(1400) { r.nextFloat() to r.nextFloat() } }

private fun trim(path: Path, amount: Float): Path {
    val m = PathMeasure(); m.setPath(path, false)
    return Path().also { m.getSegment(0f, m.length * amount.coerceIn(0f, 1f), it, true) }
}

private fun prog(t: Float, start: Float, dur: Float, easing: Easing) = easing.transform(((t - start) / dur).coerceIn(0f, 1f))

private fun keys(t: Float, easing: Easing, vararg frames: Pair<Float, Float>): Float {
    val i = frames.indexOfLast { it.first <= t }.coerceIn(0, frames.size - 2)
    val (a, va) = frames[i]; val (b, vb) = frames[i + 1]
    return va + (vb - va) * easing.transform(((t - a) / (b - a)).coerceIn(0f, 1f))
}

private fun svg(d: String) = PathParser().parsePathString(d).toPath()

private const val END = 4300L
private const val REDUCED_END = 2000L
private const val WORD = "The Guide"
private val FlameOuter = svg("M27 8c5 7 8 11 8 16a8 8 0 0 1-16 0c0-5 3-9 8-16z")
private val FlameInner = svg("M27 18c2 3 3.5 5 3.5 7a3.5 3.5 0 0 1-7 0c0-2 1.5-4 3.5-7z")
private val BodyPath = svg("M6 40c6-3 36-3 42 0l-4 10c-1 3-4 5-8 5H18c-4 0-7-2-8-5z")
private val HandlePath = svg("M48 41c4 0 6 2 6 4s-2 3-5 3")
private val Catch = CubicBezierEasing(.2f, .9f, .1f, 1.2f)
private val Flicker = CubicBezierEasing(.42f, 0f, .58f, 1f)
private val Night = Color(0xFF070A12)
private val Ink = Color(0xFFFFF8E8)
private val Gold = Color(243, 201, 105)
private val Flame = Color(0xFFFFE7A8)
private val Amber = Color(0xFFE9A44C)
private val FlameCore = Color(0xFFFFF6D8)
private val LampTop = Color(0xFFE8C27A)
private val LampBottom = Color(0xFF8C6A30)
private val LampHandle = Color(0xFFB88A3E)
