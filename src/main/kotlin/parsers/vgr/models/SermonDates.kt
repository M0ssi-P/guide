package parsers.vgr.models

import ui.toOrdinal
import java.time.Month

fun Sermon.getDdMmYy(): String {
    this.dateCode.split("-").let {
        var monInt = it[1].filter { a -> a.isDigit() }.take(2).toInt()
        val day = it[1].filter { a -> a.isDigit() }.takeLast(2).toInt().toOrdinal()
        val mon = Month.of(monInt).getDisplayName(java.time.format.TextStyle.SHORT,java.util.Locale.ENGLISH)
        val year = "19${it[0]}"

        return "$day $mon $year"
    }
}