package com.xenoorigin.xelauncher.wallpaper.ascii

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.wallpaper.AsciiCollection
import com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer
import com.xenoorigin.xelauncher.wallpaper.allWallpapers
import com.xenoorigin.xelauncher.wallpaper.asciiPixelScale
import com.xenoorigin.xelauncher.wallpaper.byId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AsciiRendererTest {
    private val lake get() = AsciiCollection[1]

    @Test fun collectionHasEightWithLiveFirst() {
        assertThat(AsciiCollection.map { it.id }).containsExactly(
            "ascii_live", "ascii_lake", "ascii_rain", "ascii_dawn", "ascii_pines", "ascii_city", "ascii_space", "ascii_lighthouse",
        ).inOrder()
        assertThat(AsciiCollection.first().live).isTrue()
        assertThat(allWallpapers().byId("ascii_city")).isNotNull()
    }

    @Test fun rendersAtTheRequestedSize() {
        val b = WallpaperRenderer.render(lake, 1080, 2340)
        assertThat(b.width to b.height).isEqualTo(1080 to 2340)
    }

    @Test fun pixelScaleIsAWholeNumber() {
        assertThat(asciiPixelScale(1080f)).isEqualTo(3)
        assertThat(asciiPixelScale(1080f * 1.12f)).isEqualTo(3)
        assertThat(asciiPixelScale(1440f)).isEqualTo(4)
        assertThat(asciiPixelScale(100f)).isEqualTo(1)
    }

    @Test fun pixelsAreWholeBlocks() {
        val b = WallpaperRenderer.render(lake, 1080, 2340)
        val (ox, oy) = WallpaperRenderer.asciiOrigin(1080, 2340)
        val rnd = java.util.Random(3)
        var checked = 0
        while (checked < 200) {
            val bx = rnd.nextInt(355); val by = rnd.nextInt(770)
            val x0 = ox + bx * 3; val y0 = oy + by * 3
            if (x0 < 0 || y0 < 0 || x0 + 2 >= 1080 || y0 + 2 >= 2340) continue
            val c = b.getPixel(x0, y0)
            if (c == 0xFF18181A.toInt()) continue
            for (dy in 0..2) for (dx in 0..2) assertThat(b.getPixel(x0 + dx, y0 + dy)).isEqualTo(c)
            checked++
        }
    }

    @Test fun noEmptyStripAtTheEdges() {
        val b = WallpaperRenderer.render(lake, 1209, 2620)
        for (y in listOf(0, 1300, 2619)) for (x in listOf(0, 1208)) {
            assertThat(Color.alpha(b.getPixel(x, y))).isEqualTo(255)
            assertThat(b.getPixel(x, y)).isNotEqualTo(0xFF000000.toInt())
        }
    }

    @Test fun thumbnailsKeepDetail() {
        val b = WallpaperRenderer.render(AsciiCollection[5], 135, 292)
        val colours = (0 until 292).flatMap { y -> (0 until 135).map { b.getPixel(it, y) } }.toSet().size
        assertThat(colours).isAtLeast(60)
    }

    @Test fun colourComesFromThePreset() {
        val mono = WallpaperRenderer.render(lake, 360, 784)
        val acc = WallpaperRenderer.render(lake.copy(color = AsciiColor.ACCENT, accent = 0xFF2F8BFF), 360, 784)
        var bluer = 0
        for (y in 0 until 784 step 2) for (x in 0 until 360 step 2) {
            val a = acc.getPixel(x, y)
            if (a != mono.getPixel(x, y) && Color.blue(a) > Color.red(a) + 30) bluer++
        }
        assertThat(bluer).isGreaterThan(200)
    }

    @Test fun homeAsciiFollowsSettings() {
        val base = com.xenoorigin.xelauncher.settings.XeSettings()
        assertThat(com.xenoorigin.xelauncher.wallpaper.homeAscii(base)).isNull()
        val on = base.copy(wallpaperId = "ascii_city", asciiColor = AsciiColor.ACCENT, accent = 0xFF2F8BFF)
        val p = com.xenoorigin.xelauncher.wallpaper.homeAscii(on)!!
        assertThat(p.id).isEqualTo("ascii_city")
        assertThat(p.color).isEqualTo(AsciiColor.ACCENT)
        assertThat(p.accent).isEqualTo(0xFF2F8BFF)
        assertThat(com.xenoorigin.xelauncher.wallpaper.homeAscii(on.copy(liveOnHome = false))).isNull()
    }

    /** Contact sheet for review: app/src/test/screenshots/ascii_wallpapers.png */
    @Test fun contactSheet() {
        val items = AsciiCollection + listOf(lake.copy(color = AsciiColor.SKY), lake.copy(color = AsciiColor.ACCENT, accent = 0xFF2F8BFF))
        val tw = 360; val th = 780; val cols = 5
        val sheet = Bitmap.createBitmap(tw * cols, th * ((items.size + cols - 1) / cols), Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet)
        items.forEachIndexed { i, p -> c.drawBitmap(WallpaperRenderer.render(p, tw, th), (i % cols * tw).toFloat(), (i / cols * th).toFloat(), null) }
        File(SHOT_DIR).mkdirs()
        File(SHOT_DIR, "ascii_wallpapers.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
