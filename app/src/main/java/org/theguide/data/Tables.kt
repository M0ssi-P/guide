package org.theguide.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okHttpClient
import okhttp3.Request
import org.theguide.App
import parsers.bible.models.ILanguage
import parsers.vgr.FULL_SERMON_SQL
import parsers.vgr.OmegaCatalog
import parsers.vgr.SermonRow
import parsers.vgr.Table
import parsers.vgr.get
import parsers.vgr.groupSermonRows
import parsers.vgr.models.Sermon
import parsers.vgr.VgrMedia
import store.FileStore
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import kotlin.coroutines.coroutineContext

/** A Table language the file store offers: its names, download size and how many sermons have a voice in it. */
data class TableOffer(val iso3: String, val iso1: String, val name: String, val localName: String, val bytes: Long?, val voices: Int?)

/**
 * The Table on this device: one SQLite database per language (the same file the desktop merges), downloaded gzipped
 * from the file store, read straight from disk.
 */
object Tables {
    private val dir get() = File(App.context.filesDir, "tables").apply { mkdirs() }
    fun file(iso3: String) = File(dir, "${iso3.lowercase()}.db")
    fun installed(): List<String> = dir.listFiles { f -> f.name.endsWith(".db") }?.map { it.nameWithoutExtension } ?: emptyList()
    fun has(iso3: String) = file(iso3).isFile

    private val languageList by lazy { kotlinx.coroutines.runBlocking { Table().getAllLanguages() } }
    fun language(iso3: String): ILanguage? = languageList.firstOrNull { it.iso63903.equals(iso3, true) }
    /** "Kiswahili" for "swa". */
    fun localName(iso3: String) = language(iso3)?.let { it.localName.ifBlank { it.name } } ?: iso3.uppercase()
    fun englishName(iso3: String) = language(iso3)?.name ?: iso3.uppercase()
    /** The 2-letter tag the Table uses ("sw"). */
    fun tag(iso3: String) = language(iso3)?.iso63901 ?: iso3.take(2)

    /** What the store offers, with sizes and voice counts (null while unknown). */
    suspend fun offers(withVoices: Boolean): List<TableOffer> = withContext(Dispatchers.IO) {
        val ids = FileStore.list("table")
        val cat = if (withVoices) runCatching { OmegaCatalog.get() }.getOrNull() else null
        coroutineScope {
            ids.map { iso3 ->
                async {
                    val l = language(iso3)
                    val voices = cat?.let { c ->
                        val lid = c.languages[iso3.lowercase()] ?: return@let null
                        c.files.keys.filter { it.second == lid }.map { it.first }.toSet().size
                    }
                    TableOffer(iso3, l?.iso63901 ?: iso3.take(2), l?.name ?: iso3.uppercase(), l?.localName?.ifBlank { null } ?: l?.name ?: iso3.uppercase(),
                        FileStore.size(FileStore.tableUrl(iso3)), voices)
                }
            }.awaitAll()
        }
    }

    /**
     * Downloads (or updates) and unpacks one language; [progress] gets (done, total) of the download. A cut-off
     * download picks up where it stopped next time, and the old copy stays readable until the new one is complete.
     */
    suspend fun download(iso3: String, progress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val target = file(iso3)
        val gz = File(target.parentFile, target.name + ".gz")
        val tmp = File(target.parentFile, target.name + ".tmp")
        val etag = FileStore.tableEtag(iso3)
        VgrMedia.download(FileStore.tableUrl(iso3), gz) { progress((it * 1000).toLong(), 1000) }
        try {
            GZIPInputStream(gz.inputStream().buffered(), 64 * 1024).use { input -> tmp.outputStream().use { input.copyTo(it, 256 * 1024) } }
            close(iso3)
            lists.remove(iso3)
            target.delete()
            if (!tmp.renameTo(target)) throw IOException("Couldn't save ${target.name}")
            etag?.let { etagFile(iso3).writeText(it) }
        } finally {
            gz.delete(); tmp.delete()
        }
    }

    /** The store's etag of the copy on this device. */
    private fun etagFile(iso3: String) = File(dir, "${iso3.lowercase()}.etag")

    /** Installed languages with a newer database on the store; one installed before these checks is taken as current. */
    suspend fun stale(): List<String> = installed().filter { iso3 ->
        val latest = FileStore.tableEtag(iso3) ?: return@filter false
        val mine = etagFile(iso3)
        if (!mine.isFile) { mine.writeText(latest); false } else mine.readText() != latest
    }

    private val open = ConcurrentHashMap<String, SQLiteDatabase>()
    private fun db(iso3: String): SQLiteDatabase? {
        if (!has(iso3)) return null
        return open.getOrPut(iso3) { SQLiteDatabase.openDatabase(file(iso3).path, null, SQLiteDatabase.OPEN_READONLY) }
    }
    private fun close(iso3: String) { open.remove(iso3)?.close() }

    private val lists = ConcurrentHashMap<String, List<Sermon>>()

    /** Every sermon in [iso3], in the Table's order. */
    fun sermons(iso3: String): List<Sermon> = lists.getOrPut(iso3) {
        val d = db(iso3) ?: return emptyList()
        d.rawQuery("SELECT * FROM sermons ORDER BY created_at ASC, rowid ASC", null).use { c -> buildList { while (c.moveToNext()) add(c.sermon()) } }
    }

    fun sermon(iso3: String, code: String): Sermon? = sermons(iso3).firstOrNull { it.dateCode.equals(code, true) }

    /** The sermon with its sections, paragraphs and timed sentences. */
    suspend fun full(iso3: String, sermon: Sermon): Sermon = withContext(Dispatchers.IO) {
        val d = db(iso3) ?: return@withContext sermon
        val rows = d.rawQuery(FULL_SERMON_SQL, arrayOf(sermon.stringId)).use { c ->
            buildList {
                while (c.moveToNext()) add(SermonRow(
                    sectionId = c.str("sec_id")!!,
                    sectionCanHit = c.bool("sec_can_hit"),
                    sectionIsHeader = c.bool("sec_is_header"),
                    sectionKey = c.str("sec_key"),
                    sectionParagraph = c.str("sec_paragraph"),
                    paragraphId = c.str("para_id"),
                    paragraphHubId = c.int("para_hub_id"),
                    paragraphNumber = c.int("para_number"),
                    paragraphText = c.str("para_text"),
                    paragraphIsFirst = c.bool("para_is_first"),
                    lineDisplayNumber = c.int("line_display_number")?.let { it != 0 },
                    lineNumber = c.int("line_number"),
                    lineSegmentsJson = c.str("line_segments"),
                ))
            }
        }
        sermon.copy(sections = groupSermonRows(rows))
    }

    private fun Cursor.idx(n: String) = getColumnIndexOrThrow(n)
    private fun Cursor.str(n: String): String? = idx(n).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.int(n: String): Int? = idx(n).let { if (isNull(it)) null else getInt(it) }
    private fun Cursor.bool(n: String): Boolean = idx(n).let { !isNull(it) && getInt(it) != 0 }

    private fun Cursor.sermon() = Sermon(
        id = null, stringId = str("id")!!, date = str("date") ?: "", dateCode = str("date_code") ?: "", language = str("language") ?: "",
        location = str("location") ?: "", nextSermonDate = str("next_sermon_date") ?: "", prevSermonDate = str("prev_sermon_date") ?: "",
        title = str("title") ?: "", sortDate = str("sort_date") ?: "", minutes = int("minutes"), hasSubtitle = bool("has_subtitle"),
        isCab = bool("is_cab"), totalSections = int("total_sections"), c = int("c") ?: 0, i = int("i") ?: 0, ct = str("ct") ?: "",
    )
}
