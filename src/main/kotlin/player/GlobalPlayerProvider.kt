package player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mossip.auraplayer.engine.AuraPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mvvm.ViewModel

enum class GlobalPlayerType {
    BIBLE,
    SERMON,
    PODCAST,
    LIVE
}

class GlobalPlayerViewModel: ViewModel() {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    private fun load(url: String) {
        _url.value = url
    }

    fun loadNowPlaying(nowPlaying: NowPlaying) {
        _nowPlaying.value = nowPlaying
        load(nowPlaying.url)
    }

    fun play() {
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    data class NowPlaying(
        val id: String,
        val title: String,
        val subtitle: String,
        val type: GlobalPlayerType,
        val isPlaying: Boolean,
        val currentPosition: Long,
        val url: String,
    )
}

val LocalPlayer = staticCompositionLocalOf<GlobalPlayerViewModel> {
    error("Player not found")
}

val LocalPlayerState = staticCompositionLocalOf<AuraPlayer> {
    error("Player not found")
}

@Composable
fun GlobalPlayerProvider(player: AuraPlayer, content: @Composable () -> Unit) {
    val localPlayer = remember { GlobalPlayerViewModel() }
    val mediaUrl by localPlayer.url.collectAsState()

    LaunchedEffect(mediaUrl) {
        mediaUrl?.let {
            player.loadFile(it)
            player.setPause(true)
        }
    }

    CompositionLocalProvider(
        LocalPlayer provides localPlayer,
        LocalPlayerState provides player,
    ) {
        content()
    }
}