package mvvm

import APP_VERSION
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import rs.ProgressInputStream
import client
import isNewerVersion
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import loadData
import saveData
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant

@Serializable
data class GithubRelease(
    val tag_name: String,
    val html_url: String,
    val body: String? = null,
    val assets: List<Asset>?
) {
    @Serializable
    data class Asset(
        val name: String,
        val browser_download_url: String,
        val size: Long,
        val digest: String,

    )
}

class UpdateViewModel: ViewModel() {
    var updateInfo by mutableStateOf<GithubRelease?>(null)
        private set

    var checking by mutableStateOf(false)
        private set

    fun checkForUpdates() {
        val lastCheck = loadData<Instant>("update_last_checked")
        val now = Instant.now()

        if (lastCheck != null && Duration.between(lastCheck, now).toHours() < 24) {
            return
        }

        viewModelScope.launch {
            checking = true
            try {
                val latest = client.get("https://api.github.com/repos/${dbNames.OWNER}/${dbNames.REPO}/releases/latest").parsed<GithubRelease>()
                if (isNewerVersion(APP_VERSION, latest.tag_name)) {
                    updateInfo = latest
                }
                updateInfo = latest
                saveData("update_last_checked", now)
            } catch (e: Exception) {
                println("Update check failed: ${e.message}")
            } finally {
                checking = false
            }
        }
    }

    suspend fun downloadAsset(
        asset: GithubRelease.Asset,
        destination: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): Boolean {
        val tempFile = File(destination.absolutePath + ".tmp")
        try {

            client.get(asset.browser_download_url).let { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val body = response.body
                val totalBytes = body.contentLength()

                body.use { body ->
                    val inputStream = ProgressInputStream(body.byteStream(), totalBytes, onProgress)
                    inputStream.use { inputStream ->
                        tempFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                            output.flush()
                        }
                    }
                }
            }

            if (destination.exists()) destination.delete()
            tempFile.renameTo(destination)

            println("Download complete: ${destination.name}")
            return true
        } catch (e: Exception) {
            println("Download failed: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            return false
        }
    }

    fun runUpdaterAndApp(updaterFile: File) {
        try {
            println("Running updater...")
            ProcessBuilder(updaterFile.absolutePath)
                .inheritIO()
                .start()

            println("Updater finished, launching app...")
            System.exit(0)
        } catch (e: Exception) {
            println("Failed to run updater or launch app: ${e.message}")
        }
    }
}