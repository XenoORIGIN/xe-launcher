package com.xenoorigin.xelauncher.wallpaper

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import androidx.compose.ui.graphics.asAndroidBitmap

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WallpaperControllerTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val size = { 90 to 195 }

    @Test fun apply_writesFile_persistsId_andIsRestoredByNewInstance() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "wp_test")
        val dir = File(ctx.filesDir, "wp-${System.nanoTime()}")
        val c = WallpaperController(ctx, settings, dir, size)
        c.apply(XeCollection.byId("xe_blue")!!, WallpaperTarget.HOME)
        assertThat(File(dir, "current.png").exists()).isTrue()
        assertThat(settings.current().wallpaperId).isEqualTo("xe_blue")

        val again = WallpaperController(ctx, settings, dir, size)
        again.restore()
        assertThat(again.current.value).isNotNull()
        assertThat(again.current.value!!.width).isEqualTo(90)
        sc.cancel()
    }

    @Test fun liveSky_redrawsOnlyWhenChosen_andOnlyWhenTheTimeOfDayMovesOn() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "wp_sky")
        val dir = File(ctx.filesDir, "wp-${System.nanoTime()}")
        val c = WallpaperController(ctx, settings, dir, size)
        c.apply(XeCollection.byId("xe_blue")!!, WallpaperTarget.HOME)
        val base = c.systemWallpaperSetCount
        c.refreshLiveSky(minute = 12 * 60)
        assertThat(c.systemWallpaperSetCount).isEqualTo(base) // not the chosen wallpaper: untouched

        c.apply(WallpaperPreset.Sky(minute = 12 * 60), WallpaperTarget.HOME)
        val afterApply = c.systemWallpaperSetCount
        c.refreshLiveSky(minute = 12 * 60 + 5)
        assertThat(c.systemWallpaperSetCount).isEqualTo(afterApply) // same 20-minute slot
        val noon = c.current.value!!.asAndroidBitmap().getPixel(45, 20)
        c.refreshLiveSky(minute = 23 * 60)
        assertThat(c.systemWallpaperSetCount).isEqualTo(afterApply + 1)
        val night = c.current.value!!.asAndroidBitmap().getPixel(45, 20)
        assertThat(android.graphics.Color.blue(night)).isLessThan(android.graphics.Color.blue(noon))
        assertThat(settings.current().wallpaperId).isEqualTo(LIVE_SKY_ID)
        sc.cancel()
    }

    @Test fun restore_withNothingSaved_rendersDefaultWithoutTouchingSystemWallpaper() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "wp_test2")
        val dir = File(ctx.filesDir, "wp-${System.nanoTime()}")
        val c = WallpaperController(ctx, settings, dir, size)
        c.restore()
        assertThat(c.current.value).isNotNull()
        assertThat(c.systemWallpaperSetCount).isEqualTo(0)
        sc.cancel()
    }

    @Test fun applyImage_fitsPictureToScreen_andMakesBackdrop() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "wp_test3")
        val dir = File(ctx.filesDir, "wp-${System.nanoTime()}").apply { mkdirs() }
        val pic = File(dir, "imported_1.jpg")
        android.graphics.Bitmap.createBitmap(400, 200, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
            .let { b -> pic.outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) } }
        val c = WallpaperController(ctx, settings, dir, size)
        c.applyImage(pic, WallpaperAdjust(dim = .2f), WallpaperTarget.HOME)
        assertThat(c.current.value!!.width).isEqualTo(90)
        assertThat(c.current.value!!.height).isEqualTo(195)
        assertThat(c.backdrop.value).isNotNull()
        assertThat(c.imports()).containsExactly(pic)
        sc.cancel()
    }

    @Test fun liveSkyLeavesTheSystemLiveWallpaperAlone() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "wp_sky_live-${System.nanoTime()}")
        val dir = File(ctx.filesDir, "wp-${System.nanoTime()}")
        val c = WallpaperController(ctx, settings, dir, size, liveWallpaperActive = { true })
        settings.update { it.copy(wallpaperId = LIVE_SKY_ID) }
        c.refreshLiveSky(minute = 12 * 60)
        c.refreshLiveSky(minute = 23 * 60)
        assertThat(c.systemWallpaperSetCount).isEqualTo(0)
        assertThat(c.current.value).isNotNull() // Xe's own copy still follows the time
        sc.cancel()
    }

    @Test fun glassBackdropIsSmallBlurredAndBrighter() {
        val src = android.graphics.Bitmap.createBitmap(900, 1950, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until 900) { val c = if ((x / 10) % 2 == 0) android.graphics.Color.BLACK else android.graphics.Color.WHITE; for (y in 0 until 1950 step 13) src.setPixel(x, y, c) }
        android.graphics.Canvas(src).let { cv -> for (x in 0 until 90) cv.drawRect((x * 10).toFloat(), 0f, (x * 10 + 10).toFloat(), 1950f, android.graphics.Paint().apply { color = if (x % 2 == 0) android.graphics.Color.BLACK else android.graphics.Color.WHITE }) }
        fun lum(b: android.graphics.Bitmap): Double { var t = 0.0; var n = 0; for (y in 0 until b.height step 7) for (x in 0 until b.width step 3) { t += android.graphics.Color.red(b.getPixel(x, y)); n++ }; return t / n }
        val g = WallpaperRenderer.glassBackdrop(src)
        assertThat(g.width).isEqualTo(540) // 2.0: sharper copy so the lens edge visibly bends it
        assertThat(lum(g)).isGreaterThan(lum(src))
        var maxStep = 0
        val y = g.height / 2
        for (x in 1 until g.width) maxStep = maxOf(maxStep, kotlin.math.abs(android.graphics.Color.red(g.getPixel(x, y)) - android.graphics.Color.red(g.getPixel(x - 1, y))))
        assertThat(maxStep).isLessThan(128)
    }

    @Test fun controllerPublishesGlassBackdrop() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "wp_gb-${System.nanoTime()}")
        val c = WallpaperController(ctx, settings, File(ctx.filesDir, "wp-${System.nanoTime()}"), size)
        c.apply(XeCollection.first(), WallpaperTarget.HOME)
        assertThat(c.glassBackdrop.value).isNotNull()
        sc.cancel()
    }
    private fun newController(tag: String, sc: CoroutineScope): Triple<WallpaperController, SettingsStore, File> {
        val settings = SettingsStore(ctx, sc, "$tag-${System.nanoTime()}")
        val dir = File(ctx.filesDir, "wp-${System.nanoTime()}")
        return Triple(WallpaperController(ctx, settings, dir, size), settings, dir)
    }

    @Test fun asciiLive_refreshesOnTheQuarterHour() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val (c, _, _) = newController("wp_al", sc)
        c.apply(AsciiCollection.first().copy(minute = 12 * 60), WallpaperTarget.HOME)
        val afterApply = c.systemWallpaperSetCount
        c.refreshLiveSky(minute = 12 * 60 + 5)
        assertThat(c.systemWallpaperSetCount).isEqualTo(afterApply)
        val noon = c.current.value!!.asAndroidBitmap().let { b -> (0 until b.height).sumOf { y -> (0 until b.width).sumOf { x -> android.graphics.Color.red(b.getPixel(x, y)) } } }
        c.refreshLiveSky(minute = 23 * 60)
        assertThat(c.systemWallpaperSetCount).isEqualTo(afterApply + 1)
        val night = c.current.value!!.asAndroidBitmap().let { b -> (0 until b.height).sumOf { y -> (0 until b.width).sumOf { x -> android.graphics.Color.red(b.getPixel(x, y)) } } }
        assertThat(night).isNotEqualTo(noon)
        sc.cancel()
    }

    @Test fun asciiStill_neverRefreshes() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val (c, _, _) = newController("wp_as", sc)
        c.apply(AsciiCollection.first { it.id == "ascii_city" }, WallpaperTarget.HOME)
        val n = c.systemWallpaperSetCount
        c.refreshLiveSky(minute = 23 * 60)
        assertThat(c.systemWallpaperSetCount).isEqualTo(n)
        sc.cancel()
    }

    @Test fun lockOnly_doesNotOverwriteHome() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val (c, settings, dir) = newController("wp_lo", sc)
        c.apply(XeCollection.byId("xe_blue")!!, WallpaperTarget.HOME)
        val home = File(dir, "current.png").readBytes()
        c.apply(AsciiCollection.first().copy(minute = 12 * 60), WallpaperTarget.LOCK)
        assertThat(settings.current().liveOnHome).isFalse()
        c.refreshLiveSky(minute = 23 * 60)
        assertThat(File(dir, "current.png").readBytes()).isEqualTo(home)
        sc.cancel()
    }

    @Test fun liveSkyLockOnly_sameFix() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val (c, _, dir) = newController("wp_slo", sc)
        c.apply(XeCollection.byId("xe_blue")!!, WallpaperTarget.HOME)
        val home = File(dir, "current.png").readBytes()
        c.apply(WallpaperPreset.Sky(minute = 12 * 60), WallpaperTarget.LOCK)
        c.refreshLiveSky(minute = 23 * 60)
        assertThat(File(dir, "current.png").readBytes()).isEqualTo(home)
        sc.cancel()
    }

    @Test fun reapplyAscii_usesNewColour() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "wp_ra-${System.nanoTime()}")
        val c = WallpaperController(ctx, settings, File(ctx.filesDir, "wp-${System.nanoTime()}"), { 360 to 780 })
        c.apply(AsciiCollection.first { it.id == "ascii_lake" }, WallpaperTarget.HOME)
        fun bluish() = c.current.value!!.asAndroidBitmap().let { b -> (0 until b.height step 2).sumOf { y -> (0 until b.width step 2).count { x -> val p = b.getPixel(x, y); android.graphics.Color.blue(p) > android.graphics.Color.red(p) + 30 } } }
        val before = bluish()
        settings.update { it.copy(asciiColor = com.xenoorigin.xelauncher.settings.AsciiColor.ACCENT, accent = 0xFF2F8BFF) }
        c.reapplyAscii()
        assertThat(bluish()).isGreaterThan(before + 200)
        sc.cancel()
    }

    @Test fun mirrorDecision() {
        assertThat(shouldMirrorHome(true, false, true)).isTrue()
        assertThat(shouldMirrorHome(true, false, false)).isFalse()
        assertThat(shouldMirrorHome(true, true, true)).isFalse()
    }
    @Test fun lockOnlyStill_keepsTheHomeAsciiWallpaper() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val (c, settings, dir) = newController("wp_keep", sc)
        c.apply(AsciiCollection.first().copy(minute = 12 * 60), WallpaperTarget.HOME)
        c.apply(XeCollection.byId("xe_blue")!!, WallpaperTarget.LOCK)
        assertThat(settings.current().wallpaperId).isEqualTo(ASCII_LIVE_ID)
        assertThat(settings.current().liveOnHome).isTrue()
        assertThat(settings.current().liveSkyLock).isFalse()
        val pic = File(dir, "imported_9.jpg").also { dir.mkdirs() }
        android.graphics.Bitmap.createBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888).let { b -> pic.outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) } }
        c.applyImage(pic, WallpaperAdjust(), WallpaperTarget.LOCK)
        assertThat(settings.current().wallpaperId).isEqualTo(ASCII_LIVE_ID)
        sc.cancel()
    }
}
