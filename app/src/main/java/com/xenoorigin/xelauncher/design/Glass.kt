package com.xenoorigin.xelauncher.design

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@kotlinx.serialization.Serializable enum class GlassQuality { HIGH, NORMAL, SAVER }

/** Everything that defines how a Liquid Glass panel is rendered. */
data class GlassStyle(
    val blurRadius: Dp,
    val tint: Color,
    val rimAlpha: Float,
    val refraction: Float,
    val noiseAlpha: Float,
    val dark: Boolean,
)

fun glassStyleFor(q: GlassQuality, dark: Boolean, sdk: Int = Build.VERSION.SDK_INT): GlassStyle {
    val canBlur = sdk >= 31
    val canRefract = sdk >= 33
    val blur = when {
        !canBlur -> 0.dp
        q == GlassQuality.HIGH -> 24.dp
        q == GlassQuality.NORMAL -> 20.dp
        else -> 0.dp
    }
    val baseTint = if (dark) Color.Black.copy(alpha = .28f) else Color.White.copy(alpha = .18f)
    // Without blur the panel needs a denser film to stay legible.
    val tint = if (blur == 0.dp) baseTint.copy(alpha = if (dark) .62f else .55f) else baseTint
    return GlassStyle(
        blurRadius = blur,
        tint = tint,
        rimAlpha = if (q == GlassQuality.SAVER) .45f else .8f,
        refraction = if (q == GlassQuality.HIGH && canRefract) 14f else 0f,
        noiseAlpha = if (blur > 0.dp) .06f else 0f,
        dark = dark,
    )
}

val LocalGlassStyle = compositionLocalOf { glassStyleFor(GlassQuality.NORMAL, dark = true) }
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Blur + tint + specular rim + inner glow, clipped to [shape].
 * Refraction needs an isolated layer, so use [GlassSurface] when it should apply.
 */
fun Modifier.liquidGlass(shape: Shape, style: GlassStyle, haze: HazeState?): Modifier {
    var m = this.clip(shape)
    if (haze != null && style.blurRadius > 0.dp) {
        m = m.hazeEffect(
            state = haze,
            style = HazeStyle(
                backgroundColor = if (style.dark) Color.Black else Color.White,
                tints = listOf(HazeTint(style.tint)),
                blurRadius = style.blurRadius,
                noiseFactor = style.noiseAlpha,
            ),
        )
    }
    return m.glassFilmAndRim(shape, style, drawFilm = haze == null || style.blurRadius == 0.dp)
}

/** Draws the film (when not blurred), inner glows and the bright rim. */
fun Modifier.glassFilmAndRim(shape: Shape, style: GlassStyle, drawFilm: Boolean = false): Modifier =
    drawWithCache {
        val outline: Outline = shape.createOutline(size, layoutDirection, this)
        val stroke = 1.2.dp.toPx()
        val rim = Brush.sweepGradient(
            0.00f to Color.White.copy(alpha = .10f * style.rimAlpha),
            0.12f to Color(0xFFA0FFF0).copy(alpha = .30f * style.rimAlpha),
            0.30f to Color.White.copy(alpha = .55f * style.rimAlpha),
            0.50f to Color.White.copy(alpha = .10f * style.rimAlpha),
            0.62f to Color(0xFFAAC8FF).copy(alpha = .35f * style.rimAlpha),
            0.70f to Color.White.copy(alpha = .95f * style.rimAlpha),
            0.78f to Color(0xFFFFAAE6).copy(alpha = .30f * style.rimAlpha),
            1.00f to Color.White.copy(alpha = .10f * style.rimAlpha),
            center = Offset(size.width / 2f, size.height / 2f),
        )
        val sheen = Brush.linearGradient(
            0f to Color.White.copy(alpha = if (style.dark) .16f else .28f),
            .35f to Color.White.copy(alpha = .03f),
            .55f to Color.Transparent,
            1f to Color.White.copy(alpha = if (style.dark) .07f else .12f),
            start = Offset.Zero, end = Offset(size.width, size.height),
        )
        val topGlow = Brush.verticalGradient(
            0f to Color.White.copy(alpha = .22f), .35f to Color.Transparent,
            .78f to Color.Transparent, 1f to Color.White.copy(alpha = .12f),
        )
        onDrawWithContent {
            if (drawFilm) drawOutline(outline, style.tint)
            drawOutline(outline, sheen)
            drawOutline(outline, topGlow)
            drawContent()
            drawOutline(outline, rim, style = Stroke(width = stroke))
        }
    }

/**
 * A Liquid Glass panel. The glass is drawn on its own layer (so refraction never bends [content]),
 * then [content] is drawn on top.
 */
@Composable
fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    style: GlassStyle = LocalGlassStyle.current,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val haze = LocalHazeState.current
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .refraction(shape, style.refraction)
                .liquidGlass(shape, style, haze),
        )
        content()
    }
}
