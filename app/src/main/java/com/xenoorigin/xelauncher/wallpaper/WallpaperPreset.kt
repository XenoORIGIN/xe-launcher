package com.xenoorigin.xelauncher.wallpaper

import java.io.File

sealed interface WallpaperPreset {
    val id: String
    val name: String

    /** Soft blended colour fields. Colours are ARGB; the first is the base. */
    data class Mesh(override val id: String, override val name: String, val colors: List<Long>, val seed: Int, val grain: Float = .035f, val glow: Float = 1f) : WallpaperPreset

    /** Flowing bands plus Liquid Glass lenses. */
    data class Abstract(override val id: String, override val name: String, val colors: List<Long>, val seed: Int, val shapes: Int = 4, val lenses: Int = 2) : WallpaperPreset

    /** Liquid glass art: flowing ribbons, a frosted aurora or chrome waves. */
    data class Liquid(override val id: String, override val name: String, val colors: List<Long>, val seed: Int, val style: LiquidStyle) : WallpaperPreset

    /** Out-of-focus light through leaves or blossoms: lots of soft discs and sparkles for Liquid Glass to bend. */
    data class Bokeh(override val id: String, override val name: String, val colors: List<Long>, val seed: Int) : WallpaperPreset

    /** The sky right now: colours, sun or moon and stars follow the time of day ([minute] fixes a time, for previews). */
    data class Sky(override val id: String = LIVE_SKY_ID, override val name: String = "Live Sky", val minute: Int? = null) : WallpaperPreset

    /**
     * A scene drawn in Xe's own text glyphs. [live] = moves and follows the time of day ([minute] fixes a time,
     * for previews). [color] and [accent] come from the settings when applied (see [withColors]).
     */
    data class Ascii(
        override val id: String,
        override val name: String,
        val scene: com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene,
        val live: Boolean = false,
        val minute: Int? = null,
        val color: com.xenoorigin.xelauncher.settings.AsciiColor = com.xenoorigin.xelauncher.settings.AsciiColor.MONO,
        val accent: Long = 0xFFFFFFFF,
    ) : WallpaperPreset

    data class Image(val file: File) : WallpaperPreset {
        override val id: String get() = "image:${file.name}"
        override val name: String get() = file.nameWithoutExtension
    }
}

const val LIVE_SKY_ID = "live_sky"
const val ASCII_LIVE_ID = "ascii_live"

/** Wallpapers that follow the time of day: Xe keeps the phone's copy of them current. */
fun isTimeWallpaper(id: String): Boolean = id == LIVE_SKY_ID || id == ASCII_LIVE_ID

/** What Liquid Glass may bend: Xe's copy of the wallpaper, a copy of the ASCII scene Xe draws, or nothing. */
enum class GlassCopy { WALLPAPER, ASCII_SCENE, NONE }

/**
 * Which copy matches what Home shows. ASCII scenes are drawn live by Xe, so the glass gets a still of the same
 * scene; a lock-screen-only wallpaper isn't on Home, so the glass shows no copy (owner's report, 2.0.1).
 */
fun glassCopyFor(s: com.xenoorigin.xelauncher.settings.XeSettings): GlassCopy = when {
    !s.liveOnHome -> GlassCopy.NONE
    allWallpapers().byId(s.wallpaperId) is WallpaperPreset.Ascii -> GlassCopy.ASCII_SCENE
    else -> GlassCopy.WALLPAPER
}

/** The ASCII wallpaper shown on Home right now, with the chosen colours (null if Home shows something else). */
fun homeAscii(s: com.xenoorigin.xelauncher.settings.XeSettings): WallpaperPreset.Ascii? =
    (allWallpapers().byId(s.wallpaperId) as? WallpaperPreset.Ascii)?.takeIf { s.liveOnHome }?.withColors(s)

fun WallpaperPreset.Ascii.withColors(s: com.xenoorigin.xelauncher.settings.XeSettings) = copy(color = s.asciiColor, accent = s.accent)

/** ASCII wallpapers: one that follows the time, then seven still scenes. */
val AsciiCollection: List<WallpaperPreset.Ascii> = run {
    listOf(
        WallpaperPreset.Ascii(ASCII_LIVE_ID, "ASCII Live", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.LAKE, live = true),
        WallpaperPreset.Ascii("ascii_lake", "Lake", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.LAKE),
        WallpaperPreset.Ascii("ascii_rain", "Rain", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.RAIN),
        WallpaperPreset.Ascii("ascii_dawn", "Dawn", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.DAWN),
        WallpaperPreset.Ascii("ascii_pines", "Pines", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.PINES),
        WallpaperPreset.Ascii("ascii_city", "City", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.CITY),
        WallpaperPreset.Ascii("ascii_space", "Space", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.SPACE),
        WallpaperPreset.Ascii("ascii_lighthouse", "Lighthouse", com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene.LIGHTHOUSE),
    )
}

enum class LiquidStyle { RIBBONS, AURORA, CHROME }

val XeCollection: List<WallpaperPreset> = listOf(
    WallpaperPreset.Mesh("xe_black", "Xe Black", listOf(0xFF000000, 0xFF8A8A8A, 0xFF4A4A4A, 0xFF2A2A2A), seed = 11, grain = .03f, glow = .5f),
    WallpaperPreset.Sky(),
    WallpaperPreset.Bokeh("bokeh_garden", "Garden Light", listOf(0xFF12341C, 0xFF2E6A2C, 0xFFB4C85A, 0xFF78B446, 0xFFC8DC78, 0xFFFAF0AA), seed = 201),
    WallpaperPreset.Bokeh("bokeh_blossom", "Blossom", listOf(0xFF1E1222, 0xFF5A2350, 0xFFDC78AA, 0xFFF096C8, 0xFFFFC8E6, 0xFFFFF0FA), seed = 202),
    WallpaperPreset.Bokeh("bokeh_night", "City Night", listOf(0xFF060814, 0xFF10163A, 0xFF3C50B4, 0xFF5A8CFF, 0xFFFFC878, 0xFF78E6FF), seed = 203),
    WallpaperPreset.Abstract("xe_blue", "Xe Blue", listOf(0xFF05082A, 0xFF2F4BFF, 0xFF7A5CFF, 0xFF1E8BFF, 0xFFC27BFF), seed = 7),
    WallpaperPreset.Mesh("graphite", "Graphite", listOf(0xFF111114, 0xFF5E5E66, 0xFF2C2C30, 0xFF9A9AA2), seed = 23, glow = .7f),
    WallpaperPreset.Mesh("midnight", "Midnight", listOf(0xFF03041A, 0xFF1B2A8C, 0xFF3A1E7A, 0xFF0B4C8C), seed = 31),
    WallpaperPreset.Mesh("aurora", "Aurora", listOf(0xFF020A10, 0xFF0FB89A, 0xFF4C2CB8, 0xFF1FD1C8), seed = 41),
    WallpaperPreset.Mesh("sunset", "Sunset", listOf(0xFF1A0610, 0xFFFF7A3D, 0xFFE8457E, 0xFF7B3FD1), seed = 53),
    WallpaperPreset.Abstract("ocean", "Ocean", listOf(0xFF021226, 0xFF0A84FF, 0xFF40C8E0, 0xFF1A4BD1, 0xFF7FE3FF), seed = 61),
    WallpaperPreset.Mesh("rose", "Rose", listOf(0xFFFFE9EF, 0xFFFF9EB8, 0xFFFFC9A8, 0xFFE7B8FF), seed = 71, grain = .02f),
    WallpaperPreset.Abstract("mono_wave", "Mono Wave", listOf(0xFF000000, 0xFF3A3A3A, 0xFFBDBDBD, 0xFF6E6E6E, 0xFFFFFFFF), seed = 83, lenses = 3),
    WallpaperPreset.Abstract("violet_glass", "Violet Glass", listOf(0xFF0D0420, 0xFF8E5CFF, 0xFFD27BFF, 0xFF4B2CD1, 0xFFFF8AD8), seed = 97, lenses = 3),
    WallpaperPreset.Mesh("ember", "Ember", listOf(0xFF100302, 0xFFFF453A, 0xFFFF9F0A, 0xFF8A1A10), seed = 101),
    WallpaperPreset.Liquid("liquid_azure", "Liquid Azure", listOf(0xFF030B1E, 0xFF1E6BFF, 0xFF34D1FF, 0xFF8E7BFF), seed = 131, style = LiquidStyle.RIBBONS),
    WallpaperPreset.Liquid("liquid_rose", "Liquid Rose", listOf(0xFF1A0612, 0xFFFF5E8A, 0xFFFFB38A, 0xFFC46BFF), seed = 137, style = LiquidStyle.RIBBONS),
    WallpaperPreset.Liquid("frost_aurora", "Frost Aurora", listOf(0xFF020812, 0xFF19E3B5, 0xFF7A5CFF, 0xFF3FA9FF), seed = 139, style = LiquidStyle.AURORA),
    WallpaperPreset.Liquid("northern_mint", "Northern Mint", listOf(0xFF03100C, 0xFF5CFF9E, 0xFF1FD1C8, 0xFFB8FF6B), seed = 149, style = LiquidStyle.AURORA),
    WallpaperPreset.Liquid("chrome_night", "Chrome Night", listOf(0xFF06070A, 0xFFE8ECF4, 0xFF6C7488, 0xFF9AA6C2), seed = 151, style = LiquidStyle.CHROME),
    WallpaperPreset.Liquid("chrome_gold", "Chrome Gold", listOf(0xFF120A02, 0xFFFFE3A3, 0xFFB07A2A, 0xFFFFC85C), seed = 157, style = LiquidStyle.CHROME),
    WallpaperPreset.Mesh("mint", "Mint", listOf(0xFFEFFFF8, 0xFF9DF0D2, 0xFFB8E6FF, 0xFFD9FFC2), seed = 113, grain = .02f),
)

/** Wallpapers unlocked by easter eggs; not shown in the wallpaper picker. */
val SecretCollection: List<WallpaperPreset> = listOf(
    WallpaperPreset.Mesh("supernova", "Supernova", listOf(0xFF0A0014, 0xFFFF3D7F, 0xFFFFB340, 0xFF3D8BFF, 0xFFB03DFF), seed = 127, glow = 1.1f),
)

fun allWallpapers(): List<WallpaperPreset> = XeCollection + AsciiCollection + SecretCollection

fun List<WallpaperPreset>.byId(id: String): WallpaperPreset? = firstOrNull { it.id == id }
