package live

import androidx.compose.runtime.mutableStateMapOf
import db.controller.table.Languages.getAllLanguages
import db.controller.table.Languages.importTables
import db.runAndReturn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mvvm.ShareViewModels
import parsers.bible.models.ILanguage
import rs.Bucket
import ui.UiState
import ui.config.DB
import java.nio.file.Files

/** A Table language on this computer (a row of `tables`). */
data class TableLang(val id: String, val tag: String, val name: String, val localName: String, val iso3: String)

/**
 * Installs one more Table language the same way first-run setup does (download the Infobase, unpack, merge),
 * with progress per language for the join page.
 */
object TableInstaller {
    /** 0..1 while installing, by language tag. */
    val progress = mutableStateMapOf<String, Float>()
    private val cancelled = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /** Stops an install in progress; it gives up at the next chunk it reads. */
    fun cancel(tag: String) { cancelled += tag }

    /** What step a [progress] value is on. */
    fun stage(p: Float) = when { p < 0.85f -> "Downloading ${(p / 0.85f * 100).toInt()}%"; p < 0.9f -> "Unpacking"; else -> "Merging into your library" }

    fun installed(): List<TableLang> = runCatching {
        DB.connection("main.db").runAndReturn("SELECT id, tag, name, local_name, iso_63903 FROM tables") { rs ->
            TableLang(rs.getString("id"), rs.getString("tag"), rs.getString("name"), rs.getString("local_name") ?: "", rs.getString("iso_63903") ?: "")
        }
    }.getOrDefault(emptyList())

    /** Languages the server offers a Table for that aren't installed yet. */
    fun available(): List<ILanguage> {
        val have = installed().map { it.tag }.toSet()
        val offered = (ShareViewModels.globalViewModel.languageData.value as? UiState.Success)?.data
            ?: runCatching { DB.connection("main.db").getAllLanguages() }.getOrDefault(emptyList())
        return offered.filter { it.tag !in have }.sortedBy { it.name }
    }

    suspend fun install(language: ILanguage): Boolean = withContext(Dispatchers.IO) {
        val config = ShareViewModels.globalViewModel
        progress[language.tag] = 0.01f
        try {
            val tempDir = Files.createTempDirectory("tables-")
            val zipPath = tempDir.resolve("${language.iso63901}-table.db.gz")
            Bucket(config.rs).file("table/db/download/table/${language.iso63903}")
                .createReadStream { read, total ->
                    if (language.tag in cancelled) throw java.io.InterruptedIOException("cancelled")
                    if (total > 0) progress[language.tag] = (read.toFloat() / total * 0.85f).coerceAtLeast(0.01f)
                }
                .use { input -> Files.newOutputStream(zipPath).use { input.copyTo(it) } }
            val extracted = config.extractDbAndJson(zipPath, tempDir)
            progress[language.tag] = 0.9f
            val id = DB.connection("main.db").importTables(language)
            config.mergeDb(extracted.dbPath, id)
            tempDir.toFile().deleteRecursively()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            cancelled -= language.tag
            progress.remove(language.tag)
        }
    }
}

