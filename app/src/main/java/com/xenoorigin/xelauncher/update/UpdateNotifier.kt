package com.xenoorigin.xelauncher.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xenoorigin.xelauncher.BuildConfig
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Looks for a new version now and then (the launcher is always running) and announces each new
 * version once with a notification. [available] drives the in-app "update available" banner.
 */
class UpdateNotifier(
    private val settings: SettingsStore,
    private val check: suspend () -> UpdateResult = { UpdateChecker.check(BuildConfig.VERSION_NAME) },
    private val post: (ReleaseInfo) -> Unit,
) {
    private val _available = MutableStateFlow<ReleaseInfo?>(null)
    val available: StateFlow<ReleaseInfo?> = _available.asStateFlow()

    suspend fun checkNow(): UpdateResult {
        val r = check()
        if (r is UpdateResult.Available) {
            _available.value = r.release
            val s = settings.settings.first()
            if (s.updateNotify && s.lastNotifiedTag != r.release.tag) {
                post(r.release)
                settings.update { it.copy(lastNotifiedTag = r.release.tag) }
            }
        } else if (r is UpdateResult.UpToDate) _available.value = null
        return r
    }

    fun start(scope: CoroutineScope) = scope.launch {
        delay(20_000) // let the launcher settle after boot
        while (true) {
            runCatching { checkNow() }
            delay(6 * 60 * 60 * 1000L)
        }
    }

    companion object {
        private const val CHANNEL = "updates"

        /** Posts "Xe Launcher x.y.z is ready"; tapping it downloads the APK in the browser. */
        fun systemPoster(context: Context): (ReleaseInfo) -> Unit = { r ->
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.updates_channel), NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(
                context, 1, Intent(Intent.ACTION_VIEW, Uri.parse(r.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(context.getString(R.string.update_notif_title, r.tag.removePrefix("v")))
                .setContentText(context.getString(R.string.update_notif_text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) runCatching { nm.notify(7001, n) }
        }
    }
}
