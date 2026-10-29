package parsers.vgr

import okHttpClient
import java.io.File
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Minimal protobuf reading: (field, wire type, value) where value is a Long (varint) or ByteArray (length-delimited). */
internal object Proto {
    fun varint(b: ByteArray, at: Int): Pair<Long, Int> {
        var r = 0L; var s = 0; var i = at
        while (true) {
            val x = b[i++].toInt() and 0xff
            r = r or ((x and 0x7f).toLong() shl s); s += 7
            if (x < 0x80) return r to i
        }
    }

    fun fields(b: ByteArray, from: Int = 0, to: Int = b.size): List<Triple<Int, Int, Any>> {
        val out = ArrayList<Triple<Int, Int, Any>>()
        var i = from
        while (i < to) {
            val (k, j) = varint(b, i); i = j
            val f = (k ushr 3).toInt(); val t = (k and 7).toInt()
            when (t) {
                0 -> { val (v, n) = varint(b, i); i = n; out += Triple(f, t, v) }
                2 -> { val (len, n) = varint(b, i); out += Triple(f, t, b.copyOfRange(n, n + len.toInt())); i = n + len.toInt() }
                1 -> i += 8
                5 -> i += 4
                else -> error("wire type $t")
            }
        }
        return out
    }

    fun ints(b: ByteArray): Map<Int, Long> = fields(b).filter { it.second == 0 }.associate { it.first to it.third as Long }
}

/**
 * VGR's time map for one recording, from its `vgr_dynamic_product_marker` audio tag: anchor pairs saying which stretch of
 * the main (English) timeline every Table time is on corresponds to which stretch of this recording. All in milliseconds.
 */
class VgrTimeMap(private val timeline: LongArray, private val recording: LongArray) {
    // anchors are flattened to points (each pair's start and end), so gaps between pairs interpolate too
    init { require(timeline.size == recording.size && timeline.isNotEmpty()) }

    /** A Table (timeline) time to this recording's time, in seconds. */
    fun toRecording(seconds: Double): Double = convert(seconds * 1000, timeline, recording) / 1000

    /** This recording's time to the Table timeline, in seconds. */
    fun toTimeline(seconds: Double): Double = convert(seconds * 1000, recording, timeline) / 1000

    /** The next anchor point on the timeline after [seconds] (a pair's start or end), where English can wait. */
    fun nextTimelinePoint(seconds: Double): Double? {
        val ms = (seconds * 1000).toLong()
        val i = timeline.binarySearch(ms).let { if (it >= 0) it + 1 else -it - 1 }
        return timeline.getOrNull(i)?.div(1000.0)
    }

    private fun convert(ms: Double, from: LongArray, to: LongArray): Double {
        val i = from.binarySearch(ms.toLong()).let { if (it >= 0) it else -it - 2 }
        // before the first anchor: from 0 up to it proportionally
        if (i < 0) return if (from[0] > 0) ms.coerceAtLeast(0.0) * to[0] / from[0] else ms
        if (i >= from.size - 1) return to.last() + (ms - from.last()) // past the last anchor: normal speed
        val f = (ms - from[i]) / (from[i + 1] - from[i]).coerceAtLeast(1)
        return to[i] + f * (to[i + 1] - to[i])
    }

    companion object {
        /** Decodes the tag's base64 value. Pairs are field 6: {1: {1: timeline start, 2: end}, 3: {1: recording start, 2: end}}. */
        fun decode(base64: String): VgrTimeMap? = runCatching {
            val bytes = Base64.getDecoder().decode(base64.trim().let { it + "=".repeat((4 - it.length % 4) % 4) })
            val tl = ArrayList<Long>(); val rec = ArrayList<Long>()
            for ((f, t, v) in Proto.fields(bytes)) {
                if (f != 6 || t != 2) continue
                val pair = Proto.fields(v as ByteArray)
                val a = pair.firstOrNull { it.first == 1 }?.third as? ByteArray ?: continue
                val b = pair.firstOrNull { it.first == 3 }?.third as? ByteArray ?: continue
                val ai = Proto.ints(a); val bi = Proto.ints(b)
                // each pair contributes its start and end point; points must keep increasing on both sides
                for ((x, y) in listOf((ai[1] ?: 0) to (bi[1] ?: 0), (ai[2] ?: 0) to (bi[2] ?: 0))) {
                    if (tl.isEmpty() || (x > tl.last() && y >= rec.last())) { tl += x; rec += y }
                }
            }
            if (tl.size < 2) null else VgrTimeMap(tl.toLongArray(), rec.toLongArray())
        }.getOrNull()

        private const val KEY = "vgr_dynamic_product_marker="

        /**
         * Finds the tag in the first part of a file. In the OpusTags header every tag is preceded by its length
         * (4 bytes, little-endian), which says exactly where the value ends.
         */
        fun fromHeader(head: ByteArray): VgrTimeMap? {
            val text = String(head, Charsets.ISO_8859_1)
            val at = text.indexOf(KEY).takeIf { it >= 4 } ?: return null
            val len = (0..3).sumOf { (head[at - 4 + it].toInt() and 0xff) shl (8 * it) }
            val end = (at + len).coerceAtMost(head.size)
            // an Ogg page break inside a long tag puts a page header in the middle; keep only base64 characters
            return decode(text.substring(at + KEY.length, end).filter { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' })
        }
    }
}

/** Time maps per audio source (file path or URL), read once in the background. Null until read, or when there is none. */
object TimeMaps {
    private const val HEAD = 2_000_000
    private val headers = ConcurrentHashMap<String, Result<AudioHeader?>>()
    private val synopsisRegex = Regex(""""sermonId"\s*:\s*(\d+).*?"languageId"\s*:\s*(\d+)""")

    /** What VGR wrote into a recording's header: its time map, and which sermon and language it is (from `synopsis`). */
    class AudioHeader(val map: VgrTimeMap?, val sermonId: Int?, val languageId: Int?)

    /** The header of [source] (path or URL), read once in the background; null until read. */
    fun header(source: String?): AudioHeader? {
        source ?: return null
        headers[source]?.let { return it.getOrNull() }
        headers[source] = Result.success(null) // reading; also stops repeat requests
        thread(isDaemon = true, name = "vgr-time-map") {
            headers[source] = runCatching {
                val head = head(source)
                val syn = synopsisRegex.find(String(head, Charsets.ISO_8859_1))
                AudioHeader(VgrTimeMap.fromHeader(head), syn?.groupValues?.get(1)?.toInt(), syn?.groupValues?.get(2)?.toInt())
            }
        }
        return null
    }

    fun get(source: String?): VgrTimeMap? = header(source)?.map

    private fun head(source: String): ByteArray {
        val f = File(source)
        if (f.isFile) return f.inputStream().use { it.readNBytes(HEAD) }
        val req = okhttp3.Request.Builder().url(source).header("Range", "bytes=0-${HEAD - 1}").build()
        return okHttpClient.newCall(req).execute().use { it.body?.byteStream()?.readNBytes(HEAD) ?: ByteArray(0) }
    }
}

/** Table time (timeline) ↔ the recording's own time, through its map when it has one. */
fun VgrTimeMap?.toRecording(t: Double) = this?.toRecording(t) ?: t
fun VgrTimeMap?.toTimeline(t: Double) = this?.toTimeline(t) ?: t
