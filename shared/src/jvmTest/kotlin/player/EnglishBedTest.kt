package player

import parsers.vgr.VgrTimeMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The English following the Swahili "Kiongozi" (62-1014E) through VGR's real time map. */
class EnglishBedTest {
    private val map = assertNotNull(VgrTimeMap.decode(javaClass.getResource("/vgr/kiongozi-62-1014E-marker.txt")!!.readText()))

    @Test fun inStepPlaysAtNormalSpeed() {
        val target = map.toTimeline(1.0)
        assertEquals(BedAction.Play(1.0), decideBed(target, target, false, map.nextTimelinePoint(target)))
    }

    @Test fun aheadWaitsOnlyPastTwoSecondsAndAtTheEndOfAStretch() {
        // 1s ahead: keeps playing
        assertEquals(BedAction.Play(1.0), decideBed(100.0, 101.0, false, 101.1))
        // 2.5s ahead and about to reach a boundary: waits there
        assertEquals(BedAction.Hold, decideBed(100.0, 102.5, false, 102.6))
        // 2.5s ahead but mid-stretch: finishes the stretch first (no cut word)
        assertEquals(BedAction.Play(1.0), decideBed(100.0, 102.5, false, 104.0))
        // waiting until the Swahili catches up
        assertEquals(BedAction.Hold, decideBed(102.0, 102.5, true, 102.6))
        assertEquals(BedAction.Play(1.0), decideBed(102.6, 102.5, true, 102.6))
    }

    @Test fun jumpsOnlyWhenMoreThanTwoAndAHalfSecondsBehind() {
        assertEquals(BedAction.Play(1.0), decideBed(102.0, 100.0, false, null))
        assertIs<BedAction.Seek>(decideBed(103.0, 100.0, false, null))
    }

    @Test fun liveFollowsTheHostAtSteadySpeed() {
        assertEquals(BedAction.Play(1.0), decideLiveBed(500.0, 500.1, false))
        assertEquals(BedAction.Hold, decideLiveBed(500.0, 500.6, false)) // ahead of the host: waits
        assertEquals(BedAction.Play(1.0), decideLiveBed(500.7, 500.6, true))
        assertIs<BedAction.Seek>(decideLiveBed(502.0, 500.0, false)) // 2s behind: one jump
    }

    /** One hour of normal listening through the real map, five steps a second, as the app runs it. */
    @Test fun oneHourOfKiongozi() {
        var bed = 0.0; var holding = false; var waits = 0; var waited = 0.0; var seeks = 0; var cuts = 0
        var t = 0.0; var holdStart = 0.0
        while (t < 3600) {
            val target = map.toTimeline(t)
            when (val a = decideBed(target, bed, holding, map.nextTimelinePoint(bed))) {
                is BedAction.Seek -> { bed = a.to; holding = false; seeks++ }
                BedAction.Hold -> if (!holding) {
                    holding = true; holdStart = t
                    // a wait that begins within 0.25s after a stretch started would cut its first word
                    val prev = map.nextTimelinePoint(bed - 0.25)
                    if (prev != null && prev < bed - 0.02) cuts++
                }
                is BedAction.Play -> if (holding) { holding = false; waits++; waited += t - holdStart }
            }
            if (!holding) bed += 0.2
            t += 0.2
        }
        val perMinute = waits / 60.0
        println("waits/min %.1f, avg %.1fs, seeks %d, cut words %d".format(perMinute, waited / waits, seeks, cuts))
        assertTrue(perMinute in 1.8..3.0, "waits per minute $perMinute")
        assertTrue(cuts <= 3, "cut words $cuts")
    }

    @Test fun mixNeverReachesZero() {
        assertEquals(0.9 to 0.1, mixFactors(0))
        assertEquals(0.1 to 0.9, mixFactors(100))
        assertEquals(0.85 to 0.15, mixFactors(15))
    }
}

class PanTest {
    @Test fun neverGoneFromTheOtherEar() {
        for (mix in listOf(15, 50, 85)) {
            val s = panSpread(mix)
            val (tl, tr) = panGains(s)   // translation, leaning right
            val (el, er) = panGains(-s)  // English, leaning left
            assertTrue(tl >= PAN_FLOOR && tr > tl, "translation at $mix: $tl / $tr")
            assertTrue(er >= PAN_FLOOR && el > er, "English at $mix: $el / $er")
        }
    }

    @Test fun evenIsFullyApartAndTheEndsSitNearTheMiddle() {
        assertEquals(1.0, panSpread(50), 1e-9)
        assertEquals(PAN_FLOOR to 1.0, panGains(1.0).let { (l, r) -> l to Math.round(r * 100) / 100.0 })
        assertTrue(panSpread(15) < 0.35 && panSpread(85) < 0.35)
        assertEquals(panSpread(15), panSpread(85), 1e-9)
    }
}
