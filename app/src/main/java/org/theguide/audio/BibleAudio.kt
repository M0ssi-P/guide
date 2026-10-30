package org.theguide.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import org.theguide.App
import parsers.bible.models.ChapterAud

/** A Bible chapter read aloud, on its own player so a sermon's track and place stay as they were; it lights each verse as it is read. */
object BibleAudio {
    private val player by lazy {
        ExoPlayer.Builder(App.context).build().apply {
            addListener(object : Player.Listener { override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying } })
        }
    }
    var url by mutableStateOf<String?>(null); private set
    var playing by mutableStateOf(false); private set
    var ms by mutableLongStateOf(0L); private set

    fun mp3(a: ChapterAud) = a.playOrDwnLoad.formattedMp3.let { if (it.startsWith("//")) "https:$it" else it }

    fun toggle(a: ChapterAud) {
        val u = mp3(a)
        when {
            u != url -> { url = u; ms = 0; player.setMediaItem(MediaItem.fromUri(u)); player.prepare(); player.play(); Audio.pause() }
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    fun stop() { if (url != null) { player.stop(); url = null; ms = 0 } }

    /** Follows the position while a screen shows the chapter. */
    suspend fun follow() { while (true) { ms = player.currentPosition; delay(100) } }

    /** The verse being read in [a] and how far through it (0..1), while [a] is the one loaded. */
    fun lit(a: ChapterAud?): Pair<Int, Float>? {
        if (a == null || url != mp3(a)) return null
        val t = a.timings.firstOrNull { ms >= it.startMs && ms < it.endMs } ?: return null
        return t.verse to ((ms - t.startMs).toFloat() / (t.endMs - t.startMs).coerceAtLeast(1))
    }
}
