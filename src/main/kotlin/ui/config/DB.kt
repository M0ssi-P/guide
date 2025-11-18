package ui.config

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DB {
    private val connections = mutableMapOf<String, Connection>()
    private val dbDir = File(System.getProperty("user.home"), "TheGuide/db")

    init {
        dbDir.mkdirs()
    }

    @Synchronized
    fun connection(dbPath: String): Connection {
        return connections.getOrPut(dbPath) {
            val dbFile = File(dbDir, dbPath)

            val conn = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            conn.createStatement().use { stmt ->
                // Enable foreign key constraints
                stmt.execute("PRAGMA foreign_keys = ON;")
            }

            println("✅ Connected to SQLite database at: ${dbFile.absolutePath}")
            conn
        }
    }

    @Synchronized
    fun closeConnection(dbName: String) {
        connections.remove(dbName)?.let {
            try {
                it.close()
                println("🔒 Closed DB: $dbName")
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    fun closeAll() {
        connections.forEach { (name, conn) ->
            try {
                conn.close()
                println("🔒 Closed DB: $name")
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
        connections.clear()
    }
}