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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale

/**
 * Full-screen frosted background made from the pre-blurred wallpaper. Costs one bitmap draw per frame,
 * unlike live blur, so opening the drawer or settings stays smooth on mid-range phones.
 */
@Composable
fun BlurredBackdrop(controller: WallpaperController, tint: Color, modifier: Modifier = Modifier) {
    val bmp by controller.backdrop.collectAsState()
    Box(modifier.fillMaxSize().background(Color.Black)) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low) }
        Box(Modifier.fillMaxSize().background(tint))
    }
}
