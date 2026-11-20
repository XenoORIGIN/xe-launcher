package com.xenoorigin.xelauncher.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.xenoorigin.xelauncher.icons.IconShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Continuous-curvature "squircle" (superellipse, n = 5). */
object SquircleShape : Shape {
    private val cache = object : LinkedHashMap<Size, Path>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Size, Path>?) = size > 24
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(synchronized(cache) { cache.getOrPut(size) { path(size) } })

    private fun path(size: Size): Path {
        val p = Path()
        val a = size.width / 2f; val b = size.height / 2f; val n = 5.0
        val steps = 96
        for (i in 0..steps) {
            val t = 2 * Math.PI * i / steps
            val c = cos(t); val s = sin(t)
            val x = (a * sign(c) * abs(c).pow(2 / n)).toFloat() + a
            val y = (b * sign(s) * abs(s).pow(2 / n)).toFloat() + b
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }
}

/** Rounded on three corners, nearly sharp bottom-right: a teardrop. */
val TeardropShape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 12, bottomStartPercent = 50)

fun IconShape.toShape(): Shape = when (this) {
    IconShape.SQUIRCLE -> SquircleShape
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED -> RoundedCornerShape(22)
    IconShape.SQUARE -> RoundedCornerShape(10)
    IconShape.TEARDROP -> TeardropShape
}
