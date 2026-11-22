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
}
