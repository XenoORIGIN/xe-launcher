package com.xenoorigin.xelauncher

import com.xenoorigin.xelauncher.widgets.clamp
import com.xenoorigin.xelauncher.design.glassTile
import androidx.lifecycle.repeatOnLifecycle
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalHazeState
import com.xenoorigin.xelauncher.design.XeMotion
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.design.liquidGlass
import com.xenoorigin.xelauncher.di.AppGraph
import com.xenoorigin.xelauncher.drawer.AppDrawer
import com.xenoorigin.xelauncher.drawer.CategoryEditor
import com.xenoorigin.xelauncher.drawer.DrawerLook
import com.xenoorigin.xelauncher.drawer.DrawerMenuSheet
import com.xenoorigin.xelauncher.drawer.DrawerState
import com.xenoorigin.xelauncher.drawer.EditorActions
import com.xenoorigin.xelauncher.home.AppCell
import com.xenoorigin.xelauncher.home.DropRequest
import com.xenoorigin.xelauncher.home.EditBar
import com.xenoorigin.xelauncher.home.EditAppSheet
import com.xenoorigin.xelauncher.drawer.ArrangeApps
import com.xenoorigin.xelauncher.drawer.CategoryApps
import com.xenoorigin.xelauncher.drawer.BulkIcons
import com.xenoorigin.xelauncher.home.FolderCell
import com.xenoorigin.xelauncher.home.FolderPopup
import com.xenoorigin.xelauncher.home.GestureResolver
import com.xenoorigin.xelauncher.home.HomeCallbacks
import com.xenoorigin.xelauncher.home.HomeCell
import com.xenoorigin.xelauncher.home.HomeLayout
import com.xenoorigin.xelauncher.home.HomeLook
import com.xenoorigin.xelauncher.home.HomeScreen
import com.xenoorigin.xelauncher.home.IconMenu
import com.xenoorigin.xelauncher.home.MenuAction
import com.xenoorigin.xelauncher.home.MoveToCategorySheet
import com.xenoorigin.xelauncher.home.expandNotificationShade
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalGrayscaleContrast
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.search.SearchPage
import com.xenoorigin.xelauncher.settings.GestureAction
import com.xenoorigin.xelauncher.settings.ThemeMode
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.settings.ui.SettingsHost
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MenuSource { HOME, DRAWER, FOLDER }
private data class MenuState(val bounds: Rect, val app: AppEntry?, val cell: HomeCell?, val source: MenuSource)

@Composable
fun XeApp(graph: AppGraph) {
    val loaded by graph.settings.settings.collectAsState(initial = null)
    val settings = loaded ?: return Box(Modifier.fillMaxSize().background(Color.Black)) {}
    val ctx = LocalContext.current
    val systemOff = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val dark = when (settings.themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.AUTO -> isSystemInDarkTheme() }
    XeTheme(
        dark = dark, accent = Color(settings.accent.toInt()), glassQuality = settings.glassQuality,
        animationSpeed = if (systemOff) AnimationSpeed.OFF else settings.animationSpeed,
        pressSquish = settings.pressSquish, fontScale = settings.fontScale,
        rimTint = rememberRimTint(settings.timeTint), uiStyle = settings.uiStyle,
    ) {
        val haze = remember { HazeState() }
        // Liquid Glass looks through a frosted copy of the wallpaper, lined up with where the wallpaper sits now.
        val glassBg by graph.wallpaper.glassBackdrop.collectAsState()
        // Glass only looks through the copy when it matches what's on Home (not over live ASCII scenes).
        val sees by androidx.compose.runtime.rememberUpdatedState(com.xenoorigin.xelauncher.wallpaper.glassSeesWallpaper(settings))
        val glassBackdrop = remember { com.xenoorigin.xelauncher.design.GlassBackdrop({ if (sees) glassBg else null }, { androidx.compose.ui.geometry.Offset.Zero }, { androidx.compose.ui.geometry.Size(1f, 1f) }) }
        CompositionLocalProvider(
            LocalHazeState provides haze, LocalIconRepository provides graph.icons, LocalGrayscaleContrast provides settings.grayscaleContrast,
            com.xenoorigin.xelauncher.icons.LocalGlyphScale provides settings.glyphScale,
            com.xenoorigin.xelauncher.design.LocalLiquidGlass provides (settings.iconStyle == IconStyle.GLASS_CLEAR),
            com.xenoorigin.xelauncher.design.LocalGlassBackdrop provides glassBackdrop,
        ) {
            val customIcons by graph.customIcons.files.collectAsState()
            val unread by com.xenoorigin.xelauncher.badges.Badges.counts.collectAsState()
            val studioTheme by graph.studioTheme.theme.collectAsState()
            CompositionLocalProvider(
                com.xenoorigin.xelauncher.icons.LocalCustomIcons provides customIcons,
                com.xenoorigin.xelauncher.icons.LocalStudioThemeVersion provides studioTheme?.version,
                com.xenoorigin.xelauncher.design.LocalIconGlass provides (if (settings.animationSpeed == AnimationSpeed.OFF) com.xenoorigin.xelauncher.settings.IconGlass.OFF else settings.iconGlass),
                com.xenoorigin.xelauncher.badges.LocalBadgeCounts provides unread,
                com.xenoorigin.xelauncher.badges.LocalBadgeStyle provides settings.badgeStyle,
            ) { LauncherRoot(graph, settings) }
        }
    }
}

@Composable
private fun LauncherRoot(graph: AppGraph, settings: XeSettings) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val speed = LocalAnimationSpeed.current
    val powerSave = com.xenoorigin.xelauncher.design.rememberPowerSave()
    val scope = rememberCoroutineScope()
    val apps by graph.labeledApps.collectAsState()
    val categories by graph.categories.categories.collectAsState(initial = emptyList())
    val editable by graph.categories.editable.collectAsState(initial = emptyList())
    val layout by graph.home.layout.collectAsState(initial = HomeLayout.Empty)

    val drawer = remember { Animatable(0f) }
    val drawerSounded = remember { booleanArrayOf(false) }
    val drawerState = remember { DrawerState(pageCount = { categories.size.coerceAtLeast(1) }) }
    val homePager = rememberPagerState { layout.pages.size + 1 }
    var searchOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { com.xenoorigin.xelauncher.sound.XeSound.init(ctx) }
    androidx.compose.runtime.SideEffect { com.xenoorigin.xelauncher.sound.XeSound.enabled = settings.clickSounds }
    var termiOpen by remember { mutableStateOf(false) }
    // Each time Home comes back, Termi collects creator-app minutes and the night-rest bonus.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        graph.scope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { graph.termi.onHomeShown() } }
    }
    var drawPad by remember { mutableStateOf(false) }
    var launchBubble by remember { mutableStateOf<Rect?>(null) }
    var menu by remember { mutableStateOf<MenuState?>(null) }
    var openFolder by remember { mutableStateOf<Pair<Long, Rect>?>(null) }
    var drawerMenu by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<Boolean?>(null) } // true = start adding
    var moveFor by remember { mutableStateOf<AppEntry?>(null) }
    var editFor by remember { mutableStateOf<AppEntry?>(null) }
    var pictureFor by remember { mutableStateOf<AppEntry?>(null) }
    var arrange by remember { mutableStateOf<com.xenoorigin.xelauncher.categories.CategoryWithApps?>(null) }
    var bulkIcons by remember { mutableStateOf(false) }
    var manageCategory by remember { mutableStateOf<Long?>(null) }
    var noteFor by remember { mutableStateOf<Long?>(null) }
    var photoFor by remember { mutableStateOf<Long?>(null) }
    var photoVersion by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val launchStats by graph.db.launches().observe().collectAsState(initial = emptyList())
    val photoDir = remember { java.io.File(ctx.filesDir, "widget_photos") }
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = photoFor; photoFor = null
        if (uri != null && id != null) scope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val bmp = android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
                        d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        val big = maxOf(info.size.width, info.size.height)
                        if (big > 1200) { val f = 1200f / big; d.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1)) }
                    }
                    photoDir.mkdirs()
                    java.io.File(photoDir, "$id.jpg").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
                }
            }
            photoVersion++
        }
    }
    var widgetPicker by remember { mutableStateOf(false) }
    /** The widget being moved/resized in place on Home. */
    var widgetEditId by remember { mutableStateOf<Long?>(null) }
    var pendingWidget by remember { mutableStateOf<Pair<Int, android.appwidget.AppWidgetProviderInfo>?>(null) }
    val host = graph.widgetHost
    fun removeWidget(wc: com.xenoorigin.xelauncher.home.WidgetCell) = scope.launch {
        val hid = graph.home.removeWidget(wc.id); if (hid > 0) host.release(hid)
        java.io.File(photoDir, "${wc.id}.jpg").delete()
        graph.settings.update { st -> st.copy(widgetNotes = st.widgetNotes - wc.id.toString()) }
    }
    val conf = androidx.compose.ui.platform.LocalConfiguration.current
    val cellWDp = (conf.screenWidthDp - 20f) / settings.homeColumns
    val cellHDp = (conf.screenHeightDp - 230f) / settings.homeRows
    fun spanOf(info: android.appwidget.AppWidgetProviderInfo) =
        com.xenoorigin.xelauncher.widgets.XeWidgetHost.spanFor(info, density.density, cellWDp, cellHDp, settings.homeColumns, settings.homeRows)
    fun placeWidget(id: Int, info: android.appwidget.AppWidgetProviderInfo) {
        val (w, h) = spanOf(info)
        scope.launch { graph.home.addWidget(homePager.currentPage, w, h, id, null) }
    }
    val configureLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        val p = pendingWidget; pendingWidget = null
        if (p != null) { if (r.resultCode == android.app.Activity.RESULT_OK) placeWidget(p.first, p.second) else host.release(p.first) }
    }
    fun configureOrPlace(id: Int, info: android.appwidget.AppWidgetProviderInfo) {
        val optional = android.os.Build.VERSION.SDK_INT >= 31 &&
            (info.widgetFeatures and android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        if (info.configure == null || optional) { placeWidget(id, info); return }
        pendingWidget = id to info
        val i = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(info.configure)
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        runCatching { configureLauncher.launch(i) }.onFailure {
            // Not openable directly: let the system start it for the host, and place the widget now.
            pendingWidget = null
            runCatching { host.startAppWidgetConfigureActivityForResult(ctx as android.app.Activity, id, 0, 77, null) }
            placeWidget(id, info)
        }
    }
    val bindLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        val p = pendingWidget
        if (p != null) {
            if (r.resultCode == android.app.Activity.RESULT_OK) configureOrPlace(p.first, p.second)
            else { host.release(p.first); pendingWidget = null }
        }
    }
    fun addWidget(choice: com.xenoorigin.xelauncher.widgets.WidgetChoice) {
        widgetPicker = false
        when (choice) {
            is com.xenoorigin.xelauncher.widgets.WidgetChoice.Glass -> scope.launch {
                graph.home.addWidget(homePager.currentPage, choice.kind.spanX.coerceAtMost(settings.homeColumns), choice.kind.spanY, 0, choice.kind.id)
            }
            is com.xenoorigin.xelauncher.widgets.WidgetChoice.Provider -> {
                val info = choice.info
                val id = runCatching { host.allocateAppWidgetId() }.getOrNull()
                if (id == null) { Toast.makeText(ctx, ctx.getString(R.string.widget_failed), Toast.LENGTH_SHORT).show(); return }
                val bound = runCatching { host.manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null) }.getOrDefault(false)
                if (bound) configureOrPlace(id, info) else {
                    pendingWidget = id to info
                    val i = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                    runCatching { bindLauncher.launch(i) }.onFailure {
                        host.release(id); pendingWidget = null
                        Toast.makeText(ctx, ctx.getString(R.string.widget_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val iconPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        val app = pictureFor
        pictureFor = null
        if (uri != null && app != null) scope.launch {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val bmp = android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
                        d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        val big = maxOf(info.size.width, info.size.height)
                        if (big > 512) { val f = 512f / big; d.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1)) }
                    }
                    graph.customIcons.set(app.key, bmp)
                }.isSuccess
            }
            if (!ok) Toast.makeText(ctx, ctx.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
        }
    }
    var settingsRoute by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var crashAsk by remember { mutableStateOf(graph.crashes.pending() != null) }

    // Glass light follows the phone's tilt, only while Xe is on screen.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val tiltSensor = remember { com.xenoorigin.xelauncher.design.TiltSensor(ctx) }
    // Tilt feeds the glass light and the depth parallax; the sensor runs only while one of them is on.
    val tiltOn = (settings.tiltShine || settings.depthParallax) && speed != AnimationSpeed.OFF && !powerSave
    androidx.compose.runtime.DisposableEffect(tiltOn, lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && tiltOn) tiltSensor.start()
            if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) tiltSensor.stop()
        }
        lifecycle.addObserver(obs)
        if (tiltOn && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) tiltSensor.start()
        onDispose { lifecycle.removeObserver(obs); tiltSensor.stop() }
    }

    // Coming back from an app, Home settles in from slightly larger, like the app shrinking back into it.
    val homeReturn = remember { Animatable(1f) }
    androidx.compose.runtime.DisposableEffect(lifecycle, speed) {
        var away = false
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) away = true
            if (e == androidx.lifecycle.Lifecycle.Event.ON_START && away) {
                away = false
                // Back on Home: time spent in apps has changed, so rankings are read again (cached 10 min).
                // Usage access may have just been granted in Android's settings: read again now, not in 10 minutes.
                if (!graph.usage.usingTime) graph.usage.refresh()
                graph.usageTick.value++
                if (speed != AnimationSpeed.OFF) scope.launch { homeReturn.snapTo(0f); homeReturn.animateTo(1f, XeMotion.spring(speed, dampingRatio = .8f, stiffness = 260f)) }
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // Stable objects: they only change when settings do, so the drawer and home don't recompose every frame.
    val styleOf: (AppEntry) -> IconStyle = remember(settings.iconOverrides, settings.iconStyle) {
        { a -> settings.iconOverrides[a.key.encode()] ?: settings.iconStyle }
    }
    val homeLook = remember(settings, styleOf) { HomeLook(settings.homeColumns, settings.homeRows, settings.dockSize, settings.iconSizeDp, settings.showLabels, settings.iconShape, styleOf, settings.liquidDock && settings.animationSpeed != AnimationSpeed.OFF) }
    val usageTick by graph.usageTick.collectAsState()
    // "Most used" = time spent in the app when Usage access is on, else how often it was opened.
    val useCounts = remember(launchStats, usageTick, apps) {
        val counts = launchStats.associate { it.appKey to it.count }
        apps.associate { a -> a.key.encode() to graph.usage.score(a.key.packageName, counts[a.key.encode()] ?: 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }
    }
    val drawerLook = remember(settings, styleOf, useCounts) {
        DrawerLook(styleOf, settings.iconShape, settings.iconSizeDp, settings.showLabels, settings.categoryBar, settings.drawerLayout, { a -> useCounts[a.key.encode()] ?: 0 }, settings.categoryAccent)
    }
    // Read the animated drawer position only through these, never directly in composition.
    val drawerShown by remember { derivedStateOf { drawer.value > 0f } }
    val drawerFull by remember { derivedStateOf { drawer.value >= 1f } }

    // Android 13+: ask once for permission to show update notifications.
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(settings.askedNotifications) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && !settings.askedNotifications) {
            kotlinx.coroutines.delay(4000)
            graph.settings.update { it.copy(askedNotifications = true) }
            if (ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                runCatching { notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
            }
        }
    }

    // Render every icon ahead of time so the drawer opens with icons already in place.
    LaunchedEffect(apps, categories.size, settings.iconStyle, settings.iconOverrides, settings.iconSizeDp, settings.grayscaleContrast) {
        val px = with(density) { settings.iconSizeDp.dp.roundToPx() }
        // What's on screen first: Home and dock, then the drawer in category order, then anything left.
        val homeKeys = (layout.dock + layout.pages.flatten()).flatMap { c ->
            when (c) { is com.xenoorigin.xelauncher.home.AppCell -> listOf(c.app.key); is com.xenoorigin.xelauncher.home.FolderCell -> c.apps.map { it.key }; else -> emptyList() }
        }
        val ordered = (homeKeys + categories.flatMap { c -> c.apps.map { it.key } } + apps.map { it.key }).distinct()
        graph.icons.prewarm(ordered, { k -> settings.iconOverrides[k.encode()] ?: settings.iconStyle }, px, settings.grayscaleContrast)
    }

    fun update(f: (XeSettings) -> XeSettings) = scope.launch { graph.settings.update(f) }

    // ---------- Play ----------
    val toy = com.xenoorigin.xelauncher.play.rememberIconToy(active = drawerFull && settings.toyIcons && speed != AnimationSpeed.OFF)
    // One-off upgrades: double tap draws a letter (if it did nothing); dust starts counting from today.
    LaunchedEffect(settings.playVersion, settings.appDust, settings.dustSince) {
        if (settings.playVersion < 1) update { it.copy(playVersion = 1, doubleTap = if (it.doubleTap == GestureAction.NONE) GestureAction.DRAW_LETTER else it.doubleTap) }
        if (settings.appDust && settings.dustSince == 0L) update { it.copy(dustSince = System.currentTimeMillis()) }
    }
    val lastLaunch = remember(launchStats) { launchStats.associate { it.appKey to it.lastLaunch } }
    val dustNow = remember(launchStats, settings.dustWiped) { System.currentTimeMillis() }
    val drawerPlay = remember(settings.categoryGlow, settings.pond, settings.tiltShine, settings.barMagnify, settings.appDust, settings.dustSince, settings.dustWiped, lastLaunch, speed) {
        com.xenoorigin.xelauncher.drawer.DrawerPlay(
            glow = settings.categoryGlow,
            pond = settings.pond && speed != AnimationSpeed.OFF,
            sheen = settings.tiltShine && speed != AnimationSpeed.OFF,
            magnify = settings.barMagnify && speed != AnimationSpeed.OFF,
            dust = if (!settings.appDust) ({ 0f }) else ({ a ->
                val k = a.key.encode()
                com.xenoorigin.xelauncher.play.dustLevel(dustNow, com.xenoorigin.xelauncher.play.lastUse(lastLaunch[k] ?: 0L, a.installedAt, settings.dustWiped[k] ?: 0L, settings.dustSince))
            }),
            onWiped = { a -> update { it.copy(dustWiped = it.dustWiped + (a.key.encode() to System.currentTimeMillis())) } },
        )
    }
    // Outside Xe, a 15-minute job keeps the phone's copy of the live sky fresh.
    LaunchedEffect(settings.wallpaperId) {
        com.xenoorigin.xelauncher.wallpaper.LiveSkyWallpaper.schedule(ctx, com.xenoorigin.xelauncher.wallpaper.isTimeWallpaper(settings.wallpaperId))
    }
    // The live sky keeps up with the time while Xe is on screen.
    LaunchedEffect(settings.wallpaperId, lifecycle) {
        if (!com.xenoorigin.xelauncher.wallpaper.isTimeWallpaper(settings.wallpaperId)) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) { graph.wallpaper.refreshLiveSky(); delay(60_000) }
        }
    }

    fun settleDrawer(open: Boolean) = scope.launch {
        // One sound per real change: opening a closed drawer, or closing an open one (not a drag that snaps back).
        if (open != drawerSounded[0]) { drawerSounded[0] = open; com.xenoorigin.xelauncher.sound.XeSound.play(if (open) com.xenoorigin.xelauncher.sound.XeSound.Kind.OPEN else com.xenoorigin.xelauncher.sound.XeSound.Kind.CLOSE) }
        drawer.animateTo(if (open) 1f else 0f, XeMotion.spring(speed, dampingRatio = .86f, stiffness = 380f))
    }
    fun closeAll() {
        menu = null; openFolder = null; drawerMenu = false; editor = null; moveFor = null; editFor = null; arrange = null; bulkIcons = false
        widgetPicker = false; widgetEditId = null; manageCategory = null; noteFor = null
        searchOpen = false; termiOpen = false; settingsRoute = null; editMode = false; drawPad = false
    }

    fun launch(app: AppEntry, r: Rect) {
        val opts = com.xenoorigin.xelauncher.home.launchOptions(view, r)
        com.xenoorigin.xelauncher.sound.XeSound.play(com.xenoorigin.xelauncher.sound.XeSound.Kind.LAUNCH)
        if (com.xenoorigin.xelauncher.home.showBubble(settings.glassLaunch, speed, r)) launchBubble = r
        graph.apps.launch(app.key, android.graphics.Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt()), opts)
        scope.launch {
            graph.categories.recordLaunch(app.key)
            delay(350); drawer.snapTo(0f); drawerSounded[0] = false; searchOpen = false; menu = null; openFolder = null; drawPad = false
        }
    }

    fun perform(action: GestureAction?) {
        when (action) {
            GestureAction.OPEN_DRAWER -> settleDrawer(true)
            GestureAction.NOTIFICATIONS -> expandNotificationShade(ctx)
            GestureAction.OPEN_SEARCH -> searchOpen = true
            GestureAction.OPEN_SETTINGS -> settingsRoute = "root"
            GestureAction.EDIT_MODE -> editMode = true
            GestureAction.DRAW_LETTER -> drawPad = true
            GestureAction.NONE, null -> Unit
        }
    }

    LaunchedEffect(Unit) {
        graph.homePressed.collect {
            val anyOverlay = menu != null || openFolder != null || drawerMenu || editor != null || moveFor != null ||
                editFor != null || arrange != null || manageCategory != null || noteFor != null || bulkIcons || widgetPicker || widgetEditId != null || searchOpen || termiOpen || settingsRoute != null || editMode || drawPad
            if (anyOverlay) closeAll()
            else if (drawerShown) settleDrawer(false)
            else homePager.animateScrollToPage(0)
        }
    }
    BackHandler(enabled = drawerShown) { settleDrawer(false) }
    BackHandler(enabled = editMode) { editMode = false }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = with(density) { maxHeight.toPx() }
        val threshold = with(density) { 48.dp.toPx() }
        val fling = with(density) { 900.dp.toPx() }
        var totalDy by remember { mutableStateOf(0f) }
        var maxPointers by remember { mutableIntStateOf(1) }

        val widthPx = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
        val tiltPx = with(density) { 12.dp.toPx() }
        val parallax: () -> androidx.compose.ui.geometry.Offset = {
            val pos = homePager.currentPage + homePager.currentPageOffsetFraction
            when {
                // Depth: the wallpaper lags behind the pages and drifts with the phone's tilt.
                settings.depthParallax && speed != AnimationSpeed.OFF ->
                    com.xenoorigin.xelauncher.wallpaper.parallaxOffset(pos, homePager.pageCount, com.xenoorigin.xelauncher.design.GlassTilt.value.value, widthPx, tiltPx)
                settings.wallpaperParallax -> {
                    val count = (homePager.pageCount - 1).coerceAtLeast(1)
                    androidx.compose.ui.geometry.Offset(-(pos / count - .5f) * with(density) { 24.dp.toPx() }, 0f)
                }
                else -> androidx.compose.ui.geometry.Offset.Zero
            }
        }
        // The glass follows the wallpaper's own movement and the full screen it covers.
        val screenSize = androidx.compose.ui.geometry.Size(with(density) { maxWidth.toPx() }, heightPx)
        com.xenoorigin.xelauncher.design.LocalGlassBackdrop.current?.let { it.parallaxPx = parallax; it.screen = { screenSize } }
        com.xenoorigin.xelauncher.wallpaper.WallpaperLayer(
            graph.wallpaper, parallaxPx = parallax,
            liveSky = settings.wallpaperId == com.xenoorigin.xelauncher.wallpaper.LIVE_SKY_ID && settings.liveOnHome,
            ascii = remember(settings.wallpaperId, settings.liveOnHome, settings.asciiColor, settings.accent) { com.xenoorigin.xelauncher.wallpaper.homeAscii(settings) },
            animate = speed != AnimationSpeed.OFF && !powerSave,
        )
        val lens = remember(settings.glassLens, settings.wallpaperParallax) {
            if (settings.glassLens) com.xenoorigin.xelauncher.home.LensState({ graph.wallpaper.current.value }, parallax) else null
        }

        // ---------- Home ----------
        CompositionLocalProvider(
            com.xenoorigin.xelauncher.widgets.LocalWidgetActions provides remember(photoVersion, settings.widgetNotes, launchStats, apps, styleOf, settings.iconShape, settings.accent, settings.asciiColor, editMode, speed, powerSave) {
                com.xenoorigin.xelauncher.widgets.WidgetActions(
                    openSearch = { searchOpen = true },
                    launch = { a -> launch(a, Rect.Zero) },
                    suggested = {
                        val byKey = apps.associateBy { it.key.encode() }
                        launchStats.sortedByDescending { it.count }.mapNotNull { byKey[it.appKey] }.filter { it.key.encode() !in settings.hiddenApps }
                    },
                    style = styleOf, shape = settings.iconShape,
                    photo = { id -> java.io.File(photoDir, "$id.jpg").takeIf { it.exists() } },
                    pickPhoto = { id ->
                        if (!editMode) { photoFor = id; photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    },
                    note = { id -> settings.widgetNotes[id.toString()].orEmpty() },
                    editNote = { id -> if (!editMode) noteFor = id },
                    termi = graph.termi,
                    openTermi = { if (!editMode) termiOpen = true },
                    termiBody = settings.accent,
                    asciiColor = settings.asciiColor,
                    // Not while Termi's own page covers Home: one animated Termi at a time.
                    animate = speed != AnimationSpeed.OFF && !powerSave && !termiOpen,
                    editing = editMode,
                )
            },
            com.xenoorigin.xelauncher.home.LocalLens provides lens,
            com.xenoorigin.xelauncher.home.LocalWidgetContent provides { cell ->
                com.xenoorigin.xelauncher.widgets.WidgetContent(cell, host)
            },
        ) {
        HomeScreen(
            layout, homeLook, homePager, editMode,
            HomeCallbacks(
                onLaunch = ::launch,
                onItemMenu = { cell, app, b -> if (!editMode) menu = MenuState(b, app, cell, MenuSource.HOME) },
                onWidgetEdit = { c -> if (!settings.layoutLocked) widgetEditId = c.id },
                onFolderOpen = { f, b -> openFolder = f.folderId to b },
                onEmptyLongPress = { if (!settings.layoutLocked) editMode = true },
                onEmptyTap = { if (editMode) editMode = false },
                onDoubleTap = { perform(settings.doubleTap.takeUnless { it == GestureAction.NONE }) },
                onDrop = { d ->
                    scope.launch {
                        when (d) {
                            is DropRequest.Move -> graph.home.move(d.itemId, d.page, d.x, d.y)
                            is DropRequest.Onto -> graph.home.dropOnto(d.itemId, d.targetId)
                            is DropRequest.ToDock -> graph.home.moveToDock(d.itemId, d.position)
                        }
                    }
                },
                onDragStart = { menu = null; if (!settings.layoutLocked) editMode = true },
            ),
            Modifier
                .graphicsLayer { val p = drawer.value; val r = homeReturn.value; val back = com.xenoorigin.xelauncher.design.homeReturnScale(r); alpha = (1f - p) * (.35f + .65f * r); scaleX = (1f - .06f * p) * back; scaleY = scaleX }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val down = ev.changes.count { it.pressed }
                            if (down == 0) maxPointers = 1 else if (down > maxPointers) maxPointers = down
                        }
                    }
                }
                .draggable(
                    rememberDraggableState { delta ->
                        totalDy += delta
                        if (settings.swipeUp == GestureAction.OPEN_DRAWER && !editMode && (drawer.value > 0f || totalDy < 0f) && maxPointers == 1) {
                            scope.launch { drawer.snapTo((drawer.value - delta / heightPx).coerceIn(0f, 1f)) }
                        }
                    },
                    Orientation.Vertical,
                    enabled = !drawerFull,
                    onDragStarted = { totalDy = 0f },
                    onDragStopped = { v ->
                        if (drawer.value > 0f) settleDrawer(v < -fling * .6f || (drawer.value > .35f && v < fling * .6f))
                        else perform(GestureResolver.resolve(0f, totalDy, v, maxPointers, settings, threshold, fling).takeUnless { editMode })
                    },
                ),
            widgetEdit = widgetEditId?.let { id ->
                com.xenoorigin.xelauncher.home.WidgetEditing(
                    id,
                    place = { pl -> scope.launch { if (!graph.home.placeWidget(id, pl.x, pl.y, pl.w, pl.h)) Toast.makeText(ctx, ctx.getString(R.string.widget_no_space), Toast.LENGTH_SHORT).show() } },
                    remove = { layout.pages.flatten().filterIsInstance<com.xenoorigin.xelauncher.home.WidgetCell>().firstOrNull { it.id == id }?.let(::removeWidget); widgetEditId = null },
                    done = { widgetEditId = null },
                    content = { cell, span ->
                        val k = com.xenoorigin.xelauncher.widgets.GlassWidgetKind.of(cell.kind)
                        if (k != null) androidx.compose.runtime.CompositionLocalProvider(com.xenoorigin.xelauncher.widgets.LocalWidgetSpan provides k.clamp(span)) {
                            com.xenoorigin.xelauncher.widgets.GlassWidget(k, cell.id)
                        } else androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxSize().glassTile(androidx.compose.foundation.shape.RoundedCornerShape(26.dp), LocalGlassStyle.current, Color.White.copy(alpha = .14f)),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) { androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ti_layout_grid), null, tint = Color.White.copy(alpha = .8f)) }
                    },
                )
            },
        )
        }
        AnimatedVisibility(
            editMode,
            enter = fadeIn() + androidx.compose.animation.slideInVertically { -it },
            exit = fadeOut() + androidx.compose.animation.slideOutVertically { -it },
        ) {
            EditBar(
            onWidgets = { widgetPicker = true },
            onWallpaper = { settingsRoute = "wallpaper" }, onSettings = { settingsRoute = "root" },
            onAddPage = { scope.launch { graph.home.addPage(); homePager.animateScrollToPage(homePager.pageCount - 1) } },
            onDone = { editMode = false; scope.launch { graph.home.removeEmptyPages() } },
        )
        }

        // ---------- App drawer (follows the finger) ----------
        if (drawerShown) {
            val closeConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (drawer.value < 1f && available.y < 0f) {
                            scope.launch { drawer.snapTo((drawer.value - available.y / heightPx).coerceIn(0f, 1f)) }
                            return Offset(0f, available.y)
                        }
                        return Offset.Zero
                    }
                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        if (available.y > 0f && source == NestedScrollSource.UserInput) {
                            scope.launch { drawer.snapTo((drawer.value - available.y / heightPx).coerceIn(0f, 1f)) }
                            return Offset(0f, available.y)
                        }
                        return Offset.Zero
                    }
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (drawer.value < 1f) { settleDrawer(!(available.y > fling * .5f || drawer.value < .65f)); return available }
                        return Velocity.Zero
                    }
                }
            }
            com.xenoorigin.xelauncher.wallpaper.BlurredBackdrop(
                graph.wallpaper, Color.Black.copy(alpha = .38f),
                Modifier.graphicsLayer { alpha = drawer.value.coerceIn(0f, 1f) },
            )
            androidx.compose.runtime.CompositionLocalProvider(
                com.xenoorigin.xelauncher.drawer.LocalDrawerProgress provides { drawer.value },
                com.xenoorigin.xelauncher.play.LocalIconToy provides toy.takeIf { settings.toyIcons && speed != AnimationSpeed.OFF },
            ) {
            AppDrawer(
                categories, drawerState, drawerLook,
                onLaunch = ::launch,
                onLongPress = { a, b -> menu = MenuState(b, a, null, MenuSource.DRAWER) },
                onSearch = { searchOpen = true },
                onStore = {
                    runCatching {
                        ctx.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MARKET).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
                onMenu = { drawerMenu = true },
                onEditCategories = { editor = false },
                play = drawerPlay,
                modifier = Modifier
                    .graphicsLayer { val p = drawer.value; translationY = (1f - p) * heightPx * .35f; alpha = p.coerceIn(0f, 1f) }
                    .nestedScroll(closeConnection)
                    .draggable(
                        rememberDraggableState { delta -> scope.launch { drawer.snapTo((drawer.value - delta / heightPx).coerceIn(0f, 1f)) } },
                        Orientation.Vertical,
                        onDragStopped = { v -> settleDrawer(!(v > fling * .5f || drawer.value < .65f)) },
                    ),
            )
            }
        }

        // ---------- Overlays ----------
        AnimatedVisibility(searchOpen, enter = fadeIn() + scaleIn(initialScale = .96f), exit = fadeOut() + scaleOut(targetScale = .96f)) {
            Box(Modifier.fillMaxSize()) {
                com.xenoorigin.xelauncher.wallpaper.BlurredBackdrop(graph.wallpaper, Color.Black.copy(alpha = .45f))
                SearchPage(
                    apps.filter { it.key.encode() !in settings.hiddenApps }, graph.apps::allShortcuts, styleOf, settings.iconShape,
                    settings.searchCalculator, settings.searchWeb, settings.searchAutoKeyboard,
                    onLaunch = ::launch,
                    onLongPress = { a, b -> menu = MenuState(b, a, null, MenuSource.DRAWER) },
                    onShortcut = { graph.apps.startShortcut(it, null); searchOpen = false },
                    onWeb = { q ->
                        val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { ctx.startActivity(i) }.onFailure {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    },
                    onClose = { searchOpen = false },
                )
            }
        }

        AnimatedVisibility(termiOpen, enter = fadeIn() + scaleIn(initialScale = .96f), exit = fadeOut() + scaleOut(targetScale = .96f)) {
            Box(Modifier.fillMaxSize()) {
                com.xenoorigin.xelauncher.wallpaper.BlurredBackdrop(graph.wallpaper, Color.Black.copy(alpha = .55f))
                val usageOk = remember(termiOpen) { com.xenoorigin.xelauncher.apps.UsageTime.hasAccess(ctx) }
                com.xenoorigin.xelauncher.termi.TermiScreen(
                    graph.termi, remember(apps) { apps.map { it.key.packageName to it.label }.distinctBy { it.first } }, usageOk,
                    onGrantUsage = { com.xenoorigin.xelauncher.apps.UsageTime.requestAccess(ctx) },
                    onClose = { termiOpen = false },
                    accent = settings.accent, ascii = settings.asciiColor, animate = speed != AnimationSpeed.OFF && !powerSave,
                )
            }
        }

        AnimatedVisibility(drawPad, enter = fadeIn() + scaleIn(initialScale = 1.04f), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                com.xenoorigin.xelauncher.wallpaper.BlurredBackdrop(graph.wallpaper, Color.Black.copy(alpha = .55f))
                com.xenoorigin.xelauncher.play.DrawPad(
                    apps.filter { it.key.encode() !in settings.hiddenApps },
                    remember(launchStats) { launchStats.associate { it.appKey to it.count } }, styleOf, settings.iconShape,
                    onLaunch = ::launch, onClose = { drawPad = false },
                )
            }
        }

        openFolder?.let { (fid, from) ->
            val folder = (layout.pages.flatten() + layout.dock).filterIsInstance<FolderCell>().firstOrNull { it.folderId == fid }
            if (folder == null) openFolder = null else FolderPopup(
                folder, from, homeLook,
                onRename = { n -> scope.launch { graph.home.renameFolder(fid, n) } },
                onLaunch = ::launch,
                onLongPress = { a, b -> menu = MenuState(b, a, folder, MenuSource.FOLDER) },
                onDismiss = { openFolder = null },
            )
        }

        DrawerMenuSheet(
            drawerMenu, settings, onDismiss = { drawerMenu = false }, onUpdate = { update(it) },
            onAddCategory = { drawerMenu = false; editor = true },
            onEditCategories = { drawerMenu = false; editor = false },
            onHiddenApps = { drawerMenu = false; settingsRoute = "hidden" },
            onAllSettings = { drawerMenu = false; settingsRoute = "root" },
            onArrange = {
                drawerMenu = false
                val pages = if (settings.categoryBar) categories else null
                arrange = pages?.getOrNull(drawerState.pager.currentPage) ?: com.xenoorigin.xelauncher.categories.CategoryWithApps(
                    -1, ctx.getString(R.string.all_apps), "apps", null, categories.flatMap { it.apps },
                )
            },
            onBulkIcons = { drawerMenu = false; bulkIcons = true },
        )
        arrange?.let { cat ->
            ArrangeApps(
                cat.title, cat.apps, styleOf, settings.iconShape,
                onSave = { list ->
                    update { st ->
                        val ranks = st.appOrder.toMutableMap()
                        list.forEachIndexed { i, a -> ranks[a.key.encode()] = i }
                        st.copy(appOrder = ranks, drawerSort = com.xenoorigin.xelauncher.settings.DrawerSort.CUSTOM)
                    }
                },
                onClose = { arrange = null },
            )
        }
        if (bulkIcons) BulkIcons(
            apps.filter { it.key.encode() !in settings.hiddenApps }, styleOf, settings.iconShape,
            onApply = { keys, style ->
                update { st -> st.copy(iconOverrides = if (style == null) st.iconOverrides - keys else st.iconOverrides + keys.associateWith { style }) }
            },
            onClose = { bulkIcons = false },
        )
        editor?.let { adding ->
            CategoryEditor(
                editable,
                EditorActions(
                    add = { n, i -> scope.launch { graph.categories.add(n, i) } },
                    rename = { id, n -> scope.launch { graph.categories.rename(id, n) } },
                    setIcon = { id, i -> scope.launch { graph.categories.setIcon(id, i) } },
                    delete = { id -> scope.launch { graph.categories.delete(id) } },
                    reorder = { ids -> scope.launch { graph.categories.reorder(ids) } },
                    setHidden = { id, h -> update { st -> st.copy(hiddenCategories = if (h) st.hiddenCategories + id else st.hiddenCategories - id) } },
                    resetAutomatic = { id -> scope.launch { graph.categories.resetAutomatic(id) } },
                    resetAll = { scope.launch { graph.categories.resetToDefaults(); update { it.copy(hiddenCategories = emptySet()) } } },
                    manageApps = { id -> manageCategory = id },
                    setAccent = { id, c -> update { st -> st.copy(categoryAccent = if (c == null) st.categoryAccent - id else st.categoryAccent + (id to c)) } },
                    setSort = { id, so -> update { st -> st.copy(categorySort = if (so == com.xenoorigin.xelauncher.settings.CategorySort.FOLLOW) st.categorySort - id else st.categorySort + (id to so)) } },
                ),
                startAdding = adding, onClose = { editor = null },
                hidden = settings.hiddenCategories,
                counts = categories.associate { it.id to it.apps.size },
                accents = settings.categoryAccent, sorts = settings.categorySort,
                background = { com.xenoorigin.xelauncher.wallpaper.BlurredBackdrop(graph.wallpaper, Color.Black.copy(alpha = .72f)) },
            )
        }
        manageCategory?.let { catId ->
            val title = editable.firstOrNull { it.first.id == catId }?.second.orEmpty()
            val members = categories.firstOrNull { it.id == catId }?.apps.orEmpty().map { it.key.encode() }.toSet()
            val whereOf = categories.flatMap { c -> c.apps.map { it.key.encode() to c.title } }.toMap()
            CategoryApps(
                title, apps.filter { it.key.encode() !in settings.hiddenApps }, members, { a -> whereOf[a.key.encode()].orEmpty() },
                styleOf, settings.iconShape,
                onToggle = { a, on -> scope.launch { if (on) graph.categories.moveApp(a.key, catId) else graph.categories.clearOverride(a.key) } },
                onClose = { manageCategory = null },
                background = { com.xenoorigin.xelauncher.wallpaper.BlurredBackdrop(graph.wallpaper, Color.Black.copy(alpha = .72f)) },
            )
        }
        MoveToCategorySheet(moveFor, editable, onPick = { id -> moveFor?.let { a -> scope.launch { graph.categories.moveApp(a.key, id) } }; moveFor = null }, onDismiss = { moveFor = null })
        val editing = editFor
        EditAppSheet(
            editing,
            originalLabel = editing?.let { e -> graph.apps.apps.value.firstOrNull { it.key == e.key }?.label },
            currentStyle = editing?.let { settings.iconOverrides[it.key.encode()] },
            hasCustomIcon = editing?.let { graph.customIcons.get(it.key) != null } == true,
            shape = settings.iconShape,
            onSave = { name, style ->
                editing?.let { a ->
                    val k = a.key.encode()
                    update { st ->
                        st.copy(
                            labelOverrides = if (name == null) st.labelOverrides - k else st.labelOverrides + (k to name),
                            iconOverrides = if (style == null) st.iconOverrides - k else st.iconOverrides + (k to style),
                        )
                    }
                }
                editFor = null
            },
            onPickImage = {
                pictureFor = editing
                iconPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onResetImage = { editing?.let { a -> scope.launch(kotlinx.coroutines.Dispatchers.IO) { graph.customIcons.clear(a.key) } } },
            onDismiss = { editFor = null },
        )

        if (widgetPicker) com.xenoorigin.xelauncher.widgets.WidgetPicker(host, ::spanOf, onPick = ::addWidget, onClose = { widgetPicker = false })
        com.xenoorigin.xelauncher.widgets.NoteSheet(
            noteFor?.let { settings.widgetNotes[it.toString()].orEmpty() }, visible = noteFor != null,
            onSave = { text -> noteFor?.let { id -> update { st -> st.copy(widgetNotes = st.widgetNotes + (id.toString() to text)) } }; noteFor = null },
            onDismiss = { noteFor = null },
        )

        menu?.let { m ->
            val app = m.app
            val actions = buildList {
                when (m.source) {
                    MenuSource.DRAWER -> if (app != null) add(MenuAction(R.drawable.ti_home, stringResource(R.string.add_to_home)) {
                        scope.launch { graph.home.addApp(app.key, homePager.currentPage) }; menu = null; settleDrawer(false)
                    })
                    MenuSource.HOME, MenuSource.FOLDER -> if (m.cell is com.xenoorigin.xelauncher.home.WidgetCell) {
                        val wc = m.cell
                        add(MenuAction(R.drawable.ti_arrows_diagonal, stringResource(R.string.widget_edit_place)) { widgetEditId = wc.id; menu = null })
                        add(MenuAction(R.drawable.ti_trash, stringResource(R.string.widget_remove)) { removeWidget(wc); menu = null })
                    } else m.cell?.let { cell ->
                        add(MenuAction(R.drawable.ti_x, stringResource(R.string.remove_from_home)) {
                            scope.launch {
                                if (m.source == MenuSource.FOLDER && app != null) graph.home.removeApp(app.key) else graph.home.remove(cell.id)
                            }
                            menu = null
                        })
                    }
                }
                if (app != null) {
                    add(MenuAction(R.drawable.ti_pencil, stringResource(R.string.edit_app)) { editFor = app; menu = null })
                    add(MenuAction(R.drawable.ti_category, stringResource(R.string.move_to_category)) { moveFor = app; menu = null })
                    add(MenuAction(R.drawable.ti_eye_off, stringResource(R.string.hide_app)) { update { it.copy(hiddenApps = it.hiddenApps + app.key.encode()) }; menu = null })
                    add(MenuAction(R.drawable.ti_info_circle, stringResource(R.string.app_info)) { graph.apps.openAppInfo(app.key); menu = null })
                    add(MenuAction(R.drawable.ti_trash, stringResource(R.string.uninstall)) { graph.apps.uninstall(app.key); menu = null })
                }
            }
            val shortcuts = remember(app) { app?.let { graph.apps.shortcuts(it.key) }.orEmpty() }
            IconMenu(
                m.bounds, shortcuts, { graph.apps.shortcutIcon(it, ctx.resources.displayMetrics.densityDpi) },
                onShortcut = { graph.apps.startShortcut(it, null); menu = null },
                actions = actions, onDismiss = { menu = null },
            )
        }

        // The app crashed last time: offer to email a report.
        com.xenoorigin.xelauncher.design.GlassSheet(crashAsk, { crashAsk = false }) {
            val scheme = com.xenoorigin.xelauncher.design.LocalXeScheme.current
            androidx.compose.material3.Text(stringResource(R.string.crash_title), style = com.xenoorigin.xelauncher.design.XeType.Title, color = scheme.text)
            androidx.compose.material3.Text(stringResource(R.string.crash_body), style = com.xenoorigin.xelauncher.design.XeType.Body, color = scheme.textSecondary)
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                com.xenoorigin.xelauncher.design.GlassPillButton(stringResource(R.string.crash_skip), { graph.crashes.clear(); crashAsk = false }, Modifier.weight(1f))
                com.xenoorigin.xelauncher.design.GlassPillButton(stringResource(R.string.crash_send), { crashAsk = false; settingsRoute = "bugs" }, Modifier.weight(1f), icon = R.drawable.ti_mail)
            }
        }

        // Settings rise in over the home screen and sink away when closed.
        var lastRoute by remember { mutableStateOf("root") }
        settingsRoute?.let { lastRoute = it }
        AnimatedVisibility(
            settingsRoute != null,
            enter = fadeIn(androidx.compose.animation.core.tween(220)) + scaleIn(initialScale = .94f) + androidx.compose.animation.slideInVertically { it / 10 },
            exit = fadeOut(androidx.compose.animation.core.tween(180)) + scaleOut(targetScale = .96f),
        ) {
            key(lastRoute) { SettingsHost(graph, lastRoute, onClose = { settingsRoute = null }) }
        }

        // ---------- First-run welcome: intro, language, default Home, tutorial, practice ----------
        val stage = settings.onboarding
        fun goTo(next: com.xenoorigin.xelauncher.settings.OnboardingStage, extra: (XeSettings) -> XeSettings = { it }) =
            scope.launch { graph.settings.update { extra(it).copy(onboarding = next) } }
        val isDefault = { com.xenoorigin.xelauncher.onboarding.isDefaultHome(ctx) }
        // "Show the welcome tour again" in Settings starts over from the intro.
        LaunchedEffect(stage) { if (stage == com.xenoorigin.xelauncher.settings.OnboardingStage.INTRO) { settingsRoute = null; menu = null; editMode = false } }
        if (stage == com.xenoorigin.xelauncher.settings.OnboardingStage.PRACTICE) {
            val steps = remember(settings.layoutLocked) { com.xenoorigin.xelauncher.onboarding.practiceSteps(!settings.layoutLocked) }
            var index by remember { mutableStateOf(0) }
            var burst by remember { mutableStateOf(0) }
            LaunchedEffect(steps) {
                androidx.compose.runtime.snapshotFlow { com.xenoorigin.xelauncher.onboarding.HomeState(drawer.value, menu != null, editMode) }
                    .collect { st -> index = com.xenoorigin.xelauncher.onboarding.advancePractice(steps, index, st) }
            }
            val finished = index >= steps.size
            LaunchedEffect(finished) {
                if (finished) { burst++; kotlinx.coroutines.delay(2400); goTo(com.xenoorigin.xelauncher.settings.OnboardingStage.DONE) }
            }
            val step = steps.getOrNull(index)
            val atBottom = step == com.xenoorigin.xelauncher.onboarding.PracticeStep.EXIT_EDIT || step == com.xenoorigin.xelauncher.onboarding.PracticeStep.HOLD_ICON
            com.xenoorigin.xelauncher.onboarding.PracticeOverlay(
                step, index, steps.size, finished, onSkip = { goTo(com.xenoorigin.xelauncher.settings.OnboardingStage.DONE) },
                modifier = Modifier.align(if (atBottom) androidx.compose.ui.Alignment.BottomCenter else androidx.compose.ui.Alignment.TopCenter)
                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.Companion.safeDrawing).padding(vertical = if (atBottom) 150.dp else 70.dp),
            )
            com.xenoorigin.xelauncher.settings.ui.GlassBurst(burst, androidx.compose.ui.geometry.Offset(with(density) { maxWidth.toPx() } / 2f, heightPx * .25f))
        }
        launchBubble?.let { r ->
            com.xenoorigin.xelauncher.home.LaunchBubble(r, speed, Color(settings.accent.toInt())) { launchBubble = null }
        }

        if (stage < com.xenoorigin.xelauncher.settings.OnboardingStage.PRACTICE) {
            com.xenoorigin.xelauncher.onboarding.OnboardingHost(
                stage,
                onIntroDone = { goTo(com.xenoorigin.xelauncher.settings.OnboardingStage.LANGUAGE) },
                onLanguage = { lang ->
                    scope.launch {
                        graph.settings.update { it.copy(language = lang, onboarding = com.xenoorigin.xelauncher.onboarding.nextStage(com.xenoorigin.xelauncher.settings.OnboardingStage.LANGUAGE, isDefault())) }
                        // Switching language rebuilds the screen; the stage is already saved, so the welcome carries on.
                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(if (lang == com.xenoorigin.xelauncher.settings.AppLanguage.EN) "en" else "th"))
                    }
                },
                onSetDefault = {
                    goTo(com.xenoorigin.xelauncher.settings.OnboardingStage.SLIDES) { it.copy(firstRunDone = true) }
                    runCatching { (ctx as? android.app.Activity)?.startActivityForResult(com.xenoorigin.xelauncher.onboarding.requestDefaultHomeIntent(ctx), 7) }
                },
                onDefaultLater = { goTo(com.xenoorigin.xelauncher.settings.OnboardingStage.SLIDES) { it.copy(firstRunDone = true) } },
                onPractice = { goTo(com.xenoorigin.xelauncher.settings.OnboardingStage.PRACTICE) },
                onSkipTutorial = { goTo(com.xenoorigin.xelauncher.settings.OnboardingStage.DONE) },
            )
        }
    }
}

/** The rim colour for now, re-checked every five minutes; clear when the setting is off. */
@Composable
private fun rememberRimTint(on: Boolean): Color {
    if (!on) return Color.Transparent
    val minute by androidx.compose.runtime.produceState(com.xenoorigin.xelauncher.wallpaper.minuteNow()) {
        while (true) { delay(5 * 60_000L); value = com.xenoorigin.xelauncher.wallpaper.minuteNow() }
    }
    val sun = remember { com.xenoorigin.xelauncher.wallpaper.sunToday() }
    return remember(minute / 5, sun) { com.xenoorigin.xelauncher.design.rimTintAt(minute, sun) }
}

@Suppress("unused") private val unusedAppCell = AppCell::class
