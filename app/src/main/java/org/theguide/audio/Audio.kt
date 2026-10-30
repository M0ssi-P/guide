package org.theguide.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.theguide.App
import org.theguide.data.Prefs
import org.theguide.data.Sentence
import org.theguide.data.Tables
import org.theguide.data.isTimed
import org.theguide.data.locateTime
import org.theguide.data.sentences
import parsers.vgr.OmegaCatalog
import parsers.vgr.TimeMaps
import parsers.vgr.VgrMedia
import parsers.vgr.get
import parsers.vgr.models.Sermon
import parsers.vgr.toRecording
import parsers.vgr.toTimeline
import player.BedAction
import player.decideBed
import player.decideLiveBed
import player.mixFactors
import player.panGains
import player.panSpread
import java.io.File
import kotlin.math.abs

/** What is loaded: a sermon in one Table language, its sentences, and where its audio comes from. */
data class Track(val iso3: String, val sermon: Sermon, val items: List<Sentence>, val source: String, val local: Boolean)

/**
 * The sermon player: one ExoPlayer for the recording and, under a translation, a second one with Brother Branham's
 * English following it through VGR's time map (as the desktop's EnglishBed), each placed left/right by a [PanProcessor].
 */
@OptIn(UnstableApi::class)
object Audio {
    private val ctx: Context get() = App.context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainPan = PanProcessor()
    private val bedPan = PanProcessor()

    val main: ExoPlayer by lazy { build(mainPan, focus = true) }
    private val bed: ExoPlayer by lazy { build(bedPan, focus = false) }

    private fun build(pan: PanProcessor, focus: Boolean): ExoPlayer =
        ExoPlayer.Builder(ctx, object : DefaultRenderersFactory(ctx) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(pan)).setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams).build()
        }).setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), focus)
            .setHandleAudioBecomingNoisy(focus).build()

    var track by mutableStateOf<Track?>(null); private set
    /** The English sermon's sentences, for the line under the translation. */
    var english by mutableStateOf<List<Sentence>?>(null); private set
    var playing by mutableStateOf(false); private set
    var position by mutableDoubleStateOf(0.0); private set
    var duration by mutableDoubleStateOf(0.0); private set
    var speed by mutableFloatStateOf(1f); private set
    /** Minutes left on the sleep timer; -1 = at the end of the paragraph; 0 = off. */
    var sleep by mutableIntStateOf(0); private set
    private var sleepAt = 0L
    private var sleepPara = -1
    /** True while the English voice is following (a translation with VGR's time map). */
    var bedActive by mutableStateOf(false); private set
    /** 0..1, the listener's own volume over both voices (Live's slider). */
    var volume by mutableFloatStateOf(1f)
    /** Set by Live while following a host: the host's timeline place and whether it is playing. */
    @Volatile var liveHost: (() -> Pair<Double, Boolean>?)? = null
    /** Set by Live: it moves the main player itself, so the reading logic below stays out of the way. */
    @Volatile var liveDriven = false

    private var bedFor: String? = null
    private var holding = false
    private var lastSaved = 0L
    private var listenedSince = 0L

    init {
        scope.launch { while (true) { tick(); delay(200) } }
    }

    fun audioDir(iso3: String) = File(ctx.filesDir, "audio/${iso3.uppercase()}").apply { mkdirs() }

    /** This sermon's recording on the device, if it is there. */
    suspend fun localFile(iso3: String, sermon: Sermon): File? = withContext(Dispatchers.IO) {
        val sizes = runCatching { VgrMedia.catalogFiles(sermon.i, iso3) }.getOrDefault(emptyList()).map { it.size }.toSet()
        val title = OmegaCatalog.loaded?.sermons?.get(sermon.i)?.second ?: sermon.title
        VgrMedia.findVgrFile(audioDir(iso3), iso3.uppercase(), sermon.dateCode, sizes, title)
    }

    /** Where to play [sermon] from: the file on the device, else its stream link; null when it has no voice. */
    suspend fun source(iso3: String, sermon: Sermon): Pair<String, Boolean>? {
        localFile(iso3, sermon)?.let { return it.absolutePath to true }
        return runCatching { VgrMedia.audioUrl(sermon.i, iso3, sermon.dateCode) }.getOrNull()?.let { it to false }
    }

    /** Loads [sermon] in [iso3] and plays it from [at] seconds; streaming meanwhile keeps a copy for next time. */
    suspend fun play(iso3: String, sermon: Sermon, at: Double = 0.0, autoplay: Boolean = true): Boolean {
        val full = if (sermon.sections == null) Tables.full(iso3, sermon) else sermon
        val (src, local) = source(iso3, full) ?: return false
        val t = Track(iso3, full, full.sentences(), src, local)
        track = t
        english = if (!iso3.equals("eng", true) && Tables.has("eng")) Tables.sermon("eng", full.dateCode)?.let { Tables.full("eng", it).sentences() } else null
        TimeMaps.header(src)
        main.setMediaItem(MediaItem.Builder().setUri(src).setMediaMetadata(
            MediaMetadata.Builder().setTitle(full.title).setArtist("${full.dateCode}, ${Tables.localName(iso3)}").build()
        ).build())
        main.prepare()
        if (at > 0) main.seekTo((at * 1000).toLong())
        main.playWhenReady = autoplay
        bedFor = null
        if (!local) keepCopy(iso3, full, src)
        return true
    }

    private fun keepCopy(iso3: String, sermon: Sermon, url: String) = scope.launch(Dispatchers.IO) {
        val ext = if (url.substringBefore('?').endsWith(".opus", true)) "opus" else "m4a"
        val title = OmegaCatalog.get()?.sermons?.get(sermon.i)?.second ?: sermon.title
        runCatching { VgrMedia.download(url, File(audioDir(iso3), VgrMedia.fileName(iso3, sermon.dateCode, title, ext))) {} }
    }

    fun toggle() { if (main.isPlaying) main.pause() else { if (main.playbackState == Player.STATE_ENDED) main.seekTo(0); main.play() } }
    fun pause() = main.pause()
    fun resume() = main.play()
    fun seek(seconds: Double) { main.seekTo((seconds.coerceAtLeast(0.0) * 1000).toLong()); position = seconds }
    fun setRate(r: Float) { speed = r; main.playbackParameters = PlaybackParameters(r) }
    /** Live's rate changes, which don't touch the speed the listener chose. */
    fun liveRate(r: Double) { main.playbackParameters = PlaybackParameters(r.toFloat()) }
    fun sleepFor(minutes: Int) { sleep = minutes; sleepAt = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else 0; sleepPara = currentSentence()?.number ?: -1 }

    /** The Table timeline time the recording is at (the English time, the same in every language). */
    fun timeline(seconds: Double = position): Double = TimeMaps.get(track?.source).toTimeline(seconds)
    fun recordingTime(timeline: Double): Double = TimeMaps.get(track?.source).toRecording(timeline)

    /** The sentence being spoken now and how far into it. */
    fun currentIndex(): Pair<Int, Float>? {
        val t = track ?: return null
        if (t.items.isEmpty()) return null
        return if (isTimed(t.items)) locateTime(t.items, timeline(), duration)
        else {
            val shares = org.theguide.data.cumulativeShares(t.items)
            val f = if (duration > 0) (position / duration).toFloat() else 0f
            val i = shares.indexOfFirst { it >= f }.coerceAtLeast(0)
            val from = shares.getOrNull(i - 1) ?: 0f
            i to ((f - from) / ((shares[i] - from).coerceAtLeast(1e-6f))).coerceIn(0f, 1f)
        }
    }
    fun currentSentence(): Sentence? = currentIndex()?.let { track?.items?.getOrNull(it.first) }

    /** The English sentence at the same moment, for the line under the translation. */
    fun englishLine(): String? {
        val e = english ?: return null
        if (!isTimed(e)) return null
        return e.getOrNull(locateTime(e, timeline(), Double.MAX_VALUE).first)?.text
    }

    /** Starts [sentence] (seconds on its recording, via the time map). */
    fun playSentence(s: Sentence) {
        val t = track ?: return
        val start = s.start ?: run {
            val i = t.items.indexOf(s)
            val shares = org.theguide.data.cumulativeShares(t.items)
            ((shares.getOrNull(i - 1) ?: 0f) * duration).toDouble().let { return seek(it).also { main.play() } }
        }
        seek(recordingTime(start)); main.play()
    }

    /** First sentence of paragraph [n]. */
    fun playParagraph(n: Int) { track?.items?.firstOrNull { it.number >= n }?.let(::playSentence) }

    fun stepParagraph(delta: Int) {
        val cur = currentSentence()?.number ?: return
        val t = track ?: return
        val numbers = t.items.map { it.number }.distinct()
        val i = numbers.indexOf(cur)
        numbers.getOrNull((i + delta).coerceIn(0, numbers.lastIndex))?.let(::playParagraph)
    }

    private suspend fun tick() {
        playing = main.isPlaying
        position = main.currentPosition / 1000.0
        duration = main.duration.takeIf { it > 0 }?.div(1000.0) ?: duration
        val t = track ?: return
        if (playing) {
            if (listenedSince == 0L) listenedSince = System.currentTimeMillis()
            if (System.currentTimeMillis() - lastSaved > 5000) {
                lastSaved = System.currentTimeMillis()
                Prefs.saveResume(Prefs.Resume(t.iso3, t.sermon.dateCode, t.sermon.title, position, duration, currentSentence()?.number ?: 0))
            }
        } else if (listenedSince > 0) {
            val minutes = ((System.currentTimeMillis() - listenedSince) / 60000).toInt()
            if (minutes >= 1) Prefs.log("Listened to ${t.sermon.title}, paragraph ${currentSentence()?.number ?: 0}", "listen", minutes)
            listenedSince = 0
        }
        // sleep timer
        if (sleep > 0 && sleepAt > 0 && System.currentTimeMillis() >= sleepAt) { main.pause(); sleepFor(0) }
        if (sleep < 0 && playing) {
            val n = currentSentence()?.number
            if (sleepPara < 0) sleepPara = n ?: -1
            else if (n != null && n != sleepPara) { main.pause(); sleepFor(0) }
        }
        bedTick(t)
    }

    /** The English voice underneath: the desktop's EnglishBed, step for step. */
    private suspend fun bedTick(t: Track) {
        val header = TimeMaps.header(t.source)
        val map = header?.map
        val sermonId = header?.sermonId
        val (mf, bf) = mixFactors(Prefs.mix)
        if (t.iso3.equals("eng", true) || (header != null && (map == null || sermonId == null))) { stopBed(); main.volume = volume; return }
        if (header == null) return // still reading the file header
        if (bedFor != t.source) {
            bedFor = t.source
            val english = englishSource(sermonId!!) ?: run { stopBed(); return }
            bed.setMediaItem(MediaItem.fromUri(english))
            bed.prepare()
            holding = false
            bedActive = true
        }
        if (!bedActive) return
        main.volume = (volume * mf).toFloat()
        bed.volume = (volume * bf).toFloat()
        val s = panSpread(Prefs.mix)
        val (ml, mr) = panGains(s); val (bl, br) = panGains(-s)
        mainPan.left = ml.toFloat(); mainPan.right = mr.toFloat(); mainPan.placed = Prefs.split
        bedPan.left = bl.toFloat(); bedPan.right = br.toFloat(); bedPan.placed = Prefs.split
        val host = liveHost?.invoke()
        val on = host?.second ?: main.isPlaying
        if (!on) { bed.pause(); return }
        if (bed.duration <= 0) return // loading
        val bedTime = bed.currentPosition / 1000.0
        val action = if (host != null) decideLiveBed(host.first, bedTime, holding)
            else decideBed(map.toTimeline(position), bedTime, holding, map?.nextTimelinePoint(bedTime))
        when (action) {
            is BedAction.Seek -> { bed.seekTo((action.to.coerceAtLeast(0.0) * 1000).toLong()); holding = false; bed.play() }
            BedAction.Hold -> if (!holding) { holding = true; bed.pause() }
            is BedAction.Play -> {
                if (holding || !bed.isPlaying) { holding = false; bed.play() }
                if (abs(bed.playbackParameters.speed - action.rate) > 0.01) bed.playbackParameters = PlaybackParameters(action.rate.toFloat())
            }
        }
    }

    private suspend fun englishSource(sermonId: Int): String? = withContext(Dispatchers.IO) {
        val cat = OmegaCatalog.get() ?: return@withContext null
        val f = cat.find(sermonId, "eng", OmegaCatalog.OPUS) ?: cat.find(sermonId, "eng", OmegaCatalog.M4A) ?: return@withContext null
        val (code, title) = cat.sermons[f.sermonId] ?: return@withContext null
        VgrMedia.findVgrFile(audioDir("ENG"), "ENG", code, setOf(f.size), title)?.absolutePath
            ?: runCatching { VgrMedia.urlFor(f.fileId, f.mediaTypeId, f.fileTypeId) }.getOrNull()?.also { url ->
                scope.launch(Dispatchers.IO) { runCatching { VgrMedia.download(url, File(audioDir("ENG"), cat.englishName(f.sermonId, "opus") ?: "$code VGR.opus")) {} } }
            }
    }

    private fun stopBed() {
        if (!bedActive) return
        bedActive = false
        bed.pause()
        mainPan.placed = false
    }
}
