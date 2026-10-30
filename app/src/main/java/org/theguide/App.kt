package org.theguide

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import client
import com.lagradost.nicehttp.Requests
import storeDir
import defaultHeaders
import Mapper
import okHttpClient
import okhttp3.Cache
import okhttp3.OkHttpClient
import parsers.vgr.omegaDir
import java.io.File
import java.util.concurrent.TimeUnit

/** Sets up the shared code's network and folders before anything reads them. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        storeDir = File(filesDir, "data")
        omegaDir = File(filesDir, "omega")
        okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .cache(Cache(File(cacheDir, "http_cache"), 50L * 1024L * 1024L))
            .build()
        client = Requests(okHttpClient, defaultHeaders, defaultCacheTime = 6, defaultCacheTimeUnit = TimeUnit.HOURS, responseParser = Mapper)
    }

    companion object {
        lateinit var instance: App
            private set
        val context: Context get() = instance

        /** True on Android TV and Google TV: the TV screens are shown. */
        val isTv: Boolean get() =
            (instance.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
