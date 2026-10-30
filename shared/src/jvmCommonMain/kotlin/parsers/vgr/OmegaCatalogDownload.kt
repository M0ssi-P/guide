package parsers.vgr

import client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okHttpClient
import okhttp3.Request
import java.io.File

private const val INFO = "https://updater.branham.org/api/table/omega-media-catalog"
/** Where the catalog is cached: ~/TheGuide/omega on the desktop; Android points it into the app's files. */
var omegaDir: File = File(System.getProperty("user.home"), "TheGuide/omega")
private val dir get() = omegaDir.apply { mkdirs() }
private val lock = Mutex()

/** [OmegaCatalog], cached in ~/TheGuide/omega and re-downloaded only for a new version. */
suspend fun OmegaCatalog.get(): OmegaCatalog.Parsed? = loaded ?: lock.withLock {
    loaded ?: withContext(Dispatchers.IO) {
        runCatching {
            val info = Regex(""""version"\s*:\s*(\d+).*?"downloadUrl"\s*:\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
                .find(client.get(INFO).text)
            val cached = dir.listFiles { f -> f.name.startsWith("omni-") }?.maxByOrNull { it.lastModified() }
            val file = if (info == null) cached else File(dir, "omni-${info.groupValues[1]}.bin").also { f ->
                if (!f.isFile) {
                    okHttpClient.newCall(Request.Builder().url(info.groupValues[2]).build()).execute().use { res ->
                        val tmp = File(dir, f.name + ".part")
                        res.body!!.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                        tmp.renameTo(f)
                    }
                    dir.listFiles { o -> o.name.startsWith("omni-") && o != f }?.forEach { it.delete() }
                }
            }
            file?.let { parse(it.readBytes()) }
        }.onFailure { it.printStackTrace() }.getOrNull()?.also { loaded = it }
    }
}
