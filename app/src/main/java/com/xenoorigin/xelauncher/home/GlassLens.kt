package com.xenoorigin.xelauncher.home

import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.design.GlassTilt
import kotlin.math.roundToInt

/** A glass lens the finger can drag over the Home wallpaper (hold an empty spot, then drag). */
@Stable
class LensState(val wallpaper: () -> ImageBitmap?, val parallaxPx: () -> Offset) {
    var active by mutableStateOf(false)
        private set
    var target by mutableStateOf(Offset.Zero)
        private set

    fun begin(at: Offset) { target = at; active = true }
    fun moveTo(at: Offset) { target = at }
    fun end() { active = false }
}

val LocalLens = staticCompositionLocalOf<LensState?> { null }

/** Where the wallpaper bitmap sits on screen: drawn cropped to fill, scaled [WALL_SCALE], shifted by parallax. */
internal data class WallMapping(val scale: Float, val offset: Offset)

internal fun wallMapping(bmp: IntSize, screen: Size, parallaxPx: Offset): WallMapping {
    val k = maxOf(screen.width / bmp.width, screen.height / bmp.height) * com.xenoorigin.xelauncher.wallpaper.WALL_SCALE
    return WallMapping(k, Offset(screen.width / 2f - bmp.width * k / 2f + parallaxPx.x, screen.height / 2f - bmp.height * k / 2f + parallaxPx.y))
}

// Bends the wallpaper like a drop of glass: slight zoom in the middle, strong bend at the edge.
private const val LENS_AGSL = """
uniform shader img;
uniform float2 c;
uniform float r;
uniform float k;
uniform float2 o;
half4 main(float2 p) {
    float2 d = p - c;
    float rr = length(d) / r;
    if (rr > 1.0) return half4(0);
    float s = 0.26 * (1.0 - rr * rr) + 0.72 * smoothstep(0.6, 1.0, rr);
    float2 q = p - d * s * 0.45;
    half4 col = img.eval((q - o) / k);
    col.rgb = col.rgb * 1.06 + 0.025;
    return col * smoothstep(1.0, 0.975, rr);
}
"""

@Composable
fun GlassLens(lens: LensState, modifier: Modifier = Modifier) {
    val appear by animateFloatAsState(if (lens.active) 1f else 0f, spring(dampingRatio = .55f, stiffness = 320f), label = "lens")
    val pos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    LaunchedEffect(lens) {
        var was = false
        snapshotFlow { lens.target to lens.active }.collect { (t, a) ->
            // Appear right under the finger, then trail it with a little liquid lag.
            if (a && !was) pos.snapTo(t) else if (a) pos.animateTo(t, spring(dampingRatio = .72f, stiffness = 700f))
            was = a
        }
    }
    if (appear < .005f && !lens.active) return
    // If a phone cannot compile the shader, fall back to the plain magnifier rather than crash.
    val shader = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { LensShader() }.getOrNull() else null }
    Canvas(modifier.fillMaxSize()) {
        val v = pos.velocity
        val stretch = (v.getDistance() / 4000f).coerceAtMost(.22f)
        val r = 58.dp.toPx() * (.55f + .45f * appear) * (1f + stretch * .5f)
        val c = pos.value
        val alpha = appear.coerceIn(0f, 1f)
        // Shadow under the drop.
        drawCircle(Brush.radialGradient(listOf(Color.Black.copy(alpha = .32f * alpha), Color.Transparent), center = c + Offset(0f, r * .22f), radius = r * 1.35f), r * 1.35f, c + Offset(0f, r * .22f))
        val bmp = lens.wallpaper()
        if (bmp != null) {
            val m = wallMapping(IntSize(bmp.width, bmp.height), size, lens.parallaxPx())
            if (shader != null && Build.VERSION.SDK_INT >= 33) shader.draw(this, bmp, c, r, m, alpha) else magnify(bmp, c, r, m, alpha)
        } else {
            drawCircle(Color.White.copy(alpha = .12f * alpha), r, c)
        }
        lensRim(c, r, alpha)
    }
}

@RequiresApi(33)
private class LensShader {
    private val shader = RuntimeShader(LENS_AGSL)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bound: ImageBitmap? = null

    fun draw(scope: DrawScope, bmp: ImageBitmap, c: Offset, r: Float, m: WallMapping, alpha: Float) {
        if (bound !== bmp) {
            val a = bmp.asAndroidBitmap()
            shader.setInputShader("img", BitmapShader(a, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP))
            bound = bmp
        }
        shader.setFloatUniform("c", c.x, c.y)
        shader.setFloatUniform("r", r)
        shader.setFloatUniform("k", m.scale)
        shader.setFloatUniform("o", m.offset.x, m.offset.y)
        paint.shader = shader
        paint.alpha = (alpha * 255).roundToInt()
        scope.drawIntoCanvas { it.nativeCanvas.drawCircle(c.x, c.y, r, paint) }
    }
}

/** Older Android: a plain magnifier, no bending. */
private fun DrawScope.magnify(bmp: ImageBitmap, c: Offset, r: Float, m: WallMapping, alpha: Float) {
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    clipPath(circle) {
        withTransform({ scale(1.22f, 1.22f, c) }) {
            drawImage(
                bmp, dstOffset = IntOffset(m.offset.x.roundToInt(), m.offset.y.roundToInt()),
                dstSize = IntSize((bmp.width * m.scale).roundToInt(), (bmp.height * m.scale).roundToInt()), alpha = alpha,
            )
        }
    }
}

private fun DrawScope.lensRim(c: Offset, r: Float, alpha: Float) {
    val tilt = GlassTilt.value.value
    // Bright top-left highlight and a softer bottom glow, nudged by tilt.
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = .38f * alpha), Color.Transparent), center = c + Offset(r * (-.28f + .3f * tilt.x), r * (-.42f + .25f * tilt.y)), radius = r * .62f),
        r, c,
    )
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.White.copy(alpha = .18f * alpha)), center = c, radius = r), r, c)
    drawCircle(
        Brush.sweepGradient(
            listOf(Color.White.copy(alpha = .9f * alpha), Color(0xFFAAC8FF).copy(alpha = .35f * alpha), Color.White.copy(alpha = .15f * alpha), Color(0xFFFFAAE6).copy(alpha = .35f * alpha), Color.White.copy(alpha = .9f * alpha)),
            center = c,
        ),
        r, c, style = Stroke(width = 1.4.dp.toPx()),
    )
}
