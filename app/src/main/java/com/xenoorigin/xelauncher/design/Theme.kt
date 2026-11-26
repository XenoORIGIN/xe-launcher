package com.xenoorigin.xelauncher.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalXeScheme = staticCompositionLocalOf { xeScheme(true, XeColors.White) }

@Composable
fun XeTheme(
    dark: Boolean = true,
    accent: Color = XeColors.White,
    glassQuality: GlassQuality = GlassQuality.NORMAL,
    animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    pressSquish: Boolean = true,
    fontScale: Float = 1f,
    rimTint: Color = Color.Transparent,
    uiStyle: UiStyle = UiStyle.GLASS,
    content: @Composable () -> Unit,
) {
    val scheme = remember(dark, accent) { xeScheme(dark, accent) }
    val glass = remember(glassQuality, dark, rimTint, uiStyle) { glassStyleFor(glassQuality, dark, ui = uiStyle).copy(rimTint = rimTint) }
    val d = LocalDensity.current
    val m3 = if (dark) darkColorScheme(primary = scheme.accent, background = Color.Black, surface = Color(0xFF111114))
    else lightColorScheme(primary = scheme.accent)
    MaterialTheme(colorScheme = m3) {
        CompositionLocalProvider(
            LocalXeScheme provides scheme,
            LocalGlassStyle provides glass,
            LocalUiStyle provides uiStyle,
            LocalAnimationSpeed provides animationSpeed,
            LocalPressSquish provides pressSquish,
            LocalDensity provides Density(d.density, d.fontScale * fontScale),
            content = content,
        )
    }
}
