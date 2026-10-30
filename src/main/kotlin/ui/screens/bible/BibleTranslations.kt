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
import androidx.compose.runtime.rememberCoroutineScope
import mvvm.BibleLibrary
import mvvm.ShareViewModels
import parsers.bible.models.IBibleVersion
import saveData

private fun IBibleVersion.item(meta: String, p: Float? = null) =
    PickItem("$id", localAbbreviation.ifBlank { abbreviation }, localTitle.ifBlank { title }, language.localName.ifBlank { language.name }, meta, p, p?.let { "${(it * 100).toInt()}%" })

private fun mb(bytes: Long) = if (bytes <= 0) "" else " · ${maxOf(1, Math.round(bytes / 1e6))} MB"

/**
 * "Bible translations": what you are reading pinned on top, then every version on the file store grouped by language.
 * Each version is one download; picking one that isn't here yet downloads it, then the reader crossfades to the same
 * chapter in it.
 */
@Composable
fun BibleTranslationsModal(model: BibleViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("All") }
    val scope = rememberCoroutineScope()
    BibleLibrary.changed.value // redraw when a version arrives, updates or goes
    val cur = model.currentVersion
    val byLanguage = remember { mutableStateMapOf<String, List<IBibleVersion>>() }
    LaunchedEffect(model.languages) { model.languages.forEach { l -> byLanguage[l.tag] = model.versionsOf(l.tag) } }
    val q = query.trim().lowercase()
    fun langOf(v: IBibleVersion) = v.language.localName.ifBlank { v.language.name }
    fun keep(v: IBibleVersion) = q.isEmpty() || "${v.abbreviation} ${v.localAbbreviation} ${v.title} ${v.localTitle} ${v.language.name} ${v.language.localName}".lowercase().contains(q)
    fun open(v: IBibleVersion) { model.switchVersion(v); onDismiss() }
    fun get(v: IBibleVersion) { scope.launch { BibleLibrary.download(v) } }

    PickerModal(
        "Bible translations", "Pick what you read. Each one downloads once, then works offline.", query, { query = it }, "Find a translation or language", onDismiss,
        top = { cur?.let { CurrentPick(it.item(langOf(it))) } }
    ) {
        val languages = model.languages.filter { byLanguage[it.tag].orEmpty().isNotEmpty() }
        PickerChips(listOf("All") + languages.map { it.localName }, lang) { lang = it }
        var shown = 0
        languages.filter { lang == "All" || it.localName == lang }.forEach { l ->
            val list = byLanguage[l.tag].orEmpty().filter { it.id != cur?.id && keep(it) }
            if (list.isEmpty()) return@forEach
            PickerSection(l.localName.uppercase())
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                list.forEach { v ->
                    val i = shown++
                    val p = BibleLibrary.progress[v.id]
                    when {
                        p != null -> AvailablePick(v.item("${langOf(v)} · Downloading", p), i, showCode = true, onGet = {}, onCancel = null)
                        BibleLibrary.isDownloaded(v) && BibleLibrary.hasUpdate(v) ->
                            AvailablePick(v.item("${langOf(v)} · A newer edition is ready${mb(BibleLibrary.size(v))}"), i, showCode = true, onGet = { get(v) }, onCancel = null, action = "Update") { open(v) }
                        BibleLibrary.isDownloaded(v) -> DownloadedPick(v.item("${langOf(v)} · On this computer"), i) { open(v) }
                        BibleLibrary.failed[v.id] == true ->
                            AvailablePick(v.item("${langOf(v)} · Couldn't download"), i, showCode = true, onGet = { get(v) }, onCancel = null, action = "Try again") { open(v) }
                        else -> AvailablePick(v.item("${langOf(v)}${mb(BibleLibrary.size(v))}"), i, showCode = true, onGet = { get(v) }, onCancel = null) { open(v) }
                    }
                }
            }
        }
        if (shown == 0) PickerNote(if (model.languages.isEmpty()) "The list comes from the internet the first time; check the connection." else "No other translation matches.")
    }
}
