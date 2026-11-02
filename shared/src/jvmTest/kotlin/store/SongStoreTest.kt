package store

import parsers.bt.JdbcSql
import parsers.bt.openSql
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongStoreTest {
    private val file = File.createTempFile("songbook", ".db")

    init { openSql = { JdbcSql(it) } }

    @AfterTest fun clean() { file.delete() }

    @Test fun readsTheStoreFormat() {
        // the schema the essentials worker writes (src/songbook/songbook.ts)
        JdbcSql(file).apply {
            listOf(
                "CREATE TABLE book (id TEXT PRIMARY KEY, title TEXT NOT NULL, description TEXT, language TEXT NOT NULL, version TEXT NOT NULL, source TEXT)",
                "CREATE TABLE category (id TEXT PRIMARY KEY, name_en TEXT NOT NULL, name_sw TEXT NOT NULL, sort INTEGER NOT NULL)",
                "CREATE TABLE song (book_id TEXT NOT NULL, number INTEGER NOT NULL, label TEXT NOT NULL, title TEXT NOT NULL, english_title TEXT, category TEXT NOT NULL, author TEXT, year TEXT, song_url TEXT, reference TEXT, PRIMARY KEY (book_id, label))",
                "CREATE TABLE verse (book_id TEXT NOT NULL, label TEXT NOT NULL, pos INTEGER NOT NULL, type TEXT NOT NULL, n INTEGER, text TEXT NOT NULL, PRIMARY KEY (book_id, label, pos))",
                "INSERT INTO book VALUES ('only-believe', 'Only Believe', 'Songs', 'en', 'abc123', 'sv')",
                "INSERT INTO category VALUES ('unknown', 'Unknown', 'Haijulikani', 19), ('praise', 'Praise and worship', 'Sifa na kuabudu', 0)",
                "INSERT INTO song VALUES ('only-believe', 2, '2B', 'Amazing Grace', NULL, 'praise', 'John Newton', '1779', NULL, 'SSS 1'), ('only-believe', 2, '2A', 'Second', NULL, 'unknown', NULL, NULL, NULL, NULL), ('only-believe', 1, '1', 'Only Believe', NULL, 'unknown', NULL, NULL, 'https://x/0001.mp3', NULL)",
                "INSERT INTO verse VALUES ('only-believe', '1', 1, 'verse', 1, 'Fear not, little flock'), ('only-believe', '1', 0, 'chorus', NULL, 'Only believe,\nonly believe')",
            ).forEach { exec(it) }
            close()
        }
        val b = SongStore.read(file)
        assertEquals("Only Believe", b.title)
        assertEquals("abc123", b.version)
        assertEquals(listOf("1", "2A", "2B"), b.songs.map { it.label })
        val one = b.songs[0]
        assertEquals("https://x/0001.mp3", one.songUrl)
        assertEquals("Haijulikani", one.category?.sw)
        assertEquals(listOf(true, false), one.parts.map { it.chorus })
        assertEquals("Only believe,\nonly believe", one.parts[0].text)
        assertNull(one.parts[0].n)
        assertEquals("John Newton", b.songs[2].author)
        assertEquals("SSS 1", b.songs[2].reference)
        assertTrue(b.songs[2].parts.isEmpty())
    }

    @Test fun parsesTheIndex() {
        val list = SongStore.parseIndex("""[{"id":"nyimbo-za-wokovu","title":"Nyimbo za Wokovu","language":"sw","songCount":360,"hasAudio":0,"file":"songbooks/nyimbo-za-wokovu.db.gz","size":200000,"version":"v1","updated":"2026-11-02","extra":1},
            {"id":"only-believe","title":"Only Believe","language":"en","songCount":272,"hasAudio":39,"file":"songbooks/only-believe.db.gz","version":"v2"}]""")
        assertEquals(listOf("Nyimbo za Wokovu", "Only Believe"), list.map { it.title })
        assertEquals(39, list[1].hasAudio)
    }
}
