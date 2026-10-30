package org.theguide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import loadData
import org.theguide.App
import parsers.bible.BibleDb
import parsers.bible.BibleStore
import parsers.bible.models.IBibleVersion
import parsers.bible.models.IChapterContent
import parsers.bible.models.ILanguage
import parsers.bible.models.VerseLineEnum
import saveData

/** One verse: its number and its words. */
data class Verse(val n: Int, val text: String) : java.io.Serializable

/**
 * The Bibles on the file store (R2): every version of every language it lists, each one database downloaded once
 * ("Get it") and read on the device through the shared [BibleDb].
 */
object BibleRepo {
    /** What the store lists, by version id: language and catalogue entry (kept so the list opens offline). */
    private val entries: HashMap<Int, Pair<ILanguage, BibleStore.Entry>> = loadData("bible_r2_entries") ?: HashMap()
    private var fetched = false
    private val dbs = mutableMapOf<String, SQLiteDatabase>()

    /** Every listed version, by language then abbreviation; those on the device carry their books. */
    var versions by mutableStateOf(listed()); private set
    /** 0..1 while a version downloads, by id. */
    val downloading = mutableStateMapOf<Int, Float>()
    /** Versions whose last download failed. */
    val failed = mutableStateMapOf<Int, Boolean>()

    /** Fetches the store's lists once per run (the last ones stay when offline). */
    suspend fun loadVersions() = withContext(Dispatchers.IO) {
        if (fetched) return@withContext
        runCatching {
            val found = HashMap<Int, Pair<ILanguage, BibleStore.Entry>>()
            BibleStore.languages().forEach { l -> BibleStore.versions(l.tag).forEach { found[it.id] = l to it } }
            entries.clear(); entries.putAll(found)
            saveData("bible_r2_entries", entries)
            fetched = true
        }
        versions = listed()
    }

    private fun listed() = entries.values.sortedWith(compareBy({ it.first.name }, { it.second.abbreviation })).map { (l, e) ->
        db(e.id)?.let { runCatching { it.version(l) }.getOrNull() } ?: with(BibleStore) { e.toVersion(l) }
    }

    private fun file(id: Int) = entries[id]?.let { (l, e) -> BibleStore.dbFile(App.context.filesDir, l.tag, e) }

    private fun db(id: Int): BibleDb? {
        val f = file(id)?.takeIf { it.isFile } ?: return null
        val sql = synchronized(dbs) { dbs.getOrPut(f.path) { SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY) } }
        return BibleDb { q, args ->
            sql.rawQuery(q, args.map { "$it" }.toTypedArray()).use { c ->
                buildList {
                    while (c.moveToNext()) add((0 until c.columnCount).map { i ->
                        when (c.getType(i)) { Cursor.FIELD_TYPE_INTEGER -> c.getLong(i); Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i); Cursor.FIELD_TYPE_NULL -> null; else -> c.getString(i) }
                    })
                }
            }
        }
    }

    private fun close(id: Int) { file(id)?.let { f -> synchronized(dbs) { dbs.remove(f.path)?.close() } } }

    fun version(id: Int) = versions.firstOrNull { it.id == id }
    /** True when [v] is on the device. */
    fun offline(v: IBibleVersion) = file(v.id)?.isFile == true
    fun hasUpdate(v: IBibleVersion) = entries[v.id]?.let { (_, e) -> file(v.id)?.let { BibleStore.hasUpdate(it, e) } } == true
    fun size(v: IBibleVersion) = entries[v.id]?.second?.size ?: 0L

    /** A chapter of a version on the device, with its audio and verse timings; null when it isn't downloaded. */
    suspend fun content(v: IBibleVersion, usfm: String, ch: Int): IChapterContent? = withContext(Dispatchers.IO) {
        runCatching { db(v.id)?.chapter(v, usfm, ch) }.getOrNull()
    }

    /** The verses of a chapter as plain text (a verse split over paragraphs joined up). */
    fun verses(doc: IChapterContent?): List<Verse> {
        val verses = linkedMapOf<Int, StringBuilder>()
        doc?.chapter.orEmpty().forEach { sec ->
            sec.content.orEmpty().forEach { p ->
                p.verses.forEach { verse ->
                    val words = verse.content.filter { it.type == VerseLineEnum.Word }.joinToString("") { it.content ?: "" }
                    verses.getOrPut(verse.number) { StringBuilder() }.append(' ').append(words)
                }
            }
        }
        return verses.filterKeys { it > 0 }.map { (n, t) -> Verse(n, t.toString().trim().replace(Regex("\\s+"), " ")) }
    }

    suspend fun chapter(v: IBibleVersion, usfm: String, ch: Int): List<Verse> = verses(content(v, usfm, ch))

    /** "Get it" (or Update): the version's database; true when it is on the device afterwards. */
    suspend fun download(v: IBibleVersion): Boolean {
        val (_, e) = entries[v.id] ?: return false
        if (v.id in downloading) return false
        downloading[v.id] = 0f
        failed.remove(v.id)
        try {
            close(v.id)
            BibleStore.download(e, file(v.id)!!) { downloading[v.id] = it }
        } catch (t: Throwable) {
            failed[v.id] = true
            return false
        } finally {
            downloading.remove(v.id)
        }
        versions = listed()
        return true
    }

    fun remove(v: IBibleVersion) {
        close(v.id); file(v.id)?.let(BibleStore::remove); versions = listed()
    }
}

/** The canon in order, grouped as on the Bible boards, with the boards' short names. */
object Canon {
    data class Group(val name: String, val color: Long, val books: List<String>)
    val ot = listOf(
        Group("Law", 0xFFC98B4A, listOf("GEN", "EXO", "LEV", "NUM", "DEU")),
        Group("History", 0xFF8E7B5A, listOf("JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST")),
        Group("Poetry", 0xFFB5627A, listOf("JOB", "PSA", "PRO", "ECC", "SNG")),
        Group("Prophets", 0xFF6F7FB8, listOf("ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL")),
    )
    val nt = listOf(
        Group("Gospels and Acts", 0xFFE3B456, listOf("MAT", "MRK", "LUK", "JHN", "ACT")),
        Group("Letters", 0xFF7F9CC9, listOf("ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN", "3JN", "JUD")),
        Group("Prophecy", 0xFFC46A5A, listOf("REV")),
    )
    val order = (ot + nt).flatMap { it.books }
    private val shorts = mapOf(
        "GEN" to "Gen", "EXO" to "Exo", "LEV" to "Lev", "NUM" to "Num", "DEU" to "Deu", "JOS" to "Jos", "JDG" to "Jdg", "RUT" to "Rut",
        "1SA" to "1 Sa", "2SA" to "2 Sa", "1KI" to "1 Ki", "2KI" to "2 Ki", "1CH" to "1 Ch", "2CH" to "2 Ch", "EZR" to "Ezr", "NEH" to "Neh", "EST" to "Est",
        "JOB" to "Job", "PSA" to "Psa", "PRO" to "Pro", "ECC" to "Ecc", "SNG" to "Son", "ISA" to "Isa", "JER" to "Jer", "LAM" to "Lam", "EZK" to "Eze",
        "DAN" to "Dan", "HOS" to "Hos", "JOL" to "Joel", "AMO" to "Amos", "OBA" to "Oba", "JON" to "Jon", "MIC" to "Mic", "NAM" to "Nah", "HAB" to "Hab",
        "ZEP" to "Zep", "HAG" to "Hag", "ZEC" to "Zec", "MAL" to "Mal", "MAT" to "Matt", "MRK" to "Mark", "LUK" to "Luke", "JHN" to "John", "ACT" to "Acts",
        "ROM" to "Rom", "1CO" to "1 Co", "2CO" to "2 Co", "GAL" to "Gal", "EPH" to "Eph", "PHP" to "Phil", "COL" to "Col", "1TH" to "1 Th", "2TH" to "2 Th",
        "1TI" to "1 Ti", "2TI" to "2 Ti", "TIT" to "Tit", "PHM" to "Phm", "HEB" to "Heb", "JAS" to "Jas", "1PE" to "1 Pe", "2PE" to "2 Pe",
        "1JN" to "1 Jn", "2JN" to "2 Jn", "3JN" to "3 Jn", "JUD" to "Jude", "REV" to "Rev",
    )
    fun short(usfm: String) = shorts[usfm] ?: usfm.lowercase().replaceFirstChar { it.uppercase() }
    fun isOld(usfm: String) = ot.any { usfm in it.books }
}
