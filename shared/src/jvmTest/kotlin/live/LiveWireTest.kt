package live

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveWireTest {
    @Test
    fun readsAHostAnnouncement() {
        val h = parseAnnouncement("THEGUIDE-LIVE|2|47802|Church PC|A Guide|62-1014E|14|4821|1700000000000", "192.168.1.20", 5L)!!
        assertEquals(LiveHostInfo("192.168.1.20", 47802, "Church PC", "A Guide", "62-1014E", 14, 5L, "4821", 1700000000000), h)
    }

    @Test
    fun ignoresOtherPackets() {
        assertNull(parseAnnouncement("SOMETHING|2|47802|x|y|z|1|2", "1.2.3.4", 0))
        assertNull(parseAnnouncement("THEGUIDE-LIVE|2|47802", "1.2.3.4", 0))
    }
}
