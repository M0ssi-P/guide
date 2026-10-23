package ui.screens.table

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.composables.arrow2left
import com.composables.arrow2right
import com.composables.pauseIcon
import com.composables.play2Icon
import com.composables.settingsIcon
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.painterResource
import ui.theme.LocalTheme
import kotlin.math.roundToInt

/**
 * The floating bottom transport bar (`.player-capsule` in the CSS): language
 * pill, rewind/play/forward/repeat, and a click-and-drag scrub bar. Shared
 * by both the Listen and Read modes, exactly like the HTML mockup.
 */
@Composable
fun PlayerCapsule(
    languageCode: String,
    playing: Boolean,
    repeatOn: Boolean,
    progress: Float,
    elapsedLabel: String,
    remainingLabel: String,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {

    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }

    val width by animateDpAsState(if (expanded) 900.dp else 560.dp, tween(420))
    val height by animateDpAsState(if (expanded) 236.dp else 64.dp, tween(420))
    val radius by animateDpAsState(if (expanded) 20.dp else 32.dp, tween(420))
    val compactAlpha by animateFloatAsState(if (expanded) 0f else 1f, tween(200))
    val expandedAlpha by animateFloatAsState(if (expanded) 1f else 0f, tween(260, delayMillis = 60))

    Column(
        modifier = modifier
            .widthIn(max = 900.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TableColors.BgElev.copy(alpha = 0.9f))
            .border(1.dp, TableColors.Divider, RoundedCornerShape(22.dp))
            .padding(horizontal = 26.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(60.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(TableColors.BgElev)
                    .border(1.dp, TableColors.Divider, RoundedCornerShape(9.dp))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(languageCode, style = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TableColors.InkMid))
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportIcon(::arrow2left, "Back 10s", onClick = onRewind)
                Spacer(Modifier.width(24.dp))
                PlayButton(playing = playing, onClick = onPlayPause)
                Spacer(Modifier.width(24.dp))
                TransportIcon(::arrow2right, "Forward 10s", onClick = onForward)
            }

            TransportIcon(
                ::settingsIcon,
                "Repeat",
                onClick = onToggleRepeat,
                active = repeatOn
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(elapsedLabel, style = TextStyle(fontFamily = InterFamily, fontSize = 10.5.sp, color = TableColors.InkMid), modifier = Modifier.width(56.dp))
            ScrubTrack(progress = progress, onSeek = onSeek, modifier = Modifier.weight(1f))
            Text(remainingLabel, style = TextStyle(fontFamily = InterFamily, fontSize = 10.5.sp, color = TableColors.InkMid), modifier = Modifier.width(56.dp))
        }
    }
}

@Composable
private fun TransportIcon(
    icon: (c: Color) -> ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) TableColors.Gold.copy(alpha = 0.13f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon(if (active) TableColors.GoldBright else TableColors.InkMid),
            contentDescription = label,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(TableColors.Gold)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (playing) pauseIcon(TableColors.PlayInk) else play2Icon(TableColors.PlayInk),
            contentDescription = if (playing) "Pause" else "Play",
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Click-and-drag progress bar (`.scrub-track`/`.scrub-fill`/`.scrub-thumb`),
 *  same technique as the existing Wick `PlayerBar`'s `ScrubTrack`. */
@Composable
private fun ScrubTrack(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var widthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier
            .height(14.dp)
            .onGloballyPositioned { widthPx = it.size.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (widthPx > 0f) onSeek((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    if (widthPx > 0f) onSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            }
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().height(3.dp)
                .clip(RoundedCornerShape(3.dp)).background(TableColors.Divider)
        )
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp)
                .clip(RoundedCornerShape(3.dp)).background(TableColors.Gold)
        )
        val thumbOffsetPx = (widthPx * progress) - with(density) { 5.dp.toPx() }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                .size(10.dp)
                .clip(CircleShape)
                .background(TableColors.GoldBright)
        )
    }
}
