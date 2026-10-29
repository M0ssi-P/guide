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

/** How far the English may run ahead of the translation before it waits (outside Live). */
const val BED_LEAD = 2.0

/**
 * Normal listening. [target] is where the English should be: the translation's time mapped onto the Table timeline,
 * which is the English recording's own time. English plays at 1.0×; the translation usually takes longer, so once the
 * English is more than [BED_LEAD] ahead it waits — only at the END of a stretch ([nextPoint] just ahead), never just
 * after a new one started, so no word is cut. More than 2.5s behind it jumps.
 */
fun decideBed(target: Double, bedTime: Double, holding: Boolean, nextPoint: Double?): BedAction {
    val drift = target - bedTime
    return when {
        drift > 2.5 || drift < -8.0 -> BedAction.Seek(target)
        holding -> if (drift >= 0) BedAction.Play(1.0) else BedAction.Hold
        drift < -BED_LEAD && nextPoint != null && nextPoint - bedTime < 0.25 -> BedAction.Hold
        else -> BedAction.Play(1.0)
    }
}

/** Live: [target] is the host's place on the English timeline. Steady 1.0×; waits while ahead of the host, jumps when 1.5s behind. */
fun decideLiveBed(target: Double, bedTime: Double, holding: Boolean): BedAction {
    val drift = target - bedTime
    return when {
        drift > 1.5 || drift < -8.0 -> BedAction.Seek(target)
        holding -> if (drift >= 0) BedAction.Play(1.0) else BedAction.Hold
        drift < -0.3 -> BedAction.Hold
        else -> BedAction.Play(1.0)
    }
}

/** The least of a voice kept in the other ear, so with one earbud nothing is ever gone completely. */
const val PAN_FLOOR = 0.18

/**
 * How far apart the voices sit for [mix] (English %): 0.2 near the ends ("Mostly …", both close to the centre) up to
 * 1.0 at Even (fully apart). The translation leans right by this much, the English left.
 */
fun panSpread(mix: Int): Double = 0.2 + 0.8 * (1 - abs(mix.coerceIn(MIX_MIN, MIX_MAX) - 50) / 40.0).coerceIn(0.0, 1.0)

/** (left, right) gains for a voice at [position] (−1 left … 1 right): equal power, the far ear never below [PAN_FLOOR]. */
fun panGains(position: Double): Pair<Double, Double> {
    val a = (position.coerceIn(-1.0, 1.0) + 1) * Math.PI / 4
    return maxOf(kotlin.math.cos(a), PAN_FLOOR) to maxOf(kotlin.math.sin(a), PAN_FLOOR)
}

/** mpv audio filter placing a (downmixed) voice at [position]; empty = centred, as without the setting. */
internal fun panFilter(position: Double?): String {
    position ?: return ""
    val (l, r) = panGains(position)
    return "lavfi=[aformat=channel_layouts=mono,pan=stereo|c0=%.2f*c0|c1=%.2f*c0]".format(java.util.Locale.ROOT, l, r)
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

    /**
     * Set by Live while following a host: the host's place on the English timeline (seconds, already moved on by the
     * time the message took) and whether the host is playing. Null outside Live.
     */
    @Volatile var liveHost: (() -> Pair<Double, Boolean>?)? = null

    private var bed: AuraPlayer? = null
    private var forMain: String? = null
    private var holding = false
    private var userVolume: Double? = null
    private var lastMainSet = -1.0
    private var shownMain = -1.0
    private var shownBed = -1.0

    /** "Separate voices left/right": translation right, English left. On by default; remembered. */
    var separate by mutableStateOf(loadData<Boolean>("voice_separate") ?: true)
        private set
    fun updateSeparate(v: Boolean) { separate = v; saveData("voice_separate", v) }

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
        applyPan(main, p)
        // in Live the English follows the host, not this listener's translation (its speed changes and waits)
        val host = liveHost?.invoke()
        val playing = host?.second ?: (main.playerState.value == PlayerState.PLAYING)
        if (!playing) { p.setPause(true); return }
        if (p.duration.value <= 0) return // loading
        val bedTime = p.currentTime.value
        val action = if (host != null) decideLiveBed(host.first, bedTime, holding)
            else decideBed(map.toTimeline(main.currentTime.value), bedTime, holding, map?.nextTimelinePoint(bedTime))
        when (val a = action) {
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

    private var panWanted = "" to ""
    private var panSince = 0L
    private var panApplied = "" to ""

    /**
     * Changing mpv's audio filter restarts it, so the pan is only changed once the slider has rested for 400ms: one
     * smooth switch instead of a click on every step.
     */
    private fun applyPan(main: AuraPlayer, p: AuraPlayer) {
        val s = panSpread(mix)
        val want = if (separate) panFilter(s) to panFilter(-s) else "" to ""
        val now = System.currentTimeMillis()
        if (want != panWanted) { panWanted = want; panSince = now; return }
        if (want == panApplied || now - panSince < 400) return
        setFilter(main, want.first); setFilter(p, want.second)
        panApplied = want
    }

    // shortcut: AuraPlayer keeps setPropertyString private; expose it there (and an af-command for gliding pans) in the next AuraPlayer release
    private fun setFilter(player: AuraPlayer, af: String) = runCatching {
        AuraPlayer::class.java.getDeclaredMethod("setPropertyString", java.lang.Long.TYPE, String::class.java, String::class.java)
            .apply { isAccessible = true }.invoke(player, player.nativeCtx, "af", af)
    }.onFailure { it.printStackTrace() }

    private fun shownFactor() = (shownMain / (userVolume ?: 100.0)).coerceAtLeast(0.1)

    private fun stop(main: AuraPlayer) {
        if (!active) return
        active = false
        bed?.setPause(true)
        userVolume?.let { main.setVolume(it) }
        if (panApplied.first.isNotEmpty()) setFilter(main, "")
        panApplied = "" to ""; panWanted = "" to ""
        userVolume = null
        lastMainSet = -1.0; shownMain = -1.0; shownBed = -1.0
    }

    fun release() { bed?.release(); bed = null }
}
