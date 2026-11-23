package com.xenoorigin.xelauncher.di

import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.icons.CustomIconStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
    val customIcons = CustomIconStore(File(context.filesDir, "custom_icons"))
    /** Xe's copy of the icon theme applied in Xe Studio. */
    val studioTheme = com.xenoorigin.xelauncher.studio.StudioThemeStore(
        File(context.filesDir, "studio_theme"), context.contentResolver,
        isTrusted = { com.xenoorigin.xelauncher.studio.StudioThemeStore.studioIsGenuine(context) },
    )
    val icons = IconRepository(
        { apps.icon(it, context.resources.displayMetrics.densityDpi.coerceAtLeast(480)) }, File(context.cacheDir, "icons"),
        customIcons::get, customIcons::version,
        studioTheme = { studioTheme.theme.value }, studioImage = studioTheme::image,
    )

    /** Installed apps with the user's own names applied. Everything shown on screen uses this list. */
    val labeledApps: StateFlow<List<AppEntry>> = combine(apps.apps, settings.settings.map { it.labelOverrides }.distinctUntilChanged()) { list, names ->
        if (names.isEmpty()) list else list.map { a -> names[a.key.encode()]?.let { a.copy(label = it) } ?: a }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val wallpaper = WallpaperController(context, settings)
    val widgetHost = com.xenoorigin.xelauncher.widgets.XeWidgetHost(context)
    val updates = com.xenoorigin.xelauncher.update.UpdateNotifier(settings, post = com.xenoorigin.xelauncher.update.UpdateNotifier.systemPoster(context))

    /** Fires when the Home button is pressed while Xe is already in front. */
    val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val crashes = com.xenoorigin.xelauncher.bugs.CrashStore(File(context.filesDir, "crashes"))

    fun categoryLabel(b: BuiltInCategory): String = context.getString(
        when (b) {
            BuiltInCategory.COMMUNICATION -> R.string.cat_communication
            BuiltInCategory.SOCIAL -> R.string.cat_social
            BuiltInCategory.GAMES -> R.string.cat_games
            BuiltInCategory.MEDIA -> R.string.cat_media
            BuiltInCategory.TOOLS -> R.string.cat_tools
            BuiltInCategory.SHOPPING -> R.string.cat_shopping
            BuiltInCategory.FINANCE -> R.string.cat_finance
            BuiltInCategory.TRAVEL -> R.string.cat_travel
            BuiltInCategory.WORK -> R.string.cat_work
            BuiltInCategory.OTHER -> R.string.cat_other
        },
    )

    /** Time spent per app (needs Usage access), else launch counts. */
    /** Bumped when Xe comes back to the front or a usage reading lands, so time-spent rankings are read again. */
    val usageTick = kotlinx.coroutines.flow.MutableStateFlow(0)
    val usage = com.xenoorigin.xelauncher.apps.UsageTime(
        com.xenoorigin.xelauncher.apps.UsageTime.systemSource(context),
        runner = { r -> scope.launch(Dispatchers.IO) { r() } },
        onChanged = { usageTick.value++ },
    )
    /** Termi the pet: saved in its own file, credited from creator apps and night rest. */
    val termi = com.xenoorigin.xelauncher.termi.TermiStore(
        java.io.File(context.filesDir, "termi.json"),
        usageToday = com.xenoorigin.xelauncher.termi.TermiUsage.today(context),
        nightScreenMs = com.xenoorigin.xelauncher.termi.TermiUsage.screenOn(context),
        defaultCreators = com.xenoorigin.xelauncher.termi.TermiUsage.installedDefaults(context),
    )
    val categories = CategoryRepository(db, labeledApps, settings.settings, usage::usage, usageTick, ::categoryLabel)

    val home = HomeRepository(
        db, labeledApps, settings.settings,
        folderNameFor = { categoryLabel(Categorizer.categorize(it.key.packageName, it.androidCategory)) },
        defaults = ::defaultDockApps,
        isInstalled = { pkg -> runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess },
    )

    init {
        scope.launch {
            categories.ensureDefaults()
            if (settings.current().categoriesVersion < 2) {
                categories.upgradeBuiltIns()
                settings.update { it.copy(categoriesVersion = 2) }
            }
            wallpaper.restore()
            updates.start(scope)
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
