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
        // the opening sting plays over the app on a cold start (not after a rotation); "intro_at" freezes it, for checking frames
        val introAt = intent.getLongExtra("intro_at", -1L).takeIf { it >= 0 }
        var intro = savedInstanceState == null
        val reduced = android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        setContent {
            val showIntro = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(intro) }
            androidx.compose.foundation.layout.Box {
                if (tv) TvApp() else PhoneApp()
                if (showIntro.value) org.theguide.ui.Intro(
                    org.theguide.ui.Fonts.family(org.theguide.ui.Face.Serif, 300, false, 72f), tv, reduced,
                    onSound = ::playIntroSound, onDone = { showIntro.value = false; intro = false }, at = introAt,
                )
            }
        }
        // for the weekly letter's notification
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    /** The sting's sound on the media stream: never when the phone is on silent or vibrate, or when turned off. */
    private fun playIntroSound() {
        if (!org.theguide.data.Prefs.openingSound) return
        val am = getSystemService(android.media.AudioManager::class.java)
        if (!App.isTv && am.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL) return
        runCatching {
            android.media.MediaPlayer.create(this, R.raw.intro, android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build(), am.generateAudioSessionId())
                ?.apply { setOnCompletionListener { it.release() }; start() }
        }
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
