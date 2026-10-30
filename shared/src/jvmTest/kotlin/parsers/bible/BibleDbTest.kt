package parsers.bible

import parsers.bible.models.ILanguage
import parsers.bible.models.Paragraph
import parsers.bible.models.VerseLineDisplay
import java.io.File
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleDbTest {
    private val file = File.createTempFile("bible", ".db")
    private val conn = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").also { c ->
        // a tiny version in the file store's schema: John 3:16-17 under a heading, with its audio and timings
        c.createStatement().use { st ->
            listOf(
                "CREATE TABLE version (id INTEGER PRIMARY KEY, abbreviation TEXT, local_abbreviation TEXT, title TEXT, local_title TEXT, language_tag TEXT, has_audio BOOLEAN, copyright TEXT, publisher TEXT)",
                "CREATE TABLE books (id TEXT PRIMARY KEY, usfm TEXT, position INTEGER, human TEXT, human_long TEXT, abbreviation TEXT, canon TEXT, has_audio BOOLEAN)",
                "CREATE TABLE chapters (id TEXT PRIMARY KEY, book_id TEXT, number INTEGER, usfm TEXT, next_usfm TEXT, prev_usfm TEXT)",
                "CREATE TABLE paragraphs (id TEXT PRIMARY KEY, chapter_id TEXT, position INTEGER, type TEXT, is_heading BOOLEAN, heading TEXT)",
                "CREATE TABLE lines (id TEXT PRIMARY KEY, paragraph_id TEXT, position INTEGER, verse INTEGER, display_number BOOLEAN, segments TEXT)",
                "CREATE TABLE audio (id TEXT PRIMARY KEY, voice_id INTEGER, chapter_id TEXT, title TEXT, is_default BOOLEAN, mp3_url TEXT, hls_url TEXT, duration_ms INTEGER)",
                "CREATE TABLE audio_timings (audio_id TEXT, chapter_id TEXT, verse INTEGER, start_ms INTEGER, end_ms INTEGER)",
                "INSERT INTO version VALUES (1, 'KJV', 'KJV', 'King James Version', NULL, 'eng', 1, 'Crown copyright', 'CUP')",
                "INSERT INTO books VALUES ('b1', 'GEN', 0, 'Genesis', 'The First Book of Moses', 'Gen', 'ot', 1), ('b2', 'JHN', 1, 'John', NULL, NULL, 'nt', 1)",
                "INSERT INTO chapters VALUES ('g1', 'b1', 1, 'GEN.1', 'GEN.2', NULL), ('g2', 'b1', 2, 'GEN.2', NULL, 'GEN.1'), ('j3', 'b2', 3, 'JHN.3', NULL, NULL)",
                "INSERT INTO paragraphs VALUES ('h', 'j3', 0, 's1', 1, 'God so loved'), ('p1', 'j3', 1, 'p', 0, NULL), ('b', 'j3', 2, 'b', 0, NULL), ('p2', 'j3', 3, 'q1', 0, NULL)",
                """INSERT INTO lines VALUES ('l1', 'p1', 0, 16, 1, '[{"content":"For God so loved the world, ","style":["Normal"],"isJesus":true},{"content":"LORD","style":["SmallCaps"],"isJesus":false}]'),
                   ('l2', 'p2', 0, 17, 1, '[{"content":"For God sent not","style":["Italic"],"isJesus":true},{"content":"","style":["Normal"],"isJesus":false}]'),
                   ('l3', 'p2', 1, 17, 0, '[{"content":"his Son","style":["Normal"],"isJesus":true}]')""",
                "INSERT INTO audio VALUES ('a1', 8, 'j3', 'The Listener''s Bible', 1, 'https://cdn/JHN/3.mp3', NULL, 294400)",
                "INSERT INTO audio_timings VALUES ('a1', 'j3', 17, 9000, 15000), ('a1', 'j3', 16, 3000, 9000)",
            ).forEach(st::execute)
        }
    }
    private val db = BibleDb { sql, args ->
        conn.prepareStatement(sql).use { st ->
            args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
            st.executeQuery().use { rs -> buildList { while (rs.next()) add((1..rs.metaData.columnCount).map { rs.getObject(it) }) } }
        }
    }
    private val eng = ILanguage(111, null, "en", "eng", "English", "English", "eng", true, true, 3, "ltr", null)

    @AfterTest fun close() { conn.close(); file.delete() }

    @Test fun readsTheVersionWithBooksAndChapters() {
        val v = db.version(eng)
        assertEquals("KJV", v.abbreviation)
        assertEquals("King James Version", v.localTitle)
        assertEquals("Crown copyright", v.copyrightLong?.text)
        assertEquals(listOf("GEN", "JHN"), v.books!!.map { it.usfm })
        assertEquals(listOf("GEN.1", "GEN.2"), v.books!![0].chapters.map { it.usfm })
        assertEquals(listOf("1", "2"), v.books!![0].chapters.map { it.human })
        assertEquals("John", v.books!![1].humanLong) // no long name: the short one
    }

    @Test fun mapsAChapter() {
        val c = db.chapter(db.version(eng), "JHN", 3)
        val sections = c.chapter!!
        assertEquals(1, sections.size)
        assertEquals("God so loved", sections[0].heading?.heading)
        val (p1, p2) = sections[0].content!!
        assertEquals(Paragraph.Normal, p1.type)
        assertEquals(Paragraph.Q1, p2.type)
        val v16 = p1.verses.single()
        assertEquals(16, v16.number)
        assertTrue(v16.content[0].isJesus)
        assertEquals(VerseLineDisplay.SmallCaps, v16.content[1].display)
        assertEquals(listOf(17, 17), p2.verses.map { it.number })
        assertEquals(listOf(true, false), p2.verses.map { it.displayNumber })
        assertEquals(1, p2.verses[0].content.size) // the empty piece is dropped
        assertEquals(VerseLineDisplay.Italic, p2.verses[0].content[0].display)
    }

    @Test fun carriesTheAudioAndVerseTimings() {
        val a = db.chapter(db.version(eng), "JHN", 3).audio!!.single()
        assertTrue(a.default)
        assertTrue(a.hasTiming)
        assertEquals("https://cdn/JHN/3.mp3", a.playOrDwnLoad.formattedMp3)
        assertEquals(listOf(16, 17), a.timings.map { it.verse })
        assertEquals(9000L, a.timings[1].startMs)
    }

    @Test fun anEmptyChapterHasNoSections() {
        assertTrue(db.chapter(db.version(eng), "GEN", 2).chapter!!.isEmpty())
        assertFalse(db.chapter(db.version(eng), "GEN", 2).audio!!.any())
    }

    @Test fun parsesTheCatalogue() {
        val langs = BibleStore.parseLanguages("""[{"id":1627,"iso63901":"sw","iso63903":"swh","name":"Swahili","localName":"Kiswahili","tag":"swh","hasAudio":true,"hasText":true,"totalVersions":13,"textDirection":"ltr","font":null},
            {"id":111,"iso63901":"en","name":"English","localName":"","tag":"eng","extra":1}]""")
        assertEquals(listOf("eng", "swh"), langs.map { it.tag })
        assertEquals("English", langs[0].localName)
        val vs = BibleStore.parseVersions("""[{"id":114,"abbreviation":"NKJV","title":"New King James Version","localTitle":"New King James Version","hasAudio":true,"key":"bible/eng/NKJV.db.gz","etag":"e1","size":9000000,"uploadedAt":"2026-10-30"},
            {"id":1,"abbreviation":"KJV","title":"King James Version","hasAudio":false,"key":"bible/eng/KJV.db.gz"}]""")
        assertEquals(listOf("KJV", "NKJV"), vs.map { it.abbreviation })
        with(BibleStore) {
            val v = vs[0].toVersion(langs[0])
            assertEquals("King James Version", v.localTitle)
            assertEquals(null, v.books)
        }
        val db = BibleStore.dbFile(File("x"), "eng", vs[1])
        assertEquals("NKJV.db", db.name)
        assertFalse(BibleStore.hasUpdate(db, vs[1]))
    }
}
