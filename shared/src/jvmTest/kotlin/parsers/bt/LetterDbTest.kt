package parsers.bt

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import saveData
import storeDir
import java.io.File
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LetterDbTest {
    private val dir = File.createTempFile("letters", "").apply { delete(); mkdirs() }

    init { openSql = { JdbcSql(it) } }

    @AfterTest fun clean() { dir.deleteRecursively() }

    private fun letter(id: Int, day: Int, text: String = "Dear Faithful", detailed: Boolean = false) = Letter(
        id = "00000000-0000-0000-0000-%012d".format(id), letterDate = LocalDate.of(2026, 10, day), serviceDate = LocalDate.of(2026, 10, day + 1),
        code = "60-0515E", title = "Adoption #1", salutation = "Dear Faithful in Christ Jesus,",
        sections = listOf(LetterSection(LetterPart.LETTER, listOf(LetterBlock(text), LetterBlock("He gave man to be an amateur god.", isQuote = true, cite = "65-0418")))),
        kind = "BT", related = listOf(RelatedService("r1", "Sunday", LocalDate.of(2026, 10, day + 1), "BT")), detailed = detailed,
    )

    @Test fun addsByIdOnlyAndNeverOverwrites() {
        val db = LetterDb(JdbcSql(File(dir, "en.db")))
        assertEquals(2, db.add(listOf(letter(1, 3), letter(2, 10))))
        assertEquals(0, db.add(listOf(letter(1, 3, "changed"), letter(1, 3))))
        assertEquals("Dear Faithful", db.byId(letter(1, 3).id)!!.letterText)
        assertEquals(2, db.count())
    }

    @Test fun aFullerCopyReplacesAPlainOneOnly() {
        val db = LetterDb(JdbcSql(File(dir, "en.db")))
        db.add(listOf(letter(1, 3)))
        db.upgrade(letter(1, 3, "with details", detailed = true))
        assertEquals("with details", db.byId(letter(1, 3).id)!!.letterText)
        db.upgrade(letter(1, 3, "again", detailed = true))
        assertEquals("with details", db.byId(letter(1, 3).id)!!.letterText)
    }

    @Test fun pagesNewestFirstAndSearches() {
        val db = LetterDb(JdbcSql(File(dir, "en.db")))
        db.add((1..25).map { letter(it, it, if (it == 7) "the Bride is ready" else "Dear Faithful") })
        assertEquals((25 downTo 16).toList(), db.page(0, 10).map { it.letterDate.dayOfMonth })
        assertEquals((15 downTo 6).toList(), db.page(10, 10).map { it.letterDate.dayOfMonth })
        assertEquals(listOf(7), db.candidates(LetterQuery("bride")).map { it.letterDate.dayOfMonth })
        assertEquals(5, db.candidates(LetterQuery(from = LocalDate.of(2026, 10, 21))).size)
        assertEquals(listOf("60-0515E" to "Adoption #1"), db.sermons())
    }

    @Test fun letterJsonRoundTrips() {
        val l = letter(1, 3, detailed = true)
        assertEquals(l, LetterJson.read(LetterJson.write(l)))
    }

    @Test fun movesSavedLettersIntoTheDatabase() = runBlocking {
        storeDir = dir
        saveData("bt_letters_xx", arrayListOf(letter(1, 3), letter(2, 4)))
        val book = LetterBook("xx")
        val shown = withTimeout(5000) { book.all.first { it.isNotEmpty() } }
        assertEquals(listOf(4, 3), shown.map { it.letterDate.dayOfMonth })
        assertTrue(File(dir, "bt_letters_xx.bak").exists())
        assertNull(File(dir, "bt_letters_xx").takeIf { it.exists() })
    }
}
