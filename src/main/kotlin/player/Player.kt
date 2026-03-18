package player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.arrow2left
import com.composables.arrow2right
import com.composables.closeIcon
import com.composables.maximize
import com.composables.nextIcon
import com.composables.pauseIcon
import com.composables.play2Icon
import com.composables.prevIcon
import com.composables.soundIcon
import com.mossi.auraplayer.ui.AuraPlayerSurface
import com.mossip.auraplayer.engine.AuraPlayer
import com.mossip.auraplayer.engine.PlayerState
import components.global.AsyncImageFromFile
import components.global.Button
import components.global.TooltipIconButton
import formatTime
import org.jetbrains.jewel.ui.component.Slider
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme
import java.io.File

@Composable
fun PlayerUI(player: AuraPlayer) {
    val theme = LocalTheme.current
    val localPlayer = LocalPlayer.current
    val nowPlaying by localPlayer.nowPlaying.collectAsState()
    val playerState by player.playerState.collectAsState()
    val duration by player.duration.collectAsState()
    val currentTime by player.currentTime.collectAsState()

    val isDragging = remember { mutableStateOf(false) }
    val sliderPosition = remember { mutableStateOf(0f) }

    val progress = if (duration > 0L)
        (currentTime/ duration).toFloat().coerceIn(0f, 1f)
    else 0f

    BoxWithConstraints(
        modifier = Modifier
            .dropShadow(
                shape = RoundedCornerShape(0.dp),
                block = {
                    color = theme.colors.border
                    spread = 1f
                    offset = Offset(0f, 0f)
                }
            )
            .background(theme.colors.surface)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 1920.dp)
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 16.dp),

            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.width(300.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    Modifier.size(64.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.colors.popup)
                ) {
                    AsyncImageFromFile("src/bible.jpg")
                }
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = nowPlaying?.title ?: "Untitled",
                        style = theme.typography.h4,
                        color = theme.colors.night,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = nowPlaying?.subtitle ?: "The Listener's Bible",
                        style = theme.typography.h4,
                        color = theme.colors.text,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TooltipIconButton(
                        icon = ::prevIcon,
                        tooltip = "Previous chapter",
                    )
                    TooltipIconButton(
                        icon = ::arrow2left,
                        tooltip = "Previous verse",
                    )
                    TooltipIconButton(
                        icon = when(playerState) {
                            PlayerState.PLAYING -> ::pauseIcon
                            PlayerState.PAUSED -> ::play2Icon
                            else -> ::play2Icon
                        },
                        tooltip = "Play",
                        onClick = {
                            if(playerState == PlayerState.PLAYING) {
                                player.setPause(true)
                            } else {
                                player.setPause(false)
                            }
                        }
                    )
                    TooltipIconButton(
                        icon = ::arrow2right,
                        tooltip = "Next verse",
                    )
                    TooltipIconButton(
                        icon = ::nextIcon,
                        tooltip = "Next chapter",
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {

                    Text(formatTime(currentTime), color = Color.Gray, fontSize = 12.sp)

                    Spacer(Modifier.width(8.dp))

                    Slider(
                        value = if (isDragging.value) sliderPosition.value else progress,
                        onValueChange = {
                            isDragging.value = true
                            sliderPosition.value = it
                        },
                        onValueChangeFinished = {
                            val seekPosition = sliderPosition.value * duration
                            player.seek(seekPosition)

                            isDragging.value = false
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.End),
            ) {
                Button(modifier = Modifier.size(25.dp)){
                    Text("1X")
                }
                TooltipIconButton(
                    icon = ::soundIcon,
                    tooltip = "Volume",
                )
                TooltipIconButton(
                    icon = ::maximize,
                    tooltip = "Fullscreen",
                )
                TooltipIconButton(
                    icon = ::closeIcon,
                    tooltip = "Close",
                )
            }
        }
        AuraPlayerSurface(player, false, modifier = Modifier.size(0.dp))
    }
}