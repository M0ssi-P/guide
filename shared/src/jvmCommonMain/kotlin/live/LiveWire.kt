package live

/*
 * The live link's wire format, shared by the desktop host and every listener (desktop, phone, TV):
 * the host announces itself with a UDP broadcast every two seconds and pushes [LiveState] over a WebSocket a few times
 * a second; listeners report back with [ListenerReport] on the same socket.
 */

const val LIVE_WS_PORT = 47802
const val LIVE_DISCOVERY_PORT = 47801
/** First field of every host announcement on [LIVE_DISCOVERY_PORT]. */
const val LIVE_MAGIC = "THEGUIDE-LIVE"

/** What the host is showing and playing right now. */
data class LiveState(
    val title: String = "",
    val code: String = "",
    val location: String = "",
    val paragraph: Int? = null,
    /** How far into [paragraph] the host is, 0..1 by characters, for listeners whose text has no matching index. */
    val fraction: Float = 0f,
    /** The live Table sentence as its segment index [section, block, sentence], and how much of it is spoken. */
    val sentence: List<Int>? = null,
    val progress: Float = 0f,
    /** Seconds the host's recording takes for [sentence], so a listener can say it in the same time. 0 = unknown. */
    val span: Double = 0.0,
    /** Seconds into the host's own recording; only meaningful to someone playing the same file. */
    val position: Double = 0.0,
    /** The host's place on the Table timeline (seconds), the same for every language: its recording time mapped through VGR's time map. 0 from older hosts. */
    val timeline: Double = 0.0,
    val playing: Boolean = false,
    /** Bumped by "Resync everyone": listeners jump straight to the host's place. */
    val resync: Int = 0,
    val hostName: String = "",
    val language: String = "",
    /** The Table language the host reads from (e.g. "en"). Empty from older hosts, which then never use the same-recording shortcut. */
    val tableLanguage: String = "",
)

data class ListenerReport(
    val id: String = "",
    val name: String = "",
    val language: String = "",
    val mode: String = "",
    val lag: Double = 0.0,
    val audio: Boolean = true,
)

data class LiveHostInfo(
    val address: String, val port: Int, val name: String, val title: String, val code: String,
    val listeners: Int, val seenAt: Long, val joinCode: String = "", val startedAt: Long = 0,
)

/**
 * A host announcement ("THEGUIDE-LIVE|2|port|name|title|code|listeners|joinCode|startedAt") heard from [address], or
 * null when it isn't one.
 */
fun parseAnnouncement(text: String, address: String, now: Long): LiveHostInfo? {
    val parts = text.split("|")
    if (parts.size < 8 || parts[0] != LIVE_MAGIC) return null
    return LiveHostInfo(
        address, parts[2].toIntOrNull() ?: LIVE_WS_PORT, parts[3], parts[4], parts[5],
        parts[6].toIntOrNull() ?: 0, now, parts[7], parts.getOrNull(8)?.toLongOrNull() ?: 0
    )
}
