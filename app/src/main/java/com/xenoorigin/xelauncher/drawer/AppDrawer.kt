package com.xenoorigin.xelauncher.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.settings.DrawerLayout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.positionInWindow
import com.xenoorigin.xelauncher.design.toShape
import com.xenoorigin.xelauncher.play.DustPuff
import com.xenoorigin.xelauncher.play.LocalIconToy
import com.xenoorigin.xelauncher.play.dust
import com.xenoorigin.xelauncher.play.pond
import com.xenoorigin.xelauncher.play.rememberPond
import com.xenoorigin.xelauncher.play.rubToClean
import com.xenoorigin.xelauncher.play.tiltSheen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.categories.CategoryWithApps
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeMotion
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.pressSquish
import com.xenoorigin.xelauncher.icons.AppIcon
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DrawerLook(
    val iconStyle: (AppEntry) -> IconStyle, val iconShape: IconShape, val iconSize: Int, val labels: Boolean, val categoryBar: Boolean,
    val layout: DrawerLayout = DrawerLayout.GRID,
    /** How many times an app has been opened, for picking the Bento's big tiles. */
    val uses: (AppEntry) -> Int = { 0 },
    /** Accent colours the user picked per category (ARGB). */
    val accents: Map<Long, Long> = emptyMap(),
)

/** The drawer's playful extras; all off by default. Icon tilt-and-shake comes in through [LocalIconToy]. */
@Immutable
data class DrawerPlay(
    val glow: Boolean = false,
    val pond: Boolean = false,
    val sheen: Boolean = false,
    val magnify: Boolean = false,
    val dust: (AppEntry) -> Float = { 0f },
    val onWiped: (AppEntry) -> Unit = {},
    val onPop: () -> Unit = {},
)

@Composable
fun AppDrawer(
    categories: List<CategoryWithApps>,
    state: DrawerState,
    look: DrawerLook,
    onLaunch: (AppEntry, Rect) -> Unit,
    onLongPress: (AppEntry, Rect) -> Unit,
    onSearch: () -> Unit,
    onStore: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onEditCategories: () -> Unit = {},
    play: DrawerPlay = DrawerPlay(),
) {
    val speed = LocalAnimationSpeed.current
    val scope = rememberCoroutineScope()
    val scheme = LocalXeScheme.current
    val pages: List<CategoryWithApps> = if (look.categoryBar) categories else listOf(
        CategoryWithApps(-1, stringResource(R.string.all_apps), "apps", null,
            categories.flatMap { it.apps }.sortedWith(com.xenoorigin.xelauncher.categories.AppSorting.alphabetical)),
    )
    val tileShape = remember(look.iconShape) { look.iconShape.toShape() }
    Box(modifier.fillMaxSize()) {
        if (play.glow && look.categoryBar) CategoryGlow(pages, state, look.accents)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 34.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                val page = pages.getOrNull(state.pager.targetPage.coerceAtMost(pages.lastIndex))
                AnimatedContent(
                    (page?.title.orEmpty()) to (page?.apps?.size ?: 0), Modifier.weight(1f),
                    transitionSpec = {
                        val d = XeMotion.durationMs(300, speed)
                        val forward = targetState.first != initialState.first
                        (slideInVertically(tween(d)) { it / 3 } + fadeIn(tween(d)) + scaleIn(tween(d), initialScale = if (forward) .92f else 1f)) togetherWith
                            (slideOutVertically(tween(d)) { -it / 3 } + fadeOut(tween(d / 2)))
                    }, label = "title",
                ) { (t, n) ->
                    Column {
                        com.xenoorigin.xelauncher.settings.ui.PageTitle(t)
                        Text(
                            if (n == 1) stringResource(R.string.app_count_one) else stringResource(R.string.app_count, n),
                            style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(start = 2.dp).testTag("appCount"),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassIconButton(R.drawable.ti_search, stringResource(R.string.search), onSearch)
                    GlassIconButton(R.drawable.ti_shopping_bag, stringResource(R.string.store), onStore)
                    GlassIconButton(R.drawable.ti_dots, stringResource(R.string.menu), onMenu)
                }
            }
            HorizontalPager(state.pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1, key = { pages.getOrNull(it)?.id ?: it }) { page ->
                val cat = pages.getOrNull(page) ?: return@HorizontalPager
                // The page slides in with its icons already drawn, so they don't play an entrance again when it settles.
                androidx.compose.runtime.CompositionLocalProvider(LocalIconShapeForTiles provides tileShape) {
                    AppGrid(page, cat.apps, look, speed, play, onLaunch, onLongPress)
                }
            }
            if (look.categoryBar && pages.size > 1) {
                val barModifier = Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 14.dp)
                if (com.xenoorigin.xelauncher.design.LocalUiStyle.current == com.xenoorigin.xelauncher.design.UiStyle.GLASS) {
                    // 2.0: the bar and a search bubble are one piece of liquid glass that melts and stretches.
                    GooeyBar(onSearch, barModifier) { m ->
                        CategoryBar(pages, state, onSelect = { scope.launch { state.animateTo(it, speed) } }, m, onLongPress = onEditCategories, magnify = play.magnify, glass = false)
                    }
                } else CategoryBar(pages, state, onSelect = { scope.launch { state.animateTo(it, speed) } }, barModifier, onLongPress = onEditCategories, magnify = play.magnify)
            } else Spacer(Modifier.navigationBarsPadding().height(14.dp))
        }
    }
}

/** Soft light in the category's colour, blending between neighbours while the pages slide. */
@Composable
private fun CategoryGlow(pages: List<CategoryWithApps>, state: DrawerState, accents: Map<Long, Long>) {
    val hues = remember(pages, accents) { pages.map { accents[it.id] ?: categoryHue(it.builtIn, it.id) } }
    Box(
        Modifier.fillMaxSize().drawBehind {
            val c = Color(blendHue(hues, state.bubbleOffsetFraction))
            val w = size.width; val h = size.height
            drawRect(Brush.radialGradient(listOf(c.copy(alpha = .30f), c.copy(alpha = .09f), Color.Transparent), center = Offset(w * .16f, h * .09f), radius = w * 1.05f))
            drawRect(Brush.radialGradient(listOf(c.copy(alpha = .13f), Color.Transparent), center = Offset(w * .96f, h * .74f), radius = w * .85f))
        },
    )
}

/** How open the drawer is (0..1), read each frame; icons cascade in with it. */
val LocalDrawerProgress = androidx.compose.runtime.staticCompositionLocalOf<() -> Float> { { 1f } }

@Composable
private fun AppGrid(
    page: Int, apps: List<AppEntry>, look: DrawerLook, speed: AnimationSpeed, play: DrawerPlay,
    onLaunch: (AppEntry, Rect) -> Unit, onLongPress: (AppEntry, Rect) -> Unit,
) {
    val toy = LocalIconToy.current
    val unit = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current
    val (pond, pondFrame) = rememberPond()
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }
            .then(
                if (play.pond) Modifier.pond(pond, pondFrame, unit,
                    onSplash = { p -> toy?.splash(page, (origin.x + p.x) / unit, (origin.y + p.y) / unit) },
                    onPop = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); play.onPop() },
                ) else Modifier,
            ),
    ) {
        if (look.layout == DrawerLayout.BENTO) BentoPage(page, apps, look, speed, play, onLaunch, onLongPress)
        else GridPage(page, apps, look, speed, play, onLaunch, onLongPress)
    }
}

@Composable
private fun GridPage(
    page: Int, apps: List<AppEntry>, look: DrawerLook, speed: AnimationSpeed, play: DrawerPlay,
    onLaunch: (AppEntry, Rect) -> Unit, onLongPress: (AppEntry, Rect) -> Unit,
) {
    val grid = drawerGridFor(apps.size)
    val sparse = grid.columns < 4
    LazyVerticalGrid(
        GridCells.Fixed(grid.columns), Modifier.fillMaxSize().testTag("drawerGrid"),
        contentPadding = PaddingValues(start = grid.sidePaddingDp.dp, end = grid.sidePaddingDp.dp, top = if (sparse) 14.dp else 10.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (sparse) 26.dp else 20.dp),
    ) {
        itemsIndexed(apps, key = { _, a -> a.key.encode() }) { i, app ->
            val size = (look.iconSize * grid.iconScale).dp
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                DrawerTile(page, i, i / grid.columns, app, size, play, speed, onLaunch, onLongPress) { m, tile ->
                    AppIcon(app, size, look.iconStyle(app), look.iconShape, look.labels, m, tileModifier = tile)
                }
            }
        }
    }
}

/**
 * The Bento page: the most used app on a big glass card, the next ones on wide cards, the rest as icons.
 * Rows are grouped into bands (the first band holds the 2-row big card) so long pages still scroll lazily.
 */
@Composable
private fun BentoPage(
    page: Int, apps: List<AppEntry>, look: DrawerLook, speed: AnimationSpeed, play: DrawerPlay,
    onLaunch: (AppEntry, Rect) -> Unit, onLongPress: (AppEntry, Rect) -> Unit,
) {
    val bento = remember(apps, look.uses) { bentoLayout(apps.map(look.uses)) }
    val bands = remember(bento) { bento.cells.groupBy { if (it.row < 2) 0 else it.row - 1 }.toSortedMap().values.toList() }
    val scheme = LocalXeScheme.current
    val glass = LocalGlassStyle.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = 14.dp; val gap = 10.dp; val vgap = 14.dp
        val cellW = (maxWidth - pad * 2 - gap * 3) / 4
        val cellH = look.iconSize.dp + (if (look.labels) 30.dp else 12.dp)
        val icon = look.iconSize.dp
        LazyColumn(
            Modifier.fillMaxSize().testTag("drawerGrid"),
            contentPadding = PaddingValues(start = pad, end = pad, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(vgap),
        ) {
            lazyItemsIndexed(bands, key = { _, b -> apps[b.first().app].key.encode() }) { bi, band ->
                val top = if (bi == 0) 0 else band.first().row
                val rowsInBand = if (bi == 0) 2 else 1
                Box(Modifier.fillMaxWidth().height(cellH * rowsInBand + vgap * (rowsInBand - 1))) {
                    band.forEach { cell ->
                        val app = apps[cell.app]
                        val w = if (cell.size == BentoSize.SMALL) cellW else cellW * 2 + gap
                        val h = if (cell.size == BentoSize.HERO) cellH * 2 + vgap else cellH
                        Box(
                            Modifier.offset(x = (cellW + gap) * cell.col, y = (cellH + vgap) * (cell.row - top)).size(w, h),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            when (cell.size) {
                                BentoSize.SMALL -> DrawerTile(page, cell.app, cell.row, app, icon, play, speed, onLaunch, onLongPress) { m, tile ->
                                    AppIcon(app, icon, look.iconStyle(app), look.iconShape, look.labels, m, tileModifier = tile)
                                }
                                BentoSize.HERO -> DrawerTile(page, cell.app, cell.row, app, icon * 1.45f, play, speed, onLaunch, onLongPress) { m, tile ->
                                    Column(
                                        m.fillMaxSize().glassTile(RoundedCornerShape(30.dp), glass, Color.White.copy(alpha = if (glass.dark) .07f else .3f))
                                            .padding(16.dp).testTag("bentoHero").semantics(mergeDescendants = true) {},
                                    ) {
                                        AppIcon(app, icon * 1.45f, look.iconStyle(app), look.iconShape, false, tileModifier = tile)
                                        Spacer(Modifier.weight(1f))
                                        Text(app.label, style = XeType.Headline, color = scheme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (look.uses(app) > 0) Text(stringResource(R.string.bento_most_used), style = XeType.Label, color = scheme.textSecondary, maxLines = 1)
                                    }
                                }
                                BentoSize.WIDE -> DrawerTile(page, cell.app, cell.row, app, icon * .8f, play, speed, onLaunch, onLongPress) { m, tile ->
                                    Row(
                                        m.fillMaxSize().glassTile(RoundedCornerShape(24.dp), glass, Color.White.copy(alpha = if (glass.dark) .06f else .26f))
                                            .padding(horizontal = 8.dp).testTag("bentoWide").semantics(mergeDescendants = true) {},
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AppIcon(app, icon * .8f, look.iconStyle(app), look.iconShape, false, tileModifier = tile)
                                        Text(app.label, style = XeType.Body.copy(fontWeight = FontWeight.SemiBold), color = scheme.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One app in the drawer, whatever it looks like: rises in with the drawer, sloshes with the toy, can be rubbed
 * clean of dust, squishes when pressed, opens on tap and shows its menu on long-press. [content] draws the app
 * using the given outer modifier and the modifier for its icon tile (sheen and dust).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerTile(
    page: Int, index: Int, row: Int, app: AppEntry, iconSize: androidx.compose.ui.unit.Dp, play: DrawerPlay, speed: AnimationSpeed,
    onLaunch: (AppEntry, Rect) -> Unit, onLongPress: (AppEntry, Rect) -> Unit,
    content: @Composable (Modifier, Modifier) -> Unit,
) {
    val drawerProgress = LocalDrawerProgress.current
    val toy = LocalIconToy.current
    val density = LocalDensity.current
    val unit = density.density
    val haptic = LocalHapticFeedback.current
    val shape = LocalIconShapeForTiles.current
    var bounds = remember { Rect.Zero }
    val src = remember { MutableInteractionSource() }
    val sizePx = with(density) { iconSize.toPx() }
    val level = play.dust(app)
    var wiped by remember(app.key) { mutableFloatStateOf(0f) }
    var puff by remember(app.key) { mutableIntStateOf(0) }
    val physics = toy?.page(page)
    Box(contentAlignment = Alignment.TopCenter) {
        content(
            Modifier
                .graphicsLayer {
                    // Rows rise in one after another as the drawer opens (and follow the finger back down).
                    val c = if (speed == AnimationSpeed.OFF) 1f else com.xenoorigin.xelauncher.design.drawerCascade(drawerProgress(), row)
                    alpha = c
                    val sc = .88f + .12f * c; scaleX = sc; scaleY = sc
                    translationY = (1f - c) * (36 + row * 12).dp.toPx()
                    rotationX = (1f - c) * 30f
                    cameraDistance = 12f * this.density
                    if (physics != null) {
                        toy.frame.intValue
                        translationX = physics.x(index).dp.toPx()
                        translationY += physics.y(index).dp.toPx()
                        rotationZ = physics.rotation(index)
                    }
                }
                .onGloballyPositioned {
                    bounds = it.boundsInWindow()
                    physics?.setCenter(index, bounds.center.x / unit, bounds.center.y / unit)
                }
                .rubToClean(
                    level > 0f, sizePx,
                    onProgress = { wiped = it },
                    onTick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    onClean = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); puff++; play.onWiped(app) },
                )
                .pressSquish(src)
                .combinedClickable(src, null, onLongClick = { onLongPress(app, bounds) }) { onLaunch(app, bounds) },
            Modifier.tiltSheen(play.sheen, shape).dust({ level }, { wiped }, app.key.hashCode(), shape),
        )
        if (puff > 0) DustPuff(puff, Modifier.size(iconSize + 30.dp).align(Alignment.TopCenter).offset(y = (-15).dp))
    }
}

/** The icon tile shape, so dust and sheen are clipped to it. */
private val LocalIconShapeForTiles = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.ui.graphics.Shape> { RoundedCornerShape(22) }
