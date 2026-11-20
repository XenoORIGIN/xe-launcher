package com.xenoorigin.xelauncher.wallpaper

import java.io.File

sealed interface WallpaperPreset {
    val id: String
    val name: String

    /** Soft blended colour fields. Colours are ARGB; the first is the base. */
    data class Mesh(override val id: String, override val name: String, val colors: List<Long>, val seed: Int, val grain: Float = .035f, val glow: Float = 1f) : WallpaperPreset

    /** Flowing bands plus Liquid Glass lenses. */
    data class Abstract(override val id: String, override val name: String, val colors: List<Long>, val seed: Int, val shapes: Int = 4, val lenses: Int = 2) : WallpaperPreset

    data class Image(val file: File) : WallpaperPreset {
        override val id: String get() = "image:${file.name}"
        override val name: String get() = file.nameWithoutExtension
    }
}

val XeCollection: List<WallpaperPreset> = listOf(
    WallpaperPreset.Mesh("xe_black", "Xe Black", listOf(0xFF000000, 0xFF8A8A8A, 0xFF4A4A4A, 0xFF2A2A2A), seed = 11, grain = .03f, glow = .5f),
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
    WallpaperPreset.Mesh("mint", "Mint", listOf(0xFFEFFFF8, 0xFF9DF0D2, 0xFFB8E6FF, 0xFFD9FFC2), seed = 113, grain = .02f),
)

fun List<WallpaperPreset>.byId(id: String): WallpaperPreset? = firstOrNull { it.id == id }
