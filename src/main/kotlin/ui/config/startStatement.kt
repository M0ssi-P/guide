package ui.config

import db.checkIfOneExist
import db.loadSql
import java.sql.Connection

fun startTableStatement(conn: Connection) {
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

        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sermons_date_code ON sermons(date_code)")
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sections_sermon_id ON sections(sermon_id)")
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_paragraphs_section_id ON paragraphs(section_id)")
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_lines_paragraph_id ON lines(paragraph_id)")
        stmt.execute("ANALYZE")

        stmt.execute("DROP TABLE IF EXISTS lines_fts")

        stmt.execute("""
            CREATE VIRTUAL TABLE IF NOT EXISTS paragraphs_fts USING fts5(
                text,
                content='paragraphs',
                content_rowid='id'
            )
        """.trimIndent())

        val ftsIsEmpty = stmt.executeQuery("SELECT NOT EXISTS (SELECT 1 FROM paragraphs_fts LIMIT 1)")
            .use { it.next(); it.getBoolean(1) }
        val paragraphsHasData = conn.checkIfOneExist("SELECT 1 FROM paragraphs LIMIT 1")

        if (ftsIsEmpty && paragraphsHasData) {
            stmt.execute("INSERT INTO paragraphs_fts(paragraphs_fts) VALUES('rebuild')")
        }

        stmt.execute("""
            CREATE TRIGGER IF NOT EXISTS paragraphs_ai AFTER INSERT ON paragraphs BEGIN
                INSERT INTO paragraphs_fts(rowid, text) VALUES (new.id, new.text);
            END
        """.trimIndent())

        stmt.execute("""
            CREATE TRIGGER IF NOT EXISTS paragraphs_ad AFTER DELETE ON paragraphs BEGIN
                INSERT INTO paragraphs_fts(paragraphs_fts, rowid, text) VALUES ('delete', old.id, old.text);
            END
        """.trimIndent())

        stmt.execute("""
            CREATE TRIGGER IF NOT EXISTS paragraphs_au AFTER UPDATE ON paragraphs BEGIN
                INSERT INTO paragraphs_fts(paragraphs_fts, rowid, text) VALUES ('delete', old.id, old.text);
                INSERT INTO paragraphs_fts(rowid, text) VALUES (new.id, new.text);
            END
        """.trimIndent())
    }
}