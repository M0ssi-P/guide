package db.controller.table

import db.controller.table.Languages.importLanguage
import db.controller.table.Sermons.importSermon
import db.generateId
import db.run
import db.runAndReturn
import kotlinx.coroutines.suspendCancellableCoroutine
import parsers.bible.models.ILanguage
import parsers.vgr.models.Sermon
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
            if (rs.next()) {
                id = rs.getString("id")
            }
        }

        return id
    }
}

suspend fun Connection.importLanguageSuspand(
    languageData: ILanguage
): Unit = suspendCancellableCoroutine { cont ->
    importLanguage(languageData)
    cont.resume(value = Unit) {}
}