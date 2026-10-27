package activity

import androidx.compose.runtime.mutableStateOf
import loadData
import saveData
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ActivityKind(val label: String, val tag: String) {
    BIBLE("Bible", "BIB"),
    TABLE("Table", "TBL"),
    LISTENING("Listening", "AUD"),
    LIVE("Live", "LIVE"),
    HIGHLIGHTS("Highlights", "HL"),
    LETTERS("Letters", "LTR"),
}

/**
 * One line of the Recent feed. [key] identifies the thing (a sermon, chapter, letter…) so the same
 * thing done twice on one day updates its row instead of adding another.
 */
data class ActivityEvent(
    val id: String,
    val kind: ActivityKind,
    val title: String,
    val meta: String,
    val at: Long,
    val key: String,
    val action: String,
    val quote: String? = null,
    val seconds: Long = 0,
) : Serializable {
    val day: LocalDate get() = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
}

enum class Range(val label: String, val days: Long) { WEEK("Week", 7), MONTH("Month", 30), YEAR("Year", 365) }

data class Badge(
    val mark: String, val name: String, val desc: String,
    val progress: Float, val status: String, val earnedOn: LocalDate?, val kind: ActivityKind?,
)

data class Milestone(val title: String, val whenLabel: String, val done: Boolean, val next: Boolean)

/** Everything My Activity shows comes from here; it is filled by hooks in the Bible, Table, player, letters and live code. */
object ActivityLog {
    private const val EVENTS = "activity_events"
    private const val LISTEN = "activity_listen"
    private const val PARAS = "activity_paras"
    private const val FINISHED = "activity_finished"
    private const val EARNED = "activity_badges"

    val events = mutableStateOf(loadData<ArrayList<ActivityEvent>>(EVENTS)?.toList() ?: emptyList())
    /** Listening seconds per day (ISO date). */
    val listening = mutableStateOf(loadData<HashMap<String, Long>>(LISTEN) ?: hashMapOf())
    /** Table paragraphs read per day (ISO date). */
    val paragraphs = mutableStateOf(loadData<HashMap<String, Int>>(PARAS) ?: hashMapOf())
    /** Sermon ids read to the last page. */
    val finished = mutableStateOf(loadData<HashSet<String>>(FINISHED) ?: hashSetOf())
    private val earned = mutableStateOf(loadData<HashMap<String, String>>(EARNED) ?: hashMapOf())
    private var lastListenSave = 0L

    private fun today() = LocalDate.now()

    fun record(kind: ActivityKind, title: String, meta: String, key: String, action: String, quote: String? = null, seconds: Long = 0) {
        val now = System.currentTimeMillis()
        val list = events.value.toMutableList()
        val same = list.indexOfFirst { it.key == key && it.kind == kind && it.day == today() }
        val old = if (same >= 0) list.removeAt(same) else null
        val event = old?.copy(title = title, meta = meta, at = now, quote = quote ?: old.quote, seconds = seconds)
            ?: ActivityEvent("${now}-${key.hashCode()}", kind, title, meta, now, key, action, quote, seconds)
        list.add(0, event)
        events.value = list.take(3000)
        saveData(EVENTS, ArrayList(events.value))
        checkBadges()
    }

    /** Called about once a second while a sermon plays. */
    fun addListening(seconds: Long, key: String, title: String, meta: String) {
        if (seconds <= 0) return
        val day = today().toString()
        listening.value = HashMap(listening.value).apply { this[day] = (this[day] ?: 0L) + seconds }
        val total = (events.value.firstOrNull { it.key == key && it.kind == ActivityKind.LISTENING && it.day == today() }?.seconds ?: 0L) + seconds
        val now = System.currentTimeMillis()
        // the feed row and the per-day map are written at most every 30s; a crash loses under half a minute
        if (now - lastListenSave > 30_000 || total < 5) {
            lastListenSave = now
            record(ActivityKind.LISTENING, "Listened to $title", "$meta · ${shortDuration(total)}", key, "Resume", seconds = total)
            saveData(LISTEN, listening.value)
        } else {
            events.value = events.value.map { if (it.key == key && it.kind == ActivityKind.LISTENING && it.day == today()) it.copy(seconds = total, meta = "$meta · ${shortDuration(total)}") else it }
        }
    }

    fun paragraphsRead(count: Int) {
        if (count <= 0) return
        val day = today().toString()
        paragraphs.value = HashMap(paragraphs.value).apply { this[day] = (this[day] ?: 0) + count }
        saveData(PARAS, paragraphs.value)
    }

    fun finishedSermon(id: String, title: String, code: String) {
        if (id in finished.value) return
        finished.value = HashSet(finished.value).apply { add(id) }
        saveData(FINISHED, finished.value)
        record(ActivityKind.TABLE, "Finished $title", "$code · ${finished.value.size} sermons finished", "finish:$id", "Open")
    }

    // ---------- derived ----------

    fun activeDays(): Set<LocalDate> =
        events.value.map { it.day }.toSet() + listening.value.filterValues { it > 0 }.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Days in a row up to today (or up to yesterday, so a streak isn't lost before you open the app). */
    fun currentStreak(days: Set<LocalDate> = activeDays()): Int {
        var d = if (today() in days) today() else today().minusDays(1)
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }

    fun longestStreak(days: Set<LocalDate> = activeDays()): Int {
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (d in days.sorted()) {
            run = if (prev != null && ChronoUnit.DAYS.between(prev, d) == 1L) run + 1 else 1
            best = maxOf(best, run); prev = d
        }
        return best
    }

    /** Activity amount per day for the last [weeks] weeks, oldest first, columns of Monday→Sunday. */
    fun heat(weeks: Int = 15): List<Pair<LocalDate, Int>> {
        val start = today().minusDays(today().dayOfWeek.value - 1L).minusWeeks(weeks - 1L)
        val perDay = events.value.groupingBy { it.day }.eachCount()
        return (0 until weeks * 7).map { i ->
            val d = start.plusDays(i.toLong())
            val minutes = (listening.value[d.toString()] ?: 0L) / 60
            d to ((perDay[d] ?: 0) + (minutes / 15).toInt())
        }
    }

    private fun inRange(day: LocalDate, range: Range, offset: Long = 0) =
        !day.isAfter(today().minusDays(range.days * offset)) && day.isAfter(today().minusDays(range.days * (offset + 1)))

    fun listeningSeconds(range: Range, offset: Long = 0): Long =
        listening.value.entries.sumOf { (k, v) -> runCatching { LocalDate.parse(k) }.getOrNull()?.takeIf { inRange(it, range, offset) }?.let { v } ?: 0L }

    /** [range] split into 7 equal buckets of listening seconds, oldest first. */
    fun listeningBuckets(range: Range): List<Long> {
        val size = range.days / 7.0
        val out = LongArray(7)
        listening.value.forEach { (k, v) ->
            val d = runCatching { LocalDate.parse(k) }.getOrNull() ?: return@forEach
            val ago = ChronoUnit.DAYS.between(d, today())
            if (ago in 0 until range.days) out[(6 - (ago / size).toInt()).coerceIn(0, 6)] += v
        }
        return out.toList()
    }

    fun count(kind: ActivityKind, range: Range): Int = events.value.count { it.kind == kind && inRange(it.day, range) }
    fun paragraphsIn(range: Range): Int =
        paragraphs.value.entries.sumOf { (k, v) -> runCatching { LocalDate.parse(k) }.getOrNull()?.takeIf { inRange(it, range) }?.let { v } ?: 0 }

    private fun total(kind: ActivityKind) = events.value.count { it.kind == kind }
    private fun totalHours() = listening.value.values.sum() / 3600f

    fun badges(): List<Badge> {
        val streak = currentStreak(); val longest = longestStreak()
        val chapters = total(ActivityKind.BIBLE); val sermons = finished.value.size
        val hours = totalHours(); val live = total(ActivityKind.LIVE)
        fun on(name: String) = earned.value[name]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return listOf(
            Badge("30", "Faithful Flame", "Keep a 30-day streak", maxOf(streak, longest) / 30f, "${minOf(maxOf(streak, longest), 30)} / 30 days", on("Faithful Flame"), null),
            Badge("7", "Week Warrior", "Active 7 days in a row", longest / 7f, "${minOf(longest, 7)} / 7 days", on("Week Warrior"), null),
            Badge("B", "Berean", "Read 100 Bible chapters", chapters / 100f, "$chapters / 100", on("Berean"), ActivityKind.BIBLE),
            Badge("T", "At the Table", "Finish 25 sermons", sermons / 25f, "$sermons / 25", on("At the Table"), ActivityKind.TABLE),
            Badge("H", "Hearer", "Listen for 50 hours", hours / 50f, "${hours.toInt()}h / 50h", on("Hearer"), ActivityKind.LISTENING),
            Badge("L", "One Body", "Join 10 live services", live / 10f, "$live / 10", on("One Body"), ActivityKind.LIVE),
        )
    }

    private fun checkBadges() {
        val newly = badges().filter { it.progress >= 1f && it.earnedOn == null }
        if (newly.isEmpty()) return
        earned.value = HashMap(earned.value).apply { newly.forEach { put(it.name, today().toString()) } }
        saveData(EARNED, earned.value)
        newly.forEach { record(ActivityKind.HIGHLIGHTS, "Earned the ${it.name} badge", it.desc, "badge:${it.name}", "View") }
    }

    fun milestones(): List<Milestone> {
        val firstChapter = events.value.filter { it.kind == ActivityKind.BIBLE }.minByOrNull { it.at }?.day
        val b = badges().associateBy { it.name }
        val streak = currentStreak()
        val chapters = total(ActivityKind.BIBLE)
        val raw = listOf(
            Triple("First chapter", firstChapter != null, firstChapter?.let { monthDay(it) } ?: "Read a chapter"),
            Triple("7-day streak", b["Week Warrior"]!!.progress >= 1f, b["Week Warrior"]!!.earnedOn?.let { monthDay(it) } ?: "in ${7 - minOf(streak, 7)} days"),
            Triple("25 sermons", b["At the Table"]!!.progress >= 1f, b["At the Table"]!!.earnedOn?.let { monthDay(it) } ?: "${25 - finished.value.size} to go"),
            Triple("30-day streak", b["Faithful Flame"]!!.progress >= 1f, b["Faithful Flame"]!!.earnedOn?.let { monthDay(it) } ?: "in ${30 - minOf(streak, 30)} days"),
            Triple("100 chapters", chapters >= 100, b["Berean"]!!.earnedOn?.let { monthDay(it) } ?: "${100 - minOf(chapters, 100)} to go"),
        )
        val nextIndex = raw.indexOfFirst { !it.second }
        return raw.mapIndexed { i, (t, done, w) -> Milestone(t, w, done, i == nextIndex) }
    }

    fun monthDay(d: LocalDate): String = "${d.dayOfMonth} ${d.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}"
}

fun shortDuration(seconds: Long): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m.toString().padStart(2, '0')}m"
        m > 0 -> "${m}m"
        else -> "${seconds}s"
    }
}
