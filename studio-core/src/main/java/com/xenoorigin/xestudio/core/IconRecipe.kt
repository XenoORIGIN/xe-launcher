package com.xenoorigin.xestudio.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The outline every icon tile is cut to. */
@Serializable enum class TileShape { CIRCLE, SQUIRCLE, ROUNDED, SQUARE, HEXAGON, FLOWER, TEARDROP }

/**
 * How the app's own symbol is coloured.
 * ORIGINAL keeps its colours, MONO makes it grey, TINT paints it one colour, DUOTONE maps dark→[IconRecipe.tint2] and light→[IconRecipe.tint].
 */
@Serializable enum class Treatment { ORIGINAL, MONO, TINT, DUOTONE }

/** One colour (solid) or several (linear gradient at [angle] degrees, 90 = top to bottom). */
@Serializable data class Fill(val colors: List<Int>, val angle: Float = 90f)

/** A hand-made replacement for one app's icon. */
@Serializable sealed interface IconOverride {
    /** A picture stored next to the recipe (file name only, no folders). */
    @Serializable @SerialName("image") data class Image(val file: String) : IconOverride
    /** A letter or symbol on its own background. */
    @Serializable @SerialName("letter") data class Letter(val text: String, val color: Int, val bg: Fill) : IconOverride
}

/**
 * An icon theme as a small recipe: how every icon is styled, plus per-app overrides keyed by
 * Xe's AppKey.encode() ("package/activity#userSerial"). Stored and shared as JSON.
 */
@Serializable data class IconRecipe(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val shape: TileShape = TileShape.SQUIRCLE,
    val background: Fill = Fill(listOf(0xFF111111.toInt())),
    val treatment: Treatment = Treatment.MONO,
    val tint: Int = 0xFFFFFFFF.toInt(),
    val tint2: Int = 0xFF888888.toInt(),
    val outlineWidth: Float = 0f,
    val outlineColor: Int = 0xFFFFFFFF.toInt(),
    val shadow: Float = 0f,
    val glyphScale: Float = 0.62f,
    val overrides: Map<String, IconOverride> = emptyMap(),
) {
    companion object {
        const val CURRENT_SCHEMA = 1
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun encode(r: IconRecipe): String = json.encodeToString(serializer(), r)

        /** Null when the text isn't a recipe, or was made by a newer Studio this app can't read yet. */
        fun decode(text: String): IconRecipe? =
            runCatching { json.decodeFromString(serializer(), text) }.getOrNull()?.takeIf { it.schemaVersion <= CURRENT_SCHEMA }
    }
}
