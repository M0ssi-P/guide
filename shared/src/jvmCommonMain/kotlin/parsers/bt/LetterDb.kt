package parsers.bt

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.File
import java.time.LocalDate

/** A SQLite connection; each platform opens its own (JDBC on the desktop, the system's SQLite on Android). */
interface Sql {
    fun rows(sql: String, args: List<Any?> = emptyList()): List<List<Any?>>
    fun exec(sql: String, args: List<Any?> = emptyList())
    fun <T> tx(block: () -> T): T
    fun close()
}

/** Set by each platform at start. */
lateinit var openSql: (File) -> Sql

/** Letters as JSON: the database's rows and the per-letter files on the file store. */
object LetterJson {
    private val gson = GsonBuilder().registerTypeAdapter(LocalDate::class.java, object : TypeAdapter<LocalDate?>() {
        override fun write(w: JsonWriter, v: LocalDate?) { if (v == null) w.nullValue() else w.value(v.toString()) }
        override fun read(r: JsonReader): LocalDate? = if (r.peek() == JsonToken.NULL) { r.nextNull(); null } else LocalDate.parse(r.nextString())
    }.nullSafe()).create()

    fun write(l: Letter): String = gson.toJson(l)
    fun read(json: String): Letter = gson.fromJson(json, Letter::class.java)
}

/**
 * One language's letters in SQLite, newest first. A letter goes in once, by its id: adding never overwrites or
 * duplicates; only a fuller copy (with the service page's details) replaces a plain one.
 */
class LetterDb(private val sql: Sql) {
    init {
        sql.exec("CREATE TABLE IF NOT EXISTS letters (id TEXT PRIMARY KEY, letter_date TEXT NOT NULL, code TEXT, kind TEXT, detailed INTEGER NOT NULL DEFAULT 0, search TEXT NOT NULL, json TEXT NOT NULL)")
        sql.exec("CREATE INDEX IF NOT EXISTS letters_date ON letters(letter_date DESC)")
    }

    private fun search(l: Letter) = "${l.code} ${l.title} ${l.salutation.orEmpty()} ${l.plainText}".lowercase()

    /** Adds the letters not here yet; returns how many were new. */
    fun add(letters: List<Letter>): Int = sql.tx {
        val have = ids()
        val fresh = letters.filter { it.id !in have }.distinctBy { it.id }
        fresh.forEach { l ->
            sql.exec("INSERT OR IGNORE INTO letters (id, letter_date, code, kind, detailed, search, json) VALUES (?, ?, ?, ?, ?, ?, ?)",
                listOf(l.id, l.letterDate.toString(), l.code, l.kind, if (l.detailed) 1 else 0, search(l), LetterJson.write(l)))
        }
        fresh.size
    }

    /** A copy with the service page's details replaces a plain one (never the other way round). */
    fun upgrade(l: Letter) {
        if (!l.detailed) return
        sql.exec("UPDATE letters SET detailed = 1, kind = ?, search = ?, json = ? WHERE id = ? AND detailed = 0", listOf(l.kind, search(l), LetterJson.write(l), l.id))
    }

    fun ids(): Set<String> = sql.rows("SELECT id FROM letters").mapTo(HashSet()) { it[0] as String }
    fun count(): Int = (sql.rows("SELECT COUNT(*) FROM letters").first()[0] as Number).toInt()

    /** Newest first, [limit] from [offset]. */
    fun page(offset: Int, limit: Int): List<Letter> =
        sql.rows("SELECT json FROM letters ORDER BY letter_date DESC, id LIMIT ? OFFSET ?", listOf(limit, offset)).map { LetterJson.read(it[0] as String) }

    fun byId(id: String): Letter? = sql.rows("SELECT json FROM letters WHERE id = ?", listOf(id)).firstOrNull()?.let { LetterJson.read(it[0] as String) }

    fun byCodeDate(code: String, date: LocalDate): Letter? =
        sql.rows("SELECT json FROM letters WHERE code = ? AND letter_date = ?", listOf(code, date.toString())).firstOrNull()?.let { LetterJson.read(it[0] as String) }

    /** The letters a query can match (dates, sermon, kind and the words narrowed in SQL); [Letters.search] ranks them. */
    fun candidates(q: LetterQuery): List<Letter> {
        val where = mutableListOf<String>(); val args = mutableListOf<Any?>()
        q.from?.let { where += "letter_date >= ?"; args += it.toString() }
        q.to?.let { where += "letter_date <= ?"; args += it.toString() }
        q.code?.let { where += "code = ?"; args += it }
        q.kind?.let { where += "kind = ?"; args += it }
        q.text.trim().takeIf { it.isNotEmpty() }?.let { where += "search LIKE ?"; args += "%${it.lowercase()}%" }
        val clause = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")
        return sql.rows("SELECT json FROM letters $clause ORDER BY letter_date DESC", args).map { LetterJson.read(it[0] as String) }
    }

    /** Distinct sermons and kinds, for the archive's filters. */
    fun sermons(): List<Pair<String, String>> = sql.rows("SELECT code, json FROM letters GROUP BY code ORDER BY MAX(letter_date) DESC").map { it[0] as String to LetterJson.read(it[1] as String).title }
    fun kinds(): List<String> = sql.rows("SELECT DISTINCT kind FROM letters WHERE kind IS NOT NULL").map { it[0] as String }
}
