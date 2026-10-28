package ui.screens.projection

import activity.ActivityKind
import activity.ActivityLog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mossip.auraplayer.engine.AuraPlayer
import com.mossip.auraplayer.engine.PlayerState
import db.controller.table.Sermons.getFullSermon
import db.controller.table.Sermons.getSermonsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import live.ListenerReport
import live.LiveConnection
import live.LiveHostInfo
import live.LiveState
import live.SyncAction
import live.decideSync
import live.TableLang
import loadData
import parsers.vgr.SermonMedia
import parsers.vgr.models.Sermon
import player.GlobalPlayerType
import player.GlobalPlayerViewModel
import player.LocalPlayer
import player.LocalPlayerState
import presentation.DeckItem
import presentation.locate
import saveData
import ui.config.DB
import ui.screens.reading.ReadingStores
import java.util.UUID
import kotlin.math.abs

/** The sermon in one language: its timed sentences and where its audio is (null = no recording in that language). */
data class Prepared(val sermon: Sermon?, val items: List<DeckItem>, val audio: String?, val local: Boolean)

/** Whether a language can follow this sermon, worked out for the language step. */
sealed class LangStatus {
    object Checking : LangStatus()
    object NoSermon : LangStatus()
    data class Ready(val prepared: Prepared) : LangStatus()
}

suspend fun prepareLanguage(lang: TableLang, code: String): Prepared? = withContext(Dispatchers.IO) {
    val db = DB.connection("main.db")
    val sermon = runCatching { db.getSermonsList(lang.id).firstOrNull { it.dateCode.equals(code, true) }?.let { db.getFullSermon(it) } }.getOrNull() ?: return@withContext null
    val local = runCatching { SermonMedia.findLocal(sermon) }.getOrNull()
    val audio = local?.absolutePath ?: runCatching { SermonMedia.audioUrl(sermon) }.getOrNull()
    Prepared(sermon, sermon.deckItems(), audio, local != null)
}

/**
 * Everything about following a live host from this computer. It outlives the screens so leaving the
 * listener page for a moment doesn't drop the service.
 */
object ListenSession {
    private val id = UUID.randomUUID().toString()
    var host by mutableStateOf<LiveHostInfo?>(null)
    var name by mutableStateOf(loadData<String>("live_name") ?: "")
    var lang by mutableStateOf<TableLang?>(null)
    var prepared by mutableStateOf<Prepared?>(null)
    /** The English text and audio, for "Hear English audio instead" and "Show the English under mine". */
    var english by mutableStateOf<Prepared?>(null)
    var useEnglishAudio by mutableStateOf(false)
    var textOnly by mutableStateOf(false)
    var mode by mutableStateOf("Center")

    var state by mutableStateOf<LiveState?>(null)
    var connected by mutableStateOf(false)
    var rtt by mutableStateOf<Long?>(null)
    /** Seconds between where the audio is and where it should be. */
    var lag by mutableStateOf(0.0)
    var listening by mutableStateOf(false)
    private var connection: LiveConnection? = null
    private var lastResync = -1
    /** When the last state arrived, to move its position on by the time it has been in flight here. */
    @Volatile private var receivedAt = 0L
    private var lastSeek = 0L
    private var lastHostPlaying: Boolean? = null
    /** Debug readout (host place → our place, drift, rate); shown on the listener page while on. */
    var debug by mutableStateOf(System.getProperty("guide.liveDebug") != null)
    var debugLine by mutableStateOf("")

    fun rememberName(n: String, keep: Boolean) {
        name = n
        if (keep) saveData("live_name", n)
    }

    fun connect(h: LiveHostInfo) {
        if (connection?.host?.address == h.address && connected) return
        connection?.close()
        host = h
        connection = LiveConnection(h, onState = { state = it; receivedAt = System.currentTimeMillis(); connected = true }, onPong = { rtt = it }, onClosed = { connected = false }).also { it.connect() }
    }

    fun reconnect() { host?.let { connection?.close(); connection = null; connected = false; connect(it) } }
    fun ping() = connection?.ping()

    fun leave() {
        connection?.close()
        connection = null
        connected = false
        listening = false
        state = null
        host = null
    }

    /** The audio the listener hears and the timed sentences that belong to it. */
    fun audioSource(): Prepared? = when {
        textOnly -> null
        useEnglishAudio -> english
        else -> prepared
    }?.takeIf { it.audio != null }

    /** Where the host is, mapped into [items]: by the segment index first, else by place in the paragraph. */
    fun locateIn(items: List<DeckItem>, s: LiveState): Pair<Int, Float> =
        items.indexOfFirst { it.key != null && it.key == s.sentence }.takeIf { it >= 0 }?.let { it to s.progress }
            ?: locate(items, s.paragraph, s.fraction)

    /**
     * Seconds into [src]'s own recording for the host's place. Same recording as the host: its position as is.
     * Otherwise the same sentence in this language's own timings (languages are timed differently); untimed text
     * falls back to its share of the characters times the length of the recording.
     */
    /** The host's sentence in our recording: where we should be, and where that sentence starts and ends. */
    private data class Place(val target: Double, val start: Double, val end: Double)

    private fun placeIn(src: Prepared, s: LiveState, duration: Double): Place? {
        val items = src.items
        val (i, p) = locateIn(items, s)
        val it = items.getOrNull(i) ?: return null
        val start = it.start
        if (start == null || !presentation.isTimed(items)) {
            // untimed text: its share of the characters times the length of the recording
            if (duration <= 0) return null
            val shares = cumulativeShares(items)
            val from = (shares.getOrNull(i - 1) ?: 0f) * duration
            val to = (shares.getOrNull(i) ?: 1f) * duration
            return Place(from + p * (to - from), from, to)
        }
        val end = it.end ?: items.drop(i + 1).firstNotNullOfOrNull { n -> n.start } ?: (start + 0.1)
        return Place(start + p * (end - start), start, end)
    }

    /** Where our own sentence (the one being heard) starts, to know when we are at a boundary. */
    private fun currentStart(src: Prepared, time: Double, duration: Double): Double {
        val items = src.items
        if (!presentation.isTimed(items)) {
            val shares = cumulativeShares(items)
            val i = shares.indexOfFirst { it * duration > time }.coerceAtLeast(0)
            return (shares.getOrNull(i - 1) ?: 0f) * duration
        }
        return items.getOrNull(presentation.locateTime(items, time, duration).first)?.start ?: 0.0
    }

    /** True while we are paused because we got ahead of the host and are waiting for it at a sentence boundary. */
    private var holding = false
    private var jumpNext = true
    private var rate = 1.0
    private var lastHostPos = 0.0
    private var lastHostAt = 0L
    var syncState by mutableStateOf("")

    /**
     * One sync step, run five times a second. Languages are timed differently (a sentence may start at 2s in Swahili and
     * 0.5s in English), so the listener stays on the host's *sentence*, not its clock: see [decideSync].
     * Host pause / play applies straight away; with the same recording as the host its position is used directly.
     */
    suspend fun syncStep(engine: AuraPlayer, offsetMs: Int) {
        val src = audioSource() ?: return
        val s = state ?: return
        if (!connected) { engine.setSpeed(1.0); return }
        if (s.playing != lastHostPlaying) { engine.setPause(!s.playing); lastHostPlaying = s.playing; holding = false }
        val duration = engine.duration.value
        if (duration <= 0) return // still loading: seeking now would be lost
        val now = System.currentTimeMillis()
        // the host jumped (seek or new sentence by hand) when its position moved far from what time alone explains
        val expected = if (s.playing) (now - lastHostAt) / 1000.0 else 0.0
        val hostJumped = lastHostAt > 0 && abs((s.position - lastHostPos) - expected) > 3.0
        lastHostPos = s.position; lastHostAt = now
        val resync = lastResync >= 0 && s.resync != lastResync
        lastResync = s.resync
        if (!s.playing) { syncState = "host paused"; return }
        if (now - lastSeek < 1200 && !resync) return // let a seek land before judging again
        val ourTime = engine.currentTime.value
        val offset = offsetMs / 1000.0
        val inFlight = ((now - receivedAt) / 1000.0).coerceIn(0.0, 1.0)

        val same = s.language.isNotBlank() && s.language == src.sermon?.language && src.sermon?.dateCode.equals(s.code, true)
        val action = if (same) {
            val target = s.position + inFlight + offset
            val e = target - ourTime
            lag = abs(e)
            if (jumpNext || resync || hostJumped || abs(e) > 2.0) SyncAction.Seek(target, fade = !jumpNext) else SyncAction.Play(1.0 + (e * 0.1).coerceIn(-0.05, 0.05))
        } else {
            val place = placeIn(src, s, duration) ?: return
            val ourLen = place.end - place.start
            val ratio = if (s.span > 0.05 && ourLen > 0.05) ourLen / s.span else 1.0
            val target = place.target + inFlight * ratio + offset
            lag = abs(target - ourTime)
            decideSync(
                ourTime, target, place.end + offset, hostLeft = (1 - s.progress) * s.span,
                baseRatio = ratio, currentStart = currentStart(src, ourTime - offset, duration) + offset, jumpNow = jumpNext || resync || hostJumped,
            )
        }
        jumpNext = false
        when (action) {
            is SyncAction.Hold -> if (!holding) { holding = true; engine.setPause(true) }
            is SyncAction.Seek -> {
                if (holding) { holding = false; engine.setPause(false) }
                if (action.fade) fadeSeek(engine, action.to.coerceIn(0.0, duration)) else engine.seek(action.to.coerceIn(0.0, duration))
                lastSeek = now
            }
            is SyncAction.Play -> {
                if (holding) { holding = false; engine.setPause(false) }
                // ease towards the new speed over ~300ms instead of stepping
                rate += (action.rate - rate) * 0.67
                engine.setSpeed(rate)
            }
        }
        syncState = when (action) { is SyncAction.Hold -> "holding at boundary"; is SyncAction.Seek -> "jump"; else -> "playing" }
        if (debug) debugLine = "host ${s.sentence ?: s.paragraph} %.2f · lag %.2fs · %.2f× · %s".format(s.progress, lag, rate, syncState)
    }

    /** A 150ms fade out, the jump, and a fade back in, so a jump at a boundary doesn't click. */
    private suspend fun fadeSeek(engine: AuraPlayer, to: Double) {
        val v = engine.volume.value
        for (k in 4 downTo 0) { engine.setVolume(v * k / 5); delay(30) }
        engine.seek(to)
        for (k in 1..5) { delay(30); engine.setVolume(v * k / 5) }
    }

    /** A new audio was loaded: re-apply the host's play state and let the first sync seek straight away. */
    fun audioLoaded() { lastHostPlaying = null; lastSeek = 0; holding = false; jumpNext = true; rate = 1.0 }

    fun report(audioOn: Boolean) {
        connection?.report(ListenerReport(id, name.ifBlank { "Listener" }, lang?.name ?: "", if (!audioOn) "Text only" else modeLabel(mode), lag, audioOn))
    }

    fun modeLabel(m: String) = when (m) { "Center" -> "Auto-center"; "Read" -> "Read along"; else -> "Scroll" }

    fun logJoin() {
        val h = host ?: return
        ActivityLog.record(ActivityKind.LIVE, "Joined ${state?.title ?: h.title} live", "${lang?.name ?: ""} · hosted by ${h.name}", "live:${h.address}:${state?.code ?: h.code}", "Rejoin")
    }
}

/** Keeps the audio on the host's sentence while the listener page is up. */
@Composable
fun ListenSync() {
    val engine = LocalPlayerState.current
    val globalPlayer = LocalPlayer.current
    val prefs = ReadingStores.listener.prefs
    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            val src = ListenSession.audioSource()
            val s = ListenSession.state
            if (src != null && s != null) {
                val audioId = "live:${s.code}:${src.audio}"
                if (globalPlayer.nowPlaying.value?.id != audioId) {
                    globalPlayer.loadNowPlaying(GlobalPlayerViewModel.NowPlaying(audioId, s.title, "Live · ${s.code}", GlobalPlayerType.LIVE, true, 0L, src.audio!!))
                    ListenSession.audioLoaded()
                } else ListenSession.syncStep(engine, prefs.value.offsetMs)
            } else if (ListenSession.textOnly && engine.playerState.value == PlayerState.PLAYING && globalPlayer.nowPlaying.value?.id?.startsWith("live:") == true) {
                engine.setPause(true)
            }
            if (tick % 5 == 0) { ListenSession.report(src != null); ListenSession.ping() }
            // the link dropped: try again every couple of seconds while the audio carries on
            if (!ListenSession.connected && tick > 0 && tick % 10 == 0) ListenSession.reconnect()
            tick++
            delay(200)
        }
    }
}
