package com.xenoorigin.xelauncher.design

import androidx.compose.ui.graphics.Color

object XeColors {
    val Black = Color(0xFF000000)
    val Ink = Color(0xFF0B0B0D)
    val White = Color(0xFFFFFFFF)
    val TextSecondaryDark = Color(0xB3FFFFFF)
    val TextSecondaryLight = Color(0x99000000)
    val Accents = listOf(
        0xFFFFFFFF, 0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF375F, 0xFFFF9F0A, 0xFFFFD60A, 0xFF30D158, 0xFF40C8E0,
    ).map { Color(it) }
}

data class XeScheme(val dark: Boolean, val text: Color, val textSecondary: Color, val accent: Color, val background: Color)

fun xeScheme(dark: Boolean, accent: Color): XeScheme = if (dark) {
    XeScheme(true, XeColors.White, XeColors.TextSecondaryDark, accent, XeColors.Black)
} else {
    XeScheme(false, XeColors.Black, XeColors.TextSecondaryLight, if (accent == XeColors.White) XeColors.Black else accent, Color(0xFFF2F2F7))
}
