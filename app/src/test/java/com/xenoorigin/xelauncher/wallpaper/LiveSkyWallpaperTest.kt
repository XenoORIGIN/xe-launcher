package com.xenoorigin.xelauncher.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LiveSkyWallpaperTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private class FakeHandler {
        val pending = mutableListOf<Pair<Runnable, Long>>()
        fun post(r: Runnable, delay: Long) { pending += r to delay }
        fun remove(r: Runnable) { pending.removeAll { it.first === r } }
        fun runNext() { val (r, _) = pending.removeAt(0); r.run() }
    }

    @Test fun engineDoesNotTickWhileInvisible() {
        val h = FakeHandler(); var draws = 0
        val ticker = SkyTicker(h::post, h::remove) { draws++ }
        ticker.onVisibility(true)
        assertThat(draws).isEqualTo(1)
        assertThat(h.pending).hasSize(1)
        assertThat(h.pending.single().second).isEqualTo(SkyTicker.TICK_MS)
        h.runNext()
        assertThat(draws).isEqualTo(2)
        ticker.onVisibility(false)
        assertThat(h.pending).isEmpty()
        ticker.onVisibility(false)
        assertThat(draws).isEqualTo(2)
    }

    @Test fun lockUpdatesOnlyWhenBucketChanged() {
        assertThat(lockNeedsUpdate(lastWrittenBucket = -1, minute = 600)).isTrue()
        assertThat(lockNeedsUpdate(lastWrittenBucket = skyBucket(600), minute = 610)).isFalse()
        assertThat(lockNeedsUpdate(lastWrittenBucket = skyBucket(600), minute = 616)).isTrue()
    }

    @Test fun homeMirrorRunsOnlyWhenNeeded() {
        assertThat(shouldMirrorHome(liveSkyChosen = true, liveWallpaperActive = false)).isTrue()
        assertThat(shouldMirrorHome(liveSkyChosen = true, liveWallpaperActive = true)).isFalse()
        assertThat(shouldMirrorHome(liveSkyChosen = false, liveWallpaperActive = false)).isFalse()
    }

    @Test fun pickerIntentNamesOurService() {
        val i = LiveSkyWallpaper.pickerIntent(ctx)
        assertThat(i.action).isEqualTo(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        @Suppress("DEPRECATION")
        val cn = i.getParcelableExtra<ComponentName>(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT)
        assertThat(cn).isEqualTo(ComponentName(ctx, LiveSkyWallpaperService::class.java))
    }

    @Test fun serviceIsDeclaredAsALiveWallpaper() {
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getServiceInfo(ComponentName(ctx, LiveSkyWallpaperService::class.java), android.content.pm.PackageManager.GET_META_DATA)
        assertThat(info.permission).isEqualTo("android.permission.BIND_WALLPAPER")
        assertThat(info.metaData.containsKey("android.service.wallpaper")).isTrue()
    }
}
