package player

import kotlin.test.Test
import kotlin.test.assertEquals

class PanFilterTest {
    @Test fun filterOnlyWhenSeparated() {
        assertEquals("", panFilter(null))
        assertEquals("lavfi=[aformat=channel_layouts=mono,pan=stereo|c0=0.18*c0|c1=1.00*c0]", panFilter(1.0))
    }
}
