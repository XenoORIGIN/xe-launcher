package com.xenoorigin.xelauncher.design

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.settings.IconGlass
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * How far Home and the drawer have scrolled (px) and how fast (px/s). Written by the pagers, read only
 * while drawing, so icons can react to scrolling without recomposing.
 */
object ScrollFlow {
    val offset = mutableFloatStateOf(0f)
    val velocity = mutableFloatStateOf(0f)
    private var lastOffset = 0f
    private var lastNanos = 0L

    /** Feed the current scroll position; velocity is derived and smoothed. */
    fun update(offsetPx: Float, nanos: Long = System.nanoTime()) {
        if (lastNanos != 0L) {
            val dt = ((nanos - lastNanos) / 1e9f).coerceAtLeast(1e-3f)
            val v = (offsetPx - lastOffset) / dt
            velocity.floatValue = velocity.floatValue * .6f + v * .4f
        }
        lastOffset = offsetPx; lastNanos = nanos
        offset.floatValue = offsetPx
    }

    /** Called when scrolling settles. */
    fun settle() { velocity.floatValue = 0f; lastNanos = 0L }
}

val LocalIconGlass = staticCompositionLocalOf { IconGlass.LIGHT }

/** Where the highlight sits across an icon, -1 (left edge) … 1 (right edge), for a scroll and a tilt. */
fun highlightShift(offsetPx: Float, tiltX: Float, widthPx: Float): Float {
    val phase = offsetPx / widthPx.coerceAtLeast(1f)
    return (sin(phase * PI.toFloat()) * .7f + tiltX * .35f).coerceIn(-1f, 1f)
}

const val LENS_REST = 2f
const val LENS_MAX = 11f

/** Lens bend in dp: a little at rest, more the faster the page moves. */
fun lensStrength(speedPxPerS: Float): Float = (LENS_REST + abs(speedPxPerS) / 600f).coerceAtMost(LENS_MAX)

/** How bright the sliding highlight is for each mode. */
fun sheenAlpha(mode: IconGlass): Float = when (mode) { IconGlass.OFF -> 0f; IconGlass.LIGHT -> .22f; IconGlass.LENS -> .16f }

/** A diagonal band of light across the icon that slides with scrolling and tilt. Draw-only reads. */
fun Modifier.iconSheen(shape: Shape, mode: IconGlass): Modifier {
    val a = sheenAlpha(mode)
    if (a <= 0f) return this
    return drawWithContent {
        drawContent()
        val s = highlightShift(ScrollFlow.offset.floatValue, GlassTilt.value.value.x, 1080f)
        val cx = size.width * (.5f + .55f * s)
        val band = size.width * .42f
        val brush = Brush.linearGradient(
            0f to Color.Transparent, .5f to Color.White.copy(alpha = a), 1f to Color.Transparent,
            start = Offset(cx - band, 0f), end = Offset(cx + band * .4f, size.height),
        )
        drawOutline(shape.createOutline(size, layoutDirection, this), brush)
    }
}

private var lensSource: String? = null

/**
 * Bends the icon like a glass lens; [strength] (dp) is read while drawing so it can follow scroll speed.
 * Android 13+ only; on older phones, or if the shader can't compile, it returns the modifier unchanged.
 */
fun Modifier.iconLens(shape: Shape, strength: () -> Float): Modifier =
    if (Build.VERSION.SDK_INT < 33) this else composed {
        val ctx = LocalContext.current
        val shader = remember {
            val src = lensSource ?: ctx.resources.openRawResource(R.raw.glass).bufferedReader().use { it.readText() }.also { lensSource = it }
            runCatching { RuntimeShader(src) }.getOrNull()
        } ?: return@composed this
        graphicsLayer {
            val outline = shape.createOutline(size, androidx.compose.ui.unit.LayoutDirection.Ltr, this)
            val r = (outline as? androidx.compose.ui.graphics.Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: (minOf(size.width, size.height) / 2f)
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", r)
            shader.setFloatUniform("strength", strength() * density)
            shader.setFloatUniform("edge", size.minDimension * .28f)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
    }
