package com.xenoorigin.xelauncher.design

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.xenoorigin.xelauncher.R

private var cachedSource: String? = null

/** Lens-like edge refraction (API 33+). No-op when [strength] is 0 or unsupported. */
fun Modifier.refraction(shape: Shape, strength: Float): Modifier =
    if (strength <= 0f || Build.VERSION.SDK_INT < 33) this else composed {
        val ctx = LocalContext.current
        val density = LocalDensity.current
        val src = remember {
            cachedSource ?: ctx.resources.openRawResource(R.raw.glass).bufferedReader().use { it.readText() }
                .also { cachedSource = it }
        }
        val shader = remember(src) { createShader(src) } ?: return@composed this
        graphicsLayer {
            val corner = cornerRadiusPx(shape, size, density)
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", corner)
            shader.setFloatUniform("strength", strength * density.density)
            shader.setFloatUniform("edge", 18f * density.density)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            clip = false
        }
    }

@RequiresApi(33)
private fun createShader(src: String): RuntimeShader? =
    try { RuntimeShader(src) } catch (e: IllegalArgumentException) { null } // shader compiler rejected it: no refraction

private fun cornerRadiusPx(shape: Shape, size: Size, density: androidx.compose.ui.unit.Density): Float {
    val o = shape.createOutline(size, LayoutDirection.Ltr, density)
    return when (o) {
        is androidx.compose.ui.graphics.Outline.Rounded -> o.roundRect.topLeftCornerRadius.x
        is androidx.compose.ui.graphics.Outline.Rectangle -> 0f
        else -> minOf(size.width, size.height) / 2f
    }
}
