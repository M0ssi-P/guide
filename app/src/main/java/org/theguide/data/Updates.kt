package org.theguide.data

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.theguide.App
import org.theguide.BuildConfig
import parsers.vgr.VgrMedia
import java.io.File

/**
 * New versions from the same GitHub releases as the desktop: the release's .apk. Downloads in the app, then hands it to
 * Android's installer (which asks the user once to allow installs from The Guide).
 */
object Updates {
    enum class Stage { None, Available, Downloading, Ready, Installing, Done }

    var stage by mutableStateOf(Stage.None); private set
    var version by mutableStateOf(""); private set
    var sizeMb by mutableStateOf(0); private set
    var pct by mutableFloatStateOf(0f); private set
    /** "Title" → "what it does", from the release notes' bullet lines. */
    var notes by mutableStateOf<List<Pair<String, String>>>(emptyList()); private set
    private var url: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val file get() = File(App.context.cacheDir, "update.apk")

    private fun newer(a: String, b: String): Boolean {
        val x = a.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) { val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }; if (d != 0) return d > 0 }
        return false
    }

    private fun parseNotes(body: String) = body.lines().map { it.trim() }.filter { it.startsWith("- ") || it.startsWith("* ") }.map { line ->
        val t = line.drop(2).replace("**", "")
        val cut = t.indexOfAny(charArrayOf(':', '.')).takeIf { it in 1..40 }
        if (cut != null) t.substring(0, cut).trim() to t.substring(cut + 1).trim() else "" to t
    }.take(4)

    fun check() = scope.launch {
        val current = BuildConfig.VERSION_NAME
        // just updated: say what is new, once
        if (Prefs.updateSeen.isNotEmpty() && Prefs.updateSeen != current) { version = current; stage = Stage.Done }
        Prefs.updateSeen = current
        runCatching {
            val json = withContext(Dispatchers.IO) { client.get("https://api.github.com/repos/M0ssi-P/guide/releases/latest").text }
            val o = JSONObject(json)
            val tag = o.getString("tag_name")
            val assets = o.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }.firstOrNull { it.getString("name").endsWith(".apk", true) }
            if (stage == Stage.Done) notes = parseNotes(o.optString("body"))
            if (apk != null && newer(tag, current) && stage == Stage.None) {
                version = tag.removePrefix("v"); url = apk.getString("browser_download_url")
                sizeMb = Math.round(apk.optLong("size") / 1e6).toInt()
                notes = parseNotes(o.optString("body"))
                stage = Stage.Available
            }
        }
    }

    fun download() {
        val u = url ?: return
        stage = Stage.Downloading; pct = .02f
        scope.launch {
            runCatching { VgrMedia.download(u, file) { pct = it } }
                .onSuccess { pct = 1f; stage = Stage.Ready }
                .onFailure { stage = Stage.Available }
        }
    }

    /** Hands the downloaded app to Android's installer; The Guide restarts on the new version. */
    fun install() {
        stage = Stage.Installing
        val uri = FileProvider.getUriForFile(App.context, "${App.context.packageName}.files", file)
        App.context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun dismissDone() { stage = Stage.None }
}
