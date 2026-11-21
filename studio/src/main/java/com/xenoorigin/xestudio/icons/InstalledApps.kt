package com.xenoorigin.xestudio.icons

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An app on the phone, keyed exactly like Xe Launcher's AppKey.encode() ("package/activity#userSerial"),
 * so an override made here lands on the same tile in Xe.
 */
data class InstalledApp(val key: String, val label: String, val icon: Drawable?)

fun interface AppSource { suspend fun apps(): List<InstalledApp> }

/** Every launchable app for every profile on the phone, sorted by name. */
class LauncherAppSource(private val context: Context) : AppSource {
    override suspend fun apps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val la = context.getSystemService(LauncherApps::class.java)
        val um = context.getSystemService(UserManager::class.java)
        la.profiles.flatMap { user ->
            val serial = um.getSerialNumberForUser(user)
            la.getActivityList(null, user).map { a ->
                InstalledApp(
                    "${a.componentName.packageName}/${a.componentName.className}#$serial",
                    a.label.toString(),
                    runCatching { a.getIcon(0) }.getOrNull(),
                )
            }
        }.sortedBy { it.label.lowercase() }
    }
}
