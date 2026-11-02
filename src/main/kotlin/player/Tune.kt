package player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URL
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import kotlin.concurrent.thread

/** A song's MIDI tune (the song store's .mid files), on the JVM's own synthesizer: the main player has no MIDI. */
object Tune {
    var url by mutableStateOf<String?>(null); private set
    var playing by mutableStateOf(false); private set
    private var seq: Sequencer? = null

    fun toggle(u: String) {
        val s = seq
        when {
            u != url || s == null -> {
                stop(); url = u
                thread(isDaemon = true) {
                    runCatching {
                        val q = MidiSystem.getSequencer().apply {
                            open()
                            sequence = MidiSystem.getSequence(URL(u))
                            addMetaEventListener { if (it.type == 47) playing = false } // end of track
                        }
                        if (url == u) { seq = q; q.start(); playing = true } else q.close()
                    }.onFailure { if (url == u) url = null }
                }
            }
            s.isRunning -> { s.stop(); playing = false }
            else -> { if (s.tickPosition >= s.tickLength) s.tickPosition = 0; s.start(); playing = true }
        }
    }

    fun stop() { seq?.close(); seq = null; url = null; playing = false }
}
