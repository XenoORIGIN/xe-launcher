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
        // High importance so the alert pops up on screen, with Xe's glass chime. A channel's importance and
        // sound can't change once it's made, hence a new id for each change.
        const val CHANNEL_ID = "updates_alert_v2"
        private val LEGACY_CHANNELS = listOf("updates", "updates_alert")

        /** Creates the update channel (with the chime) and removes the older ones. */
        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            LEGACY_CHANNELS.forEach { runCatching { nm.deleteNotificationChannel(it) } }
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val chime = Uri.parse("android.resource://${context.packageName}/${R.raw.xe_chime}")
            val attrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.updates_channel), NotificationManager.IMPORTANCE_HIGH).apply { setSound(chime, attrs) },
            )
        }

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
            ensureChannel(context)
            val open = PendingIntent.getActivity(
                context, 1, Intent(Intent.ACTION_VIEW, Uri.parse(r.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
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
