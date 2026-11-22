package com.xenoorigin.xelauncher.wallpaper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.xenoorigin.xelauncher.design.LocalHazeState
import dev.chrisbanes.haze.hazeSource

/** Draws Xe's wallpaper; [parallaxPx] shifts it while paging and tilting (depth). Acts as the blur source. */
@Composable
fun WallpaperLayer(
    controller: WallpaperController, parallaxPx: () -> androidx.compose.ui.geometry.Offset, modifier: Modifier = Modifier,
    /** Live sky chosen: draw it live (stars, clouds) instead of the saved picture. */
    liveSky: Boolean = false, animate: Boolean = true,
) {
    val bmp by controller.current.collectAsState()
    val haze = LocalHazeState.current
    var m = modifier.fillMaxSize().background(Color.Black)
    if (haze != null) m = m.hazeSource(haze)
    Box(m) {
        if (liveSky) {
            var minute by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(minuteNow()) }
            val sun = androidx.compose.runtime.remember(minute / 60) { sunToday() }
            val visible = rememberXeVisible()
            androidx.compose.runtime.LaunchedEffect(visible) {
                if (!visible) return@LaunchedEffect
                while (true) { minute = minuteNow(); kotlinx.coroutines.delay(15_000) }
            }
            LiveSkyLayer(sun, { minute }, animate, Modifier.fillMaxSize().graphicsLayer { val p = parallaxPx(); translationX = p.x; translationY = p.y; scaleX = WALL_SCALE; scaleY = WALL_SCALE })
            return@Box
        }
        bmp?.let {
            Image(
                it, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { val p = parallaxPx(); translationX = p.x; translationY = p.y; scaleX = WALL_SCALE; scaleY = WALL_SCALE },
            )
        }
    }
}
