package com.xenoorigin.xelauncher.wallpaper.ascii

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.xenoorigin.xelauncher.wallpaper.WALL_SCALE
import com.xenoorigin.xelauncher.wallpaper.WallpaperPreset
import com.xenoorigin.xelauncher.wallpaper.asciiPixelScale
import com.xenoorigin.xelauncher.wallpaper.minuteNow
import com.xenoorigin.xelauncher.wallpaper.skyAt
import com.xenoorigin.xelauncher.wallpaper.skyLoopRuns
import com.xenoorigin.xelauncher.wallpaper.sunToday
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Counts frames drawn, for tests. */
internal object AsciiLayerProbe { @Volatile var frames = 0 }

/** One reusable frame: the field, its pixels and the bitmap they are shown from. */
private class AsciiFrame(val cols: Int, val rows: Int) {
    val field = AsciiField(cols, rows)
    val rw = cols * AsciiGlyphs.CW
    val rh = rows * AsciiGlyphs.CH
    private val pixels = IntArray(rw * rh)
    private val bitmap: Bitmap = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
    val image: ImageBitmap = bitmap.asImageBitmap()

    fun draw(p: WallpaperPreset.Ascii, t: Float, minute: Int) {
        val look = if (p.live) skyAt(p.minute ?: minute, sunToday()) else null
        field.fill(p.scene, t, look, p.color, p.accent)
        AsciiRaster.rasterize(field.cells, pixels)
        bitmap.setPixels(pixels, 0, rw, 0, 0, rw, rh)
        AsciiLayerProbe.frames++
    }
}

/**
 * An ASCII wallpaper drawn on Home. Each glyph pixel is a whole block of screen pixels (never blurred) and the
 * parallax shift is rounded to whole pixels, so nothing shimmers. The layer is drawn already enlarged to cover
 * the parallax zoom ([WALL_SCALE]). Live wallpapers move about 8 times a second while Xe is visible and
 * animations are on; otherwise they redraw once a minute. Still wallpapers draw once.
 */
@Composable
fun AsciiLayer(preset: WallpaperPreset.Ascii, parallaxPx: () -> Offset, animate: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        if (wPx <= 0 || hPx <= 0 || wPx == Int.MAX_VALUE || hPx == Int.MAX_VALUE) return@BoxWithConstraints
        val drawnW = wPx * WALL_SCALE
        val drawnH = hPx * WALL_SCALE
        val k = asciiPixelScale(drawnW)
        val cols = kotlin.math.ceil(drawnW / (AsciiGlyphs.CW * k)).toInt()
        val rows = kotlin.math.ceil(drawnH / (AsciiGlyphs.CH * k)).toInt()
        val frame = remember(cols, rows, preset) { AsciiFrame(cols, rows).also { it.draw(preset, 0f, minuteNow()) } }
        var tick by remember { mutableIntStateOf(0) }
        val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
        val running = preset.live && skyLoopRuns(animate, state)
        LaunchedEffect(frame, running) {
            if (!preset.live) return@LaunchedEffect
            var t = 0f
            if (running) {
                while (true) { delay(125); t += .125f; frame.draw(preset, t, minuteNow()); tick++ }
            } else {
                while (true) { delay(60_000); frame.draw(preset, t, minuteNow()); tick++ }
            }
        }
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                val p = parallaxPx()
                translationX = p.x.roundToInt().toFloat()
                translationY = p.y.roundToInt().toFloat()
            },
        ) {
            @Suppress("UNUSED_EXPRESSION") tick
            val ox = ((size.width - frame.rw * k) / 2f).roundToInt()
            val oy = ((size.height - frame.rh * k) / 2f).roundToInt()
            drawRect(androidx.compose.ui.graphics.Color(AsciiRaster.GROUND))
            drawImage(
                frame.image, srcOffset = IntOffset.Zero, srcSize = IntSize(frame.rw, frame.rh),
                dstOffset = IntOffset(ox, oy), dstSize = IntSize(frame.rw * k, frame.rh * k), filterQuality = FilterQuality.None,
            )
        }
    }
}
