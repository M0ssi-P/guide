package ui.config

import db.loadSql
import java.sql.Connection

fun startTableStatement(conn: Connection) {
    conn.createStatement().use { stmt ->
        val sql = listOf<String>(
            loadSql(resource = "languages.sql"),
            loadSql("sermons.sql"),
            loadSql("sections.sql"),
            loadSql("paragraphs.sql"),
            loadSql("lines.sql")
        )

        sql.forEach {
            stmt.execute(it)
        }
    }
}