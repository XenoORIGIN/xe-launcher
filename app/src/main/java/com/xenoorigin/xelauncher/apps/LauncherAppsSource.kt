package com.xenoorigin.xelauncher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class AndroidAppPlatform(private val context: Context) : AppPlatform {
    private val la = context.getSystemService(LauncherApps::class.java)
    private val um = context.getSystemService(UserManager::class.java)

    fun handle(serial: Long): UserHandle? = um.getUserForSerialNumber(serial)

    override fun profiles(): List<Profile> {
        val me = android.os.Process.myUserHandle()
        return um.userProfiles.map { Profile(um.getSerialNumberForUser(it), it == me) }
    }

    override fun activities(profile: Profile, packageName: String?): List<RawActivity> {
        val user = handle(profile.serial) ?: return emptyList()
        return runCatching { la.getActivityList(packageName, user) }.getOrDefault(emptyList()).map {
            RawActivity(
                packageName = it.componentName.packageName,
                activity = it.componentName.className,
                label = it.label?.toString().orEmpty(),
                androidCategory = it.applicationInfo.category,
                installedAt = it.firstInstallTime,
            )
        }
    }

    override fun listen(onPackageChanged: (String, Long) -> Unit, onProfilesChanged: () -> Unit): () -> Unit {
        val cb = object : LauncherApps.Callback() {
            private fun each(pkgs: Array<out String>, user: UserHandle) {
                val s = um.getSerialNumberForUser(user); pkgs.forEach { onPackageChanged(it, s) }
            }
            override fun onPackageRemoved(p: String, u: UserHandle) = each(arrayOf(p), u)
            override fun onPackageAdded(p: String, u: UserHandle) = each(arrayOf(p), u)
            override fun onPackageChanged(p: String, u: UserHandle) = each(arrayOf(p), u)
            override fun onPackagesAvailable(p: Array<out String>, u: UserHandle, r: Boolean) = each(p, u)
            override fun onPackagesUnavailable(p: Array<out String>, u: UserHandle, r: Boolean) = each(p, u)
            override fun onPackagesSuspended(p: Array<out String>, u: UserHandle) = each(p, u)
            override fun onPackagesUnsuspended(p: Array<out String>, u: UserHandle) = each(p, u)
        }
        la.registerCallback(cb, Handler(Looper.getMainLooper()))
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = onProfilesChanged()
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED); addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE); addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        }
        androidx.core.content.ContextCompat.registerReceiver(context, receiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        return { la.unregisterCallback(cb); runCatching { context.unregisterReceiver(receiver) } }
    }
}

class LauncherAppsSource(private val context: Context, scope: CoroutineScope) : AppSource {
    private val platform = AndroidAppPlatform(context)
    private val la = context.getSystemService(LauncherApps::class.java)
    private val catalog = AppCatalog(platform, scope, ownPackage = context.packageName).also { it.start() }

    override val apps: StateFlow<List<AppEntry>> = catalog.apps

    private fun cn(k: AppKey) = ComponentName(k.packageName, k.activity)

    override fun launch(key: AppKey, sourceBounds: Rect?, opts: Bundle?) {
        val user = platform.handle(key.userSerial) ?: return
        runCatching { la.startMainActivity(cn(key), user, sourceBounds, opts) }
    }

    override fun openAppInfo(key: AppKey) {
        val user = platform.handle(key.userSerial) ?: return
        runCatching { la.startAppDetailsActivity(cn(key), user, null, null) }
    }

    override fun uninstall(key: AppKey) {
        val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:${key.packageName}"))
            .putExtra(Intent.EXTRA_USER, platform.handle(key.userSerial))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }

    private fun query(pkg: String?, user: UserHandle): List<ShortcutInfo> {
        if (!la.hasShortcutHostPermission()) return emptyList()
        val q = LauncherApps.ShortcutQuery().setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
        )
        if (pkg != null) q.setPackage(pkg)
        return runCatching { la.getShortcuts(q, user) }.getOrNull().orEmpty()
    }

    override fun shortcuts(key: AppKey): List<ShortcutInfo> {
        val user = platform.handle(key.userSerial) ?: return emptyList()
        return query(key.packageName, user).filter { it.activity == null || it.activity == cn(key) }
            .sortedBy { it.rank }.take(4)
    }

    override fun allShortcuts(): List<ShortcutInfo> =
        platform.profiles().mapNotNull { platform.handle(it.serial) }.flatMap { query(null, it) }

    override fun startShortcut(s: ShortcutInfo, bounds: Rect?) {
        runCatching { la.startShortcut(s, bounds, null) }
    }

    override fun icon(key: AppKey, densityDpi: Int): Drawable? {
        val user = platform.handle(key.userSerial) ?: return null
        return runCatching { la.resolveActivity(Intent().setComponent(cn(key)), user)?.getIcon(densityDpi) }.getOrNull()
    }

    override fun shortcutIcon(s: ShortcutInfo, densityDpi: Int): Drawable? =
        runCatching { la.getShortcutIconDrawable(s, densityDpi) }.getOrNull()
}
