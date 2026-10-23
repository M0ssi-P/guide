package db.controller.table

import com.google.gson.Gson
import db.controller.table.Sermons.importLine
import db.controller.table.Sermons.importParagraphs
import db.controller.table.Sermons.importSections
import db.generateId
import db.run
import db.runAndReturn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import parsers.bible.models.ILanguage
import parsers.vgr.models.Line
import parsers.vgr.models.Paragraph
import parsers.vgr.models.Section
import parsers.vgr.models.Segment
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

    fun Connection.getFullSermon(sermon: Sermon): Sermon {
        val query = """
        SELECT 
            s.id AS sec_id, s.can_hit AS sec_can_hit, s.is_header AS sec_is_header, s.key AS sec_key, s.paragraph AS sec_paragraph, s.created_at AS sec_created_at,
            p.id AS para_id, p.hub_id AS para_hub_id, p.number AS para_number, p.text AS para_text, p.is_first AS para_is_first, p.created_at AS para_created_at,
            l.display_number AS line_display_number, l.number AS line_number, l.segments AS line_segments, l.created_at AS line_created_at
        FROM sections s
        LEFT JOIN paragraphs p ON p.section_id = s.id
        LEFT JOIN lines l ON l.paragraph_id = p.id
        WHERE s.sermon_id = ?
        ORDER BY s.rowid ASC, p.rowid ASC, l.rowid ASC
        """.trimIndent()

        // 1. Fetch all rows into a flat list of data holders
        val rows = mutableListOf<FlatSermonRow>()
        prepareStatement(query).use { stmt ->
            stmt.setString(1, sermon.stringId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    rows.add(
                        FlatSermonRow(
                            sectionId = rs.getString("sec_id"),
                            sectionCanHit = rs.getBoolean("sec_can_hit"),
                            sectionIsHeader = rs.getBoolean("sec_is_header"),
                            sectionKey = rs.getString("sec_key"),
                            sectionParagraph = rs.getString("sec_paragraph"),

                            paragraphId = rs.getString("para_id"),
                            paragraphHubId = rs.getObject("para_hub_id")?.let { rs.getInt("para_hub_id") },
                            paragraphNumber = rs.getObject("para_number")?.let { rs.getInt("para_number") },
                            paragraphText = rs.getString("para_text"),
                            paragraphIsFirst = rs.getBoolean("para_is_first"),
//                            paragraphIsQuestion = rs.getBoolean("para_is_question"),

                            lineDisplayNumber = rs.getObject("line_display_number")?.let { rs.getBoolean("line_display_number") },
                            lineNumber = rs.getObject("line_number")?.let { rs.getInt("line_number") },
                            lineSegmentsJson = rs.getString("line_segments")
                        )
                    )
                }
            }
        }

        // 2. Group flat rows into the nested domain model in memory
        val sections = rows
            .groupBy { it.sectionId }
            .map { (_, sectionRows) ->
                val firstSec = sectionRows.first()

                val paragraphs = sectionRows
                    .filter { it.paragraphId != null }
                    .groupBy { it.paragraphId!! }
                    .map { (_, paraRows) ->
                        val firstPara = paraRows.first()

                        val lines = paraRows
                            .filter { it.lineNumber != null }
                            .map { lineRow ->
                                Line(
                                    displayNumber = lineRow.lineDisplayNumber ?: false,
                                    number = lineRow.lineNumber ?: 0,
                                    segments = lineRow.lineSegmentsJson?.let {
                                        Json.decodeFromString<List<Segment>>(it).toMutableList()
                                    } ?: mutableListOf()
                                )
                            }

                        Paragraph(
                            id = (firstPara.paragraphHubId ?: 0).toLong(),
                            dbId = firstPara.paragraphId!!,
                            number = firstPara.paragraphNumber ?: 0,
                            text = firstPara.paragraphText ?: "",
                            isFirst = firstPara.paragraphIsFirst,
//                            isQuestion = firstPara.paragraphIsQuestion,
                            content = lines.toMutableList()
                        )
                    }

                Section(
                    dbId = firstSec.sectionId,
                    canHit = firstSec.sectionCanHit,
                    isHeader = firstSec.sectionIsHeader,
                    key = firstSec.sectionKey ?: "",
                    paragraph = firstSec.sectionParagraph ?: "",
                    content = paragraphs.toMutableList()
                )
            }

        return sermon.copy(sections = sections)
    }

    // Temporary data holder for the flat query results
    private data class FlatSermonRow(
        val sectionId: String,
        val sectionCanHit: Boolean,
        val sectionIsHeader: Boolean,
        val sectionKey: String?,
        val sectionParagraph: String?,
        val paragraphId: String?,
        val paragraphHubId: Int?,
        val paragraphNumber: Int?,
        val paragraphText: String?,
        val paragraphIsFirst: Boolean,
//        val paragraphIsQuestion: Boolean,
        val lineDisplayNumber: Boolean?,
        val lineNumber: Int?,
        val lineSegmentsJson: String?
    )

    fun Connection.getSermonsList(landId: String) : List<Sermon> {
        return runAndReturn(
            """
                SELECT * FROM sermons WHERE language_id = ? ORDER BY created_at ASC
            """.trimIndent(),
            landId
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
                hasSubtitle = rs.getBoolean("has_subtitle"),
                isCab = rs.getBoolean("is_cab"),
                totalSections = rs.getInt("total_sections"),
                c = rs.getInt("c"),
                i = rs.getInt("i"),
                ct = rs.getString("ct")
            )
        }
    }

    fun Connection.getThisDay(dateCode: String, langID: String): List<Sermon> {
        val mmdd = dateCode.filter { it.isDigit() }.takeLast(4)

        return runAndReturn(
            """
                SELECT * FROM sermons WHERE substr(date_code, length(date_code) - 4, 4) = ? AND language_id = ?
            """.trimIndent(),
            mmdd,
            langID
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
                hasSubtitle = rs.getBoolean("has_subtitle"),
                isCab = rs.getBoolean("is_cab"),
                totalSections = rs.getInt("total_sections"),
                c = rs.getInt("c"),
                i = rs.getInt("i"),
                ct = rs.getString("ct")
            )
        }
    }
}