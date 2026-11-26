package com.xenoorigin.xelauncher.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalHazeState
import com.xenoorigin.xelauncher.design.XeMotion
import com.xenoorigin.xelauncher.design.liquidGlass
import com.xenoorigin.xelauncher.di.AppGraph
import com.xenoorigin.xelauncher.settings.XeSettings
import kotlinx.coroutines.launch

/** Settings: a small route stack with slide transitions, drawn over blurred wallpaper. */
@Composable
fun SettingsHost(graph: AppGraph, start: String, onClose: () -> Unit) {
    val loaded by graph.settings.settings.collectAsState(initial = null)
    val settings = loaded ?: return
    val allApps by graph.labeledApps.collectAsState()
    val newVersion by graph.updates.available.collectAsState()
    val scope = rememberCoroutineScope()
    val speed = LocalAnimationSpeed.current
    val stack = remember { mutableStateListOf(start) }
    val update: ((XeSettings) -> XeSettings) -> Unit = { f -> scope.launch { graph.settings.update(f) } }
    fun push(r: String) { stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onClose() }
    BackHandler(onBack = ::pop)
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {}) {
        com.xenoorigin.xelauncher.wallpaper.BlurredBackdrop(graph.wallpaper, if (LocalGlassStyle.current.dark) Color.Black.copy(alpha = .55f) else Color.White.copy(alpha = .62f))
        val depth = stack.size
        AnimatedContent(
            stack.last() to depth,
            transitionSpec = {
                val forward = targetState.second >= initialState.second
                val d = XeMotion.durationMs(300, speed)
                // The new page glides in from the side while the old one sinks back a little, like stacked glass.
                (slideInHorizontally(tween(d, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { if (forward) it / 2 else -it / 3 } + fadeIn(tween(d)) +
                    androidx.compose.animation.scaleIn(tween(d), initialScale = if (forward) 1f else .92f)) togetherWith
                    (slideOutHorizontally(tween(d)) { if (forward) -it / 5 else it / 2 } + fadeOut(tween(d * 2 / 3)) +
                        androidx.compose.animation.scaleOut(tween(d), targetScale = if (forward) .92f else 1f))
            }, label = "settings",
        ) { (route, _) ->
            val back: () -> Unit = ::pop
            when (route) {
                "root" -> SettingsRoot(settings, ::push, onClose, newVersion?.tag, update)
                "wallpaper" -> WallpaperScreen(graph, back)
                "appearance" -> AppearanceScreen(settings, update, back)
                "icons" -> IconsScreen(settings, update, back, allApps.firstOrNull { it.key.packageName.contains("camera") } ?: allApps.firstOrNull()) { push("bulk") }
                "bulk" -> com.xenoorigin.xelauncher.drawer.BulkIcons(
                    allApps.filter { it.key.encode() !in settings.hiddenApps },
                    { a -> settings.iconOverrides[a.key.encode()] ?: settings.iconStyle }, settings.iconShape,
                    onApply = { keys, style -> update { st -> st.copy(iconOverrides = if (style == null) st.iconOverrides - keys else st.iconOverrides + keys.associateWith { style }) } },
                    onClose = back,
                )
                "studio" -> StudioScreen(::push, back)
                "widgets_help" -> WidgetsHelpScreen(back)
                "animation" -> AnimationScreen(settings, update, back)
                "play" -> PlayScreen(settings, update, ::push, back)
                "badges" -> BadgesScreen(settings, update, back)
                "gestures" -> GesturesScreen(settings, update, back)
                "backup" -> BackupScreen(graph, back)
                "pages/home" -> HomePageScreen(settings, update, ::push, back)
                "pages/apps" -> AppPageScreen(settings, update, ::push, back)
                "pages/search" -> SearchPageScreen(settings, update, back)
                "hidden" -> HiddenAppsScreen(graph, settings, update, back)
                "about" -> AboutScreen(back, settings, update, graph.updates)
                "themes" -> ThemesScreen(graph, settings, back)
                "bugs" -> BugReportScreen(graph, settings, back)
                else -> SettingsRoot(settings, ::push, onClose, update = update)
            }
        }
    }
}
