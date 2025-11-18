package parsers.vgr.models

import ui.toOrdinal
import java.time.Month

data class Sermon(
    val id: Long?,
    val stringId: String = "",
    val date: String,
    val dateCode: String,
    val language: String,
    val location: String,
    val nextSermonDate: String = "",
    val prevSermonDate: String = "",
    val title: String,
    val sortDate: String,
    val minutes: Int? = null,
    val isCab: Boolean = false,
    val sections: List<Section>? = null,
    val totalSections: Int? = null,
    val c: Int,
    val i: Int,
    val ct: String,
    val updatedAt: Long? = null,
)

fun Sermon.getDdMmYy(): String {
    this.dateCode.split("-").let {
        var monInt = it[1].filter { a -> a.isDigit() }.take(2).toInt()
        val day = it[1].filter { a -> a.isDigit() }.takeLast(2).toInt().toOrdinal()
        val mon = Month.of(monInt).getDisplayName(java.time.format.TextStyle.SHORT,java.util.Locale.ENGLISH)
        val year = "19${it[0]}"

        return "$day $mon $year"
    }
}