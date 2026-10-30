package parsers.bible

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import parsers.bible.models.*

/**
 * One Bible version's database (from [BibleStore]) read into the app's Bible models. The platform runs the SQL:
 * [rows] gives each result row as its columns in order (numbers, strings or null), the way the Table's queries are shared.
 */
class BibleDb(private val rows: (sql: String, args: List<Any>) -> List<List<Any?>>) {

    /** The version with its books and chapters, copyright and publisher. */
    fun version(language: ILanguage): IBibleVersion {
        val v = rows("SELECT id, abbreviation, local_abbreviation, title, local_title, has_audio, copyright, publisher FROM version LIMIT 1", emptyList()).first()
        val books = rows(
            "SELECT b.usfm, b.human, b.human_long, b.abbreviation, b.canon, b.has_audio, c.number, c.usfm FROM books b " +
                "JOIN chapters c ON c.book_id = b.id ORDER BY b.position, c.number", emptyList(),
        ).groupBy { it[0] as String }.map { (usfm, r) ->
            val b = r.first()
            val human = b[1] as String
            IBibleVersion.Book(
                hasText = true, hasAudio = bool(b[5]), usfm = usfm, canon = b[4] as String, human = human,
                humanLong = (b[2] as String?) ?: human, abbreviation = (b[3] as String?) ?: human,
                chapters = r.map { IBibleVersion.Chapter(toc = true, usfm = it[7] as String, human = "${int(it[6])}", canonical = true) },
            )
        }
        val abbr = v[1] as String
        return IBibleVersion(
            id = int(v[0]), abbreviation = abbr, localAbbreviation = (v[2] as String?) ?: abbr, title = v[3] as String,
            localTitle = (v[4] as String?) ?: (v[3] as String), audio = bool(v[5]), audioCount = if (bool(v[5])) 1 else 0, text = true,
            language = language, copyrightLong = (v[6] as String?)?.let { IBibleVersion.HTMLorText(null, it) },
            publisher = (v[7] as String?)?.let { IBibleVersion.Publisher(0, it, null, null, null) }, books = books,
        )
    }

    /** A chapter ("JHN", 3): headings start sections, paragraphs keep their USFM kind, verses their styled pieces; with its audio and verse timings. */
    fun chapter(version: IBibleVersion, book: String, chapter: Int): IChapterContent {
        val usfm = "$book.$chapter"
        val sections = mutableListOf<ChapterSection>()
        var paragraph: ChapterSection.IParagraph? = null
        var at = -1
        rows(
            "SELECT p.position, p.type, p.is_heading, p.heading, l.verse, l.display_number, l.segments FROM chapters c " +
                "JOIN paragraphs p ON p.chapter_id = c.id LEFT JOIN lines l ON l.paragraph_id = p.id WHERE c.usfm = ? ORDER BY p.position, l.position",
            listOf(usfm),
        ).forEach { r ->
            if (bool(r[2])) {
                sections += ChapterSection(true, false, ChapterSection.HeadingObject(r[3] as String?, null, r[1] as String?), mutableListOf())
                return@forEach
            }
            val verse = r[4] ?: return@forEach // a blank line ("b") between stanzas
            if (int(r[0]) != at) {
                at = int(r[0])
                if (sections.isEmpty()) sections += ChapterSection(false, false, ChapterSection.HeadingObject(null, null, null), mutableListOf())
                paragraph = ChapterSection.IParagraph(paragraphKind(r[1] as String)).also { sections.last().content!!.add(it) }
            }
            paragraph!!.verses += ChapterSection.IBibleVerse(int(verse), bool(r[5]), segments(r[6] as String))
        }
        return IChapterContent(version, audio(usfm), sections)
    }

    private fun audio(usfm: String): List<ChapterAud> {
        val timings = rows(
            "SELECT t.audio_id, t.verse, t.start_ms, t.end_ms FROM audio_timings t JOIN chapters c ON c.id = t.chapter_id WHERE c.usfm = ? ORDER BY t.verse",
            listOf(usfm),
        ).groupBy({ it[0] as String }, { VerseTiming(int(it[1]), long(it[2]), long(it[3])) })
        return rows(
            "SELECT a.id, a.voice_id, a.title, a.is_default, a.mp3_url, a.hls_url FROM audio a JOIN chapters c ON c.id = a.chapter_id WHERE c.usfm = ?",
            listOf(usfm),
        ).map { a ->
            val id = a[0] as String
            val t = timings[id].orEmpty()
            ChapterAud(id.hashCode(), int(a[1]), a[2] as String, t.isNotEmpty(), false, bool(a[3]), ChapterAud.DownloadUrls(a[4] as String, (a[5] as String?) ?: ""), t)
        }
    }

    @Serializable
    private data class Segment(val content: String = "", val style: List<String> = emptyList(), val isJesus: Boolean = false)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun int(x: Any?) = (x as Number).toInt()
        private fun long(x: Any?) = (x as Number).toLong()
        private fun bool(x: Any?) = when (x) { is Number -> x.toInt() != 0; is Boolean -> x; else -> false }

        fun paragraphKind(usfm: String) = when {
            usfm == "pc" -> Paragraph.PC
            usfm.startsWith("li") -> Paragraph.LI
            usfm == "q2" -> Paragraph.Q2
            usfm.startsWith("q") -> Paragraph.Q1
            usfm == "m" || usfm == "nb" || usfm == "mi" -> Paragraph.M
            else -> Paragraph.Normal
        }

        fun segments(text: String): MutableList<ChapterSection.IBibleVerseLine> =
            json.decodeFromString<List<Segment>>(text).filter { it.content.isNotEmpty() }.mapTo(mutableListOf()) { s ->
                val display = when {
                    "SmallCaps" in s.style && "Bold" in s.style -> VerseLineDisplay.BdSmallCaps
                    "SmallCaps" in s.style -> VerseLineDisplay.SmallCaps
                    "Italic" in s.style -> VerseLineDisplay.Italic
                    else -> VerseLineDisplay.Normal
                }
                ChapterSection.IBibleVerseLine(VerseLineEnum.Word, s.isJesus, display, s.content)
            }
    }
}
