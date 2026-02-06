package db.controller.table

import db.controller.table.Languages.importLanguage
import db.generateId
import db.run
import db.runAndReturn
import kotlinx.coroutines.suspendCancellableCoroutine
import parsers.bible.models.ILanguage
import java.sql.Connection

object Languages {
    fun Connection.importLanguage(languageData: ILanguage) {
        val generatedId = generateId()

        run(
            """
                INSERT INTO languages (
                    id,
                    lang_id,
                    iso_63901,
                    iso_63903,
                    name,
                    local_name,
                    tag
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            generatedId,
            languageData.id,
            languageData.iso63901,
            languageData.iso63903,
            languageData.name,
            languageData.localName,
            languageData.tag
        )
    }

    fun Connection.getLanguageDbID(languageCode: String): String? {
        var id: String? = null;

        runAndReturn(
            """
                SELECT * FROM languages WHERE tag = ? LIMIT 1
            """.trimIndent(),
            languageCode
        ) { rs ->
            id = rs.getString("id")
        }

        return id
    }

    fun Connection.getAllLanguages(): List<ILanguage> {
        return runAndReturn(
            """
                SELECT * FROM languages
            """.trimIndent()
        ) { rs ->
            ILanguage(
                id = null,
                dbId = rs.getString("id"),
                iso63901 = rs.getString("iso_63901"),
                iso63903 = rs.getString("iso_63903"),
                name = rs.getString("name"),
                localName = rs.getString("local_name"),
                tag = rs.getString("tag"),
                hasAudio = rs.getBoolean("has_audio"),
                hasText = rs.getBoolean("has_text"),
                totalVersions = rs.getInt("total_versions"),
                textDirection = rs.getString("text_direction"),
                font = null
            )
        }
    }
}

suspend fun Connection.importLanguageSuspand(
    languageData: ILanguage
): Unit = suspendCancellableCoroutine { cont ->
    importLanguage(languageData)
    cont.resume(value = Unit) {}
}