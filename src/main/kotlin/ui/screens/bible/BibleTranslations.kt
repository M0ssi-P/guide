package ui.screens.bible

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import components.global.AvailablePick
import components.global.CurrentPick
import components.global.DownloadedPick
import components.global.PickItem
import components.global.PickerChips
import components.global.PickerModal
import components.global.PickerNote
import components.global.PickerSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import loadData
import mvvm.BibleViewModel
import mvvm.ShareViewModels
import parsers.bible.models.IBibleVersion
import saveData

/** Whole translations saved for offline reading: every chapter cached the same way the reader caches one. */
object BibleDownloads {
    val downloaded = mutableStateOf(loadData<ArrayList<Int>>("bible-downloaded")?.toSet() ?: emptySet())
    /** 0..1 while downloading, by version id. */
    val progress = mutableStateMapOf<Int, Float>()
    private val cancelled = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun cancel(id: Int) { cancelled += id }

    fun download(v: IBibleVersion) {
        if (v.id in progress) return
        progress[v.id] = 0.01f
        scope.launch {
            val chapters = v.books.orEmpty().flatMap { it.chapters }.filter { it.human.toIntOrNull() != null }
            var done = 0
            var failed = 0
            try {
                chapters.chunked(8).forEach { batch ->
                    if (v.id in cancelled) return@launch
                    coroutineScope { batch.map { async { chapterIn(v, it.usfm) } }.awaitAll() }.forEach { if (it == null) failed++ }
                    done += batch.size
                    progress[v.id] = (done.toFloat() / chapters.size).coerceAtLeast(0.01f)
                }
                if (chapters.isNotEmpty() && failed == 0) {
                    downloaded.value = downloaded.value + v.id
                    saveData("bible-downloaded", ArrayList(downloaded.value))
                }
            } finally {
                cancelled -= v.id
                progress.remove(v.id)
            }
        }
    }
}

private fun IBibleVersion.item(meta: String, p: Float? = null) =
    PickItem("$id", localAbbreviation.ifBlank { abbreviation }, localTitle.ifBlank { title }, language.localName.ifBlank { language.name }, meta, p, p?.let { "${(it * 100).toInt()}%" })

/**
 * "Bible translations": what you are reading pinned on top, downloaded ones (ticked), then every translation known for the
 * languages loaded so far. Picking one closes the window and the reader crossfades to the same chapter in it.
 */
@Composable
fun BibleTranslationsModal(model: BibleViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("All") }
    var loadedLanguage by remember { mutableStateOf<String?>(null) }
    val downloaded by BibleDownloads.downloaded
    val cur = model.currentVersion
    // shortcut: lists versions of languages already loaded (this one and any opened before); others come in via the search row below.
    val all = remember(model.versions, loadedLanguage) { (model.versions + ShareViewModels.bibleVersions.values.flatten()).distinctBy { it.id } }
    val q = query.trim().lowercase()
    fun langOf(v: IBibleVersion) = v.language.localName.ifBlank { v.language.name }
    fun keep(v: IBibleVersion) = (lang == "All" || langOf(v) == lang) &&
        (q.isEmpty() || "${v.abbreviation} ${v.localAbbreviation} ${v.title} ${v.localTitle} ${v.language.name} ${v.language.localName}".lowercase().contains(q))

    LaunchedEffect(loadedLanguage) {
        val tag = loadedLanguage ?: return@LaunchedEffect
        model.loadVersions(tag)
        model.loadVersions(model.currentLanguage?.tag) // back to the reader's own list
    }

    PickerModal(
        "Bible translations", "Pick what you read. Downloaded ones work offline.", query, { query = it }, "Find a translation or language", onDismiss,
        top = { cur?.let { CurrentPick(it.item(langOf(it))) } }
    ) {
        PickerChips(listOf("All") + all.map(::langOf).distinct().sorted(), lang) { lang = it }

        PickerSection("DOWNLOADED · PINNED")
        val pinned = all.filter { it.id != cur?.id && it.id in downloaded && keep(it) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pinned.forEachIndexed { i, v -> DownloadedPick(v.item("${langOf(v)} · Offline"), i) { model.switchVersion(v); onDismiss() } }
            if (pinned.isEmpty()) PickerNote("Nothing else downloaded matches.")
        }

        PickerSection("ALL TRANSLATIONS")
        val others = all.filter { it.id != cur?.id && it.id !in downloaded && keep(it) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            others.forEachIndexed { i, v ->
                val p = BibleDownloads.progress[v.id]
                // clicking the row reads it online; Download keeps the whole translation for offline use
                AvailablePick(v.item("${langOf(v)} · reads online", p), i, showCode = true, onGet = { BibleDownloads.download(v) }, onCancel = { BibleDownloads.cancel(v.id) }) {
                    model.switchVersion(v); onDismiss()
                }
            }
            if (others.isEmpty()) PickerNote("Every translation that matches is downloaded.")
            if (q.length >= 2) {
                val known = all.map { it.language.tag }.toSet()
                model.languages.filter { it.tag !in known && (it.name.lowercase().contains(q) || it.localName.lowercase().contains(q)) }.take(5).forEachIndexed { i, l ->
                    AvailablePick(PickItem(l.tag, "", "Translations in ${l.name}", l.localName, "${l.totalVersions ?: 0} translations · load the list"), others.size + i, showCode = false,
                        onGet = { loadedLanguage = l.tag }, onCancel = null, action = "Show")
                }
            }
        }
    }
}
