package org.theguide.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import loadData
import parsers.vgr.Table
import parsers.vgr.models.Calendar
import saveData
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The quote and verse for one day, from branham.org's quote of the day. */
data class DayQuote(
    val date: String, val text: String, val title: String, val code: String, val ref: String,
    val verse: String, val verseRef: String, val paragraph: Int?,
) : java.io.Serializable

/** Quotes of the day for this month, fetched once per day and kept for offline. */
object Daily {
    private val table by lazy { Table() }
    val quotes = mutableStateMapOf<String, DayQuote>().apply { putAll(loadData<HashMap<String, DayQuote>>("daily_quotes") ?: emptyMap()) }
    /** Days of this month that have a quote. */
    var days by mutableStateOf<List<Calendar.ActiveDay>>(emptyList()); private set

    private fun month(m: Int) = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    suspend fun load(date: LocalDate): DayQuote? = withContext(Dispatchers.IO) {
        quotes[date.toString()]?.let { return@withContext it }
        runCatching {
            if (days.isEmpty()) days = table.getCalendar(current = true).activeDays
            // today's quote goes up during the US day; until then the latest one stands for today (as on the desktop)
            val day = days.firstOrNull { it.day == date.dayOfMonth }
                ?: (if (date == LocalDate.now()) days.filter { it.day <= date.dayOfMonth }.maxByOrNull { it.day } else null)
                ?: return@withContext null
            val q = table.qotd(day)
            val t = q.TheTable
            val text = t.text.trim().replace(Regex("\\s+"), " ")
            val para = Regex("^(\\d+)\\s").find(text)?.groupValues?.get(1)?.toIntOrNull()
            val verses = q.verseNumbers.let { v -> if (v.size > 1) "${v.first()}-${v.last()}" else v.firstOrNull()?.toString() ?: "" }
            DayQuote(
                date.toString(), text.replace(Regex("^\\d+\\s+"), ""), t.title, t.code,
                "${t.title}, ${month(t.month)} ${t.day}, ${t.year}${if (para != null) ", paragraph $para" else ""}",
                q.content.trim(), "${q.bookHuman} ${q.chapter}:$verses", para,
            ).also { if (day.day == date.dayOfMonth) { quotes[date.toString()] = it; saveData("daily_quotes", HashMap(quotes)) } }
        }.onFailure { it.printStackTrace() }.getOrNull()
    }
}
