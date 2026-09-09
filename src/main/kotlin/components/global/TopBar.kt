package components.global

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mossi.auraplayer.ui.logo.Fullscreen
import mvvm.BibleViewModel
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Slider
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

enum class ReaderTab { READING, PLAYER }
/** The book/chapter title, Reading/Player segmented control, font-size
 *  slider, Projection toggle and fullscreen button (`.topbar` in the CSS). */
@Composable
fun TopBar(
    model: BibleViewModel,
    tab: ReaderTab,
    onTabChange: (ReaderTab) -> Unit,
    fontStep: Int,
    onFontStepChange: (Int) -> Unit,
    projectionOn: Boolean,
    onToggleProjection: () -> Unit,
    onToggleFullscreen: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current
    val chromeAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(500))

    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer(alpha = chromeAlpha)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - (strokeWidth / 2)

                drawLine(
                    color = theme.colors.border,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BookChapterSelector(model)
            VersionSelector(model)
        }

        // Reading / Player pill tabs
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(theme.colors.popup)
                .border(1.dp, theme.colors.border, RoundedCornerShape(999.dp))
                .padding(3.dp)
        ) {
            TabPill("Reading", tab == ReaderTab.READING) { onTabChange(ReaderTab.READING) }
            TabPill("Player", tab == ReaderTab.PLAYER) { onTabChange(ReaderTab.PLAYER) }
        }

        // Font size + Projection + Fullscreen
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", style = theme.typography.tab, color = theme.colors.text)
            Spacer(Modifier.width(8.dp))
            Slider(
                value = fontStep.toFloat(),
                onValueChange = { onFontStepChange(it.toInt()) },
                valueRange = 0f..4f,
                steps = 3,
                modifier = Modifier.width(70.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("A", style = theme.typography.h4, color = theme.colors.text)
            Spacer(Modifier.width(14.dp))
            GhostButton(
                text = if (projectionOn) "Exit projection" else "Projection",
                active = projectionOn,
                onClick = onToggleProjection
            )
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onToggleFullscreen) {
                Icon(Fullscreen(theme.colors.text), contentDescription = "Full screen", tint = Color.Unspecified)
            }
        }
    }
}

@Composable
private fun TabPill(label: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current

    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) theme.colors.blue3  else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = theme.typography.semiText,
            color = if (active) theme.colors.primaryText else theme.colors.text,
        )
    }
}

@Composable
fun GhostButton(text: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current

    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) theme.colors.primary.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .border(
                width = 1.dp,
                color = if (active) theme.colors.primary.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            style = theme.typography.semiText,
            color = if (active) theme.colors.primary else theme.colors.text,
        )
    }
}

fun Modifier.layeredRadialBackground() = this.drawWithCache {
    // 1. First Radial Gradient Specs (Top-Left Warm Gold Accent)
    // CSS: ellipse 900px 500px at 18% -10%, rgba(201, 161, 95, .06), transparent 60%
    val radius1 = 900f
    val scaleY1 = 500f / 900f // Scale height relative to width to form ellipse
    val center1 = Offset(size.width * 0.18f, size.height * -0.10f)

    val brush1 = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFC9A15F).copy(alpha = 0.06f),
            0.6f to Color.Transparent
        ),
        center = center1,
        radius = radius1
    )

    val radius2 = 700f
    val scaleY2 = 600f / 700f
    val center2 = Offset(size.width * 1.0f, size.height * 1.0f)

    val brush2 = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFB0623C).copy(alpha = 0.05f),
            0.55f to Color.Transparent
        ),
        center = Offset(center2.x, center2.y / scaleY2), // Adjust origin for scaling
        radius = radius2
    )

    onDrawBehind {
        // Draw first ellipse gradient
        scale(scaleX = 1f, scaleY = scaleY1, pivot = center1) {
            drawRect(brush = brush1)
        }

        // Draw second ellipse gradient
        scale(scaleX = 1f, scaleY = scaleY2, pivot = center2) {
            drawRect(brush = brush2)
        }
    }
}
