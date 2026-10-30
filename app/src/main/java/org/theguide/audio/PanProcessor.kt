package org.theguide.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Places a voice between left and right: the input is downmixed to mono and sent to each ear with its own gain
 * ([player.panGains]). With [placed] off, sound passes through as recorded. Gains glide over each buffer, so moving the
 * balance never clicks (the desktop has to wait for the slider to rest instead).
 */
@UnstableApi
class PanProcessor : BaseAudioProcessor() {
    @Volatile var placed = false
    @Volatile var left = 1f
    @Volatile var right = 1f
    private var curL = 1f
    private var curR = 1f
    private var channels = 2
    private var wasPlaced = false

    override fun onConfigure(input: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (input.encoding != C.ENCODING_PCM_16BIT || input.channelCount !in 1..2) throw AudioProcessor.UnhandledAudioFormatException(input)
        channels = input.channelCount
        return AudioProcessor.AudioFormat(input.sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(input: ByteBuffer) {
        val frames = input.remaining() / (2 * channels)
        if (frames == 0) return
        val out = replaceOutputBuffer(frames * 4).order(ByteOrder.nativeOrder())
        val src = input.order(ByteOrder.nativeOrder())
        val on = placed
        val tl = left; val tr = right
        if (!wasPlaced && on) { curL = tl; curR = tr }
        wasPlaced = on
        for (i in 0 until frames) {
            val a = src.short.toInt()
            val b = if (channels == 2) src.short.toInt() else a
            if (on) {
                val k = i.toFloat() / frames
                val m = (a + b) * .5f
                out.putShort((m * (curL + (tl - curL) * k)).toInt().coerceIn(-32768, 32767).toShort())
                out.putShort((m * (curR + (tr - curR) * k)).toInt().coerceIn(-32768, 32767).toShort())
            } else {
                out.putShort(a.toShort()); out.putShort(b.toShort())
            }
        }
        curL = tl; curR = tr
        out.flip()
    }
}
