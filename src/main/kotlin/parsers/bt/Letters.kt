package parsers.bt

import client
import defaultHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import loadData
import logError
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import saveData
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class LetterPart(val label: String) { PRELIMINARY("Preliminary"), LETTER("Letter"), CLOSING("Closing") }

data class LetterBlock(val text: String, val isQuote: Boolean = false, val cite: String? = null) : Serializable

data class LetterSection(val part: LetterPart, val blocks: List<LetterBlock>) : Serializable

data class RelatedService(val id: String, val label: String, val date: LocalDate?, val kind: String?) : Serializable

/** One of Brother Joseph's weekly letters and the Sunday service it goes with. */
data class Letter(
    val id: String,
    val letterDate: LocalDate,
    val serviceDate: LocalDate?,
    val code: String,
    val title: String,
    val salutation: String?,
    val sections: List<LetterSection>,
    val kind: String? = null,
    val related: List<RelatedService> = emptyList(),
    val audioUrl: String? = null,
    val hookupUrl: String? = null,
    /** True once the service page itself was read (related services, audio, preliminary/closing). */
    val detailed: Boolean = false,
) : Serializable {
    val url get() = "${Letters.HOST}/en/streaming/viewservice/$id"
    val plainText get() = sections.joinToString("\n\n") { s -> s.blocks.joinToString("\n\n") { it.text } }
    val quoteText get() = sections.flatMap { it.blocks }.filter { it.isQuote }.joinToString("\n\n") { it.text }
    val letterText get() = sections.flatMap { it.blocks }.filterNot { it.isQuote }.joinToString("\n\n") { it.text }
}

enum class LookIn(val label: String) { ALL("Both"), LETTERS("Letters"), QUOTES("Quotes") }

data class LetterQuery(
    val text: String = "",
    val wholeWord: Boolean = false,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val code: String? = null,
    val kind: String? = null,
    val look: LookIn = LookIn.ALL,
    val newestFirst: Boolean = true,
)

/** [ranges] index into [snippet]. */
data class LetterHit(val letter: Letter, val matches: Int, val snippet: String, val ranges: List<IntRange>)

data class SyncState(val running: Boolean = false, val page: Int = 0, val error: String? = null)

object Letters {
    const val HOST = "https://branhamtabernacle.org"
    private const val CACHE = "bt_letters"

    private val codeRegex = Regex("""\b(\d{2}-\d{4}[A-Z]?)\b""")
    private val longDate = Regex("""(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),?\s+([A-Z][a-z]+)\.?\s+(\d{1,2}),?\s+(\d{4})""")
    private val shortDate = Regex("""\b([A-Z][a-z]{2,8})\.?\s+(\d{1,2}),?\s+(\d{4})\b""")
    private val numericDate = Regex("""\b(\d{2})\.(\d{2})\.(\d{2})\b""")
    private val guidRegex = Regex("""viewservice/([0-9A-Fa-f-]{36})""")
    private val kindRegex = Regex("""\b(HOME|YFYC|BT|CAMP)\b""")
    private val signatureRegex = Regex("""^(Bro\.?|Brother)\s+Joseph(\s+M\.)?\s+Branham\.?$""", RegexOption.IGNORE_CASE)
    private val partHeading = mapOf(
        "preliminary" to LetterPart.PRELIMINARY, "preliminaries" to LetterPart.PRELIMINARY,
        "letter" to LetterPart.LETTER, "closing" to LetterPart.CLOSING, "closings" to LetterPart.CLOSING,
    )
    private const val SERVICE_LINK = "a[href*=viewservice/]"
    private const val QUOTE = "blockquote, .BBquoteText"
    // related services aren't in the HTML: the page renders them from this inline JSON
    private val relatedJson = Regex("""relatedServiceTabs\(\{\s*services:\s*(\[.*?])\s*,\s*selected:\s*(\d+)""", RegexOption.DOT_MATCHES_ALL)

    private val _all = MutableStateFlow<List<Letter>>(loadData<ArrayList<Letter>>(CACHE)?.toList() ?: emptyList())
    val all: StateFlow<List<Letter>> = _all.asStateFlow()
    private val _sync = MutableStateFlow(SyncState())
    val sync: StateFlow<SyncState> = _sync.asStateFlow()
    private val lock = Mutex()

    fun byId(id: String): Letter? = _all.value.firstOrNull { it.id == id }

    private suspend fun store(letters: List<Letter>) = lock.withLock {
        val merged = _all.value.associateBy { it.id }.toMutableMap()
        // a detailed copy (from the service page) wins over the archive copy
        letters.forEach { l -> merged[l.id] = if (merged[l.id]?.detailed == true && !l.detailed) merged[l.id]!! else l }
        _all.value = merged.values.sortedByDescending { it.letterDate }
        saveData(CACHE, ArrayList(_all.value))
    }

    /**
     * Pulls archive pages (newest first) into the local cache. Stops at the first page that is
     * empty or fully cached, so after the first run this only fetches what's new.
     */
    suspend fun sync(full: Boolean = false) = withContext(Dispatchers.IO) {
        if (_sync.value.running) return@withContext
        _sync.value = SyncState(running = true)
        try {
            var page = 1
            while (page <= 600) {
                _sync.value = SyncState(running = true, page = page)
                val url = if (page == 1) "$HOST/en/archive" else "$HOST/en/archive/letters/$page"
                val res = client.get(url, headers = defaultHeaders)
                if (!res.isSuccessful) break
                val letters = parseArchive(res.text)
                if (letters.isEmpty()) break
                val known = _all.value.map { it.id }.toSet()
                val allKnown = letters.all { it.id in known }
                store(letters)
                if (allKnown && !full) break
                page++
                delay(250)
            }
            _sync.value = SyncState()
        } catch (e: Exception) {
            logError(e)
            _sync.value = SyncState(error = "Couldn't reach branhamtabernacle.org")
        }
    }

    /** Reads the service page for related services, audio and the preliminary/closing. */
    suspend fun detail(id: String): Letter? = withContext(Dispatchers.IO) {
        try {
            val res = client.get("$HOST/en/streaming/viewservice/$id", headers = defaultHeaders)
            if (!res.isSuccessful) return@withContext byId(id)
            val parsed = parseService(res.text, id, byId(id)) ?: return@withContext byId(id)
            store(listOf(parsed))
            parsed
        } catch (e: Exception) {
            logError(e)
            byId(id)
        }
    }

    // ---------- parsing ----------
    // Class names match the site's markup as of Oct 2026 (.letter-holder, .letter-text, .BBquoteText,
    // .service-tape-date, .hap-playlist-item); the structural heuristics behind each one are fallbacks.

    fun parseArchive(html: String): List<Letter> {
        val doc = Jsoup.parse(html, HOST)
        val holders = doc.select(".letter-holder").filter { it.selectFirst(SERVICE_LINK) != null }
        val blocks = holders.ifEmpty { null } ?: doc.select(SERVICE_LINK).map { a ->
            // widest ancestor that still holds only this one service link = one archive entry
            var el: Element = a
            while (el.parent() != null && el.parent()!!.select(SERVICE_LINK).size == 1) el = el.parent()!!
            el
        }.distinct()
        return blocks.mapNotNull { parseEntry(it) }
    }

    private fun parseEntry(block: Element): Letter? {
        val link = block.selectFirst(SERVICE_LINK) ?: return null
        val id = guidRegex.find(link.attr("href"))?.groupValues?.get(1)?.uppercase() ?: return null
        // the service line ("Oct 25, 2026 60-0515E Adoption #1") may sit just outside the <a>
        val serviceLine = (link.parent()?.takeIf { it != block } ?: link).text()
        val code = codeRegex.find(serviceLine)?.value ?: codeRegex.find(block.text())?.value ?: return null
        val title = serviceLine.substringAfter(code).trim().trimStart('-', '–', ':').trim()
        val letterDate = parseDate(block.selectFirst(".letter-date")?.text() ?: "")
            ?: parseDate(block.ownText().ifBlank { block.text() }) ?: parseDate(block.text()) ?: return null
        val text = block.selectFirst(".letter-text")
        val (salutation, sections) = if (text != null) parseBody(text, exclude = null)
        else parseBody(block, exclude = link.parent()?.takeIf { it != block } ?: link)
        return Letter(
            id = id, letterDate = letterDate, serviceDate = parseDate(serviceLine), code = code,
            title = title.ifBlank { code }, salutation = salutation, sections = sections,
            kind = kindRegex.find(serviceLine)?.value
        )
    }

    fun parseService(html: String, id: String, cached: Letter?): Letter? {
        val doc = Jsoup.parse(html, HOST)
        val body = doc.body()
        // the HTML viewservice links on this page are "Previous Services" (other sermons), not related ones
        val json = doc.select("script").firstNotNullOfOrNull { relatedJson.find(it.data()) }
        val services = json?.let { runCatching { JsonParser.parseString(it.groupValues[1]).asJsonArray.map { e -> e.asJsonObject } }.getOrNull() } ?: emptyList()
        fun str(o: com.google.gson.JsonObject, key: String) = o.get(key)?.takeUnless { it.isJsonNull }?.asString
        val related = services.mapNotNull { o ->
            val rid = str(o, "serviceGuid")?.uppercase() ?: return@mapNotNull null
            val label = listOfNotNull(str(o, "tapeDate")?.uppercase(), str(o, "tapeTitle")).joinToString(" ")
            val date = str(o, "serviceDate")?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            RelatedService(rid, label, date, str(o, "serviceLocation")?.uppercase())
        }.distinctBy { it.id }
        val selected = services.firstOrNull { str(it, "serviceId") == json?.groupValues?.get(2) }
            ?: services.firstOrNull { str(it, "serviceGuid")?.uppercase() == id }
        val publishDate = selected?.get("letter")?.takeIf { it.isJsonObject }?.asJsonObject
            ?.let { str(it, "publishDate") }?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        // the letter is the element whose direct paragraphs carry the most text
        val main = doc.selectFirst(".bottom-box .letter-holder, .letter-holder")
            ?: body.select("div, article, section, main")
                .maxByOrNull { el -> el.children().filter { it.tagName() in setOf("p", "blockquote") }.sumOf { it.text().length } }
            ?: return cached
        val (salutation, sections) = parseBody(main, exclude = null)
        if (sections.isEmpty()) return cached

        val head = (doc.select(".service-date, .service-tape-date, .service-tape-title, h1, h2, h3").joinToString(" ") { it.text() } + " " + doc.title())
        val code = cached?.code ?: codeRegex.find(head)?.value ?: return cached
        val audio = doc.selectFirst(".hap-playlist-item[data-path]")?.absUrl("data-path")?.ifBlank { null }
            ?: doc.selectFirst("audio source[src], audio[src]")?.absUrl("src")?.ifBlank { null }
            ?: doc.selectFirst("a[href~=(?i)\\.(m4a|mp3|opus)$]")?.absUrl("href")
        // the page only shows a Hookup image (no link) as of Oct 2026; the UI falls back to /en/streaming
        val hookup = doc.select(".hookup a[href], a[href]").firstOrNull { a ->
            a.text().contains("hookup", true) || a.selectFirst("img[src*=ookup]") != null
        }?.absUrl("href")?.takeIf { it.startsWith("http") }

        return Letter(
            id = id,
            letterDate = cached?.letterDate ?: publishDate ?: parseDate(head) ?: parseDate(body.text()) ?: return cached,
            serviceDate = cached?.serviceDate ?: parseDate(head),
            code = code,
            title = cached?.title ?: doc.selectFirst(".service-tape-title")?.text()?.ifBlank { null } ?: head.substringAfter(code).trim().take(80).ifBlank { code },
            salutation = salutation ?: cached?.salutation,
            sections = sections,
            kind = related.firstOrNull { it.id == id }?.kind ?: cached?.kind,
            related = related,
            audioUrl = audio,
            hookupUrl = hookup,
            detailed = true,
        )
    }

    private fun parseBody(root: Element, exclude: Element?): Pair<String?, List<LetterSection>> {
        val picked = root.select("p, blockquote, h1, h2, h3, h4, h5, h6, li")
            .filter { el -> exclude == null || (el != exclude && !el.parents().contains(exclude) && !exclude.parents().contains(el)) }
            .filter { el -> el.selectFirst(SERVICE_LINK) == null }
        val set = picked.toSet()
        val top = picked.filter { el -> el.parents().none { it in set } }

        var salutation: String? = null
        var part = LetterPart.LETTER
        val sections = linkedMapOf<LetterPart, MutableList<LetterBlock>>()
        for (el in top) {
            // keep <br> line breaks (bullet lists); plain paragraphs get normal whitespace
            val text = if (el.selectFirst("br") == null) el.text().trim()
            else el.wholeText().lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }.joinToString("\n")
            if (text.isEmpty() || longDate.matches(text) || signatureRegex.matches(text)) continue
            // "Message: Adoption #1 60-0515E" footer repeats the header
            if (text.startsWith("Message:") && codeRegex.containsMatchIn(text)) continue
            partHeading[text.lowercase().trimEnd(':', '.')]?.let { part = it; continue }
            if (salutation == null && part == LetterPart.LETTER && text.startsWith("Dear ") && text.length < 90) {
                salutation = text; continue
            }
            val italic = el.select("em, i").sumOf { it.text().length }
            val quote = el.closest(QUOTE) != null || italic > text.length * 0.6
            val cite = if (quote) codeRegex.find(text.takeLast(40))?.value else null
            sections.getOrPut(part) { mutableListOf() } += LetterBlock(text, quote, cite)
        }
        return salutation to LetterPart.entries.mapNotNull { p -> sections[p]?.let { LetterSection(p, it) } }
    }

    private val months = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

    internal fun parseDate(text: String): LocalDate? {
        fun month(name: String): Int? = runCatching {
            java.time.Month.entries.first { it.name.startsWith(name.take(3).uppercase()) }.value
        }.getOrNull()
        longDate.find(text)?.let { m ->
            val (mon, d, y) = m.destructured
            return month(mon)?.let { runCatching { LocalDate.of(y.toInt(), it, d.toInt()) }.getOrNull() }
        }
        shortDate.find(text)?.let { m ->
            val (mon, d, y) = m.destructured
            month(mon)?.let { mo -> runCatching { LocalDate.of(y.toInt(), mo, d.toInt()) }.getOrNull()?.let { return it } }
        }
        numericDate.find(text)?.let { m ->
            val (mo, d, y) = m.destructured
            return runCatching { LocalDate.of(2000 + y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
        }
        return null
    }

    // ---------- search ----------

    fun search(letters: List<Letter>, q: LetterQuery): List<LetterHit> {
        val term = q.text.trim()
        val regex = term.takeIf { it.isNotEmpty() }?.let {
            val e = Regex.escape(it)
            Regex(if (q.wholeWord) "\\b$e\\b" else e, RegexOption.IGNORE_CASE)
        }
        return letters.asSequence()
            .filter { q.from == null || !it.letterDate.isBefore(q.from) }
            .filter { q.to == null || !it.letterDate.isAfter(q.to) }
            .filter { q.code == null || it.code == q.code }
            .filter { q.kind == null || it.kind == q.kind }
            .mapNotNull { l ->
                val text = when (q.look) {
                    LookIn.ALL -> l.plainText
                    LookIn.LETTERS -> l.letterText
                    LookIn.QUOTES -> l.quoteText
                }
                if (regex == null) return@mapNotNull LetterHit(l, 0, text.take(220).let { if (text.length > 220) "$it…" else it }, emptyList())
                val found = regex.findAll(text).toList()
                if (found.isEmpty()) return@mapNotNull null
                val start = (found.first().range.first - 80).coerceAtLeast(0)
                val end = (start + 260).coerceAtMost(text.length)
                val prefix = if (start > 0) "…" else ""
                val snippet = prefix + text.substring(start, end) + if (end < text.length) "…" else ""
                val ranges = found.map { it.range }.filter { it.first >= start && it.last < end }
                    .map { (it.first - start + prefix.length)..(it.last - start + prefix.length) }
                LetterHit(l, found.size, snippet, ranges)
            }
            .sortedWith(if (q.newestFirst) compareByDescending { it.letter.letterDate } else compareBy { it.letter.letterDate })
            .toList()
    }

    fun monthName(date: LocalDate): String = date.format(months)
}
