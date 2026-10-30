package parsers.vgr

import kotlinx.serialization.json.Json
import parsers.vgr.models.Line
import parsers.vgr.models.Paragraph
import parsers.vgr.models.Section
import parsers.vgr.models.Segment

/**
 * A sermon's whole text in one query over a Table database (sections → paragraphs → lines), read the same way on the
 * desktop (JDBC) and Android (SQLite cursor). The one argument is the sermon's id.
 */
const val FULL_SERMON_SQL = """
SELECT
    s.id AS sec_id, s.can_hit AS sec_can_hit, s.is_header AS sec_is_header, s.key AS sec_key, s.paragraph AS sec_paragraph, s.created_at AS sec_created_at,
    p.id AS para_id, p.hub_id AS para_hub_id, p.number AS para_number, p.text AS para_text, p.is_first AS para_is_first, p.created_at AS para_created_at,
    l.display_number AS line_display_number, l.number AS line_number, l.segments AS line_segments, l.created_at AS line_created_at
FROM sections s
LEFT JOIN paragraphs p ON p.section_id = s.id
LEFT JOIN lines l ON l.paragraph_id = p.id
WHERE s.sermon_id = ?
ORDER BY s.rowid ASC, p.rowid ASC, l.rowid ASC
"""

/** One row of [FULL_SERMON_SQL]. */
data class SermonRow(
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
    val lineDisplayNumber: Boolean?,
    val lineNumber: Int?,
    val lineSegmentsJson: String?,
)

/** The flat rows grouped into the nested sections → paragraphs → lines, in their order. */
fun groupSermonRows(rows: List<SermonRow>): List<Section> = rows
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
