package com.xenoorigin.xelauncher.icons

import kotlinx.serialization.Serializable

/**
 * GLASS_DARK: the app's symbol, black-and-white, on a dark glass tile (default).
 * MONO: the whole icon in black-and-white, filling the shape.
 */
@Serializable enum class IconStyle { GLASS_DARK, MONO, GLASS_COLOR, GLASS_CLEAR, ORIGINAL }

@Serializable enum class IconShape { SQUIRCLE, CIRCLE, ROUNDED, SQUARE, TEARDROP }
