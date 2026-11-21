package com.xenoorigin.xelauncher.badges

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat

/**
 * Reads the notification shade (with the user's permission) to count each app's unread notifications.
 * Nothing is stored or sent anywhere: only the counts, kept in memory for the icons.
 */
class BadgeListener : NotificationListenerService() {
    override fun onListenerConnected() = recount()
    override fun onListenerDisconnected() = Badges.set(emptyMap())
    override fun onNotificationPosted(sbn: StatusBarNotification?) = recount()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = recount()

    private fun recount() {
        val um = getSystemService(UserManager::class.java)
        val active = runCatching { activeNotifications?.toList() }.getOrNull() ?: return
        Badges.set(badgeCounts(active.map { it.toNotif(um) }, own = packageName))
    }

    private fun StatusBarNotification.toNotif(um: UserManager?) = Notif(
        pkg = packageName,
        userSerial = runCatching { um?.getSerialNumberForUser(user) }.getOrNull() ?: 0L,
        number = notification.number,
        group = notification.group ?: groupKey,
        isSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        clearable = isClearable && !isOngoing,
    )

    companion object {
        fun hasAccess(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Opens the system page where Xe can be given notification access (straight to Xe where supported). */
        fun requestAccess(context: Context) {
            val detail = Intent("android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS")
                .putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", ComponentName(context, BadgeListener::class.java).flattenToString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(detail) }.isFailure) runCatching { context.startActivity(list) }
        }
    }
}
