package com.xenoorigin.xelauncher.theme

import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.settings.XeSettings

/** A ready-made look: one wallpaper with an icon style and shape. Same set as the website. */
data class ThemePreset(
    val id: String,
    val name: String,
    val wallpaperId: String,
    val style: IconStyle,
    val shape: IconShape,
    val light: Boolean = false,
    val secret: Boolean = false,
    /** The launcher's look that comes with the theme (2.0). */
    val ui: com.xenoorigin.xelauncher.design.UiStyle = com.xenoorigin.xelauncher.design.UiStyle.GLASS,
)

val XeThemes: List<ThemePreset> = listOf(
    ThemePreset("xe_black", "Xe Black", "xe_black", IconStyle.GLASS_DARK, IconShape.SQUIRCLE),
    ThemePreset("nothing", "Nothing", "xe_black", IconStyle.MONO, IconShape.CIRCLE, ui = com.xenoorigin.xelauncher.design.UiStyle.NOTHING),
    ThemePreset("garden_glass", "Garden Glass", "bokeh_garden", IconStyle.GLASS_CLEAR, IconShape.SQUIRCLE),
    ThemePreset("blossom_glass", "Blossom Glass", "bokeh_blossom", IconStyle.GLASS_CLEAR, IconShape.CIRCLE),
    ThemePreset("city_night", "City Night", "bokeh_night", IconStyle.GLASS_CLEAR, IconShape.ROUNDED),
    ThemePreset("xe_blue", "Xe Blue", "xe_blue", IconStyle.GLASS_CLEAR, IconShape.SQUIRCLE),
    ThemePreset("aurora", "Aurora", "aurora", IconStyle.GLASS_COLOR, IconShape.CIRCLE),
    ThemePreset("sunset", "Sunset", "sunset", IconStyle.GLASS_COLOR, IconShape.ROUNDED),
    ThemePreset("mono_wave", "Mono Wave", "mono_wave", IconStyle.MONO, IconShape.SQUARE),
    ThemePreset("ocean", "Ocean", "ocean", IconStyle.GLASS_CLEAR, IconShape.TEARDROP),
    ThemePreset("violet_glass", "Violet Glass", "violet_glass", IconStyle.GLASS_CLEAR, IconShape.SQUIRCLE),
    ThemePreset("ember", "Ember", "ember", IconStyle.GLASS_DARK, IconShape.CIRCLE),
    ThemePreset("rose", "Rose", "rose", IconStyle.ORIGINAL, IconShape.CIRCLE, light = true),
    ThemePreset("mint", "Mint", "mint", IconStyle.ORIGINAL, IconShape.ROUNDED, light = true),
    ThemePreset("liquid_azure", "Liquid Azure", "liquid_azure", IconStyle.GLASS_CLEAR, IconShape.SQUIRCLE),
    ThemePreset("frost_aurora", "Frost Aurora", "frost_aurora", IconStyle.GLASS_COLOR, IconShape.CIRCLE),
    ThemePreset("chrome_night", "Chrome Night", "chrome_night", IconStyle.MONO, IconShape.SQUIRCLE),
)

val Supernova = ThemePreset("nova", "Supernova", "supernova", IconStyle.GLASS_COLOR, IconShape.TEARDROP, secret = true)

/** Themes to list: the secret one only after the easter egg was found. */
fun themesFor(s: XeSettings): List<ThemePreset> = if (s.novaUnlocked) XeThemes + Supernova else XeThemes

fun ThemePreset.applyTo(s: XeSettings): XeSettings = s.copy(iconStyle = style, iconShape = shape, wallpaperId = wallpaperId, uiStyle = ui)

/** The theme the settings currently match exactly, or null when the look was customised. */
fun currentTheme(s: XeSettings, themes: List<ThemePreset>): ThemePreset? =
    themes.firstOrNull { it.wallpaperId == s.wallpaperId && it.style == s.iconStyle && it.shape == s.iconShape && it.ui == s.uiStyle }
