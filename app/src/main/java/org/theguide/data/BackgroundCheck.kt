package org.theguide.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.theguide.MainActivity
import org.theguide.R
import parsers.bt.Letter
import parsers.bt.Letters
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * At launch and once a day, in the background: brings installed Table languages up to date and fetches new letters
 * (with a notification when one arrives). It keeps going when the app is closed, and after a restart the system runs
 * it again, picking up a cut-off download where it stopped.
 */
class BackgroundCheck(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        Prefs.letterLangs.forEach { lang ->
            val book = Letters.book(lang)
            // a handful is the week's letter; more is a first download, not news
            if (book.sync() in 1..3) book.page().firstOrNull()?.let { notify(it, lang) }
        }
        Tables.stale().forEach { iso3 -> Tables.download(iso3) { _, _ -> } }
    }.fold({ Result.success() }, { Result.retry() })

    private fun notify(l: Letter, lang: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("letters", "Weekly letters", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(("letter" + lang).hashCode(), Notification.Builder(ctx, "letters").setSmallIcon(R.drawable.ic_lamp)
            .setContentTitle(l.salutation ?: "A new letter from Bro. Joseph").setContentText("${l.code} ${l.title}").setContentIntent(open).setAutoCancel(true).build())
    }

    companion object {
        fun schedule(context: Context) {
            val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val work = WorkManager.getInstance(context)
            work.enqueueUniquePeriodicWork("daily-check", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<BackgroundCheck>(1, TimeUnit.DAYS).setConstraints(online).build())
            work.enqueueUniqueWork("launch-check", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<BackgroundCheck>().setConstraints(online).build())
        }
    }
}
