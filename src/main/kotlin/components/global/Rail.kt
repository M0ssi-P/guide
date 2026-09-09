package components.global

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import icons.FilledFireIcon
import ui.theme.LocalTheme

/**
 * Left-hand "illuminated rail" — the thin vertical strip with one dot per
 * verse, a gold line running through them, and a small flame glyph that
 * glides to the active verse (`.rail`, `.rail-line`, `.tick`, `.flame` in the
 * original CSS/JS).
 */
@Composable
fun RailView(
    verseCount: Int,
    activeVerseIndex: Int,
    onSeekVerse: (Int) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current
    val chromeAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(500))

    Box(
        modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(theme.colors.menu)
            .graphicsLayer(alpha = chromeAlpha)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(vertical = 28.dp)
        ) {
            val trackHeight = maxHeight

            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(theme.colors.border)
            )

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(verseCount) { vi ->
                    val isActive = vi == activeVerseIndex
                    val isRead = vi < activeVerseIndex
                    RailTick(
                        isActive = isActive,
                        isRead = isRead,
                        onClick = { onSeekVerse(vi) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }


            val fraction = if (verseCount > 0) (activeVerseIndex + 0.5f) / verseCount else 0f
            val flameOffset by animateDpAsState(trackHeight * fraction - 10.dp, tween(450))

            // Note: the CSS gives the flame a soft drop-shadow glow
            // (`filter: drop-shadow(...)`); Compose has no cheap equivalent for
            // an icon glyph, so the glow is approximated by the bright tint alone.
            Icon(
                FilledFireIcon(theme.colors.primary),
                contentDescription = null,
                tint = theme.colors.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = flameOffset)
                    .shadow(
                        elevation = 6.dp,
                        spotColor = theme.colors.blue3,
                        ambientColor = theme.colors.blue3
                    )
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun RailTick(
    isActive: Boolean,
    isRead: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current

    val dotColor = when {
        isActive -> theme.colors.primary
        isRead -> theme.colors.blue3
        else -> theme.colors.gray2nd
    }
    val dotAlpha = if (isRead && !isActive) 0.55f else 1f
    val dotSize by animateDpAsState(if (isActive) 9.dp else 6.dp, tween(350))

    Box(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .graphicsLayer(alpha = dotAlpha)
                .background(dotColor)
                .clickable(onClick = onClick)
        )
    }
}