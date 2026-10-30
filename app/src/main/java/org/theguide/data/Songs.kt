package org.theguide.data

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import Mapper
import models.IProviderStats
import models.ISongDetails
import models.LyricType
import okHttpClient
import okhttp3.Request
import org.theguide.App
import store.FileStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A part of a song: verse number or "Ch", its lines, and whether it is the chorus. */
data class Part(val mark: String, val text: String, val chorus: Boolean)
data class Song(val n: String, val title: String, val parts: List<Part>) {
    val first get() = parts.firstOrNull()?.text?.lineSequence()?.firstOrNull() ?: ""
}
data class Songbook(val id: String, val name: String, val language: String, val songs: List<Song>, val mine: Boolean = false)

/** Song books on the device (from the store or imported) and the reader's own songs. */
object Songs {
    private val dir get() = File(App.context.filesDir, "songbooks").apply { mkdirs() }
    private val gson = Gson()
    const val MINE = "mine"

    var books by mutableStateOf(load()); private set
    var shop by mutableStateOf<List<String>?>(null); private set
    val progress = mutableStateMapOf<String, Int>()

    private fun load(): List<Songbook> {
        val list = dir.listFiles { f -> f.extension == "json" }?.mapNotNull { f -> runCatching { gson.fromJson(f.readText(), Songbook::class.java) }.getOrNull() } ?: emptyList()
        val mine = list.firstOrNull { it.id == MINE } ?: Songbook(MINE, "My songs", "", emptyList(), mine = true)
        return list.filter { it.id != MINE }.sortedBy { it.name } + mine
    }

    private fun save(b: Songbook) { File(dir, "${b.id}.json").writeText(gson.toJson(b)); books = load() }

    /** A text where blank lines separate parts and a part starting "Chorus" (or "Kiitikio") is the chorus. */
    fun parse(text: String): List<Part> {
        var v = 0
        return text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }.map { b ->
            val ch = Regex("^(chorus|kiitikio|refrain|refrão|coro)\\b", RegexOption.IGNORE_CASE).containsMatchIn(b)
            Part(if (ch) "Ch" else "${++v}", if (ch) b.replaceFirst(Regex("^\\S+\\s*"), "") else b, ch)
        }
    }

    fun addMine(title: String, lyrics: String, into: String) {
        val b = books.firstOrNull { it.id == into } ?: books.last()
        val n = ((b.songs.mapNotNull { it.n.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        save(b.copy(songs = b.songs + Song(n, title.ifBlank { "Untitled" }, parse(lyrics))))
        Prefs.log("Wrote $title", "song")
    }

    private fun fromGuide(stat: IProviderStats, songs: List<ISongDetails>) = Songbook(
        stat.saveName.ifBlank { stat.name }, stat.name, stat.language,
        songs.sortedBy { it.songNumber }.map { s ->
            var v = 0
            Song("${s.songNumber}", s.titles.firstOrNull()?.title ?: "", s.lyrics.map { l ->
                val ch = l.type == LyricType.CHORUS
                Part(if (ch) "Ch" else "${l.verseNumber ?: ++v}", l.lines.joinToString("\n"), ch)
            })
        },
    )

    private fun readGuideZip(bytes: ByteArray): Songbook? = songs.readGuideZip(bytes).firstOrNull()?.let { fromGuide(it.stat, it.songs) }

    // ---------- the shop ----------

    suspend fun refreshShop() { shop = runCatching { FileStore.list("hymns") }.getOrNull() ?: shop ?: emptyList() }
    fun nameOf(id: String) = id.split('-', '_').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    suspend fun get(id: String) = withContext(Dispatchers.IO) {
        progress[id] = 1
        try {
            val bytes = okHttpClient.newCall(Request.Builder().url(FileStore.songbookUrl(id)).build()).execute().use { r ->
                val body = r.body ?: return@withContext
                val total = body.contentLength(); val out = ByteArrayOutputStream(); val buf = ByteArray(32 * 1024); var read = 0L
                body.byteStream().use { input -> while (true) { val n = input.read(buf); if (n < 0) break; out.write(buf, 0, n); read += n; if (total > 0) progress[id] = (read * 100 / total).toInt().coerceAtLeast(1) } }
                out.toByteArray()
            }
            readGuideZip(bytes)?.let { save(it.copy(id = id)); Prefs.songbooks = Prefs.songbooks + id }
        } finally { progress.remove(id) }
    }

    // ---------- import and export ----------

    /** A Guide songbook (.zip), a plain text song or an OpenLyrics song (.xml). */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bytes = App.context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        importBytes(bytes, uri.lastPathSegment?.substringAfterLast('/') ?: "Imported")
    }

    /** Downloads a shared song file (a Guide songbook, text or OpenLyrics) from [url] and imports it. */
    suspend fun importLink(url: String): String? = withContext(Dispatchers.IO) {
        val bytes = runCatching { okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { r -> if (r.isSuccessful) r.body?.bytes() else null } }.getOrNull() ?: return@withContext null
        importBytes(bytes, url.substringBefore('?').substringAfterLast('/').ifBlank { "Imported" })
    }

    /**
     * A share link (http, downloaded and imported) or a short share code: the name of a songbook in the file store.
     * Returns what arrived, or null when nothing could be read.
     */
    suspend fun importLinkOrCode(text: String): String? {
        val t = text.trim()
        if (t.startsWith("http://", true) || t.startsWith("https://", true)) return importLink(t)
        val id = t.lowercase().replace(Regex("\\s+"), "-")
        if (id.isEmpty() || !Regex("[a-z0-9_-]+").matches(id)) return null
        runCatching { get(id) }
        return books.firstOrNull { it.id == id }?.name
    }

    /** Song bytes from a file, a link or a device nearby; returns the book or song name, or null when unreadable. */
    fun importBytes(bytes: ByteArray, name: String): String? =
        when {
            bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() ->
                runCatching { songs.readGuideZip(bytes) }.getOrDefault(emptyList()).map { fromGuide(it.stat, it.songs) }.onEach { save(it) }.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name }
            name.endsWith(".xml", true) || bytes.decodeToString().contains("<song", true) -> {
                val xml = bytes.decodeToString()
                val title = Regex("<title[^>]*>(.*?)</title>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1)?.trim() ?: name
                val text = Regex("<verse([^>]*)>(.*?)</verse>", RegexOption.DOT_MATCHES_ALL).findAll(xml).joinToString("\n\n") { m ->
                    val chorus = Regex("""name="c""").containsMatchIn(m.groupValues[1])
                    val lines = m.groupValues[2].replace(Regex("<br\\s*/?>"), "\n").replace(Regex("<[^>]+>"), "").lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
                    (if (chorus) "Chorus\n" else "") + lines
                }
                addMine(title, text, MINE); title
            }
            bytes.isEmpty() || bytes.take(512).any { it == 0.toByte() } -> null
            else -> { val t = bytes.decodeToString(); val title = t.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { name }; addMine(title, t.substringAfter('\n').trim(), MINE); title }
        }

    /** Packs [ids] as a Guide file, plain text or PDF in the cache, ready to share. */
    suspend fun export(ids: List<String>, format: String, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val chosen = books.filter { it.id in ids }
        val out = File(App.context.cacheDir, "songbooks." + when (format) { "pdf" -> "pdf"; "text" -> "txt"; else -> "zip" })
        when (format) {
            "text" -> out.writeText(chosen.joinToString("\n\n\n") { b -> b.name + "\n\n" + b.songs.joinToString("\n\n\n") { s -> "${s.n}. ${s.title}\n\n" + s.parts.joinToString("\n\n") { (if (it.chorus) "Chorus\n" else "") + it.text } } })
            "pdf" -> {
                val doc = PdfDocument(); val paint = Paint().apply { textSize = 11f }; var page = 0; var y = 0f
                var p: PdfDocument.Page? = null
                fun line(t: String, size: Float = 11f) {
                    if (p == null || y > 800) { p?.let(doc::finishPage); p = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, ++page).create()); y = 50f }
                    paint.textSize = size; p!!.canvas.drawText(t, 50f, y, paint); y += size * 1.5f
                }
                chosen.forEachIndexed { i, b -> line(b.name, 18f); b.songs.forEach { s -> line("${s.n}. ${s.title}", 14f); s.parts.forEach { pt -> pt.text.lines().forEach { line(if (pt.chorus) "    $it" else it) }; line("") } }; progress((i + 1) * 100 / chosen.size) }
                p?.let(doc::finishPage); out.outputStream().use { doc.writeTo(it) }; doc.close()
            }
            else -> ZipOutputStream(out.outputStream()).use { z ->
                chosen.forEachIndexed { i, b ->
                    z.putNextEntry(ZipEntry("${b.id}/stat.json")); z.write(gson.toJson(mapOf("language" to b.language, "name" to b.name, "saveName" to b.id, "baseURI" to "", "isWorking" to true)).toByteArray()); z.closeEntry()
                    z.putNextEntry(ZipEntry("${b.id}/songs.json"))
                    z.write(gson.toJson(b.songs.map { s ->
                        mapOf("titles" to listOf(mapOf("language" to b.language, "languageCode" to b.language, "title" to s.title)), "songNumber" to (s.n.toIntOrNull() ?: 0),
                            "lyrics" to s.parts.map { pt -> mapOf("type" to if (pt.chorus) "CHORUS" else "VERSE", "key" to null, "verseNumber" to pt.mark.toIntOrNull(), "lines" to pt.text.lines()) })
                    }).toByteArray()); z.closeEntry()
                    progress((i + 1) * 100 / chosen.size)
                }
            }
        }
        out
    }

}

