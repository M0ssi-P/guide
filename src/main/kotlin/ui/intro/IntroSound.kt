package ui.intro

import loadData
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import kotlin.math.log10

/** The opening sting's sound at about 60% volume (it is often open in church), unless turned off in Settings. */
object IntroSound {
    fun play() {
        if (loadData<Boolean>("opening_sound") == false) return
        Thread {
            runCatching {
                val src = AudioSystem.getAudioInputStream(IntroSound::class.java.getResourceAsStream("/sounds/intro.wav")!!.buffered())
                // 16-bit for any sound card (the file is 24-bit)
                val f = src.format
                val pcm = AudioSystem.getAudioInputStream(AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.sampleRate, 16, f.channels, f.channels * 2, f.sampleRate, false), src)
                val clip = AudioSystem.getClip()
                clip.open(pcm)
                (clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl)?.let { it.value = (20 * log10(0.6)).toFloat().coerceIn(it.minimum, it.maximum) }
                clip.addLineListener { e -> if (e.type == javax.sound.sampled.LineEvent.Type.STOP) clip.close() }
                clip.start()
            }
        }.apply { isDaemon = true }.start()
    }
}
