package org.theguide.data

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.theguide.App
import songs.DropReceiver
import songs.SongDropReceiver
import songs.SongDropSender

/** Song books over the Wi-Fi (shared SongDrop): this device waiting to receive, or finding devices to send to. */
object Drop {
    /** This device while it waits for songs (shown with its code and QR); null when not receiving. */
    var waiting by mutableStateOf<DropReceiver?>(null); private set
    /** Names of the books (or songs) that arrived while waiting. */
    val arrived = mutableStateListOf<String>()
    /** Devices nearby waiting for songs. */
    var nearby by mutableStateOf<List<DropReceiver>>(emptyList()); private set

    private var receiver: SongDropReceiver? = null
    private val lock by lazy { (App.context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock("guide-drop").apply { setReferenceCounted(true) } }

    val deviceName: String get() = (if (App.isTv) "TV · " else "") + (Build.MODEL ?: "Android").trim()

    fun receive() {
        if (receiver != null) return
        arrived.clear()
        receiver = SongDropReceiver(deviceName) { bytes -> Songs.importBytes(bytes, "Received")?.let { arrived.add(it) } }
        waiting = runCatching { receiver!!.start() }.getOrNull()
    }

    fun stopReceiving() { receiver?.stop(); receiver = null; waiting = null }

    fun look() { runCatching { lock.acquire() }; SongDropSender.listen { nearby = it } }

    fun stopLooking() { SongDropSender.stop(); runCatching { if (lock.isHeld) lock.release() } }

    /** Sends the books [ids] as one Guide songbook file; true when the other device took it. */
    suspend fun send(to: DropReceiver, ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        SongDropSender.send(to, Songs.export(ids, "guide") {}.readBytes())
    }
}
