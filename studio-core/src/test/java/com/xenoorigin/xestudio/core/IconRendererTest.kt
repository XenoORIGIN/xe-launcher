package com.xenoorigin.xestudio.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class IconRendererTest {
    private val size = 96
    private val bg = 0xFF112233.toInt()
    private val base = IconRecipe(background = Fill(listOf(bg)), treatment = Treatment.ORIGINAL)
    private val key = "pkg/act#0"

    private fun render(r: IconRecipe, src: android.graphics.drawable.Drawable? = ColorDrawable(Color.RED), overrides: (String) -> Bitmap? = { null }) =
        IconRenderer.render(r, key, "Camera", src, size, overrides)

    @Test fun circleCornersAreTransparent() {
        assertThat(Color.alpha(render(base.copy(shape = TileShape.CIRCLE)).getPixel(1, 1))).isEqualTo(0)
        assertThat(Color.alpha(render(base.copy(shape = TileShape.SQUARE)).getPixel(1, 1))).isEqualTo(255)
    }

    @Test fun everyShapeDrawsSomethingInTheMiddle() {
        for (s in TileShape.entries) {
            assertThat(Color.alpha(render(base.copy(shape = s)).getPixel(size / 2, size / 2))).isEqualTo(255)
        }
    }

    @Test fun backgroundFillsTileEdge() {
        assertThat(render(base.copy(shape = TileShape.SQUARE)).getPixel(3, size / 2)).isEqualTo(bg)
    }

    @Test fun monoProducesGrayGlyph() {
        val p = render(base.copy(treatment = Treatment.MONO)).getPixel(size / 2, size / 2)
        assertThat(Color.red(p)).isEqualTo(Color.green(p))
        assertThat(Color.green(p)).isEqualTo(Color.blue(p))
    }

    @Test fun tintColoursGlyph() {
        val p = render(base.copy(treatment = Treatment.TINT, tint = 0xFF00FF00.toInt())).getPixel(size / 2, size / 2)
        assertThat(Color.green(p)).isGreaterThan(200)
        assertThat(Color.red(p)).isLessThan(50)
    }

    @Test fun overrideImageWins() {
        val blue = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val r = base.copy(overrides = mapOf(key to IconOverride.Image("o.webp")))
        val p = render(r, overrides = { if (it == "o.webp") blue else null }).getPixel(size / 2, size / 2)
        assertThat(p).isEqualTo(Color.BLUE)
    }

    @Test fun missingOverrideImageFallsBackToTheAppIcon() {
        val r = base.copy(overrides = mapOf(key to IconOverride.Image("gone.webp")))
        assertThat(render(r).getPixel(size / 2, size / 2)).isEqualTo(Color.RED)
    }

    @Test fun missingSourceFallsBackToLetter() {
        val r = base.copy(shape = TileShape.SQUARE, tint = Color.WHITE)
        val bmp = render(r, src = null)
        var different = 0
        for (x in 30 until 66) for (y in 30 until 66) if (bmp.getPixel(x, y) != bg) different++
        assertThat(different).isGreaterThan(20)
    }

    @Test fun overrideForOtherAppIgnored() {
        val r = base.copy(overrides = mapOf("other/x#0" to IconOverride.Letter("Z", Color.WHITE, Fill(listOf(Color.BLUE)))))
        assertThat(render(r).getPixel(size / 2, size / 2)).isEqualTo(Color.RED)
    }
}
