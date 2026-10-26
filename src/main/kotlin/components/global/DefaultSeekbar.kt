package components.global

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mossi.auraplayer.ui.AuraSeekBar
import com.mossip.auraplayer.engine.AuraPlayer
import com.mossip.auraplayer.engine.Segment

@Composable
fun DefaultSeekBar(
    player: AuraPlayer,
    modifier: Modifier = Modifier,
    segments: List<Segment> = emptyList(),
    accent: Color = Color(0xFFE53935),
) {
    val position by player.currentTime.collectAsState()
    val duration by player.duration.collectAsState()
    val buffer by player.bufferDuration.collectAsState()

    AuraSeekBar(
        position = position,
        duration = duration,
        buffered = position + buffer,   // mpv reports cache *ahead* of position
        segments = segments,
        onSeek = player::seek,
        modifier = modifier,
        accent = accent,
    )
}