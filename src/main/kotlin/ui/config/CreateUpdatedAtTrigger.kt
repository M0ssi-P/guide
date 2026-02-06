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

fun songBooksCreateUpdatedAtTrigger(conn: Connection) {
    conn.createStatement().use { stmt ->
        val sql = listOf<String>(
            """
                CREATE TRIGGER IF NOT EXISTS update_books_updated_at
                AFTER UPDATE ON books
                BEGIN
                    UPDATE books SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
                END;
            """.trimIndent(),
            """
                CREATE TRIGGER IF NOT EXISTS update_songs_updated_at
            AFTER UPDATE ON songs
            BEGIN
                UPDATE songs SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
            END;
            """.trimIndent(),
            """
                CREATE TRIGGER IF NOT EXISTS update_lyrics_updated_at
            AFTER UPDATE ON lyrics
            BEGIN
                UPDATE lyrics SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
            END;
            """.trimIndent(),
            """
                CREATE TRIGGER IF NOT EXISTS update_favourites_updated_at
            AFTER UPDATE ON favourites
            BEGIN
                UPDATE favourites SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
            END;
            """.trimIndent()
        )

        sql.forEach {
            stmt.execute(it)
        }
    }
}