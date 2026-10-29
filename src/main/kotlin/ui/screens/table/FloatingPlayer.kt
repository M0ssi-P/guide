package ui.screens.table

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.arrow2left
import com.composables.arrow2right
import com.composables.hamburger
import com.composables.pauseIcon
import com.composables.play
import com.composables.plusIcon
import com.mossi.auraplayer.ui.AuraAudioSurface
import com.mossi.auraplayer.ui.logo.Fullscreen
import com.mossip.auraplayer.engine.AuraPlayer
import components.global.DefaultSeekBar
import ui.theme.LocalTheme

// TODO move into Theme.colors once you add an accent token (kept local so this file drops in standalone)
private val AccentGold = Color(0xFFCF9F52)
private val AccentGoldDeep = Color(0xFF8A6326)

data class PlayerTrack(
    val title: String,
    val breadcrumb: String,
    val languageCode: String,
    val elapsedLabel: String,
    val remainingLabel: String,
    val progressFraction: Float,
    val isPlaying: Boolean,
    val readAlongOn: Boolean
)

@Composable
fun FloatingPlayer(
    player: AuraPlayer,
    track: PlayerTrack,
    modifier: Modifier = Modifier,
    onTogglePlay: () -> Unit = {},
    onSkipBack: () -> Unit = {},
    onSkipForward: () -> Unit = {},
    onToggleReadAlong: () -> Unit = {},
    onLanguageClick: () -> Unit = {}
) {
    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }

    val width by animateDpAsState(if (expanded) 900.dp else 560.dp, tween(420))
    val height by animateDpAsState(if (expanded) 236.dp else 64.dp, tween(420))
    val radiuss by animateDpAsState(if (expanded) 20.dp else 32.dp, tween(420))
    val compactAlpha by animateFloatAsState(if (expanded) 0f else 1f, tween(200))
    val expandedAlpha by animateFloatAsState(if (expanded) 1f else 0f, tween(260, delayMillis = 60))

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .dropShadow(
                shape = RoundedCornerShape(radiuss),
                block = {
                    color = theme.colors.border
                    spread = 1f
                    offset = Offset(0f, 0f)
                }
            )
            .dropShadow(
                shape = RoundedCornerShape(radiuss),
                block = {
                    color = Color.Black.copy(alpha = 0.12f)
                    alpha = 1f
                    spread = -6f
                    radius = 28f
                    offset = Offset(0f, 14f)
                }
            )
            .background(theme.colors.popupLayer, shape = RoundedCornerShape(radiuss))
            .border(1.dp, theme.colors.border, RoundedCornerShape(radiuss))
    ) {
        // compact bar
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(compactAlpha)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AlbumMark(size = 36.dp)

            IconButton14(icon = ::arrow2left, contentDescription = "Back 15 seconds", onClick = onSkipBack)

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(theme.colors.primary)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (track.isPlaying) pauseIcon(theme.colors.primaryText) else play(theme.colors.primaryText),
                    contentDescription = if (track.isPlaying) "Pause" else "Play",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton14(icon = ::arrow2right, contentDescription = "Forward 15 seconds", onClick = onSkipForward, mirrored = true)

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(track.elapsedLabel, color = theme.colors.text.copy(alpha = 0.55f), fontSize = 10.5.sp)
                DefaultSeekBar(player, modifier = Modifier.weight(1f), accent = theme.colors.primary)
                Text(track.remainingLabel, color = theme.colors.text.copy(alpha = 0.55f), fontSize = 10.5.sp)
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(theme.colors.text.copy(alpha = 0.06f))
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Fullscreen(theme.colors.text.copy(alpha = 0.8f)), contentDescription = "Expand player", tint = Color.Unspecified, modifier = Modifier.size(15.dp))
            }
        }

        AuraAudioSurface(player = player, content = {})

        // expanded card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(expandedAlpha)
                .padding(start = 26.dp, end = 26.dp, top = 12.dp, bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { expanded = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.colors.text.copy(alpha = 0.18f))
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AlbumMark(size = 52.dp, iconSize = 20.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, color = theme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(track.breadcrumb, color = theme.colors.text.copy(alpha = 0.55f), fontSize = 12.sp)
                }
                components.global.VoicesChip()
                components.global.LanguageChip(track.languageCode, "Audio and text language: ${track.languageCode}", height = 32.dp, onClick = onLanguageClick)
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(track.elapsedLabel, color = theme.colors.text.copy(alpha = 0.55f), fontSize = 12.sp)
                DefaultSeekBar(player = player, modifier = Modifier.weight(1f), accent = theme.colors.primary)
                Text(track.remainingLabel, color = theme.colors.text.copy(alpha = 0.55f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill(icon = ::play, label = "1×")
                    Pill(icon = ::hamburger, label = "Chapters")
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    SkipButton(icon = ::arrow2left, label = "15", onClick = onSkipBack)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(theme.colors.primary)
                            .clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (track.isPlaying) pauseIcon(theme.colors.night) else play(theme.colors.night),
                            contentDescription = if (track.isPlaying) "Pause" else "Play",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    SkipButton(icon = ::arrow2right, label = "15", onClick = onSkipForward, mirrored = true)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.colors.text.copy(alpha = 0.05f))
                        .clickable(onClick = onToggleReadAlong)
                        .padding(start = 14.dp, top = 7.dp, bottom = 7.dp, end = 8.dp)
                ) {
                    Icon(plusIcon(theme.colors.text.copy(alpha = 0.75f)), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(13.dp))
                    Text("Read-Along", color = theme.colors.text.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(17.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (track.readAlongOn) AccentGold else theme.colors.text.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14161C))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumMark(size: Dp, iconSize: Dp = size * 0.42f) {
    val theme = LocalTheme.current

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.3f).dp))
            .background(Brush.linearGradient(listOf(theme.colors.primary, theme.colors.blue3))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = play(theme.colors.night),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun IconButton14(
    icon: (e: Color) -> ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    mirrored: Boolean = false
) {
    val theme = LocalTheme.current
    Icon(
        imageVector = icon(theme.colors.text.copy(alpha = 0.8f)),
        contentDescription = contentDescription,
        tint = Color.Unspecified,
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer { scaleX = if (mirrored) -1f else 1f }
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SkipButton(
    icon: (e: Color) -> ImageVector,
    label: String,
    onClick: () -> Unit,
    mirrored: Boolean = false
) {
    val theme = LocalTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(
            icon(theme.colors.text.copy(alpha = 0.85f)),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer { scaleX = if (mirrored) -1f else 1f }
        )
        Text(label, color = theme.colors.text.copy(alpha = 0.55f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Pill(icon: (e: Color) -> ImageVector, label: String) {
    val theme = LocalTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(theme.colors.text.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Icon(icon(theme.colors.text.copy(alpha = 0.85f)), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(13.dp))
        Text(label, color = theme.colors.text.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}