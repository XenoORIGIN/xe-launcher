package com.xenoorigin.xelauncher.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

data class DrawerLook(val iconStyle: (AppEntry) -> IconStyle, val iconShape: IconShape, val iconSize: Int, val labels: Boolean, val categoryBar: Boolean)

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
) {
    val speed = LocalAnimationSpeed.current
    val scope = rememberCoroutineScope()
    val scheme = LocalXeScheme.current
    val pages: List<CategoryWithApps> = if (look.categoryBar) categories else listOf(
        CategoryWithApps(-1, stringResource(R.string.all_apps), "apps", null,
            categories.flatMap { it.apps }.sortedWith(com.xenoorigin.xelauncher.categories.AppSorting.alphabetical)),
    )
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 40.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            val title = pages.getOrNull(state.pager.targetPage.coerceAtMost(pages.lastIndex))?.title.orEmpty()
            AnimatedContent(
                title, Modifier.weight(1f),
                transitionSpec = {
                    val d = XeMotion.durationMs(260, speed)
                    (slideInVertically(tween(d)) { it / 3 } + fadeIn(tween(d))) togetherWith (slideOutVertically(tween(d)) { -it / 3 } + fadeOut(tween(d / 2)))
                }, label = "title",
            ) { t -> Text(t, style = XeType.LargeTitle, color = scheme.text, maxLines = 1) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassIconButton(R.drawable.ti_search, stringResource(R.string.search), onSearch)
                GlassIconButton(R.drawable.ti_shopping_bag, stringResource(R.string.store), onStore)
                GlassIconButton(R.drawable.ti_dots, stringResource(R.string.menu), onMenu)
            }
        }
        HorizontalPager(state.pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1, key = { pages.getOrNull(it)?.id ?: it }) { page ->
            val cat = pages.getOrNull(page) ?: return@HorizontalPager
            // The page slides in with its icons already drawn, so they don't play an entrance again when it settles.
            AppGrid(cat.apps, look, speed, onLaunch, onLongPress)
        }
        if (look.categoryBar && pages.size > 1) {
            CategoryBar(pages, state, onSelect = { scope.launch { state.animateTo(it, speed) } },
                Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 14.dp), onLongPress = onEditCategories)
        } else Spacer(Modifier.navigationBarsPadding().height(14.dp))
    }
}

/** How open the drawer is (0..1), read each frame; icons cascade in with it. */
val LocalDrawerProgress = androidx.compose.runtime.staticCompositionLocalOf<() -> Float> { { 1f } }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(
    apps: List<AppEntry>, look: DrawerLook, speed: AnimationSpeed,
    onLaunch: (AppEntry, Rect) -> Unit, onLongPress: (AppEntry, Rect) -> Unit,
) {
    LazyVerticalGrid(
        GridCells.Fixed(4), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(apps, key = { _, a -> a.key.encode() }) { i, app ->
            val drawerProgress = LocalDrawerProgress.current
            var bounds = remember { Rect.Zero }
            val src = remember { MutableInteractionSource() }
            val density = LocalDensity.current
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                AppIcon(
                    app, look.iconSize.dp, look.iconStyle(app), look.iconShape, look.labels,
                    Modifier
                        .graphicsLayer {
                            // Rows rise in one after another as the drawer opens (and follow the finger back down).
                            val row = i / 4
                            val c = if (speed == AnimationSpeed.OFF) 1f else com.xenoorigin.xelauncher.design.drawerCascade(drawerProgress(), row)
                            alpha = c
                            val sc = .88f + .12f * c; scaleX = sc; scaleY = sc
                            translationY = (1f - c) * with(density) { (36 + row * 12).dp.toPx() }
                            rotationX = (1f - c) * 30f
                            cameraDistance = 12f * density.density
                        }
                        .onGloballyPositioned { bounds = it.boundsInWindow() }
                        .pressSquish(src)
                        .combinedClickable(src, null, onLongClick = { onLongPress(app, bounds) }) { onLaunch(app, bounds) },
                )
            }
        }
    }
}
