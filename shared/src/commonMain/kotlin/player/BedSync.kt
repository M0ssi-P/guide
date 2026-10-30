package player

import kotlin.math.PI
import kotlin.math.abs

/** What the English player does next to stay with the translated recording. */
sealed class BedAction {
    data class Play(val rate: Double) : BedAction()
    object Hold : BedAction()
    data class Seek(val to: Double) : BedAction()
}

const val MIX_MIN = 10
const val MIX_MAX = 90

/** How far the English may run ahead of the translation before it waits (outside Live). */
const val BED_LEAD = 2.0

/**
 * Normal listening. [target] is where the English should be: the translation's time mapped onto the Table timeline,
 * which is the English recording's own time. English plays at 1.0×; the translation usually takes longer, so once the
 * English is more than [BED_LEAD] ahead it waits — only at the END of a stretch ([nextPoint] just ahead), never just
 * after a new one started, so no word is cut. More than 2.5s behind it jumps.
 */
fun decideBed(target: Double, bedTime: Double, holding: Boolean, nextPoint: Double?): BedAction {
    val drift = target - bedTime
    return when {
        drift > 2.5 || drift < -8.0 -> BedAction.Seek(target)
        holding -> if (drift >= 0) BedAction.Play(1.0) else BedAction.Hold
        drift < -BED_LEAD && nextPoint != null && nextPoint - bedTime < 0.25 -> BedAction.Hold
        else -> BedAction.Play(1.0)
    }
}

/** Live: [target] is the host's place on the English timeline. Steady 1.0×; waits while ahead of the host, jumps when 1.5s behind. */
fun decideLiveBed(target: Double, bedTime: Double, holding: Boolean): BedAction {
    val drift = target - bedTime
    return when {
        drift > 1.5 || drift < -8.0 -> BedAction.Seek(target)
        holding -> if (drift >= 0) BedAction.Play(1.0) else BedAction.Hold
        drift < -0.3 -> BedAction.Hold
        else -> BedAction.Play(1.0)
    }
}

/** The least of a voice kept in the other ear, so with one earbud nothing is ever gone completely. */
const val PAN_FLOOR = 0.18

/**
 * How far apart the voices sit for [mix] (English %): 0.2 near the ends ("Mostly …", both close to the centre) up to
 * 1.0 at Even (fully apart). The translation leans right by this much, the English left.
 */
fun panSpread(mix: Int): Double = 0.2 + 0.8 * (1 - abs(mix.coerceIn(MIX_MIN, MIX_MAX) - 50) / 40.0).coerceIn(0.0, 1.0)

/** (left, right) gains for a voice at [position] (−1 left … 1 right): equal power, the far ear never below [PAN_FLOOR]. */
fun panGains(position: Double): Pair<Double, Double> {
    val a = (position.coerceIn(-1.0, 1.0) + 1) * PI / 4
    return maxOf(kotlin.math.cos(a), PAN_FLOOR) to maxOf(kotlin.math.sin(a), PAN_FLOOR)
}

/** Loudness of (translation, English) for [mix] = English percent: main (100 − mix)%, English mix%, of the user's volume. */
fun mixFactors(mix: Int): Pair<Double, Double> {
    val m = mix.coerceIn(MIX_MIN, MIX_MAX)
    return (100 - m) / 100.0 to m / 100.0
}
