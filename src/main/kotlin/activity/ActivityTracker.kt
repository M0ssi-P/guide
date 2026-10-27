package activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mossip.auraplayer.engine.PlayerState
import kotlinx.coroutines.delay
import player.LocalPlayer
import player.LocalPlayerState

/** Adds a second of listening to the activity log for every second the global player is playing. */
@Composable
fun ActivityTracker() {
    val engine = LocalPlayerState.current
    val nowPlaying by LocalPlayer.current.nowPlaying.collectAsState()
    val state by engine.playerState.collectAsState()
    LaunchedEffect(nowPlaying?.id, state) {
        val np = nowPlaying ?: return@LaunchedEffect
        if (state != PlayerState.PLAYING) return@LaunchedEffect
        while (true) {
            delay(1000)
            ActivityLog.addListening(1, "listen:${np.id}", np.title, np.subtitle)
        }
    }
}
