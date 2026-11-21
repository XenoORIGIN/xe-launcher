package com.xenoorigin.xelauncher.theme

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.wallpaper.SecretCollection
import com.xenoorigin.xelauncher.wallpaper.XeCollection
import com.xenoorigin.xelauncher.wallpaper.allWallpapers
import com.xenoorigin.xelauncher.wallpaper.byId
import org.junit.Test

class ThemePresetsTest {
    @Test fun tenThemes_matchTheWebsite_startingWithXeBlack() {
        assertThat(XeThemes.map { it.id }).containsExactly(
            "xe_black", "xe_blue", "aurora", "sunset", "mono_wave", "ocean", "violet_glass", "ember", "rose", "mint",
        ).inOrder()
    }

    @Test fun everyThemeUsesARealWallpaper() {
        XeThemes.forEach { assertThat(XeCollection.byId(it.wallpaperId)).isNotNull() }
        assertThat(SecretCollection.byId(Supernova.wallpaperId)).isNotNull()
        assertThat(allWallpapers().byId("supernova")).isNotNull()
    }

    @Test fun applying_setsIconStyleShapeAndWallpaper() {
        val t = XeThemes.first { it.id == "aurora" }
        val s = t.applyTo(XeSettings())
        assertThat(s.iconStyle).isEqualTo(IconStyle.GLASS_COLOR)
        assertThat(s.iconShape).isEqualTo(IconShape.CIRCLE)
        assertThat(s.wallpaperId).isEqualTo("aurora")
    }

    @Test fun currentTheme_isFoundFromSettings_orNullWhenCustomised() {
        val t = XeThemes.first { it.id == "ocean" }
        assertThat(currentTheme(t.applyTo(XeSettings()), XeThemes)).isEqualTo(t)
        assertThat(currentTheme(t.applyTo(XeSettings()).copy(iconShape = IconShape.SQUARE), XeThemes)).isNull()
    }

    @Test fun secretTheme_onlyListedOnceUnlocked() {
        assertThat(themesFor(XeSettings())).doesNotContain(Supernova)
        assertThat(themesFor(XeSettings(novaUnlocked = true)).last()).isEqualTo(Supernova)
    }
}
