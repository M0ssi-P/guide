package player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.arrow2left
import com.composables.arrow2right
import com.composables.nextIcon
import com.composables.pauseIcon
import com.composables.play2Icon
import com.composables.prevIcon
import com.composables.settingsIcon
import com.mossi.auraplayer.ui.AuraAudioSurface
import com.mossip.auraplayer.engine.PlayerState
import components.global.GhostButton
import components.global.PopoverAnchored
import components.global.TooltipIconButton
import formatTime
import icons.VolumeFull
import icons.VolumeMuted
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import ui.theme.Arbutus
import ui.theme.Inter
import ui.theme.LocalTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PlayerBar(
    playing: Boolean,
    onPlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    elapsedSeconds: Float,
    totalDurationSeconds: Float,
    onSeekFraction: (Float) -> Unit,
    speed: Float,
    onCycleSpeed: () -> Unit,
    muted: Boolean,
    onToggleMuted: () -> Unit,
    autoAdvance: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
    voice: String,
    onVoiceChange: (String) -> Unit,
    translation: String,
    onTranslationChange: (String) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current
    val theme = LocalTheme.current
    val chromeAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(500))

    Row(
        modifier
            .fillMaxWidth()
            .height(84.dp)
            .graphicsLayer(alpha = chromeAlpha)
            .background(theme.colors.popup.copy(alpha = 0.86f))
            .drawTopDivider(theme.colors.border)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(
            Modifier.width(220.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .drawWithCache {
                        val angleInRadians = Math.toRadians((160 - 90).toDouble())
                        val dx = cos(angleInRadians).toFloat()
                        val dy = sin(angleInRadians).toFloat()

                        val center = Offset(size.width / 2f, size.height / 2f)
                        val start = Offset(center.x - dx * size.width / 2f, center.y - dy * size.height / 2f)
                        val end = Offset(center.x + dx * size.width / 2f, center.y + dy * size.height / 2f)

                        val brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF23252A),
                                Color(0xFF16181C)),
                            start = start,
                            end = end
                        )

                        onDrawBehind {
                            drawRect(brush)
                        }
                    }
                    .border(1.dp, theme.colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("G", fontFamily = Arbutus, fontSize = 19.sp, color = theme.colors.primary)
            }
            Column {
                AuraAudioSurface(player, content = {})
                Text("Genesis 4", fontFamily = Inter, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = theme.colors.primaryText)
                Text("King James Version", style = theme.typography.tab, color = theme.colors.text)
            }
        }

        // -- center: transport controls + scrub --
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                TooltipIconButton(
                    icon = ::prevIcon,
                    tooltip = "Previous chapter",
                    onClick = onPrevChapter
                )
                TooltipIconButton(
                    icon = ::arrow2left,
                    tooltip = "Previous verse",
                    onClick = onPrevChapter
                )
                TooltipIconButton(
                    icon = when(playing) {
                        true -> ::pauseIcon
                        false -> ::play2Icon
                    },
                    tooltip = "Play",
                    onClick = onPlayPause
                )
                TooltipIconButton(
                    icon = ::arrow2right,
                    tooltip = "Next verse",
                    onClick = onForward10
                )
                TooltipIconButton(
                    icon = ::nextIcon,
                    tooltip = "Next chapter",
                    onClick = onNextChapter
                )
            }
            Row(
                Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    formatTime(elapsedSeconds.toDouble()),
                    style = theme.typography.tab, color = theme.colors.text,
                    modifier = Modifier.width(34.dp)
                )
                ScrubTrack(
                    progress = (elapsedSeconds / totalDurationSeconds).coerceIn(0f, 1f),
                    onSeek = onSeekFraction,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatTime(totalDurationSeconds.toDouble()),
                    style = theme.typography.tab, color = theme.colors.text,
                    modifier = Modifier.width(34.dp)
                )
            }
        }

        // -- right: speed / volume / settings --
        Box(Modifier.width(220.dp), contentAlignment = Alignment.CenterEnd) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(text = formatSpeed(speed), active = speed != 1f, onClick = onCycleSpeed)
                TooltipIconButton(
                    icon = if(muted) ::VolumeMuted else ::VolumeFull,
                    tooltip = if(muted) "Unmute" else "Mute",
                    onClick = onNextChapter
                )
                PopoverAnchored(
                    modifier = Modifier
                        .dropShadow(
                            shape = RoundedCornerShape(6.dp),
                            block = {
                                color = theme.colors.border
                                spread = 1f
                                offset = Offset(0f, 0f)
                            }
                        )
                        .dropShadow(
                            shape = RoundedCornerShape(6.dp),
                            block = {
                                color = Color.Black.copy(alpha = 0.12f)
                                alpha = 1f
                                spread = -6f
                                radius = 28f
                                offset = Offset(0f, 14f)
                            }
                        )
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            theme.colors.popup
                        ),
                    animate = false,
                    popup = {
                        SettingsPopover(
                            autoAdvance = autoAdvance,
                            onAutoAdvanceChange = onAutoAdvanceChange,
                            voice = voice,
                            onVoiceChange = onVoiceChange,
                            translation = translation,
                            onTranslationChange = onTranslationChange
                        )
                    }
                ) {
                    TooltipIconButton(
                        icon = ::settingsIcon,
                        tooltip = "Settings",
                        onClick = onNextChapter
                    )
                }
            }
        }
    }
}

private fun Modifier.drawTopDivider(color: Color): Modifier = this.drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 1.dp.toPx()
    )
}

private fun formatSpeed(speed: Float): String {
    val trimmed = if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()
    return "${trimmed}×"
}

@Composable
private fun ScrubTrack(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    var widthPx by remember { mutableStateOf(0f) }

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
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(theme.colors.border)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(theme.colors.primary)
        )
        val thumbOffsetPx = (widthPx * progress) - with(androidx.compose.ui.platform.LocalDensity.current) { 5.dp.toPx() }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { androidx.compose.ui.unit.IntOffset(thumbOffsetPx.toInt(), 0) }
                .size(10.dp)
                .clip(CircleShape)
                .background(theme.colors.primary)
        )
    }
}

@Composable
private fun SettingsPopover(
    autoAdvance: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
    voice: String,
    onVoiceChange: (String) -> Unit,
    translation: String,
    onTranslationChange: (String) -> Unit
) {
    val theme = LocalTheme.current
    val voices = listOf("Narrator — David", "Narrator — Ruth")
    val translations = listOf("KJV", "ESV", "NIV")

    Column(
        Modifier
            .width(200.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-advance", style = theme.typography.tab, color = theme.colors.text)
            Switch(
                checked = autoAdvance,
                onCheckedChange = onAutoAdvanceChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.colors.primary,
                    checkedTrackColor = theme.colors.primary.copy(alpha = 0.35f),
                    uncheckedThumbColor = theme.colors.gray2nd,
                    uncheckedTrackColor = theme.colors.border
                )
            )
        }
        PickerRow("Voice", voice, voices, onVoiceChange)
        PickerRow("Translation", translation, translations, onTranslationChange)
    }
}

@Composable
private fun PickerRow(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    val theme = LocalTheme.current

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = theme.typography.body, color = theme.colors.text)
        Box(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(theme.colors.surface)
                .border(1.dp, theme.colors.border, RoundedCornerShape(6.dp))
                .clickable {
                    val next = options[(options.indexOf(value) + 1) % options.size]
                    onChange(next)
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(value, style = theme.typography.tab, color = theme.colors.primaryText)
        }
    }
}
