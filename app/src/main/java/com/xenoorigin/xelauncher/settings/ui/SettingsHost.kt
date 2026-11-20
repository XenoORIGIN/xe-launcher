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
    val scope = rememberCoroutineScope()
    val speed = LocalAnimationSpeed.current
    val stack = remember { mutableStateListOf(start) }
    val update: ((XeSettings) -> XeSettings) -> Unit = { f -> scope.launch { graph.settings.update(f) } }
    fun push(r: String) { stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onClose() }
    BackHandler(onBack = ::pop)
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {}) {
        Box(Modifier.fillMaxSize().liquidGlass(RectangleShape, LocalGlassStyle.current.let { it.copy(rimAlpha = 0f, tint = if (it.dark) Color.Black.copy(alpha = .62f) else Color.White.copy(alpha = .7f)) }, LocalHazeState.current))
        val depth = stack.size
        AnimatedContent(
            stack.last() to depth,
            transitionSpec = {
                val forward = targetState.second >= initialState.second
                val d = XeMotion.durationMs(300, speed)
                (slideInHorizontally(tween(d)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(d))) togetherWith
                    (slideOutHorizontally(tween(d)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(d / 2)))
            }, label = "settings",
        ) { (route, _) ->
            val back: () -> Unit = ::pop
            when (route) {
                "root" -> SettingsRoot(settings, ::push, onClose)
                "wallpaper" -> WallpaperScreen(graph, back)
                "appearance" -> AppearanceScreen(settings, update, back)
                "icons" -> IconsScreen(settings, update, back)
                "animation" -> AnimationScreen(settings, update, back)
                "gestures" -> GesturesScreen(settings, update, back)
                "backup" -> BackupScreen(graph, back)
                "pages/home" -> HomePageScreen(settings, update, ::push, back)
                "pages/apps" -> AppPageScreen(settings, update, ::push, back)
                "pages/search" -> SearchPageScreen(settings, update, back)
                "hidden" -> HiddenAppsScreen(graph, settings, update, back)
                "about" -> AboutScreen(back)
                else -> SettingsRoot(settings, ::push, onClose)
            }
        }
    }
}
