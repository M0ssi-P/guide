package live

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import loadData
import mvvm.ShareViewModels
import parsers.vgr.VgrMedia
import saveData
import store.FileStore
import storeDir
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * At launch and every day after: fetches new letters and brings installed Table languages up to date in the background. The download sits in
 * the app's folder, so one cut off by closing the app picks up where it stopped on the next launch; the language is
 * only replaced, in one transaction, once the new copy is whole.
 */
object TableUpdates {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            while (true) {
                runCatching { parsers.bt.Letters.sync() }
                runCatching { check() }.onFailure { it.printStackTrace() }
                delay(24 * 60 * 60 * 1000L)
            }
        }
    }

    private suspend fun check() {
        ShareViewModels.globalViewModel.dbReady.await()
        // the store's etag of each language's copy on this computer
        val etags = loadData<HashMap<String, String>>("table-etags") ?: HashMap()
        for (t in TableInstaller.installed().filter { it.iso3.isNotBlank() }) {
            val iso3 = t.iso3.lowercase()
            val latest = FileStore.tableEtag(iso3) ?: continue
            val mine = etags[iso3]
            // installed before these checks: taken as current
            if (mine == null) { etags[iso3] = latest; saveData("table-etags", etags); continue }
            if (mine == latest) continue
            val dir = File(storeDir, "updates").apply { mkdirs() }
            val gz = File(dir, "$iso3.db.gz")
            val db = File(dir, "$iso3.db")
            try {
                VgrMedia.download(FileStore.tableUrl(iso3), gz) {}
                GZIPInputStream(gz.inputStream().buffered(), 64 * 1024).use { input -> db.outputStream().use { input.copyTo(it, 256 * 1024) } }
                ShareViewModels.globalViewModel.mergeDb(db.toPath(), t.id, replace = true)
                etags[iso3] = latest
                saveData("table-etags", etags)
            } finally {
                gz.delete(); db.delete()
            }
        }
    }
}
