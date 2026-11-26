package com.xenoorigin.xelauncher.theme

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.UiStyle
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.wallpaper.allWallpapers
import com.xenoorigin.xelauncher.wallpaper.byId
import org.junit.Test

class Themes2Test {
    @Test fun nothingThemeSwitchesTheWholeLook() {
        val t = XeThemes.first { it.id == "nothing" }
        val s = t.applyTo(XeSettings())
        assertThat(s.uiStyle).isEqualTo(UiStyle.NOTHING)
        assertThat(currentTheme(s, XeThemes)).isEqualTo(t)
    }

    @Test fun glassThemesTurnTheGlassLookBackOn() {
        val s = XeThemes.first { it.id == "garden_glass" }.applyTo(XeSettings(uiStyle = UiStyle.NOTHING))
        assertThat(s.uiStyle).isEqualTo(UiStyle.GLASS)
        assertThat(s.wallpaperId).isEqualTo("bokeh_garden")
    }

    @Test fun everyThemeWallpaperExists() {
        XeThemes.forEach { assertThat(allWallpapers().byId(it.wallpaperId)).isNotNull() }
    }
}
