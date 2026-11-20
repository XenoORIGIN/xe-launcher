package com.xenoorigin.xelauncher

import android.app.ActivityOptions
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.xenoorigin.xelauncher.home.EditIconSheet
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
    ) {
        val haze = remember { HazeState() }
        CompositionLocalProvider(
            LocalHazeState provides haze, LocalIconRepository provides graph.icons, LocalGrayscaleContrast provides settings.grayscaleContrast,
        ) { LauncherRoot(graph, settings) }
    }
}

@Composable
private fun LauncherRoot(graph: AppGraph, settings: XeSettings) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val speed = LocalAnimationSpeed.current
    val scope = rememberCoroutineScope()
    val apps by graph.apps.apps.collectAsState()
    val categories by graph.categories.categories.collectAsState(initial = emptyList())
    val editable by graph.categories.editable.collectAsState(initial = emptyList())
    val layout by graph.home.layout.collectAsState(initial = HomeLayout.Empty)

    val drawer = remember { Animatable(0f) }
    val drawerState = remember { DrawerState(pageCount = { categories.size.coerceAtLeast(1) }) }
    val homePager = rememberPagerState { layout.pages.size + 1 }
    var searchOpen by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<MenuState?>(null) }
    var openFolder by remember { mutableStateOf<Pair<Long, Rect>?>(null) }
    var drawerMenu by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<Boolean?>(null) } // true = start adding
    var moveFor by remember { mutableStateOf<AppEntry?>(null) }
    var styleFor by remember { mutableStateOf<AppEntry?>(null) }
    var settingsRoute by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }

    fun styleOf(a: AppEntry): IconStyle = settings.iconOverrides[a.key.encode()] ?: settings.iconStyle
    val homeLook = HomeLook(settings.homeColumns, settings.homeRows, settings.dockSize, settings.iconSizeDp, settings.showLabels, settings.iconShape, ::styleOf)
    val drawerLook = DrawerLook(::styleOf, settings.iconShape, settings.iconSizeDp, settings.showLabels, settings.categoryBar)

    fun update(f: (XeSettings) -> XeSettings) = scope.launch { graph.settings.update(f) }
    fun settleDrawer(open: Boolean) = scope.launch {
        drawer.animateTo(if (open) 1f else 0f, XeMotion.spring(speed, dampingRatio = .86f, stiffness = 380f))
    }
    fun closeAll() {
        menu = null; openFolder = null; drawerMenu = false; editor = null; moveFor = null; styleFor = null
        searchOpen = false; settingsRoute = null; editMode = false
    }

    fun launch(app: AppEntry, r: Rect) {
        val opts = if (r.width > 0) ActivityOptions.makeClipRevealAnimation(view, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt()).toBundle() else null
        graph.apps.launch(app.key, android.graphics.Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt()), opts)
        scope.launch {
            graph.categories.recordLaunch(app.key)
            delay(350); drawer.snapTo(0f); searchOpen = false; menu = null; openFolder = null
        }
    }

    fun perform(action: GestureAction?) {
        when (action) {
            GestureAction.OPEN_DRAWER -> settleDrawer(true)
            GestureAction.NOTIFICATIONS -> expandNotificationShade(ctx)
            GestureAction.OPEN_SEARCH -> searchOpen = true
            GestureAction.OPEN_SETTINGS -> settingsRoute = "root"
            GestureAction.EDIT_MODE -> editMode = true
            GestureAction.NONE, null -> Unit
        }
    }

    LaunchedEffect(Unit) {
        graph.homePressed.collect {
            val anyOverlay = menu != null || openFolder != null || drawerMenu || editor != null || moveFor != null ||
                styleFor != null || searchOpen || settingsRoute != null || editMode
            if (anyOverlay) closeAll()
            else if (drawer.value > 0f) settleDrawer(false)
            else homePager.animateScrollToPage(0)
        }
    }
    BackHandler(enabled = drawer.value > 0f) { settleDrawer(false) }
    BackHandler(enabled = editMode) { editMode = false }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = with(density) { maxHeight.toPx() }
        val threshold = with(density) { 48.dp.toPx() }
        val fling = with(density) { 900.dp.toPx() }
        var totalDy by remember { mutableStateOf(0f) }
        var maxPointers by remember { mutableIntStateOf(1) }

        com.xenoorigin.xelauncher.wallpaper.WallpaperLayer(graph.wallpaper, parallaxPx = {
            if (!settings.wallpaperParallax) 0f else {
                val count = (homePager.pageCount - 1).coerceAtLeast(1)
                val f = (homePager.currentPage + homePager.currentPageOffsetFraction) / count
                -(f - .5f) * with(density) { 24.dp.toPx() }
            }
        })

        // ---------- Home ----------
        HomeScreen(
            layout, homeLook, homePager, editMode,
            HomeCallbacks(
                onLaunch = ::launch,
                onItemMenu = { cell, app, b -> if (!editMode) menu = MenuState(b, app, cell, MenuSource.HOME) },
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
                .graphicsLayer { val p = drawer.value; alpha = 1f - p; scaleX = 1f - .06f * p; scaleY = 1f - .06f * p }
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
                    enabled = drawer.value < 1f,
                    onDragStarted = { totalDy = 0f },
                    onDragStopped = { v ->
                        if (drawer.value > 0f) settleDrawer(v < -fling * .6f || (drawer.value > .35f && v < fling * .6f))
                        else perform(GestureResolver.resolve(0f, totalDy, v, maxPointers, settings, threshold, fling).takeUnless { editMode })
                    },
                ),
        )
        if (editMode) EditBar(
            onWidgets = { Toast.makeText(ctx, ctx.getString(R.string.coming_soon), Toast.LENGTH_SHORT).show() },
            onWallpaper = { settingsRoute = "wallpaper" }, onSettings = { settingsRoute = "root" },
            onAddPage = { scope.launch { graph.home.addPage(); homePager.animateScrollToPage(homePager.pageCount - 1) } },
            onDone = { editMode = false; scope.launch { graph.home.removeEmptyPages() } },
        )

        // ---------- App drawer (follows the finger) ----------
        if (drawer.value > 0f) {
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
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = drawer.value.coerceIn(0f, 1f) }
                    .liquidGlass(RectangleShape, LocalGlassStyle.current.copy(rimAlpha = 0f, tint = Color.Black.copy(alpha = .32f)), LocalHazeState.current),
            )
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

        // ---------- Overlays ----------
        AnimatedVisibility(searchOpen, enter = fadeIn() + scaleIn(initialScale = .96f), exit = fadeOut() + scaleOut(targetScale = .96f)) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().liquidGlass(RectangleShape, LocalGlassStyle.current.copy(rimAlpha = 0f, tint = Color.Black.copy(alpha = .4f)), LocalHazeState.current))
                SearchPage(
                    apps.filter { it.key.encode() !in settings.hiddenApps }, graph.apps::allShortcuts, ::styleOf, settings.iconShape,
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
                ),
                startAdding = adding, onClose = { editor = null },
            )
        }
        MoveToCategorySheet(moveFor, editable, onPick = { id -> moveFor?.let { a -> scope.launch { graph.categories.moveApp(a.key, id) } }; moveFor = null }, onDismiss = { moveFor = null })
        EditIconSheet(styleFor, styleFor?.let { settings.iconOverrides[it.key.encode()] }, onPick = { s ->
            styleFor?.let { a -> update { st -> st.copy(iconOverrides = if (s == null) st.iconOverrides - a.key.encode() else st.iconOverrides + (a.key.encode() to s)) } }
            styleFor = null
        }, onDismiss = { styleFor = null })

        menu?.let { m ->
            val app = m.app
            val actions = buildList {
                when (m.source) {
                    MenuSource.DRAWER -> if (app != null) add(MenuAction(R.drawable.ti_home, stringResource(R.string.add_to_home)) {
                        scope.launch { graph.home.addApp(app.key, homePager.currentPage) }; menu = null; settleDrawer(false)
                    })
                    MenuSource.HOME, MenuSource.FOLDER -> m.cell?.let { cell ->
                        add(MenuAction(R.drawable.ti_x, stringResource(R.string.remove_from_home)) {
                            scope.launch {
                                if (m.source == MenuSource.FOLDER && app != null) graph.home.removeApp(app.key) else graph.home.remove(cell.id)
                            }
                            menu = null
                        })
                    }
                }
                if (app != null) {
                    add(MenuAction(R.drawable.ti_palette, stringResource(R.string.edit_icon)) { styleFor = app; menu = null })
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

        // First run: offer to become the Home app.
        var offerDefault by remember { mutableStateOf(com.xenoorigin.xelauncher.onboarding.shouldOfferDefaultHome(com.xenoorigin.xelauncher.onboarding.isDefaultHome(ctx), settings.firstRunDone)) }
        if (offerDefault && drawer.value == 0f && !editMode) {
            com.xenoorigin.xelauncher.onboarding.SetDefaultHomeCard(
                onRequest = {
                    offerDefault = false; update { it.copy(firstRunDone = true) }
                    runCatching { (ctx as? android.app.Activity)?.startActivityForResult(com.xenoorigin.xelauncher.onboarding.requestDefaultHomeIntent(ctx), 7) }
                },
                onLater = { offerDefault = false; update { it.copy(firstRunDone = true) } },
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
            )
        }

        settingsRoute?.let { route -> SettingsHost(graph, route, onClose = { settingsRoute = null }) }
    }
}

@Suppress("unused") private val unusedAppCell = AppCell::class
