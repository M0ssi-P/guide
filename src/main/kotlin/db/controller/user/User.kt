package db.controller.user

import db.generateId
import db.run
import db.runAndReturn
import models.IProviderStats
import java.sql.Connection

data class IImages(val id: String, val location: String)

object User {
    fun Connection.importImages(location: String) {
        val generatedId = generateId()

        run(
            """
                INSERT OR IGNORE INTO images (
                    id,
                    location
                ) VALUES (?, ?)
            """.trimIndent(),
            generatedId,
            location
        )
    }

    fun Connection.getImages(): List<IImages> {
        return runAndReturn(
            "SELECT * FROM images",
        ) { rs ->
            IImages(
                id = rs.getString("id"),
                location = rs.getString("location")
            )
        }
    }

    fun Connection.deleteImages(imageId: String) {
        run(
            """
                DELETE FROM images WHERE id = ?
            """.trimIndent(),
            imageId
        )
    }
}