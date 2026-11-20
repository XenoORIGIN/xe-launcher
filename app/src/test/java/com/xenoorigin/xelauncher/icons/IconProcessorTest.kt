package com.xenoorigin.xelauncher.icons

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
class IconProcessorTest {
    private fun bmp(w: Int = 8, h: Int = 8, fill: (Int, Int) -> Int): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until w) for (y in 0 until h) setPixel(x, y, fill(x, y))
        }

    @Test fun grayscale_makesEveryOpaquePixelNeutral() {
        val src = bmp { x, _ -> if (x < 4) Color.rgb(220, 40, 60) else Color.rgb(30, 160, 250) }
        val g = IconProcessor.grayscale(src, contrast = 1.15f)
        for (x in 0 until 8) for (y in 0 until 8) {
            val p = g.getPixel(x, y)
            assertThat(Color.red(p)).isEqualTo(Color.green(p))
            assertThat(Color.green(p)).isEqualTo(Color.blue(p))
        }
    }

    @Test fun grayscale_keepsTransparentPixelsTransparent() {
        val src = bmp { x, _ -> if (x < 4) Color.TRANSPARENT else Color.RED }
        assertThat(Color.alpha(IconProcessor.grayscale(src, 1f).getPixel(1, 1))).isEqualTo(0)
    }

    @Test fun grayscale_invertsDarkGlyphSoItShowsOnDarkGlass() {
        // A black logo on transparent would vanish on a dark tile.
        val src = bmp { x, y -> if (x in 2..5 && y in 2..5) Color.rgb(10, 10, 10) else Color.TRANSPARENT }
        val g = IconProcessor.grayscale(src, 1f, lightenDarkGlyphs = true)
        assertThat(Color.red(g.getPixel(3, 3))).isGreaterThan(200)
    }

    @Test fun dominantColor_ignoresWhiteAndTransparent() {
        val src = bmp { x, y -> when { y < 2 -> Color.TRANSPARENT; x < 4 -> Color.rgb(230, 30, 30); else -> Color.WHITE } }
        val c = IconProcessor.dominantColor(src)
        assertThat(Color.red(c)).isGreaterThan(180)
        assertThat(Color.green(c)).isLessThan(80)
    }

    @Test fun dominantColor_fallsBackToGrey() {
        assertThat(IconProcessor.dominantColor(bmp { _, _ -> Color.WHITE })).isEqualTo(0xFF8E8E93.toInt())
    }
}
