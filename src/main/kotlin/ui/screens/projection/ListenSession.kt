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

    fun rememberName(n: String, keep: Boolean) {
        name = n
        if (keep) saveData("live_name", n)
    }

    fun connect(h: LiveHostInfo) {
        if (connection?.host?.address == h.address && connected) return
        connection?.close()
        host = h
        connection = LiveConnection(h, onState = { state = it; connected = true }, onPong = { rtt = it }, onClosed = { connected = false }).also { it.connect() }
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

    /** Seconds into [items]' own recording for that place, when the sentence has timings. */
    private fun timeIn(items: List<DeckItem>, s: LiveState): Double? {
        val (i, p) = locateIn(items, s)
        val it = items.getOrNull(i) ?: return null
        val start = it.start ?: return null
        val end = it.end ?: items.drop(i + 1).firstNotNullOfOrNull { n -> n.start } ?: return start
        return start + p * (end - start)
    }

    /**
     * One sync step: never the host's clock, always the same sentence in this recording's own timing.
     * Small drift is fixed by playing 0.97×–1.03×; only more than 2s jumps. With the link down, keep playing.
     */
    fun syncStep(engine: AuraPlayer, offsetMs: Int) {
        val src = audioSource() ?: return
        val s = state ?: return
        if (!connected) { engine.setSpeed(1.0); return }
        val target = timeIn(src.items, s)?.plus(offsetMs / 1000.0) ?: return
        val drift = target - engine.currentTime.value
        lag = abs(drift)
        val resync = lastResync >= 0 && s.resync != lastResync
        lastResync = s.resync
        when {
            abs(drift) > 2.0 || resync -> { engine.seek(target.coerceAtLeast(0.0)); engine.setSpeed(1.0) }
            abs(drift) < 0.08 -> engine.setSpeed(1.0)
            else -> engine.setSpeed(1.0 + (drift * 0.05).coerceIn(-0.03, 0.03))
        }
        val playing = engine.playerState.value == PlayerState.PLAYING
        if (s.playing != playing) engine.setPause(!s.playing)
    }

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
                    engine.setPause(!s.playing)
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
