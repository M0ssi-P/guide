package live

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The owner's example: Swahili runs 2h, English 1h30 (Swahili is 4/3 as long). Sentence 1 starts at 0.5s in English
 * (0.5–3.5s, 3s long) and at 2s in Swahili (2–6s, 4s long). The host plays English, the listener Swahili.
 */
class LiveSyncTest {
    private val swStart = 2.0
    private val swEnd = 6.0
    private val hostSpan = 3.0
    private val ratio = (swEnd - swStart) / hostSpan

    /** The host is [f] through sentence 1; the listener is at [ourTime] in the Swahili audio. */
    private fun at(f: Double, ourTime: Double, currentStart: Double = swStart, jump: Boolean = false) = decideSync(
        ourTime = ourTime, target = swStart + f * (swEnd - swStart), sentenceEnd = swEnd,
        hostLeft = (1 - f) * hostSpan, baseRatio = ratio, currentStart = currentStart, jumpNow = jump,
    )

    @Test fun joinSeeksStraightToTheSameSentence() {
        // host 1s into English sentence 1 (1.5s) = a third through; in Swahili that is 2 + 4/3
        val a = at(1.0 / 3, ourTime = 0.0, jump = true)
        assertIs<SyncAction.Seek>(a)
        assertEquals(2.0 + 4.0 / 3, a.to, 1e-9)
        assertEquals(false, a.fade)
    }

    @Test fun inStepItPlaysFasterSoBothFinishTheSentenceTogether() {
        val a = at(0.5, ourTime = 4.0)
        assertIs<SyncAction.Play>(a)
        assertEquals(4.0 / 3, a.rate, 1e-9) // 2s of Swahili left in the host's 1.5s of English
    }

    @Test fun slightlyBehindSpeedsUpButStaysInRange() {
        val a = at(0.5, ourTime = 3.6)
        assertIs<SyncAction.Play>(a)
        assertEquals(MAX_RATE, a.rate, 1e-9)
    }

    @Test fun aheadHoldsOnlyAtASentenceBoundary() {
        // finished Swahili sentence 1 and just started sentence 2 (6.0s) while the host is still in sentence 1
        assertIs<SyncAction.Hold>(at(0.9, ourTime = 6.1, currentStart = 6.0))
        // already mid-way into sentence 2: don't stop mid-word, just slow down
        assertEquals(SyncAction.Play(MIN_RATE), at(0.9, ourTime = 7.0, currentStart = 6.0))
    }

    @Test fun farBehindNeverSkipsItJustPlaysFaster() {
        // listener 5s back in an earlier sentence, at a boundary or not: no jump, top speed until caught up
        assertEquals(SyncAction.Play(MAX_RATE), at(0.5, ourTime = -0.9, currentStart = -1.0))
        assertEquals(SyncAction.Play(MAX_RATE), at(0.5, ourTime = 0.0, currentStart = -1.0))
    }

    @Test fun ratesAlwaysInRange() {
        for (f in listOf(0.0, 0.3, 0.6, 0.95)) for (t in listOf(1.0, 2.5, 4.0, 5.5)) {
            val a = at(f, t)
            if (a is SyncAction.Play) assertTrue(a.rate in MIN_RATE..MAX_RATE, "rate ${a.rate} at f=$f t=$t")
        }
    }
}

class SameRecordingTest {
    @Test fun sameTableLanguageAndSermonUsesTheHostTime() = assertTrue(sameRecording("en", "62-1014M", "en", "62-1014m"))
    // A Guide (English host) and Kiongozi (Swahili listener) share the date code but not the recording
    @Test fun otherLanguageMatchesBySentence() = assertEquals(false, sameRecording("en", "62-1014M", "sw", "62-1014M"))
    @Test fun otherSermonMatchesBySentence() = assertEquals(false, sameRecording("en", "62-1014M", "en", "62-1014E"))
    // older hosts don't send their Table language
    @Test fun olderHostNeverUsesTheShortcut() = assertEquals(false, sameRecording("", "62-1014M", "en", "62-1014M"))
}
