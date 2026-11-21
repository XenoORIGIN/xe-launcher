package com.xenoorigin.xelauncher.icons

import kotlinx.serialization.Serializable

/**
 * GLASS_DARK: the app's symbol, black-and-white, on a dark glass tile (default).
 * MONO: the whole icon in black-and-white, filling the shape.
 * STUDIO: the icon theme applied in Xe Studio (falls back to GLASS_DARK when there is none).
 */
@Serializable enum class IconStyle { GLASS_DARK, MONO, GLASS_COLOR, GLASS_CLEAR, ORIGINAL, STUDIO }

@Serializable enum class IconShape { SQUIRCLE, CIRCLE, ROUNDED, SQUARE, TEARDROP }
