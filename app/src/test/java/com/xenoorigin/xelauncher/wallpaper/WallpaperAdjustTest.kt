package com.xenoorigin.xelauncher.wallpaper

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WallpaperAdjustTest {
    /** Landscape photo: left half red, right half blue. */
    private fun photo(w: Int = 400, h: Int = 200) = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until w) for (y in 0 until h) setPixel(x, y, if (x < w / 2) Color.RED else Color.BLUE)
    }

    @Test fun compose_keepsAspectRatio_neverStretches() {
        // A stretched image would show both colours across the whole width; a crop of the centre shows a seam in the middle.
        val out = WallpaperRenderer.compose(photo(), 90, 195, WallpaperAdjust())
        assertThat(out.width).isEqualTo(90); assertThat(out.height).isEqualTo(195)
        assertThat(Color.red(out.getPixel(10, 100))).isGreaterThan(200)
        assertThat(Color.blue(out.getPixel(80, 100))).isGreaterThan(200)
    }

    @Test fun compose_panLeftShowsLeftSide() {
        val out = WallpaperRenderer.compose(photo(), 90, 195, WallpaperAdjust(offsetX = -1f))
        assertThat(Color.red(out.getPixel(80, 100))).isGreaterThan(200)
    }

    @Test fun compose_dimDarkens() {
        val plain = WallpaperRenderer.compose(photo(), 90, 195, WallpaperAdjust(offsetX = -1f))
        val dim = WallpaperRenderer.compose(photo(), 90, 195, WallpaperAdjust(offsetX = -1f, dim = .5f))
        assertThat(Color.red(dim.getPixel(45, 100))).isLessThan(Color.red(plain.getPixel(45, 100)) - 80)
    }

    @Test fun compose_blurSoftensTheSeam() {
        val sharp = WallpaperRenderer.compose(photo(), 90, 195, WallpaperAdjust())
        val soft = WallpaperRenderer.compose(photo(), 90, 195, WallpaperAdjust(blur = 1f))
        // Right at the seam a blurred image mixes both colours.
        val p = soft.getPixel(45, 100)
        assertThat(Color.red(p)).isIn(com.google.common.collect.Range.closed(40, 215))
        assertThat(Color.red(sharp.getPixel(40, 100))).isGreaterThan(200)
    }

    @Test fun backdrop_isSmallAndBlurred() {
        val b = WallpaperRenderer.backdrop(photo(1080, 2340))
        assertThat(b.width).isAtMost(160)
    }
}
