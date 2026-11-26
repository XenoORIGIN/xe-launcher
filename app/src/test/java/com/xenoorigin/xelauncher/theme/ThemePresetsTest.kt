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
    @Test fun seventeenThemes_matchTheWebsite_startingWithXeBlack() {
        assertThat(XeThemes.map { it.id }).containsExactly(
            "xe_black", "nothing", "garden_glass", "blossom_glass", "city_night", "xe_blue", "aurora", "sunset", "mono_wave", "ocean", "violet_glass", "ember", "rose", "mint",
            "liquid_azure", "frost_aurora", "chrome_night",
        ).inOrder()
        // The website lists the same themes, in the same order.
        val site = java.io.File("../website/play.js").readText()
        val ids = Regex("""\{ id: "([a-z_]+)", name""").findAll(site.substringAfter("const THEMES").substringBefore("];")).map { it.groupValues[1] }.toList()
        assertThat(ids).containsExactlyElementsIn(XeThemes.map { it.id }).inOrder()
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

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@org.robolectric.annotation.Config(sdk = [34])
class ThemeWallpaperExport {
    /** Writes each theme's wallpaper for the website (copied to website/assets/wall as WebP by the release step). */
    @org.junit.Test fun exportThemeWallpapersForTheWebsite() {
        val dir = java.io.File("build/site_walls").apply { mkdirs() }
        for (t in XeThemes) {
            val b = com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer.render(XeCollection.byId(t.wallpaperId)!!, 390, 845)
            java.io.File(dir, "${t.id}.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
