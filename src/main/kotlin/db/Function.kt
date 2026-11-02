package db

import disassemble
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

fun loadSql(resource: String, forDb: String = "tb"): String =
    object {}.javaClass.getResource("/sql/$forDb/$resource")!!.readText()

fun generateId(): String = UUID.randomUUID().toString()

// one connection is shared by the whole app (DB.connection): each use takes it in turn, so nothing reads it while
// another thread changes the schema or swaps a language
fun Connection.run(sql: String, vararg params: Any?) = synchronized(this) {
    this.prepareStatement(sql).use {
        params.forEachIndexed { index, param ->
            it.setObject(index + 1, param)
        }
        it.executeUpdate()
    }
    Unit
}

fun <T> Connection.runAndReturn(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): List<T> = synchronized(this) {
    this.prepareStatement(sql).use { stmt ->
        params.forEachIndexed { index, param ->
            stmt.setObject( index + 1, param)
        }
        stmt.executeQuery().use { rs ->
            val results = mutableListOf<T>()
            while (rs.next()) {
                results.add(mapper(rs))
            }
            results
        }
    }
}

fun Connection.checkIfOneExist(sql: String, vararg params: Any?): Boolean = synchronized(this) {
    this.prepareStatement(sql).use { stmt ->
        params.forEachIndexed { index, param ->
            stmt.setObject(index + 1, param)
        }
        stmt.executeQuery().use { it.next() }
    }
}

fun Connection.checkIfEmpty(tableName: String): Boolean = synchronized(this) {
    val sql = "SELECT COUNT(*) FROM $tableName"
    this.prepareStatement(sql).use { stmt ->
        stmt.executeQuery().use { if (it.next()) it.getInt(1) == 0 else true }
    }
}