package com.xenoorigin.xelauncher.wallpaper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.xenoorigin.xelauncher.design.LocalHazeState
import dev.chrisbanes.haze.hazeSource

/** Draws Xe's wallpaper; [parallaxPx] shifts it horizontally while paging. Acts as the blur source. */
@Composable
fun WallpaperLayer(controller: WallpaperController, parallaxPx: () -> Float, modifier: Modifier = Modifier) {
    val bmp by controller.current.collectAsState()
    val haze = LocalHazeState.current
    var m = modifier.fillMaxSize().background(Color.Black)
    if (haze != null) m = m.hazeSource(haze)
    Box(m) {
        bmp?.let {
            Image(
                it, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { val p = parallaxPx(); translationX = p; scaleX = 1.08f; scaleY = 1.08f },
            )
        }
    }
}
