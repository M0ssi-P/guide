package org.theguide.audio

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Keeps the sermon playing with the screen off, with the media notification and lock-screen controls. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSession.Builder(this, Audio.main).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
