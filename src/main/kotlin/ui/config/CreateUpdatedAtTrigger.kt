package ui.config

import java.sql.Connection

fun createUpdatedAtTrigger(conn: Connection) {
    conn.createStatement().use { stmt ->
        val sql = listOf<String>(
            """
                CREATE TRIGGER IF NOT EXISTS update_languages_updated_at
                AFTER UPDATE ON languages
                BEGIN
                    UPDATE languages SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
                END;
            """.trimIndent(),
            """
                CREATE TRIGGER IF NOT EXISTS update_sermons_updated_at
            AFTER UPDATE ON sermons
            BEGIN
                UPDATE sermons SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
            END;
            """.trimIndent(),
            """
                CREATE TRIGGER IF NOT EXISTS update_sections_updated_at
            AFTER UPDATE ON sections
            BEGIN
                UPDATE sections SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
            END;
            """.trimIndent(),
            """
                CREATE TRIGGER IF NOT EXISTS update_paragraphs_updated_at
            AFTER UPDATE ON paragraphs
            BEGIN
                UPDATE paragraphs SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
            END;
            """.trimIndent(),
            """
              CREATE TRIGGER IF NOT EXISTS update_lines_updated_at
            AFTER UPDATE ON lines
                BEGIN
                UPDATE lines SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
            END;  
            """.trimIndent()
        )

        sql.forEach {
            stmt.execute(it)
        }
    }
}