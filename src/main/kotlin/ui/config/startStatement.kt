package ui.config

import db.checkIfOneExist
import db.loadSql
import java.sql.Connection

fun startTableStatement(conn: Connection) = synchronized(conn) {
    conn.createStatement().use { stmt ->
        val sql = listOf(
            loadSql(resource = "languages.sql"),
            loadSql("tables.sql"),
            loadSql("sermons.sql"),
            loadSql("sections.sql"),
            loadSql("paragraphs.sql"),
            loadSql("lines.sql"),
            loadSql("images.sql", forDb = "user"),
            loadSql(resource = "books.sql", forDb = "songbooks"),
            loadSql(resource = "songs.sql", forDb = "songbooks"),
            loadSql(resource = "lyrics.sql", forDb = "songbooks"),
            loadSql(resource = "favourites.sql", forDb = "songbooks")
        )

        sql.forEach {
            stmt.execute(it)
        }

        // the song store's details, on libraries made before them
        val songColumns = conn.createStatement().use { s -> s.executeQuery("PRAGMA table_info(songs)").use { rs -> buildSet { while (rs.next()) add(rs.getString("name")) } } }
        listOf("english_title", "category", "author", "year", "song_url", "label").filter { it !in songColumns }.forEach { stmt.execute("ALTER TABLE songs ADD COLUMN $it TEXT") }

        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sermons_date_code ON sermons(date_code)")
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sections_sermon_id ON sections(sermon_id)")
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_paragraphs_section_id ON paragraphs(section_id)")
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_lines_paragraph_id ON lines(paragraph_id)")
        stmt.execute("ANALYZE")

        stmt.execute("DROP TRIGGER IF EXISTS paragraphs_ai")
        stmt.execute("DROP TRIGGER IF EXISTS paragraphs_ad")
        stmt.execute("DROP TRIGGER IF EXISTS paragraphs_au")
        stmt.execute("DROP TABLE IF EXISTS paragraphs_fts")

        stmt.execute("""
            CREATE VIRTUAL TABLE IF NOT EXISTS paragraphs_fts USING fts5(
                text, content='paragraphs', content_rowid='rowid'
            )
        """.trimIndent())

        stmt.execute("""
            CREATE TRIGGER IF NOT EXISTS paragraphs_ai AFTER INSERT ON paragraphs BEGIN
                INSERT INTO paragraphs_fts(rowid, text) VALUES (new.rowid, new.text);
            END
        """.trimIndent())

        stmt.execute("""
            CREATE TRIGGER IF NOT EXISTS paragraphs_ad AFTER DELETE ON paragraphs BEGIN
                INSERT INTO paragraphs_fts(paragraphs_fts, rowid, text) VALUES ('delete', old.rowid, old.text);
            END
        """.trimIndent())

        stmt.execute("""
            CREATE TRIGGER IF NOT EXISTS paragraphs_au AFTER UPDATE ON paragraphs BEGIN
                INSERT INTO paragraphs_fts(paragraphs_fts, rowid, text) VALUES ('delete', old.rowid, old.text);
                INSERT INTO paragraphs_fts(rowid, text) VALUES (new.rowid, new.text);
            END
        """.trimIndent())
    }
}