package com.xenoorigin.xelauncher.testutil

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.scale
import com.xenoorigin.xelauncher.design.LocalHazeState
import com.xenoorigin.xelauncher.wallpaper.byId
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

const val SHOT_DIR = "src/test/screenshots"

/** A colourful backdrop that acts as the haze source, so glass has something to blur/bend. */
@Composable
fun TestBackdrop(black: Boolean = false, content: @Composable () -> Unit) {
    val haze = remember { HazeState() }
    CompositionLocalProvider(LocalHazeState provides haze) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().hazeSource(haze)) {
                if (black) {
                    drawRect(Color.Black)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF8A8A8A), Color.Transparent), center = Offset(size.width * .2f, size.height * .3f), radius = size.width * .6f), radius = size.width * .6f, center = Offset(size.width * .2f, size.height * .3f))
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF5A5A5A), Color.Transparent), center = Offset(size.width * .9f, size.height * .75f), radius = size.width * .5f), radius = size.width * .5f, center = Offset(size.width * .9f, size.height * .75f))
                } else {
                    drawRect(Brush.linearGradient(listOf(Color(0xFF0B1A8F), Color(0xFF2F4BFF), Color(0xFF7A5CFF)), end = Offset(size.width, size.height)))
                    drawCircle(Color(0xFFC27BFF), radius = size.width * .35f, center = Offset(size.width * .85f, size.height * .45f))
                    drawCircle(Color(0xFF1E8BFF), radius = size.width * .4f, center = Offset(size.width * .1f, size.height * .85f))
                    drawRect(Color.White.copy(alpha = .9f), topLeft = Offset(size.width * .1f, size.height * .2f), size = androidx.compose.ui.geometry.Size(size.width * .8f, 10f))
                }
            }
            content()
        }
    }
}

/**
 * A real wallpaper behind Liquid Glass, drawn the way Xe draws it (zoomed by WALL_SCALE),
 * with the frosted copy provided to the glass and Liquid Glass switched on.
 */
@Composable
fun LiquidBackdrop(wall: android.graphics.Bitmap, content: @Composable () -> Unit) {
    val img = remember(wall) { wall.asImageBitmap() }
    val frosted = remember(wall) { com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer.glassBackdrop(wall).asImageBitmap() }
    val haze = remember { HazeState() }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val screen = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.geometry.Size(maxWidth.toPx(), maxHeight.toPx()) }
        CompositionLocalProvider(
            LocalHazeState provides haze,
            com.xenoorigin.xelauncher.design.LocalLiquidGlass provides true,
            com.xenoorigin.xelauncher.design.LocalGlassBackdrop provides com.xenoorigin.xelauncher.design.GlassBackdrop({ frosted }, { Offset.Zero }, { screen }),
        ) {
            androidx.compose.foundation.Image(
                img, null,
                Modifier.fillMaxSize().hazeSource(haze).scale(com.xenoorigin.xelauncher.wallpaper.WALL_SCALE),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            content()
        }
    }
}

/** A Xe wallpaper rendered at phone size, for Liquid Glass screenshots. */
fun xeWall(id: String, w: Int = 1170, h: Int = 2532): android.graphics.Bitmap =
    com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer.render(com.xenoorigin.xelauncher.wallpaper.XeCollection.byId(id)!!, w, h)
