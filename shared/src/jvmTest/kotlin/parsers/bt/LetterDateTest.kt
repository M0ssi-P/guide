package parsers.bt

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LetterDateTest {
    private val oct31 = LocalDate.of(2026, 10, 31)

    @Test fun readsTheSitesLanguages() {
        listOf(
            "Saturday, October 31, 2026", "samedi 31 octobre 2026", "sábado, 31 de octubre de 2026", "sábado, 31 de outubro de 2026",
            "Samstag, 31. Oktober 2026", "sabato 31 ottobre 2026", "lørdag 31. oktober 2026", "sâmbătă, 31 octombrie 2026",
            "суббота, 31 октября 2026", "субота, 31 жовтня 2026",
        ).forEach { assertEquals(oct31, Letters.parseDate(it), it) }
        assertEquals(LocalDate.of(2026, 10, 25), Letters.parseDate("25 oct. 2026 60-0515E Adoption #1"))
    }

    @Test fun checksFromSaturday6pmSydneyUntilTheLetterIsIn() {
        val sydney = java.time.ZoneId.of("Australia/Sydney")
        val sat7pm = java.time.ZonedDateTime.of(2026, 10, 31, 19, 0, 0, 0, sydney)
        val prevSat = LocalDate.of(2026, 10, 24)
        assertEquals(true, Letters.due(prevSat, sat7pm))                   // Saturday 7pm Sydney, last week's letter: check
        assertEquals(false, Letters.due(oct31, sat7pm))                    // this week's is in: stop
        assertEquals(false, Letters.due(oct31, sat7pm.plusDays(4)))        // Wednesday after: still nothing new
        assertEquals(true, Letters.due(null, sat7pm.plusDays(4)))          // nothing here yet: check
        assertEquals(false, Letters.due(prevSat, sat7pm.withHour(17)))     // Saturday 5pm Sydney: too early
        assertEquals(true, Letters.due(prevSat, sat7pm.withHour(18)))      // 6pm: check
        assertEquals(true, Letters.due(prevSat, sat7pm.plusDays(1)))       // Sunday and it still hasn't come: pick it up late
    }
}
