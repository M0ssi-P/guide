package ui.screens.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.AvailablePick
import components.global.InstalledPick
import components.global.PickItem
import components.global.PickerModal
import components.global.PickerNote
import components.global.PickerSection
import kotlinx.coroutines.launch
import live.TableInstaller
import mvvm.ShareViewModels
import parsers.vgr.VgrViewModel
import ui.UiState
import ui.screens.thisweek.hand
import ui.theme.LocalTheme

/** Code shown on the Table's language chips, e.g. ENG. */
fun tableLanguageCode(tag: String) = TableInstaller.installed().firstOrNull { it.tag == tag }?.iso3?.ifBlank { null }?.uppercase() ?: tag.uppercase()

/** "Table languages": switch between installed ones (the page crossfades) or download another (download, unpack, merge). */
@Composable
fun TableLanguageModal(vgr: VgrViewModel, onDismiss: () -> Unit) {
    val theme = LocalTheme.current
    // installs keep going when the window is closed
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO) }
    val settings by vgr.uiSettings.collectAsState()
    val offered by ShareViewModels.globalViewModel.languageData.collectAsState()
    var query by remember { mutableStateOf("") }
    val busy = TableInstaller.progress.keys.toSet()
    // re-read after an install finishes (busy shrinks) or the server list changes
    val installed = remember(busy, offered) { TableInstaller.installed() }
    val available = remember(busy, offered) { TableInstaller.available() }
    val q = query.trim().lowercase()
    fun match(vararg s: String) = q.isEmpty() || s.joinToString(" ").lowercase().contains(q)

    PickerModal("Table languages", "Switch what you read and hear, or get another language.", query, { query = it }, "Find a language", onDismiss) {
        PickerSection("ON THIS COMPUTER")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            installed.filter { match(it.name, it.localName, it.iso3, it.tag) }.forEachIndexed { i, l ->
                val on = l.tag == settings.currentTableLanguage
                InstalledPick(PickItem(l.tag, l.iso3.uppercase().ifBlank { l.tag.uppercase() }, l.name, l.localName, if (on) "Reading and listening now" else "Installed"), on, i) {
                    vgr.setTableLanguage(l.tag); onDismiss()
                }
            }
        }
        PickerSection("AVAILABLE TO DOWNLOAD") {
            val checking = offered is UiState.Loading
            Text(if (checking) "Checking…" else "Check for new languages", color = theme.colors.primaryBright, fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = !checking) { ShareViewModels.globalViewModel.loadLanguagesAndSave() }.hand())
        }
        val list = available.filter { match(it.name, it.localName, it.iso63903 ?: "", it.tag) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            list.forEachIndexed { i, l ->
                val p = TableInstaller.progress[l.tag]
                AvailablePick(
                    PickItem(l.tag, l.iso63903?.uppercase() ?: "", l.name, l.localName, "Sermons, quotes and search", p, p?.let { TableInstaller.stage(it) }), i, showCode = false,
                    onGet = { scope.launch { TableInstaller.install(l) } }, onCancel = { TableInstaller.cancel(l.tag) }
                )
            }
            if (list.isEmpty()) PickerNote(if (q.isNotEmpty()) "No language matches “$query”." else "You have every language the server offers right now.")
        }
        Text(
            "New languages are merged into your library, so search, highlights and Live work in them straight away. The audio for a sermon downloads separately when you play it.",
            color = theme.colors.text.copy(alpha = 0.7f), fontSize = 11.5.sp, lineHeight = 16.sp
        )
    }
}
