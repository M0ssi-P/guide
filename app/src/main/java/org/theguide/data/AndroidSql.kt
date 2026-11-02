package org.theguide.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import parsers.bt.Sql
import java.io.File

/** [parsers.bt.Sql] over the system's SQLite. */
class AndroidSql(file: File) : Sql {
    private val db = SQLiteDatabase.openOrCreateDatabase(file, null)

    override fun rows(sql: String, args: List<Any?>): List<List<Any?>> = db.rawQuery(sql, args.map { it?.toString() }.toTypedArray()).use { c ->
        buildList {
            while (c.moveToNext()) add((0 until c.columnCount).map { i ->
                when (c.getType(i)) { Cursor.FIELD_TYPE_INTEGER -> c.getLong(i); Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i); Cursor.FIELD_TYPE_NULL -> null; else -> c.getString(i) }
            })
        }
    }

    override fun exec(sql: String, args: List<Any?>) = db.execSQL(sql, args.toTypedArray())

    override fun <T> tx(block: () -> T): T {
        db.beginTransaction()
        try { return block().also { db.setTransactionSuccessful() } } finally { db.endTransaction() }
    }

    override fun close() = db.close()
}
