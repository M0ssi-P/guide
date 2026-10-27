package components.global

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The design's cubic-bezier(.2,.8,.2,1): quick start, long soft landing. */
val SoftEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** Fades and lifts content in once, like the design's `.rise` (pass a stagger delay). */
fun Modifier.rise(delayMs: Int = 0, distance: Dp = 14.dp): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        p.animateTo(1f, tween(600, easing = SoftEase))
    }
    graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * distance.toPx()
    }
}

/** Scales in with a small overshoot (.6 → 1.12 → 1), the design's `pop`. */
fun Modifier.popIn(delayMs: Int = 0): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        p.animateTo(1f, keyframes { durationMillis = 450; 1.12f at 315 using SoftEase })
    }
    graphicsLayer {
        val s = 0.6f + 0.4f * p.value.coerceAtMost(1.12f)
        scaleX = s; scaleY = s
        alpha = p.value.coerceIn(0f, 1f)
    }
}

/** Grows from the bottom edge, for bar charts. */
fun Modifier.growUp(delayMs: Int = 0): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        p.animateTo(1f, tween(700, easing = SoftEase))
    }
    graphicsLayer { scaleY = p.value; transformOrigin = TransformOrigin(0.5f, 1f) }
}

/** Animates 0 → [target] once (progress bars, rings). */
@Composable
fun animatedFill(target: Float, delayMs: Int = 0, durationMs: Int = 1000): Float {
    val p = remember { Animatable(0f) }
    LaunchedEffect(target) {
        delay(delayMs.toLong())
        p.animateTo(target, tween(durationMs, easing = SoftEase))
    }
    return p.value
}

/** Gentle flame wobble around the base. */
fun Modifier.flicker(): Modifier = composed {
    val t = rememberInfiniteTransition()
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)))
    graphicsLayer {
        transformOrigin = TransformOrigin(0.5f, 0.9f)
        val (sx, sy, r) = when {
            v < 0.35f -> Triple(1f + 0.06f * v / 0.35f, 1f - 0.04f * v / 0.35f, -2f + 4f * v / 0.35f)
            v < 0.7f -> { val k = (v - 0.35f) / 0.35f; Triple(1.06f - 0.09f * k, 0.96f + 0.09f * k, 2f - 3f * k) }
            else -> { val k = (v - 0.7f) / 0.3f; Triple(0.97f + 0.03f * k, 1.05f - 0.05f * k, -1f - 1f * k) }
        }
        scaleX = sx; scaleY = sy; rotationZ = r
    }
}

/** Slow up-down float, like the hero date tile. */
fun Modifier.bob(): Modifier = composed {
    val t = rememberInfiniteTransition()
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    graphicsLayer { translationY = -3.dp.toPx() * y }
}

/** A soft radial glow that breathes, placed behind hero cards. */
@Composable
fun GlowBlob(color: Color, size: Dp, modifier: Modifier = Modifier, periodMs: Int = 3200) {
    val t = rememberInfiniteTransition()
    val a by t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(periodMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    Box(modifier.size(size).graphicsLayer { alpha = a }.background(Brush.radialGradient(0f to color, 0.65f to Color.Transparent), CircleShape))
}

/** A live dot with a ring that pings outward. */
@Composable
fun PingDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier, active: Boolean = true) {
    val t = rememberInfiniteTransition()
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)))
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (active) Box(Modifier.size(size).graphicsLayer { scaleX = 1f + 1.6f * p; scaleY = 1f + 1.6f * p; alpha = 0.7f * (1f - p) }.background(color, CircleShape))
        Box(Modifier.size(size).background(color, CircleShape))
    }
}

/** Rings that ripple out from the centre (the "go live" invite). */
@Composable
fun Ripple(color: Color, size: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition()
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)))
    Canvas(modifier.size(size)) {
        listOf(p, (p + 0.5f) % 1f).forEach { k ->
            drawCircle(color.copy(alpha = 0.5f * (1f - k)), radius = this.size.minDimension / 2 * (0.6f + 0.6f * k), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
    }
}

/** Equalizer bars; they freeze (low) when [playing] is false. */
@Composable
fun EqBars(color: Color, modifier: Modifier = Modifier, bars: Int = 4, height: Dp = 20.dp, barWidth: Dp = 3.dp, gap: Dp = 3.dp, playing: Boolean = true) {
    val t = rememberInfiniteTransition()
    val offsets = listOf(0, 200, 400, 100, 300)
    Row(modifier.height(height), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
        repeat(bars) { i ->
            val v by t.animateFloat(
                0.3f, 1f,
                infiniteRepeatable(tween(450, delayMillis = 0, easing = FastOutSlowInEasing), RepeatMode.Reverse, androidx.compose.animation.core.StartOffset(offsets[i % offsets.size]))
            )
            Box(
                Modifier.width(barWidth).height(height)
                    .graphicsLayer { scaleY = if (playing) v else 0.3f; transformOrigin = TransformOrigin(0.5f, 1f) }
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
        }
    }
}

/** A light sweep that passes over the content every few seconds (earned badges). */
fun Modifier.shine(enabled: Boolean, delayMs: Int = 0): Modifier = if (!enabled) this else composed {
    val t = rememberInfiniteTransition()
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), initialStartOffset = androidx.compose.animation.core.StartOffset(delayMs)))
    drawWithContent {
        drawContent()
        // sweeps during the first 60% of the cycle, then rests
        val k = (p / 0.6f).coerceAtMost(1f)
        val w = size.width * 0.4f
        val x = -1.2f * w + k * (size.width + 2.4f * w)
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent), startX = x, endX = x + w), topLeft = Offset.Zero)
    }
}
