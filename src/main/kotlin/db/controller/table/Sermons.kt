package db.controller.table

import db.controller.table.Sermons.importLine
import db.controller.table.Sermons.importParagraphs
import db.controller.table.Sermons.importSections
import db.controller.table.Sermons.importSermon
import db.generateId
import db.run
import db.runAndReturn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import parsers.vgr.models.Line
import parsers.vgr.models.Paragraph
import parsers.vgr.models.Section
import parsers.vgr.models.Sermon
import java.sql.Connection

object Sermons {
    fun Connection.importSermon(sermon: Sermon, lang: String, getId: (id: String) -> Unit) {
        val generatedId = generateId()

        run(
            """
                INSERT OR IGNORE INTO sermons (
                    id,
                    language_id,
                    date,
                    date_code,
                    language,
                    location,
                    next_sermon_date,
                    prev_sermon_date,
                    title,
                    sort_date,
                    minutes,
                    is_cab,
                    total_sections,
                    c,
                    i,
                    ct
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            generatedId,
            lang,
            sermon.date,
            sermon.dateCode,
            sermon.language,
            sermon.location,
            sermon.nextSermonDate,
            sermon.prevSermonDate,
            sermon.title,
            sermon.sortDate,
            sermon.minutes,
            sermon.isCab,
            sermon.totalSections,
            sermon.c,
            sermon.i,
            sermon.ct
        )

        getId(generatedId)
    }

    fun Connection.RefillSermon(sermon: Sermon, id: String) {
        run(
            """
                UPDATE sermons SET
                    total_sections = ?,
                    location = ?
                WHERE id = ?
            """.trimIndent(),
            sermon.totalSections,
            sermon.location,
            id
        )
    }

    fun Connection.importSections(section: Section, sermonId: String, idMapper: (id: String) -> Unit) {
        val generatedId = generateId()

        run(
            """
                INSERT OR IGNORE INTO sections (
                    id,
                    sermon_id,
                    can_hit,
                    is_header,
                    key,
                    paragraph
                ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            generatedId,
            sermonId,
            section.canHit,
            section.isHeader,
            section.key,
            section.paragraph
        )

        idMapper(generatedId)
    }

    fun Connection.importParagraphs(paragraph: Paragraph, id: String, idMapper: (id: String) -> Unit) {
        val generatedId = generateId()

        run(
            """
                INSERT OR IGNORE INTO paragraphs (
                    id,
                    section_id,
                    hub_id,
                    number,
                    is_first,
                    text
                ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            generatedId,
            id,
            paragraph.id,
            paragraph.number,
            paragraph.isFirst,
            paragraph.text
        )

        idMapper(generatedId)
    }

    fun Connection.importLine(line: Line, id: String) {
        val generatedId = generateId()

        run(
            """
                INSERT OR IGNORE INTO lines (
                    id,
                    paragraph_id,
                    display_number,
                    number,
                    segments
                ) VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            generatedId,
            id,
            line.displayNumber,
            line.number,
            Json.encodeToString(line.segments)
        )
    }

    fun Connection.getThisDay(dateCode: String): List<Sermon> {
        val mmdd = dateCode.filter { it.isDigit() }.takeLast(4)

        return runAndReturn(
            "SELECT * FROM sermons WHERE substr(date_code, length(date_code) - 3, 4) = ?",
            mmdd
        ) { rs ->
            Sermon(
                id = null,
                stringId = rs.getString("id"),
                date = rs.getString("date"),
                dateCode = rs.getString("date_code"),
                language = rs.getString("language"),
                location = rs.getString("location"),
                nextSermonDate = rs.getString("next_sermon_date"),
                prevSermonDate = rs.getString("prev_sermon_date"),
                title = rs.getString("title"),
                sortDate = rs.getString("sort_date"),
                minutes = rs.getInt("minutes"),
                isCab = rs.getBoolean("is_cab"),
                totalSections = rs.getInt("total_sections"),
                c = rs.getInt("c"),
                i = rs.getInt("i"),
                ct = rs.getString("ct")
            )
        }
    }
}

suspend fun Connection.importSermonSuspend(
    sermon: Sermon,
    lang: String
): String = suspendCancellableCoroutine { cont ->
    importSermon(sermon, lang) { id ->
        cont.resume(id) {}
    }
}

suspend fun Connection.importSectionSuspend(
    section: Section,
    sermonId: String
): String = suspendCancellableCoroutine { cont ->
    importSections(section, sermonId) { id ->
        cont.resume(id) {}
    }
}

suspend fun Connection.importParagraphSuspend(
    paragraph: Paragraph,
    sectionId: String
): String = suspendCancellableCoroutine { cont ->
    importParagraphs(paragraph, sectionId) { id ->
        cont.resume(id) {}
    }
}

suspend fun Connection.importLineSuspend(
    line: Line,
    paragraphId: String
): Unit = suspendCancellableCoroutine { cont ->
    importLine(line, paragraphId)
    cont.resume(Unit) {}
}