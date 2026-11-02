package parsers.bt

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/** [Sql] over JDBC (the desktop, the weekly letters job and the tests). */
class JdbcSql(file: File) : Sql {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")

    @Synchronized override fun rows(sql: String, args: List<Any?>): List<List<Any?>> = conn.prepareStatement(sql).use { st ->
        args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
        st.executeQuery().use { rs -> buildList { while (rs.next()) add((1..rs.metaData.columnCount).map { rs.getObject(it) }) } }
    }

    @Synchronized override fun exec(sql: String, args: List<Any?>) {
        conn.prepareStatement(sql).use { st -> args.forEachIndexed { i, a -> st.setObject(i + 1, a) }; st.executeUpdate() }
    }

    @Synchronized override fun <T> tx(block: () -> T): T {
        conn.autoCommit = false
        try { return block().also { conn.commit() } } catch (e: Throwable) { conn.rollback(); throw e } finally { conn.autoCommit = true }
    }

    @Synchronized override fun close() = conn.close()
}
