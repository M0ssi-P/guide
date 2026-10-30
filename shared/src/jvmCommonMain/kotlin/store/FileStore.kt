package store

import client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okHttpClient
import okhttp3.Request

/** The Guide's file store: the Table databases per language and the song books, with what it offers. */
object FileStore {
    const val BASE = "https://theguide-file-store.vrg-theguide.workers.dev"

    /** A Table language's database, gzipped; [iso3] like "swa". */
    fun tableUrl(iso3: String) = "$BASE/table/db/download/table/${iso3.lowercase()}"
    fun songbookUrl(id: String) = "$BASE/songbooks/$id.zip"

    private val ids = Regex(""""ids"\s*:\s*\[([^\]]*)]""")

    /** What the store has of [kind] ("table": ISO3 codes, "hymns": song book ids). */
    suspend fun list(kind: String): List<String> = withContext(Dispatchers.IO) {
        val text = client.get("$BASE/table/db/list/$kind").text
        ids.find(text)?.groupValues?.get(1)?.split(',')?.map { it.trim().trim('"') }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    /** Download size in bytes, from a HEAD request; null when unknown. */
    suspend fun size(url: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            okHttpClient.newCall(Request.Builder().url(url).head().build()).execute().use { r -> r.header("Content-Length")?.toLongOrNull() }
        }.getOrNull()
    }
}
