package db

import disassemble
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

fun loadSql(resource: String, forDb: String = "tb"): String =
    object {}.javaClass.getResource("/sql/$forDb/$resource")!!.readText()

fun generateId(): String = UUID.randomUUID().toString()

fun Connection.run(sql: String, vararg params: Any?) {
    this.prepareStatement(sql).use {
        params.forEachIndexed { index, param ->
            it.setObject(index + 1, param)
        }
        it.executeUpdate()
    }
}

fun <T> Connection.runAndReturn(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): List<T> {
    this.prepareStatement(sql).use { stmt ->
        params.forEachIndexed { index, param ->
            stmt.setObject( index + 1, param)
        }
        stmt.executeQuery().use { rs ->
            val results = mutableListOf<T>()
            while (rs.next()) {
                results.add(mapper(rs))
            }
            return results
        }
    }
}

fun Connection.checkIfOneExist(sql: String, vararg params: Any?): Boolean {
    this.prepareStatement(sql).use { stmt ->
        params.forEachIndexed { index, param ->
            stmt.setObject(index + 1, param)
        }
        stmt.executeQuery().use {
            return it.next()
        }
    }
}

fun Connection.checkIfEmpty(tableName: String): Boolean {
    val sql = "SELECT COUNT(*) FROM $tableName"
    this.prepareStatement(sql).use { stmt ->
        stmt.executeQuery().use {
            if(it.next()) return it.getInt(1) == 0
        }
    }

    return true
}