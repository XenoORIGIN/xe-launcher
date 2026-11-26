package com.xenoorigin.xelauncher.wallpaper

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 2.0 bokeh wallpapers: full of light and detail so Liquid Glass has something to bend, like the owner's clip. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BokehWallpaperTest {
    private val bokeh = XeCollection.filterIsInstance<WallpaperPreset.Bokeh>()

    @Test fun threeBokehWallpapersAreInTheCollection() {
        assertThat(bokeh.map { it.id }).containsExactly("bokeh_garden", "bokeh_blossom", "bokeh_night")
    }

    /** Plenty of local detail: many neighbouring pixels differ clearly (a plain gradient has almost none). */
    @Test fun bokehHasDetailForGlassToBend() {
        for (p in bokeh) {
            val b = WallpaperRenderer.render(p, 270, 585)
            var edges = 0; var n = 0
            for (y in 0 until b.height step 3) for (x in 1 until b.width step 3) {
                val a = b.getPixel(x - 1, y); val c = b.getPixel(x, y)
                val diff = Math.abs(Color.red(a) - Color.red(c)) + Math.abs(Color.green(a) - Color.green(c)) + Math.abs(Color.blue(a) - Color.blue(c))
                if (diff > 12) edges++; n++
            }
            assertThat(edges.toFloat() / n).isGreaterThan(.02f)
        }
    }

    @Test fun sameSeedSamePicture() {
        val p = bokeh.first()
        assertThat(WallpaperRenderer.render(p, 90, 195).sameAs(WallpaperRenderer.render(p, 90, 195))).isTrue()
    }

    /** Review picture of the three bokeh wallpapers side by side. */
    @Test fun contactSheet() {
        val w = 270; val h = 585
        val sheet = android.graphics.Bitmap.createBitmap(w * bokeh.size, h, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(sheet)
        bokeh.forEachIndexed { i, p -> c.drawBitmap(WallpaperRenderer.render(p, w, h), (i * w).toFloat(), 0f, null) }
        java.io.File("build/bokeh_wallpapers.png").outputStream().use { sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        // Full-size copies for the website's theme previews (390 × 845).
        bokeh.forEach { p -> java.io.File("build/wall_${p.id}.png").outputStream().use { WallpaperRenderer.render(p, 390, 845).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
    }
}
