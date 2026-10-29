package live

/** What the listener's player should do next to stay on the host's sentence. */
sealed class SyncAction {
    data class Play(val rate: Double) : SyncAction()
    /** Wait at a sentence boundary until the host reaches the next sentence. */
    object Hold : SyncAction()
    data class Seek(val to: Double, val fade: Boolean) : SyncAction()
}

// mpv keeps the pitch when the speed changes (audio-pitch-correction), so this range still sounds natural.
// Up to 1.5× so a recording much longer than the host's (Swahili against English) stays close.
const val MIN_RATE = 0.80
const val MAX_RATE = 1.5

/**
 * Sentence-by-sentence alignment. All times are in the listener's own recording, except [hostLeft].
 * @param ourTime where the listener's audio is
 * @param target where it should be: start + fraction × length of the host's sentence in the listener's timing
 * @param sentenceEnd the end of that sentence in the listener's timing
 * @param hostLeft seconds the host's recording still needs to finish that sentence
 * @param baseRatio listener sentence length ÷ host sentence length (used when [hostLeft] is too short to divide by)
 * @param currentStart the start of the listener sentence [ourTime] is in, to tell when it is at a boundary
 * @param jumpNow join, host seek or "Resync everyone": go straight to [target], mid-sentence is fine
 */
fun decideSync(
    ourTime: Double, target: Double, sentenceEnd: Double, hostLeft: Double,
    baseRatio: Double, currentStart: Double, jumpNow: Boolean,
): SyncAction {
    if (jumpNow) return SyncAction.Seek(target.coerceAtLeast(0.0), fade = false)
    val error = target - ourTime // positive: behind the host
    val atBoundary = ourTime - currentStart < 0.3
    // finish both at once: the rest of our sentence in the time the host has left of it
    val needed = if (hostLeft > 0.4) (sentenceEnd - ourTime) / hostLeft else baseRatio
    return when {
        // ahead: we've finished the host's sentence (or more); wait at the boundary we just crossed, never mid-sentence
        ourTime >= sentenceEnd - 0.05 && error < -0.05 -> if (atBoundary) SyncAction.Hold else SyncAction.Play(MIN_RATE)
        // behind: never skip words, play faster until caught up
        error > 1.5 -> SyncAction.Play(MAX_RATE)
        else -> SyncAction.Play(needed.coerceIn(MIN_RATE, MAX_RATE))
    }
}

/**
 * True only when host and listener play the very same file: the same Table language they each chose, and the same
 * sermon (date code). Then the host's audio time is used directly; otherwise sentences are matched.
 */
fun sameRecording(hostTableLanguage: String, hostCode: String, ourTableLanguage: String?, ourCode: String?): Boolean =
    hostTableLanguage.isNotBlank() && hostTableLanguage.equals(ourTableLanguage, true) &&
        hostCode.isNotBlank() && hostCode.equals(ourCode, true)

/**
 * Some languages' Table data carries the timings of the English recording (Swahili does: its sentences end at 97 min
 * for "A Guide" while the Swahili recording runs 127 min). When the timings don't fit the recording being played,
 * stretch them to it: returns recording length ÷ last timed end, or 1 when they already fit (within 3%).
 * shortcut: a straight stretch, not true Swahili timings; replace once the data has real per-language timings.
 */
fun timeScale(lastTimedEnd: Double?, duration: Double): Double {
    if (lastTimedEnd == null || lastTimedEnd <= 0 || duration <= 0) return 1.0
    val k = duration / lastTimedEnd
    return if (kotlin.math.abs(k - 1) > 0.03) k else 1.0
}
