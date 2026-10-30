package org.theguide

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import org.theguide.audio.PlaybackService
import org.theguide.ui.phone.PhoneApp
import org.theguide.ui.tv.TvApp

class MainActivity : ComponentActivity() {
    private var controller: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        // "tv" extra: the TV screens on any device, for checking them without a TV
        val tv = App.isTv || intent.getBooleanExtra("tv", false)
        setContent { if (tv) TvApp() else PhoneApp() }
    }

    override fun onStart() {
        super.onStart()
        // binds the playback service, so a sermon keeps playing with the screen off
        controller = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
    }

    override fun onStop() {
        controller?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onStop()
    }
}
