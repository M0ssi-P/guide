package parsers.vgr

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VgrTimeMapTest {
    // the vgr_dynamic_product_marker tag of VGR's Swahili "Kiongozi" (62-1014E) opus: English timeline → Swahili recording
    private val kiongozi = assertNotNull(VgrTimeMap.decode(javaClass.getResource("/vgr/kiongozi-62-1014E-marker.txt")!!.readText()))

    @Test fun lastSentenceLandsNearTheEndOfTheSwahiliRecording() =
        // Table: 5,821,364 ms (1:37:01); in the pair 5,819,115–5,823,875 → 7,629,154–7,636,744, 47% in
        assertEquals(7632.740, kiongozi.toRecording(5821.364), 0.001)

    @Test fun firstPairAndTheGapAfterIt() {
        assertEquals(0.0, kiongozi.toRecording(1.091), 0.001) // English 1.091–3.155 = Swahili 0–1.451
        assertEquals(1.451, kiongozi.toRecording(3.155), 0.001)
        // gap 3.155–3.353 ↔ 1.451–1.453: halfway lands halfway
        assertEquals(1.452, kiongozi.toRecording(3.254), 0.001)
    }

    @Test fun inverseGoesBack() {
        for (t in listOf(10.0, 600.0, 1800.0, 3600.0, 5821.364)) assertEquals(t, kiongozi.toTimeline(kiongozi.toRecording(t)), 0.01)
    }

    @Test fun liveHostTimelineToSwahiliListener() {
        // English host at 10, 30 and 60 min on the timeline → the Swahili listener's own audio is further in
        for (m in listOf(10, 30, 60)) assert(kiongozi.toRecording(m * 60.0) > m * 60.0) { "at $m min" }
        assertEquals(7632.740, null.toRecording(5821.364).let { kiongozi.toRecording(it) }, 0.001) // English host: no map, timeline = its time
    }

    @Test fun readsTheTagByItsLengthNotPastIt() {
        val value = javaClass.getResource("/vgr/kiongozi-62-1014E-marker.txt")!!.readText()
        val tag = ("vgr_dynamic_product_marker=$value").toByteArray(Charsets.ISO_8859_1)
        val len = byteArrayOf(tag.size.toByte(), (tag.size shr 8).toByte(), (tag.size shr 16).toByte(), 0)
        // the next tag's length bytes ("p\0\0\0") would look like base64 to a pattern match
        val head = "OpusTags".toByteArray() + len + tag + byteArrayOf(0x70, 0, 0, 0) + "publisher=VGR".toByteArray()
        assertEquals(7632.740, assertNotNull(VgrTimeMap.fromHeader(head)).toRecording(5821.364), 0.001)
    }

    @Test fun noMapNoChange() {
        assertNull(VgrTimeMap.fromHeader("OpusTags title=A Guide".toByteArray()))
        assertEquals(42.0, (null as VgrTimeMap?).toRecording(42.0))
    }
}

class OmegaCatalogTest {
    private fun varint(o: ByteArrayOutputStream, v: Long) { var n = v; while (true) { val b = (n and 0x7f).toInt(); n = n ushr 7; if (n == 0L) { o.write(b); return }; o.write(b or 0x80) } }
    private fun msg(vararg fields: Pair<Int, Any>): ByteArray = ByteArrayOutputStream().also { o ->
        for ((f, v) in fields) when (v) {
            is Long -> { varint(o, (f shl 3).toLong()); varint(o, v) }
            is Int -> { varint(o, (f shl 3).toLong()); varint(o, v.toLong()) }
            is ByteArray -> { varint(o, (f shl 3 or 2).toLong()); varint(o, v.size.toLong()); o.write(v) }
            is String -> { val b = v.toByteArray(); varint(o, (f shl 3 or 2).toLong()); varint(o, b.size.toLong()); o.write(b) }
        }
    }.toByteArray()
    private fun framed(vararg m: ByteArray) = ByteArrayOutputStream().also { o -> m.forEach { varint(o, it.size.toLong()); o.write(it) } }.toByteArray()

    @Test fun findsTheSwahiliOpusOfAGuide() {
        val id = ByteArray(64) { it.toByte() }
        val cat = OmegaCatalog.parse(framed(
            msg(1 to "OMNI", 7 to msg(1 to 20, 2 to "eng"), 7 to msg(1 to 47, 2 to "swa")),
            msg(1 to 9, 2 to 6, 3 to 903, 4 to 20, 6 to 12176052L, 7 to ByteArray(64)),
            msg(1 to 3, 2 to 6, 3 to 903, 4 to 47, 6 to 15941740L, 7 to id),
        ))
        val f = assertNotNull(cat.find(903, "SWA", OmegaCatalog.OPUS))
        assertEquals(15941740L, f.size)
        assertEquals(3, f.mediaTypeId)
        assertEquals(id.joinToString("") { "%02x".format(it) }, f.fileId)
        assertNull(cat.find(903, "swa", OmegaCatalog.M4A))
    }
}
