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

    // ---- glyph extraction (removes an icon's own plate so it doesn't sit as a frame inside the tile) ----

    @Test fun extractGlyph_leavesPlainGlyphAlone() {
        val src = bmp(64, 64) { x, y -> if (x in 24..40 && y in 24..40) Color.WHITE else Color.TRANSPARENT }
        val r = IconProcessor.extractGlyph(src)
        assertThat(r.isGlyph).isTrue()
        assertThat(Color.alpha(r.bitmap.getPixel(2, 2))).isEqualTo(0)
        assertThat(Color.alpha(r.bitmap.getPixel(32, 32))).isGreaterThan(200)
    }

    @Test fun extractGlyph_removesFlatPlateAndKeepsSymbol() {
        // White rounded plate filling the icon with a black symbol in the middle (typical legacy icon).
        val src = bmp(64, 64) { x, y ->
            when {
                x in 26..38 && y in 26..38 -> Color.BLACK
                x in 4..59 && y in 4..59 -> Color.WHITE
                else -> Color.TRANSPARENT
            }
        }
        val r = IconProcessor.extractGlyph(src)
        assertThat(r.isGlyph).isTrue()
        assertThat(Color.alpha(r.bitmap.getPixel(8, 8))).isLessThan(30)   // plate gone
        assertThat(Color.alpha(r.bitmap.getPixel(32, 32))).isGreaterThan(200) // symbol kept
    }

    @Test fun extractGlyph_givesUpOnFullBleedArtwork() {
        // A photo-like icon: no single plate colour, so it must be shown whole instead.
        val src = bmp(64, 64) { x, y -> Color.rgb((x * 4) % 256, (y * 4) % 256, ((x + y) * 3) % 256) }
        assertThat(IconProcessor.extractGlyph(src).isGlyph).isFalse()
    }

    @Test fun extractGlyph_enlargesTinySymbol() {
        val src = bmp(100, 100) { x, y -> if (x in 45..54 && y in 45..54) Color.WHITE else Color.TRANSPARENT }
        val r = IconProcessor.extractGlyph(src)
        // 10px symbol (10%) is scaled up to roughly 60% of the tile.
        assertThat(Color.alpha(r.bitmap.getPixel(28, 50))).isGreaterThan(200)
    }

    @Test fun milkyWhiteKeepsShapeAndIsLight() {
        val src = bmp { x, y -> if (x < 4) Color.argb(200, 20, 40, 220) else if (y < 4) Color.argb(255, 250, 30, 10) else Color.TRANSPARENT }
        val out = IconProcessor.milkyWhite(src)
        for (x in 0 until 8) for (y in 0 until 8) {
            val a = src.getPixel(x, y); val b = out.getPixel(x, y)
            assertThat(Color.alpha(b)).isEqualTo(Color.alpha(a))
            if (Color.alpha(b) > 0) {
                assertThat(Color.red(b)).isAtLeast(216); assertThat(Color.green(b)).isAtLeast(216); assertThat(Color.blue(b)).isAtLeast(216)
            }
        }
    }

    @Test fun milkyFromFullFadesDarkAreas() {
        val src = bmp { x, _ -> if (x < 4) Color.BLACK else Color.WHITE }
        val out = IconProcessor.milkyFromFull(src)
        assertThat(Color.alpha(out.getPixel(1, 1))).isLessThan(Color.alpha(out.getPixel(6, 1)))
        assertThat(Color.red(out.getPixel(1, 1))).isAtLeast(210) // faint pixels lose a little to premultiplied alpha
    }
}
