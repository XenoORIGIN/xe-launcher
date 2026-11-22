package com.xenoorigin.xelauncher.wallpaper

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WallpaperArtTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val size = { 90 to 195 }

    /** Smooth gradients must be dithered: a column through the picture has many distinct shades, not a few bands. */
    @Test fun everyPresetRendersWithoutBanding() {
        for (p in XeCollection.filterNot { it is WallpaperPreset.Sky }) {
            val b = WallpaperRenderer.render(p, 180, 390)
            val col = (0 until b.height).map { b.getPixel(90, it) }.toSet()
            com.google.common.truth.Truth.assertWithMessage(p.id).that(col.size).isAtLeast(60)
        }
    }

    /** Contact sheet of the collection for review (app/build/wallpapers_sheet.png). */
    @Test fun contactSheet() {
        val items = XeCollection.filterNot { it is WallpaperPreset.Sky }
        val tw = 180; val th = 390; val cols = 6
        val sheet = android.graphics.Bitmap.createBitmap(tw * cols, th * ((items.size + cols - 1) / cols), android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(sheet)
        items.forEachIndexed { i, p -> c.drawBitmap(WallpaperRenderer.render(p, tw, th), (i % cols * tw).toFloat(), (i / cols * th).toFloat(), null) }
        File("build/wallpapers_sheet.png").also { it.parentFile?.mkdirs() }.outputStream().use { sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun liquidPresetsArePresentAndDistinct() {
        val liquid = XeCollection.filterIsInstance<WallpaperPreset.Liquid>()
        assertThat(liquid.size).isAtLeast(6)
        assertThat(liquid.map { it.id }.toSet()).hasSize(liquid.size)
        assertThat(liquid.map { it.style }.toSet()).containsExactlyElementsIn(LiquidStyle.entries)
        fun avg(p: WallpaperPreset): Triple<Int, Int, Int> {
            val b = WallpaperRenderer.render(p, 60, 130)
            var r = 0; var g = 0; var bl = 0; val n = b.width * b.height
            for (y in 0 until b.height) for (x in 0 until b.width) { val c = b.getPixel(x, y); r += (c shr 16) and 255; g += (c shr 8) and 255; bl += c and 255 }
            return Triple(r / n, g / n, bl / n)
        }
        val avgs = liquid.map(::avg)
        for (i in avgs.indices) for (j in i + 1 until avgs.size) {
            val (a, b) = avgs[i] to avgs[j]
            assertThat(kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second) + kotlin.math.abs(a.third - b.third)).isAtLeast(12)
        }
    }

    @Test fun builtInWallpaperRedrawnOnceAfterUpdate() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "art-${System.nanoTime()}")
        val dir = File(ctx.filesDir, "art-${System.nanoTime()}")
        WallpaperController(ctx, settings, dir, size).apply(XeCollection.byId("midnight")!!, WallpaperTarget.HOME)
        settings.update { it.copy(wallpaperArtVersion = 1) } // as if made by the old version
        val c = WallpaperController(ctx, settings, dir, size, systemWallpaperIsOurs = { true })
        c.restore()
        assertThat(c.systemWallpaperSetCount).isEqualTo(1)
        assertThat(settings.current().wallpaperArtVersion).isEqualTo(WallpaperController.ART_VERSION)
        val d = WallpaperController(ctx, settings, dir, size)
        d.restore()
        assertThat(d.systemWallpaperSetCount).isEqualTo(0)
        sc.cancel()
    }

    @Test fun importedPhotoNotRedrawn() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "art2-${System.nanoTime()}")
        val dir = File(ctx.filesDir, "art2-${System.nanoTime()}")
        val c0 = WallpaperController(ctx, settings, dir, size)
        c0.applyBitmap(android.graphics.Bitmap.createBitmap(90, 195, android.graphics.Bitmap.Config.ARGB_8888), WallpaperTarget.HOME)
        settings.update { it.copy(wallpaperId = "image:photo.jpg", wallpaperArtVersion = 1) }
        val c = WallpaperController(ctx, settings, dir, size)
        c.restore()
        assertThat(c.systemWallpaperSetCount).isEqualTo(0)
        sc.cancel()
    }

    @Test fun artRedrawSkipsTheSystemWallpaperWhenSomeoneElseSetIt() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "art3-${System.nanoTime()}")
        val dir = File(ctx.filesDir, "art3-${System.nanoTime()}")
        WallpaperController(ctx, settings, dir, size).apply(XeCollection.byId("midnight")!!, WallpaperTarget.HOME)
        settings.update { it.copy(wallpaperArtVersion = 1) }
        val c = WallpaperController(ctx, settings, dir, size, systemWallpaperIsOurs = { false })
        c.restore()
        assertThat(c.systemWallpaperSetCount).isEqualTo(0)
        assertThat(c.current.value).isNotNull()
        assertThat(settings.current().wallpaperArtVersion).isEqualTo(WallpaperController.ART_VERSION)
        sc.cancel()
    }
}
