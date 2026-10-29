package player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mossip.auraplayer.engine.AuraPlayer
import com.mossip.auraplayer.engine.NowPlayingConfig
import com.mossip.auraplayer.engine.PlayerState
import loadData
import parsers.vgr.SermonMedia
import parsers.vgr.TimeMaps
import parsers.vgr.toTimeline
import saveData
import kotlin.math.abs

/** What the English player does next to stay with the translated recording. */
sealed class BedAction {
    data class Play(val rate: Double) : BedAction()
    object Hold : BedAction()
    data class Seek(val to: Double) : BedAction()
}

const val MIX_MIN = 10
const val MIX_MAX = 90

/**
 * [target] is where the English should be: the translation's time mapped onto the Table timeline, which is the English
 * recording's own time. English plays at the main player's speed; when it gets ahead (translations usually take longer)
 * it finishes the stretch it is in and waits at the next anchor [nextPoint], never mid-stretch. More than 1s behind,
 * or far ahead after a seek, it jumps.
 */
fun decideBed(target: Double, bedTime: Double, holding: Boolean, prevPoint: Double?, nextPoint: Double?, mainRate: Double = 1.0): BedAction {
    val drift = target - bedTime
    // at a boundary: just past an anchor point, or about to reach one
    val atBoundary = (prevPoint != null && bedTime - prevPoint < 0.25) || (nextPoint != null && nextPoint - bedTime < 0.05)
    return when {
        drift > 1.0 || drift < -3.0 -> BedAction.Seek(target)
        holding -> if (drift >= 0) BedAction.Play(mainRate) else BedAction.Hold
        drift < -0.05 && atBoundary -> BedAction.Hold
        else -> BedAction.Play(mainRate)
    }
}

/** Loudness of (translation, English) for [mix] = English percent: main (100 − mix)%, English mix%, of the user's volume. */
fun mixFactors(mix: Int): Pair<Double, Double> {
    val m = mix.coerceIn(MIX_MIN, MIX_MAX)
    return (100 - m) / 100.0 to m / 100.0
}

/**
 * The English original playing quietly under a translated recording, so Brother Branham's singing and shouting (silent
 * in other languages) are still heard. Only for recordings with VGR's time map; a second, hidden player follows the
 * main one through that map. Never for English recordings.
 */
object EnglishBed {
    /** English percent, [MIX_MIN]..[MIX_MAX]; remembered. */
    var mix by mutableStateOf((loadData<Int>("voice_mix") ?: 15).coerceIn(MIX_MIN, MIX_MAX))
        private set
    /** True while an English recording is following the one playing; the voices chip shows then. */
    var active by mutableStateOf(false)
        private set
    /** The translation's language name for the labels, e.g. "Kiswahili". */
    var languageName by mutableStateOf("Translation")
        private set

    /** Download English with other languages: null = ask the first time. */
    var alwaysDownload by mutableStateOf(loadData<Boolean>("english_companion"))
        private set
    /** A download that is waiting for the first-time question. */
    var asking by mutableStateOf<parsers.vgr.models.Sermon?>(null)

    private var bed: AuraPlayer? = null
    private var forMain: String? = null
    private var holding = false
    private var userVolume: Double? = null
    private var lastMainSet = -1.0
    private var shownMain = -1.0
    private var shownBed = -1.0

    fun updateMix(v: Int) { mix = (Math.round(v / 5f) * 5).coerceIn(MIX_MIN, MIX_MAX); saveData("voice_mix", mix) }
    fun setAlways(v: Boolean?) { alwaysDownload = v; saveData("english_companion", v) }

    /** After a translated sermon is downloaded: get the English too, or ask the first time. */
    fun afterDownload(sermon: parsers.vgr.models.Sermon) {
        when (alwaysDownload) {
            true -> SermonMedia.downloadEnglishCompanionLater(sermon)
            false -> {}
            null -> asking = sermon
        }
    }

    /** One step, five times a second, from the player provider. */
    suspend fun tick(main: AuraPlayer, mainUrl: String?) {
        val header = TimeMaps.header(mainUrl)
        val map = header?.map
        val sermonId = header?.sermonId
        if (mainUrl == null || (header != null && (map == null || sermonId == null))) { stop(main); forMain = mainUrl; return }
        if (header == null) return // still reading the file header
        if (mainUrl != forMain) {
            forMain = mainUrl
            val english = SermonMedia.englishCompanion(sermonId!!) ?: run { stop(main); return }
            val p = bed ?: AuraPlayer().apply {
                nowPlayingConfig = NowPlayingConfig(nowPlayingEnabled = false, showNext = false, showPrevious = false)
                initializeRenderApi(audioOnly = true)
            }.also { bed = it }
            p.loadWhenReady(english)
            holding = false
            languageName = header.languageId?.let { SermonMedia.languageName(it) } ?: "Translation"
            active = true
        }
        val p = bed ?: return
        if (!active) return
        applyVolumes(main, p)
        if (main.playerState.value != PlayerState.PLAYING) { p.setPause(true); return }
        if (p.duration.value <= 0) return // loading
        val target = map.toTimeline(main.currentTime.value)
        when (val a = decideBed(target, p.currentTime.value, holding, map?.previousTimelinePoint(p.currentTime.value), map?.nextTimelinePoint(p.currentTime.value), main.speed.value)) {
            is BedAction.Seek -> { p.seek(a.to.coerceAtLeast(0.0)); holding = false; p.setPause(false) }
            BedAction.Hold -> if (!holding) { holding = true; p.setPause(true) }
            is BedAction.Play -> {
                if (holding || p.playerState.value != PlayerState.PLAYING) { holding = false; p.setPause(false) }
                if (abs(p.speed.value - a.rate) > 0.01) p.setSpeed(a.rate)
            }
        }
    }

    private fun applyVolumes(main: AuraPlayer, p: AuraPlayer) {
        val (mf, bf) = mixFactors(mix)
        val now = main.volume.value
        // the user's own volume: what the main player had before we turned it down, or what they set since
        if (userVolume == null || (lastMainSet >= 0 && abs(now - lastMainSet) > 0.5)) userVolume = if (lastMainSet >= 0) now / shownFactor() else now
        val v = userVolume ?: now
        // ease towards the new levels instead of jumping
        shownMain = if (shownMain < 0) v * mf else shownMain + (v * mf - shownMain) * 0.4
        shownBed = if (shownBed < 0) v * bf else shownBed + (v * bf - shownBed) * 0.4
        if (abs(now - shownMain) > 0.3) main.setVolume(shownMain)
        lastMainSet = shownMain
        p.setVolume(shownBed)
    }

    private fun shownFactor() = (shownMain / (userVolume ?: 100.0)).coerceAtLeast(0.1)

    private fun stop(main: AuraPlayer) {
        if (!active) return
        active = false
        bed?.setPause(true)
        userVolume?.let { main.setVolume(it) }
        userVolume = null
        lastMainSet = -1.0; shownMain = -1.0; shownBed = -1.0
    }

    fun release() { bed?.release(); bed = null }
}
