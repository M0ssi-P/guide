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

fun startSongBookStatement(conn: Connection) {
    conn.createStatement().use { stmt ->
        val sql = listOf<String>(
            loadSql(resource = "books.sql", forDb = "songbooks"),
            loadSql(resource = "songs.sql", forDb = "songbooks"),
            loadSql(resource = "lyrics.sql", forDb = "songbooks"),
            loadSql(resource = "favourites.sql", forDb = "songbooks")
        )

        sql.forEach {
            stmt.execute(it)
        }
    }
}