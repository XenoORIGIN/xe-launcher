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
    /** Shows the alert; true only when it was really shown (false while Android blocks Xe's notifications). */
    private val post: (ReleaseInfo) -> Boolean,
) {
    private val _available = MutableStateFlow<ReleaseInfo?>(null)
    val available: StateFlow<ReleaseInfo?> = _available.asStateFlow()

    suspend fun checkNow(): UpdateResult {
        val r = check()
        if (r is UpdateResult.Available) {
            _available.value = r.release
            val s = settings.settings.first()
            // Remember the tag only once the alert was really shown, so blocked alerts are retried later.
            if (s.updateNotify && s.lastNotifiedTag != r.release.tag && post(r.release)) {
                settings.update { it.copy(lastNotifiedTag = r.release.tag) }
            }
        } else if (r is UpdateResult.UpToDate) _available.value = null
        return r
    }

    private val lastCheck = java.util.concurrent.atomic.AtomicLong(Long.MIN_VALUE / 2)

    /** Checks unless one ran in the last [MIN_GAP_MS]; true when it checked. Called whenever Home comes back. */
    suspend fun checkIfDue(now: Long = android.os.SystemClock.elapsedRealtime()): Boolean {
        val last = lastCheck.get()
        if (now - last < MIN_GAP_MS || !lastCheck.compareAndSet(last, now)) return false
        runCatching { checkNow() }
        return true
    }

    fun start(scope: CoroutineScope) = scope.launch {
        delay(FIRST_CHECK_MS)
        while (true) {
            checkIfDue()
            delay(INTERVAL_MS)
        }
    }

    companion object {
        const val FIRST_CHECK_MS = 3_000L
        const val INTERVAL_MS = 30 * 60_000L
        const val MIN_GAP_MS = 10 * 60_000L
        // High importance so the alert pops up on screen. (A channel's importance can't change once made, hence a new id.)
        private const val CHANNEL = "updates_alert"
        private const val OLD_CHANNEL = "updates"

        /**
         * Shows the update alert right now for the newest version on GitHub, so you can see what it looks like.
         * False when Android is blocking Xe's notifications.
         */
        fun postTest(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            systemPoster(context)(ReleaseInfo("v" + BuildConfig.VERSION_NAME, UpdateChecker.STABLE_APK, ""))
            return true
        }

        /** Posts "Xe Launcher x.y.z is ready"; tapping it downloads the APK in the browser. */
        fun systemPoster(context: Context): (ReleaseInfo) -> Boolean = { r ->
            val nm = context.getSystemService(NotificationManager::class.java)
            runCatching { nm.deleteNotificationChannel(OLD_CHANNEL) }
            nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.updates_channel), NotificationManager.IMPORTANCE_HIGH))
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .build()
            NotificationManagerCompat.from(context).areNotificationsEnabled() && runCatching { nm.notify(7001, n) }.isSuccess
        }
    }
}
