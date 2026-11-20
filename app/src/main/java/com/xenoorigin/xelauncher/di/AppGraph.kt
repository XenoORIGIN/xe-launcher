package com.xenoorigin.xelauncher.di

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserManager
import android.provider.MediaStore
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.apps.LauncherAppsSource
import com.xenoorigin.xelauncher.categories.BuiltInCategory
import com.xenoorigin.xelauncher.categories.Categorizer
import com.xenoorigin.xelauncher.categories.CategoryRepository
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.home.HomeRepository
import com.xenoorigin.xelauncher.icons.IconRepository
import com.xenoorigin.xelauncher.settings.SettingsStore
import com.xenoorigin.xelauncher.wallpaper.WallpaperController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.io.File

/** Manual dependency container for the whole app. */
class AppGraph(val context: Context) {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val db: XeDatabase = XeDatabase.open(context)
    val settings = SettingsStore(context, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    val apps = LauncherAppsSource(context, scope)
    val icons = IconRepository({ apps.icon(it, context.resources.displayMetrics.densityDpi.coerceAtLeast(480)) }, File(context.cacheDir, "icons"))
    val wallpaper = WallpaperController(context, settings)

    /** Fires when the Home button is pressed while Xe is already in front. */
    val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun categoryLabel(b: BuiltInCategory): String = context.getString(
        when (b) {
            BuiltInCategory.COMMUNICATION -> R.string.cat_communication
            BuiltInCategory.SOCIAL -> R.string.cat_social
            BuiltInCategory.GAMES -> R.string.cat_games
            BuiltInCategory.MEDIA -> R.string.cat_media
            BuiltInCategory.TOOLS -> R.string.cat_tools
            BuiltInCategory.OTHER -> R.string.cat_other
        },
    )

    val categories = CategoryRepository(db, apps.apps, settings.settings, ::categoryLabel)

    val home = HomeRepository(
        db, apps.apps, settings.settings,
        folderNameFor = { categoryLabel(Categorizer.categorize(it.key.packageName, it.androidCategory)) },
        defaults = ::defaultDockApps,
        isInstalled = { pkg -> runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess },
    )

    init {
        scope.launch {
            categories.ensureDefaults()
            wallpaper.restore()
        }
        scope.launch { apps.packageChanges.collect { pkg -> icons.invalidate(pkg); home.pruneUninstalled() } }
        scope.launch {
            apps.apps.drop(1).collect { list -> if (list.isNotEmpty()) home.seedDefaultIfEmpty() }
        }
    }

    /** Phone, messages, camera and browser — whichever the user has set as default. */
    private fun defaultDockApps(): List<AppKey> {
        val pm = context.packageManager
        val serial = context.getSystemService(UserManager::class.java).getSerialNumberForUser(Process.myUserHandle())
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
        )
        val launchables = apps.apps.value
        return intents.mapNotNull { i ->
            val pkg = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName ?: return@mapNotNull null
            launchables.firstOrNull { it.key.packageName == pkg && it.key.userSerial == serial }?.key
        }
    }
}
