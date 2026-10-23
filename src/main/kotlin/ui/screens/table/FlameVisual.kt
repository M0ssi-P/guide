package ui.screens.table

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.composables.play2Icon
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import ui.theme.LocalTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FlameVisual(
    size: Dp,
    playing: Boolean,
    modifier: Modifier = Modifier,
    showRingsAndHalo: Boolean = true,
    haloBarCount: Int = 28,
    haloOuterAnchorFraction: Float = 0.93f
) {
    val coreSize = size * (104f / 280f)

    Box(modifier.size(size).padding(bottom = 26.dp), contentAlignment = Alignment.Center) {
        if (showRingsAndHalo) {
            HaloBurst(
                modifier = Modifier.size(size),
                playing = playing,
                barCount = haloBarCount,
                outerAnchorFraction = haloOuterAnchorFraction
            )
            if (playing) {
                PulsingRing(size = size * (150f / 280f), delayMs = 0)
                PulsingRing(size = size * (150f / 280f), delayMs = 850)
                PulsingRing(size = size * (150f / 280f), delayMs = 1700)
            }
        }
        FlameCore(coreSize, playing = playing)
    }
}

@Composable
private fun FlameCore(sized: Dp, playing: Boolean) {
    val theme = LocalTheme.current
    val infinite = rememberInfiniteTransition(label = "core")
    val breatheDuration = if (playing) 850 else 2250
    val flickerDuration = if (playing) 1200 else 3000

    val breathe by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val flicker by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = flickerDuration
                1.0f at 0
                1.1f at (flickerDuration * 0.45f).toInt()
                0.94f at (flickerDuration * 0.60f).toInt()
                1.0f at flickerDuration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "flicker"
    )

    val shadow1Alpha = if (playing) 0.55f else 0.35f
    val shadow1Blur = if (playing) 60.dp else 40.dp
    val shadow2Alpha = if (playing) 0.25f else 0.15f
    val shadow2Blur = if (playing) 140.dp else 90.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sized)
            .drawWithCache {
                val radius = (size.minDimension / 2f) * breathe
                val centerOffset = Offset(size.width * 0.42f, size.height * 0.38f)
                val canvasCenter = Offset(size.width / 2f, size.height / 2f)

                fun Color.adjustBrightness(factor: Float): Color {
                    return Color(
                        red = (red * factor).coerceIn(0f, 1f),
                        green = (green * factor).coerceIn(0f, 1f),
                        blue = (blue * factor).coerceIn(0f, 1f),
                        alpha = alpha
                    )
                }

                val adjustedGoldBright = theme.colors.primaryBright.adjustBrightness(flicker)
                val adjustedGold = theme.colors.primary.adjustBrightness(flicker)
                val adjustedEmber = theme.colors.primary.adjustBrightness(flicker)

                val radialBrush = Brush.radialGradient(
                    0.0f to adjustedGoldBright,
                    0.55f to adjustedGold,
                    1.0f to adjustedEmber,
                    center = centerOffset,
                    radius = radius
                )

                // Native Skia Paint for blur shadows
                val shadowPaint1 = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = adjustedGold.copy(alpha = shadow1Alpha).toArgb()
                    maskFilter = MaskFilter.makeBlur(
                        FilterBlurMode.NORMAL,
                        shadow1Blur.toPx()
                    )
                }

                val shadowPaint2 = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = adjustedGold.copy(alpha = shadow2Alpha).toArgb()
                    maskFilter = MaskFilter.makeBlur(
                        FilterBlurMode.NORMAL,
                        shadow2Blur.toPx()
                    )
                }

                onDrawBehind {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawCircle(canvasCenter.x, canvasCenter.y, radius, shadowPaint2)
                        canvas.nativeCanvas.drawCircle(canvasCenter.x, canvasCenter.y, radius, shadowPaint1)
                    }

                    drawCircle(
                        brush = radialBrush,
                        radius = radius,
                        center = canvasCenter
                    )
                }
            }
    ) {
        Icon(
            play2Icon(theme.colors.night),
            contentDescription = "Player Icon",
            tint = Color.Unspecified,
        )
    }
}

private val CssEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

@Composable
private fun PulsingRing(size: Dp, delayMs: Int) {
    val theme = LocalTheme.current

    val infinite = rememberInfiniteTransition(label = "ring")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = CssEaseOut),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delayMs)
        ),
        label = "ringT"
    )

    val scale = 0.55f + (1.35f - 0.55f) * t
    val alpha = (0.5f * (1f - t)).coerceIn(0f, 0.5f)
    Canvas(Modifier.size(size).androidx_scale(scale)) {
        drawCircle(
            color = theme.colors.primary.copy(alpha = alpha),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun HaloBurst(
    modifier: Modifier,
    playing: Boolean,
    barCount: Int,
    outerAnchorFraction: Float
) {
    val theme = LocalTheme.current

    val infinite = rememberInfiniteTransition(label = "halo")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "haloPhase"
    )
    Canvas(modifier) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val containerRadius = this.size.minDimension / 2f

        val outerR = containerRadius * outerAnchorFraction


        val restLen = containerRadius * (9f / 140f)
        val minLen = containerRadius * (7f / 140f)
        val maxLen = containerRadius * (26f / 140f)

        val baseAlpha = if (playing) 0.8f else 0.28f
        val perBarPhaseOffset = (45f / 1100f) * 2f * PI.toFloat()

        for (i in 0 until barCount) {
            val angle = (i.toFloat() / barCount) * 2f * PI.toFloat() - (PI / 2f).toFloat()
            val barLen = if (playing) {
                val heightFrac = (sin(phase + i * perBarPhaseOffset) + 1f) / 2f
                minLen + (maxLen - minLen) * heightFrac
            } else {
                restLen
            }
            val cosA = cos(angle)
            val sinA = sin(angle)
            val x0 = cx + cosA * outerR
            val y0 = cy + sinA * outerR
            val x1 = cx + cosA * (outerR - barLen)
            val y1 = cy + sinA * (outerR - barLen)
            drawLine(
                color = theme.colors.primary.copy(alpha = baseAlpha),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun Modifier.androidx_scale(scaleFactor: Float): Modifier = this.scale(scaleFactor)