package mvvm

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import loadData
import parsers.bible.BibleDb
import parsers.bible.BibleStore
import parsers.bible.BibleStore.toVersion
import parsers.bible.models.IBibleVersion
import parsers.bible.models.IChapterContent
import parsers.bible.models.ILanguage
import saveData
import storeDir
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/** The Bible versions on the file store and the ones on this computer: each is one SQLite file read through [BibleDb]. */
object BibleLibrary {
    /** What the store lists, by version id: its language and catalogue entry (kept so it opens offline). */
    private val entries: MutableMap<Int, Pair<ILanguage, BibleStore.Entry>> =
        loadData<HashMap<Int, Pair<ILanguage, BibleStore.Entry>>>("bible-r2-entries") ?: HashMap()
    /** 0..1 while a version downloads, by id. */
    val progress = mutableStateMapOf<Int, Float>()
    /** Versions whose last download failed. */
    val failed = mutableStateMapOf<Int, Boolean>()
    /** Bumped when a version arrives, updates or goes, so lists redraw. */
    val changed = mutableStateOf(0)

    private val connections = mutableMapOf<String, Connection>()

    fun register(language: ILanguage, list: List<BibleStore.Entry>) {
        list.forEach { entries[it.id] = language to it }
        saveData("bible-r2-entries", HashMap(entries))
    }

    /** The versions listed for a language (the last list fetched, so it opens offline). */
    fun listed(tag: String): List<IBibleVersion> =
        entries.values.filter { it.first.tag == tag }.sortedBy { it.second.abbreviation }.map { (l, e) -> version(l, e) }

    private fun file(id: Int) = entries[id]?.let { (l, e) -> BibleStore.dbFile(storeDir, l.tag, e) }
    fun isDownloaded(v: IBibleVersion) = file(v.id)?.isFile == true
    fun hasUpdate(v: IBibleVersion) = entries[v.id]?.let { (_, e) -> file(v.id)?.let { BibleStore.hasUpdate(it, e) } } == true
    fun size(v: IBibleVersion) = entries[v.id]?.second?.size ?: 0

    /** The version as listed, or with its books once it is on this computer. */
    fun version(language: ILanguage, e: BibleStore.Entry): IBibleVersion =
        if (file(e.id)?.isFile == true) runCatching { db(e.id)!!.version(language) }.getOrNull() ?: e.toVersion(language) else e.toVersion(language)

    private fun db(id: Int): BibleDb? {
        val f = file(id)?.takeIf { it.isFile } ?: return null
        val conn = synchronized(connections) { connections.getOrPut(f.path) { DriverManager.getConnection("jdbc:sqlite:${f.absolutePath}") } }
        return BibleDb { sql, args ->
            synchronized(conn) {
                conn.prepareStatement(sql).use { st ->
                    args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
                    st.executeQuery().use { rs -> buildList { while (rs.next()) add((1..rs.metaData.columnCount).map { rs.getObject(it) }) } }
                }
            }
        }
    }

    private fun close(f: File) = synchronized(connections) { connections.remove(f.path)?.close() }

    /** Downloads [v] (or its update); true when it is on this computer afterwards. */
    suspend fun download(v: IBibleVersion): Boolean {
        val (lang, e) = entries[v.id] ?: return false
        if (v.id in progress) return false
        val f = file(v.id)!!
        progress[v.id] = 0.01f
        failed.remove(v.id)
        try {
            close(f)
            BibleStore.download(e, f) { progress[v.id] = it.coerceAtLeast(0.01f) }
        } catch (t: Throwable) {
            failed[v.id] = true
            return false
        } finally {
            progress.remove(v.id)
        }
        ShareViewModels.bibleChapterContent.keys.removeAll { it.startsWith("${v.id}.") }
        // the listed version now has its books
        ShareViewModels.bibleVersions[lang.tag]?.let { list -> ShareViewModels.bibleVersions[lang.tag] = list.map { x -> if (x.id == v.id) version(lang, e) else x } }
        changed.value++
        return true
    }

    fun remove(v: IBibleVersion) {
        val f = file(v.id) ?: return
        close(f); BibleStore.remove(f); changed.value++
    }

    /** [v] ready to read: downloaded first when needed. Null when it couldn't be fetched. */
    suspend fun ready(v: IBibleVersion): IBibleVersion? {
        val (lang, e) = entries[v.id] ?: return v.takeIf { !it.books.isNullOrEmpty() && isDownloaded(it) }
        if (!isDownloaded(v) && !download(v)) return null
        return withContext(Dispatchers.IO) { version(lang, e) }
    }

    /** A chapter of a version on this computer; null when it isn't downloaded. */
    suspend fun chapter(v: IBibleVersion, usfm: String): IChapterContent? = withContext(Dispatchers.IO) {
        runCatching { db(v.id)?.chapter(v, usfm.substringBefore("."), usfm.substringAfter(".").toInt()) }.getOrNull()
    }
}
