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
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@kotlinx.serialization.Serializable enum class GlassQuality { HIGH, NORMAL, SAVER }

/** The look of Xe's panels: clear Liquid Glass, or flat black-and-white Nothing style with dot-matrix type. */
@kotlinx.serialization.Serializable enum class UiStyle { GLASS, NOTHING }

/** Which look is on; Nothing style swaps big Latin titles and the clock for dot-matrix type. */
val LocalUiStyle = androidx.compose.runtime.compositionLocalOf { UiStyle.GLASS }

/** The Nothing-style red, used sparingly: one dot or one active mark per screen. */
val NothingRed = Color(0xFFD71921)

/** Everything that defines how a Liquid Glass panel is rendered. */
data class GlassStyle(
    val blurRadius: Dp,
    val tint: Color,
    val rimAlpha: Float,
    val refraction: Float,
    val noiseAlpha: Float,
    val dark: Boolean,
    /** Time-of-day colour the rims lean toward; transparent leaves them white. */
    val rimTint: Color = Color.Transparent,
    /** Nothing style: an opaque flat panel with a hairline edge instead of glass. */
    val flat: Boolean = false,
    /** How far the colours split at the bending edge (0 = none). */
    val chroma: Float = 0f,
    /** How much the glass magnifies what's behind it, like a thick convex lens (0..~0.3). */
    val magnify: Float = 0f,
)

fun glassStyleFor(q: GlassQuality, dark: Boolean, sdk: Int = Build.VERSION.SDK_INT, ui: UiStyle = UiStyle.GLASS): GlassStyle {
    if (ui == UiStyle.NOTHING) return GlassStyle(
        blurRadius = 0.dp, tint = if (dark) Color(0xF2111111) else Color(0xF5F4F4F4), rimAlpha = 0f,
        refraction = 0f, noiseAlpha = 0f, dark = dark, flat = true,
    )
    val canBlur = sdk >= 31
    val canRefract = sdk >= 33
    // 2.0: clear glass, as in the owner's reference — a light frost so the bending edge shows what's behind.
    val blur = when {
        !canBlur -> 0.dp
        q == GlassQuality.HIGH -> 6.dp
        q == GlassQuality.NORMAL -> 10.dp
        else -> 0.dp
    }
    val baseTint = if (dark) Color.Black.copy(alpha = .16f) else Color.White.copy(alpha = .16f)
    // Without blur the panel needs a denser film to stay legible.
    val tint = if (blur == 0.dp) baseTint.copy(alpha = if (dark) .62f else .55f) else baseTint
    return GlassStyle(
        blurRadius = blur,
        tint = tint,
        rimAlpha = if (q == GlassQuality.SAVER) .45f else .8f,
        refraction = when { !canRefract || q == GlassQuality.SAVER -> 0f; q == GlassQuality.HIGH -> 24f; else -> 20f },
        chroma = if (canRefract && q != GlassQuality.SAVER) .7f else 0f,
        magnify = if (canRefract && q != GlassQuality.SAVER) .2f else 0f,
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
    if (haze != null && style.blurRadius > 0.dp && !style.flat) {
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

private val RimStops = floatArrayOf(0f, .12f, .30f, .50f, .62f, .70f, .78f, 1f)
private val RimColors = listOf(Color.White, Color(0xFFA0FFF0), Color.White, Color.White, Color(0xFFAAC8FF), Color.White, Color(0xFFFFAAE6), Color.White)
private val RimAlphas = floatArrayOf(.10f, .30f, .55f, .10f, .35f, .95f, .30f, .10f)

/** The bright rim, turned by [degrees] around the panel centre (sweep gradients can't rotate on their own). */
internal fun rimColors(tint: Color): List<Color> =
    if (tint.alpha <= 0f) RimColors
    else RimColors.map { androidx.compose.ui.graphics.lerp(it, tint.copy(alpha = 1f), (tint.alpha * 2f).coerceAtMost(.7f)) }

private class RimBrush(private val rimAlpha: Float, private val degrees: Float, private val tint: Color) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val base = rimColors(tint)
        val colors = IntArray(base.size) { i -> base[i].copy(alpha = RimAlphas[i] * rimAlpha).toArgb() }
        return android.graphics.SweepGradient(size.width / 2f, size.height / 2f, colors, RimStops).apply {
            if (degrees != 0f) setLocalMatrix(android.graphics.Matrix().apply { setRotate(degrees, size.width / 2f, size.height / 2f) })
        }
    }
}

/** Rim rotation for the current tilt, in 3° steps so rims are only rebuilt when the change shows. */
internal fun rimDegrees(tilt: Offset): Int = ((tilt.x * 70f + tilt.y * 20f) / 3f).roundToInt() * 3

/** Draws the film (when not blurred), inner glows and the bright rim. Light on the rim follows [GlassTilt]. */
fun Modifier.glassFilmAndRim(shape: Shape, style: GlassStyle, drawFilm: Boolean = false): Modifier =
    if (style.flat) flatPanel(shape, style) else drawWithCache {
        val outline: Outline = shape.createOutline(size, layoutDirection, this)
        val stroke = 1.2.dp.toPx()
        val rims = HashMap<Int, Brush>()
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
        val lip = Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (style.dark) .20f else .30f), .18f to Color.White.copy(alpha = .04f),
            .82f to Color.Transparent, 1f to Color.White.copy(alpha = if (style.dark) .10f else .16f),
        )
        val fringe = .7.dp.toPx()
        onDrawWithContent {
            val tilt = GlassTilt.value.value
            val deg = rimDegrees(tilt)
            val rim = rims.getOrPut(deg) { RimBrush(style.rimAlpha, deg.toFloat(), style.rimTint) }
            if (drawFilm) drawOutline(outline, style.tint)
            drawOutline(outline, sheen)
            drawOutline(outline, topGlow)
            if (tilt != Offset.Zero) {
                // A soft highlight that slides across the glass as the phone tilts.
                drawOutline(
                    outline,
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = if (style.dark) .13f else .2f), Color.Transparent),
                        center = Offset(size.width * (.5f + .45f * tilt.x), size.height * (.0f + .35f * tilt.y)),
                        radius = maxOf(size.width, size.height) * .7f,
                    ),
                )
            }
            drawContent()
            // Thickness: a soft inner band just inside the rim, brighter at the top edge, like the lip of real glass.
            drawOutline(outline, lip, style = Stroke(width = stroke * 4.5f), alpha = style.rimAlpha)
            // Faint coloured fringes either side of the rim (light splitting at the edge).
            translate(-fringe, -fringe) { drawOutline(outline, Color(0xFF7FE8FF).copy(alpha = .16f * style.rimAlpha), style = Stroke(width = stroke)) }
            translate(fringe, fringe) { drawOutline(outline, Color(0xFFFF8AD8).copy(alpha = .14f * style.rimAlpha), style = Stroke(width = stroke)) }
            drawOutline(outline, rim, style = Stroke(width = stroke))
        }
    }

/** Nothing-style panel: opaque, flat, a hairline edge and nothing else. */
private fun Modifier.flatPanel(shape: Shape, style: GlassStyle): Modifier = drawWithCache {
    val outline: Outline = shape.createOutline(size, layoutDirection, this)
    val edge = (if (style.dark) Color.White.copy(alpha = .13f) else Color.Black.copy(alpha = .10f))
    val hair = 1.dp.toPx()
    onDrawWithContent {
        drawOutline(outline, style.tint)
        drawContent()
        drawOutline(outline, edge, style = Stroke(width = hair))
    }
}

/**
 * Cheap glass for things drawn many times at once (icon tiles, list rows): a tinted film, sheen and rim,
 * with no live blur and no refraction, so a screen full of tiles stays smooth.
 */
fun Modifier.glassTile(shape: Shape, style: GlassStyle, fill: Color): Modifier =
    glassFilmAndRim(shape, style.copy(tint = fill), drawFilm = true)

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
                .refraction(shape, if (style.flat) 0f else style.refraction, style.chroma, style.magnify)
                .liquidGlass(shape, style, haze),
        )
        content()
    }
}
