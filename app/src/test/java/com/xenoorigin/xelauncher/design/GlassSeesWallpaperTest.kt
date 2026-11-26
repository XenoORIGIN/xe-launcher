package com.xenoorigin.xelauncher.design

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.wallpaper.ASCII_LIVE_ID
import com.xenoorigin.xelauncher.wallpaper.glassSeesWallpaper
import org.junit.Test

/** 2.0.2: glass may only show a copy of the wallpaper when that copy really is what's on Home. */
class GlassSeesWallpaperTest {
    @Test fun stillPresetOnHome() = assertThat(glassSeesWallpaper(XeSettings(wallpaperId = "bokeh_garden"))).isTrue()

    // ASCII scenes are drawn live (moving) by Xe, so a still copy never matches: owner's report 26 Nov.
    @Test fun notOverAsciiLive() = assertThat(glassSeesWallpaper(XeSettings(wallpaperId = ASCII_LIVE_ID))).isFalse()
    @Test fun notOverAsciiStill() = assertThat(glassSeesWallpaper(XeSettings(wallpaperId = "ascii_lake"))).isFalse()

    // The chosen wallpaper went to the lock screen only: Home shows something else.
    @Test fun notWhenHomeHasAnotherWallpaper() = assertThat(glassSeesWallpaper(XeSettings(wallpaperId = "bokeh_garden", liveOnHome = false))).isFalse()
}
