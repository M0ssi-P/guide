package update

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import components.global.SoftEase

// Shared look of the update dialog and the updater window (4dp corners everywhere except circles).

internal val R4 = RoundedCornerShape(4.dp)
internal val UInk = Color(0xFF2A1A0E)
internal val UGreen = Color(0xFF7FD9A8)
internal val URed = Color(0xFFE86A5A)
internal val URedSoft = Color(0xFFF2A79C)

/** Progress bar with the moving shimmer from the design. */
@Composable
internal fun ShimmerBar(fraction: Float, track: Color, colors: List<Color>, height: Dp = 8.dp) {
    val w by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600))
    val t = rememberInfiniteTransition()
    val x by t.animateFloat(-200f, 200f, infiniteRepeatable(tween(1200, easing = LinearEasing)))
    Box(Modifier.fillMaxWidth().height(height).clip(R4).background(track)) {
        Box(
            Modifier.fillMaxWidth(w).fillMaxHeight().clip(R4)
                .background(Brush.linearGradient(colors + colors.first(), start = Offset(x, 0f), end = Offset(x + 200f, 0f), tileMode = TileMode.Repeated))
        )
    }
}

/** The soft orange ring that breathes out of the logo and the active step. */
internal fun Modifier.pulse(color: Color, circle: Boolean, periodMs: Int = 2400): Modifier = composed {
    val t = rememberInfiniteTransition()
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart))
    drawBehind {
        // 0 → 8dp spread at half time, fading as it grows, like the CSS box-shadow pulse
        val phase = if (p < 0.5f) p * 2 else (1 - p) * 2
        val spread = 8.dp.toPx() * phase
        val a = 0.45f * (1 - phase)
        if (circle) drawCircle(color.copy(alpha = a), radius = size.minDimension / 2 + spread / 2, style = Stroke(spread))
        else drawRoundRect(
            color.copy(alpha = a), topLeft = Offset(-spread / 2, -spread / 2), size = Size(size.width + spread, size.height + spread),
            cornerRadius = CornerRadius(4.dp.toPx() + spread / 2), style = Stroke(spread)
        )
    }
}

/** Scale 0 → 1.25 → 1 when it first appears. */
internal fun Modifier.popOnce(delayMs: Int = 0): Modifier = composed {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delayMs.toLong()); s.animateTo(1f, keyframes { durationMillis = 400; 1.25f at 280 }) }
    graphicsLayer { scaleX = s.value; scaleY = s.value }
}

/** Fade in from a slight blur and 98.5% scale (the design's swap animation). */
internal fun Modifier.swapIn(key: Any?): Modifier = composed {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { a.animateTo(1f, tween(500, easing = SoftEase)) }
    graphicsLayer { alpha = a.value; val s = 0.985f + 0.015f * a.value; scaleX = s; scaleY = s }.blur((6 * (1 - a.value)).dp)
}
