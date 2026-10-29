package player

import parsers.vgr.VgrTimeMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/** The English following the Swahili "Kiongozi" (62-1014E) through VGR's real time map. */
class EnglishBedTest {
    private val map = assertNotNull(VgrTimeMap.decode(javaClass.getResource("/vgr/kiongozi-62-1014E-marker.txt")!!.readText()))

    private fun decide(swahiliTime: Double, englishTime: Double, holding: Boolean = false, rate: Double = 1.0) =
        decideBed(map.toTimeline(swahiliTime), englishTime, holding, map.previousTimelinePoint(englishTime), map.nextTimelinePoint(englishTime), rate)

    @Test fun inStepPlaysAtTheMainSpeed() {
        val target = map.toTimeline(1.0) // Swahili 1.0s → English about 2.5s (first pair 1.091–3.155 ↔ 0–1.451)
        assertEquals(BedAction.Play(1.0), decideBed(target, target, false, map.previousTimelinePoint(target), map.nextTimelinePoint(target)))
        assertEquals(BedAction.Play(1.3), decideBed(target, target, false, null, null, 1.3))
    }

    @Test fun aheadWaitsAtTheBoundaryNotMidStretch() {
        // English just past the anchor at 3.155s while the Swahili is still before it: wait there
        assertEquals(BedAction.Hold, decide(swahiliTime = 1.2, englishTime = 3.20))
        // ahead but in the middle of a stretch: keep going until the boundary
        assertEquals(BedAction.Play(1.0), decide(swahiliTime = 1.2, englishTime = 2.95))
    }

    @Test fun holdingResumesOnceTheSwahiliCatchesUp() {
        assertEquals(BedAction.Hold, decide(swahiliTime = 1.2, englishTime = 3.20, holding = true))
        assertEquals(BedAction.Play(1.0), decide(swahiliTime = 1.6, englishTime = 3.40, holding = true))
    }

    @Test fun behindMoreThanASecondJumps() {
        val a = decide(swahiliTime = 7629.154, englishTime = 5810.0) // Swahili near the end → English 5,819.1s
        assertIs<BedAction.Seek>(a)
        assertEquals(5819.115, a.to, 0.01)
    }

    @Test fun mixNeverReachesZero() {
        assertEquals(0.9 to 0.1, mixFactors(0))
        assertEquals(0.1 to 0.9, mixFactors(100))
        assertEquals(0.85 to 0.15, mixFactors(15))
    }
}
