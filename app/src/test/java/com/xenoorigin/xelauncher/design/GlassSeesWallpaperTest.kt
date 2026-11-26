package com.xenoorigin.xelauncher.design

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.wallpaper.ASCII_LIVE_ID
import com.xenoorigin.xelauncher.wallpaper.GlassCopy
import com.xenoorigin.xelauncher.wallpaper.glassCopyFor
import org.junit.Test

/** Glass may only bend a copy of what Home really shows. */
class GlassSeesWallpaperTest {
    @Test fun stillPresetUsesTheWallpaperCopy() = assertThat(glassCopyFor(XeSettings(wallpaperId = "bokeh_garden"))).isEqualTo(GlassCopy.WALLPAPER)

    // ASCII is drawn live by Xe: the glass gets its own copy of the same scene (2.1, owner's report 26 Nov).
    @Test fun asciiLiveUsesTheSameScene() = assertThat(glassCopyFor(XeSettings(wallpaperId = ASCII_LIVE_ID))).isEqualTo(GlassCopy.ASCII_SCENE)
    @Test fun asciiStillUsesTheSameScene() = assertThat(glassCopyFor(XeSettings(wallpaperId = "ascii_lake"))).isEqualTo(GlassCopy.ASCII_SCENE)

    // The chosen wallpaper went to the lock screen only: Home shows something else.
    @Test fun nothingWhenHomeHasAnotherWallpaper() = assertThat(glassCopyFor(XeSettings(wallpaperId = "bokeh_garden", liveOnHome = false))).isEqualTo(GlassCopy.NONE)
}
