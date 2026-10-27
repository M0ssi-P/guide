package db.controller.songbooks

import com.google.gson.Gson
import models.ISongDetails
import models.ISongLyricLine
import models.ISongTitle
import models.LyricType
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.Color
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

/**
 * A song book as a printable PDF: cover, owner page, contents (number, title, page), then the songs in number order.
 * The whole book also rides along as JSON in the document info, so importing it on another computer is exact;
 * a PDF made elsewhere falls back to reading its text.
 */
object SongBookPdf {
    private const val KEY = "TheGuideSongBook"
    private val gson = Gson()
    private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val page = PDRectangle.A5
    private const val M = 46f

    fun fileName(title: String) = title.ifBlank { "Song book" }.replace(Regex("""[\\/:*?"<>|]"""), "") + ".pdf"

    private class Fonts(doc: PDDocument) {
        private fun load(doc: PDDocument, name: String) = PDType0Font.load(doc, SongBookPdf::class.java.getResourceAsStream("/fonts/$name")!!)
        val body: PDFont = load(doc, "Fraunces-Regular.ttf")
        val italic: PDFont = load(doc, "Fraunces-SemiBoldItalic.ttf")
        val slab: PDFont = load(doc, "ZillaSlab-Bold.ttf")
        val sans: PDFont = load(doc, "Inter-SemiBold.ttf")
    }

    private data class Line(val text: String, val font: PDFont, val size: Float, val label: String? = null, val gapBefore: Float = 0f)

    /** Word-wraps [text] to [width] points. */
    private fun wrap(text: String, font: PDFont, size: Float, width: Float): List<String> = text.lines().flatMap { raw ->
        val out = mutableListOf<String>(); var cur = ""
        raw.split(" ").forEach { w ->
            val next = if (cur.isEmpty()) w else "$cur $w"
            if (cur.isNotEmpty() && font.getStringWidth(next) / 1000 * size > width) { out += cur; cur = w } else cur = next
        }
        out + cur
    }

    private fun songLines(s: ISongDetails, f: Fonts): List<Line> {
        val w = page.width - 2 * M - 40
        return s.lyrics.flatMap { l ->
            val label = when (l.type) { LyricType.VERSE -> "${l.verseNumber ?: ""}"; LyricType.CHORUS -> "CHORUS"; LyricType.BRIDGE -> "BRIDGE"; else -> "" }
            val font = if (l.type == LyricType.CHORUS) f.italic else f.body
            wrap(l.lines.joinToString("\n"), font, 10.5f, w).mapIndexed { i, t -> Line(t, font, 10.5f, if (i == 0) label else null, if (i == 0) 9f else 0f) }
        }
    }

    /** Pages each song starts on, given where the songs begin; a long song runs on to the next page. */
    private fun layout(songs: List<ISongDetails>, f: Fonts, first: Int): List<Int> {
        val usable = page.height - 2 * M - 40
        var p = first
        return songs.map { s ->
            val start = p
            val h = songLines(s, f).sumOf { (it.size * 1.5f + it.gapBefore).toDouble() }.toFloat()
            p += 1 + (h / usable).toInt()
            start
        }
    }

    fun write(meta: BookMeta, songsIn: List<ISongDetails>, out: File) {
        val songs = songsIn.sortedBy { it.songNumber }
        out.parentFile?.mkdirs()
        PDDocument().use { doc ->
            val f = Fonts(doc)
            numberFont = f.sans
            val perContents = 30
            val contentsPages = ((songs.size + perContents - 1) / perContents).coerceAtLeast(1)
            val starts = layout(songs, f, 3 + contentsPages)

            // 1. cover
            doc.newPage { cs ->
                var y = page.height - 150
                meta.logo?.let { path -> runCatching { PDImageXObject.createFromFile(path, doc) }.getOrNull() }?.let { img ->
                    val s = 90f; cs.drawImage(img, (page.width - s) / 2, y - s / 2, s, s); y -= s
                }
                y -= 30
                wrap(meta.title, f.slab, 24f, page.width - 2 * M).forEach { cs.centered(it, f.slab, 24f, y); y -= 30 }
                cs.setNonStrokingColor(Color(0xE0, 0x81, 0x3A)); cs.addRect(page.width / 2 - 20, y + 10, 40f, 2f); cs.fill(); cs.setNonStrokingColor(Color.BLACK)
                y -= 14
                if (meta.author.isNotBlank()) { cs.centered(meta.author, f.italic, 12f, y); y -= 22 }
                cs.centered("Created " + Instant.ofEpochMilli(meta.created).atZone(ZoneId.systemDefault()).format(dateFmt), f.sans, 8.5f, y, Color(0x8A, 0x76, 0x60))
            }
            // 2. owner
            doc.newPage(2) { cs ->
                var y = page.height / 2 + 40
                cs.text("THIS BOOK BELONGS TO", f.sans, 7.5f, M, y, Color(0x8A, 0x76, 0x60)); y -= 26
                cs.text(meta.owner.ifBlank { "______________________" }, f.body, 16f, M, y); y -= 8
                cs.setStrokingColor(Color(0xDC, 0xCB, 0xB0)); cs.moveTo(M, y); cs.lineTo(page.width - M, y); cs.stroke(); y -= 18
                meta.address.lines().forEach { cs.text(it, f.body, 10f, M, y, Color(0x4A, 0x3A, 0x2C)); y -= 14 }
            }
            // 3. contents
            songs.chunked(perContents).ifEmpty { listOf(emptyList()) }.forEachIndexed { ci, chunk ->
                doc.newPage(3 + ci) { cs ->
                    var y = page.height - M - 10
                    if (ci == 0) { cs.text("Contents", f.slab, 16f, M, y); y -= 26 }
                    chunk.forEach { s ->
                        val i = songs.indexOf(s)
                        cs.text("${s.songNumber}", f.sans, 9f, M, y, Color(0xB5, 0x56, 0x1B))
                        val title = s.titles.firstOrNull()?.title ?: ""
                        cs.text(title, f.body, 9.5f, M + 26, y)
                        val pg = "${starts[i]}"
                        val pw = f.body.getStringWidth(pg) / 1000 * 9.5f
                        cs.text(pg, f.body, 9.5f, page.width - M - pw, y)
                        y -= 15
                    }
                }
            }
            // 4. songs
            songs.forEachIndexed { i, s ->
                val lines = songLines(s, f)
                var pageNo = starts[i]
                var cs = doc.openPage(pageNo)
                var y = page.height - M - 10
                cs.text("${s.songNumber}", f.slab, 18f, M, y, Color(0xB5, 0x56, 0x1B))
                cs.text(s.titles.firstOrNull()?.title ?: "", f.slab, 14f, M + 34, y)
                y -= 26
                lines.forEach { l ->
                    if (y - l.gapBefore < M + 20) { cs.close(); pageNo++; cs = doc.openPage(pageNo); y = page.height - M - 10 }
                    y -= l.gapBefore
                    l.label?.let { cs.text(it, f.sans, 6.5f, M, y + 1, Color(0x8A, 0x76, 0x60)) }
                    cs.text(l.text, l.font, l.size, M + 40, y)
                    y -= l.size * 1.5f
                }
                cs.close()
            }
            val info = doc.documentInformation
            info.title = meta.title
            info.author = meta.author
            info.setCustomMetadataValue(KEY, gson.toJson(Payload(meta.copy(logo = null, backup = false, backupDir = null), songs, logoData(meta.logo))))
            doc.save(out)
            numberFont = null
        }
    }

    private data class Payload(val meta: BookMeta, val songs: List<ISongDetails>, val logo: String?)

    private fun logoData(path: String?) = path?.let { File(it) }?.takeIf { it.isFile && it.length() < 2_000_000 }?.let { Base64.getEncoder().encodeToString(it.readBytes()) }

    /** Reads a song book PDF: the embedded book when it's one of ours, otherwise the songs found in its text. */
    fun read(file: File, onStep: (Int) -> Unit = {}): Pair<BookMeta, List<ISongDetails>> = Loader.loadPDF(file).use { doc ->
        onStep(1)
        doc.documentInformation.getCustomMetadataValue(KEY)?.let { json ->
            val p = gson.fromJson(json, Payload::class.java)
            val logo = p.logo?.let { b64 ->
                val f = File(System.getProperty("user.home"), "TheGuide/logos/${System.currentTimeMillis()}.png").apply { parentFile.mkdirs() }
                f.writeBytes(Base64.getDecoder().decode(b64)); f.absolutePath
            }
            return@use p.meta.copy(logo = logo, created = p.meta.created) to p.songs.map { s -> s.copy(id = "", lyrics = s.lyrics.map { it.copy(id = "", songId = "") }) }
        }
        // shortcut: text fallback expects "<number> <title>" headings with numbered verses and CHORUS labels; layouts from other tools may need tidying after import
        val text = PDFTextStripper().apply { startPage = 4 }.getText(doc)
        val songs = mutableListOf<ISongDetails>()
        var number = 0; var title = ""; val parts = mutableListOf<ISongLyricLine>(); var cur: MutableList<String>? = null; var type = LyricType.VERSE; var verse = 0
        fun flushPart() { cur?.takeIf { it.isNotEmpty() }?.let { parts += ISongLyricLine(type = type, key = null, verseNumber = if (type == LyricType.VERSE) verse else 0, lines = it.toList()) }; cur = null }
        fun flushSong() { flushPart(); if (number > 0) songs += ISongDetails(titles = listOf(ISongTitle("", "", title)), songNumber = number, lyrics = parts.toList()); parts.clear(); verse = 0 }
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val heading = Regex("""^(\d{1,4})\s+(\D.*)$""").find(line)
            when {
                // songs come in number order, so a "<number> <Title>" line above the last number starts the next song
                heading != null && heading.groupValues[1].toInt() > number && heading.groupValues[2].length < 60 && looksLikeTitle(heading.groupValues[2]) -> {
                    flushSong(); number = heading.groupValues[1].toInt(); title = heading.groupValues[2]
                }
                line.equals("CHORUS", true) || line.equals("BRIDGE", true) -> { flushPart(); type = if (line.equals("CHORUS", true)) LyricType.CHORUS else LyricType.BRIDGE; cur = mutableListOf() }
                Regex("""^\d{1,2}$""").matches(line) -> { flushPart(); type = LyricType.VERSE; verse = line.toInt(); cur = mutableListOf() }
                else -> (cur ?: mutableListOf<String>().also { cur = it; type = LyricType.VERSE; verse++ }).add(line)
            }
        }
        flushSong()
        onStep(2)
        BookMeta("", file.nameWithoutExtension) to songs
    }

    private fun looksLikeTitle(s: String) = s.split(" ").all { w -> w.isEmpty() || w[0].isUpperCase() || w.length <= 3 }

    // --- small drawing helpers ---
    private fun PDDocument.newPage(number: Int = 1, draw: (PDPageContentStream) -> Unit) {
        val p = PDPage(page); addPage(p)
        PDPageContentStream(this, p).use { cs -> draw(cs); if (number > 1) cs.pageNumber(number) }
    }

    /** The content stream for page [number] (1-based), adding pages up to it. */
    private fun PDDocument.openPage(number: Int): PDPageContentStream {
        while (numberOfPages < number) addPage(PDPage(page))
        val cs = PDPageContentStream(this, getPage(number - 1), PDPageContentStream.AppendMode.APPEND, true)
        cs.pageNumber(number)
        return cs
    }

    private fun PDPageContentStream.pageNumber(n: Int) {
        val f = numberFont ?: return
        val s = "$n"; val w = f.getStringWidth(s) / 1000 * 8f
        text(s, f, 8f, (page.width - w) / 2, 24f, Color(0x8A, 0x76, 0x60))
    }
    private var numberFont: PDFont? = null

    private fun PDPageContentStream.text(s: String, font: PDFont, size: Float, x: Float, y: Float, color: Color = Color(0x2A, 0x1F, 0x15)) {
        beginText(); setFont(font, size); setNonStrokingColor(color); newLineAtOffset(x, y)
        showText(s.filter { c -> runCatching { font.encode(c.toString()) }.isSuccess })
        endText()
    }

    private fun PDPageContentStream.centered(s: String, font: PDFont, size: Float, y: Float, color: Color = Color(0x2A, 0x1F, 0x15)) {
        val w = font.getStringWidth(s.filter { c -> runCatching { font.encode(c.toString()) }.isSuccess }) / 1000 * size
        text(s, font, size, (page.width - w) / 2, y, color)
    }
}
