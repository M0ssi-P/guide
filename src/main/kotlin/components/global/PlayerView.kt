package components.global

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import ui.theme.Arbutus
import ui.theme.Inter
import ui.theme.LocalTheme
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun PlayerView(
    elapsedSeconds: Float,
    totalDurationSeconds: Float,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DropCap()
            Spacer(Modifier.height(22.dp))
            Text(
                "Genesis 4",
                style = TextStyle(
                    fontFamily = Arbutus,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp,
                    color = theme.colors.primaryText
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "KING JAMES VERSION · READ-ALONG",
                style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 12.5.sp,
                    color = theme.colors.text,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(44.dp))
            Waveform(
                elapsedSeconds = elapsedSeconds,
                totalDurationSeconds = totalDurationSeconds,
                playing = playing
            )
        }
    }
}

@Composable
private fun DropCap() {
    val theme = LocalTheme.current

    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = theme.colors.border,
                radius = size.minDimension / 2 * 0.96f,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color =theme.colors.border,
                radius = size.minDimension / 2 * 0.8f,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Text(
            "A",
            style = TextStyle(
                fontFamily = Arbutus,
                fontWeight = FontWeight.SemiBold,
                fontSize = 76.sp,
                brush = Brush.linearGradient(listOf(theme.colors.primary, theme.colors.blue3)),
            )
        )
    }
}

@Composable
private fun Waveform(
    elapsedSeconds: Float,
    totalDurationSeconds: Float,
    playing: Boolean
) {
    val theme = LocalTheme.current
    val barCount = 46
    var seed by remember { mutableStateOf(0) }
    LaunchedEffect(playing, elapsedSeconds) {
        if (playing) seed++
    }
    val heights = remember(seed) {
        val rnd = Random(seed)
        List(barCount) { i ->
            val base = 8 + (kotlin.math.sin(i * 0.6) * 10 + 14).roundToInt()
            if (playing) (6 + rnd.nextInt(0, 34)) else base
        }
    }

    Row(
        Modifier.height(56.dp).width(280.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { i, h ->
            val cutoff = (i.toFloat() / barCount) * totalDurationSeconds
            val targetColor = when {
                kotlin.math.abs(cutoff - elapsedSeconds) < 3f -> theme.colors.primary
                cutoff < elapsedSeconds - 3f -> theme.colors.primary.copy(alpha = 0.55f)
                else -> theme.colors.border
            }

            val animatedHeight by animateDpAsState(
                targetValue = h.dp,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            val animatedColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 320)
            )

            Box(
                Modifier
                    .weight(1f)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(animatedColor)
            )
        }
    }
}
