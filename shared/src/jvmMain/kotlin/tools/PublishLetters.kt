package tools

import Mapper
import client
import com.lagradost.nicehttp.Requests
import defaultHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import parsers.bt.JdbcSql
import parsers.bt.Letter
import parsers.bt.LetterDb
import parsers.bt.LetterJson
import parsers.bt.Letters
import parsers.bt.openSql
import parsers.vgr.VgrMedia
import store.FileStore
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.system.exitProcess

/**
 * The weekly letters job (.github/workflows/letters.yml). For each of the site's letter languages (or the ones given),
 * it starts from the database already on the file store, adds the letters the site has that it doesn't (by id, so
 * nothing is overwritten or duplicated), and publishes through the worker:
 *   letters/<lang>/<id>.json for each new letter, then letters/<lang>.db.gz, then letters/<lang>/latest.json
 *   (the newest ids, which the apps check), so an app never sees an id before its letter is there.
 * Needs THEGUIDE_ADMIN_TOKEN: the worker's ADMIN_TOKEN. LETTERS_DRY=1 builds without uploading (and needs no token);
 * LETTERS_PAGES limits how many archive pages are read (for trying it out).
 */
private val dry = System.getenv("LETTERS_DRY") == "1"
private val maxPages = System.getenv("LETTERS_PAGES")?.toIntOrNull() ?: 600

fun main(args: Array<String>) = runBlocking {
    val token = System.getenv("THEGUIDE_ADMIN_TOKEN")?.takeIf { it.isNotBlank() } ?: if (dry) "" else run { System.err.println("THEGUIDE_ADMIN_TOKEN is not set"); exitProcess(2) }
    okHttpClient = OkHttpClient.Builder().followRedirects(true).callTimeout(10, TimeUnit.MINUTES).build()
    client = Requests(okHttpClient, defaultHeaders, responseParser = Mapper)
    openSql = { JdbcSql(it) }
    val work = Files.createTempDirectory("letters").toFile()
    val failed = (args.toList().ifEmpty { Letters.languages }).filter { lang ->
        runCatching { publish(lang, token, work) }.onFailure { System.err.println("$lang: failed: $it") }.isFailure
    }
    work.deleteRecursively()
    if (failed.isNotEmpty()) exitProcess(1)
}

private suspend fun publish(lang: String, token: String, work: File) {
    val dbFile = File(work, "$lang.db")
    val gz = File(work, "$lang.db.gz")
    // what's already published, if anything
    val had = runCatching {
        VgrMedia.download("${FileStore.BASE}/files/letters/$lang.db.gz", gz) {}
        GZIPInputStream(gz.inputStream().buffered()).use { i -> dbFile.outputStream().use { i.copyTo(it) } }
    }.isSuccess
    val sql = JdbcSql(dbFile)
    val db = LetterDb(sql)
    val known = db.ids()

    // archive pages, newest first, up to the first page with nothing new (every page on the first build)
    val fresh = linkedMapOf<String, Letter>()
    var page = 1
    while (page <= maxPages) {
        val url = if (page == 1) "${Letters.HOST}/$lang/archive" else "${Letters.HOST}/$lang/archive/letters/$page"
        val res = client.get(url, headers = defaultHeaders)
        if (!res.isSuccessful) break
        val letters = Letters.parseArchive(res.text)
        if (letters.isEmpty()) break
        val new = letters.filter { it.id !in known && it.id !in fresh }
        new.forEach { fresh[it.id] = it }
        if (new.isEmpty() && had) break
        page++
        delay(300)
    }
    // each new letter with its service page: related services, audio, preliminary and closing
    val letters = fresh.values.map { l ->
        delay(300)
        runCatching {
            client.get("${Letters.HOST}/$lang/streaming/viewservice/${l.id}", headers = defaultHeaders).takeIf { it.isSuccessful }?.let { Letters.parseService(it.text, l.id, l) }
        }.getOrNull() ?: l
    }
    db.add(letters)
    val newest = db.page(0, 100)
    val count = db.count()
    sql.close()
    println("$lang: ${letters.size} new, $count in all")
    if ((letters.isEmpty() && had) || dry) return

    letters.forEach { put("letters/$lang/${it.id}.json", LetterJson.write(it).toByteArray(), "application/json", token) }
    GZIPOutputStream(gz.outputStream()).use { out -> dbFile.inputStream().use { it.copyTo(out) } }
    put("letters/$lang.db.gz", gz.readBytes(), "application/gzip", token)
    // "newest" (the latest letter's date) lets the workflow's catch-up runs see whether this week's letter is in
    val latest = """{"lang":"$lang","count":$count,"newest":"${newest.firstOrNull()?.letterDate ?: ""}","updatedAt":"${Instant.now()}","ids":[${newest.joinToString(",") { "\"${it.id}\"" }}]}"""
    put("letters/$lang/latest.json", latest.toByteArray(), "application/json", token)
}

private fun put(key: String, bytes: ByteArray, type: String, token: String) {
    val req = Request.Builder().url("${FileStore.BASE}/files/$key").header("Authorization", "Bearer $token").put(bytes.toRequestBody(type.toMediaType())).build()
    okHttpClient.newCall(req).execute().use { if (!it.isSuccessful) error("upload $key: HTTP ${it.code}") }
}
