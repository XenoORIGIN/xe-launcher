package com.xenoorigin.xestudio.core

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** The outline of a tile [sizePx] wide, starting at (0,0). */
fun tilePath(shape: TileShape, sizePx: Float): Path {
    val s = sizePx
    val c = s / 2f
    return Path().apply {
        when (shape) {
            TileShape.CIRCLE -> addCircle(c, c, c, Path.Direction.CW)
            TileShape.SQUARE -> addRect(0f, 0f, s, s, Path.Direction.CW)
            TileShape.ROUNDED -> addRoundRect(RectF(0f, 0f, s, s), s * .22f, s * .22f, Path.Direction.CW)
            TileShape.SQUIRCLE -> polar(this, c) { t ->
                // Superellipse |x|^n + |y|^n = 1 with n = 5: softer than a rounded square, like a phone icon.
                val ct = cos(t); val st = sin(t)
                Pair(sign(ct) * abs(ct).pow(2.0 / 5.0), sign(st) * abs(st).pow(2.0 / 5.0))
            }
            TileShape.HEXAGON -> {
                for (i in 0 until 6) {
                    val a = PI / 6 + i * PI / 3
                    val x = c + c * cos(a).toFloat(); val y = c + c * sin(a).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            TileShape.FLOWER -> polar(this, c) { t ->
                val r = 0.86 + 0.14 * cos(6 * t)
                Pair(r * cos(t), r * sin(t))
            }
            TileShape.TEARDROP -> {
                // Round everywhere except a sharp top-left corner.
                addRoundRect(RectF(0f, 0f, s, s), floatArrayOf(s * .08f, s * .08f, c, c, c, c, c, c), Path.Direction.CW)
            }
        }
    }
}

private inline fun polar(p: Path, c: Float, f: (Double) -> Pair<Double, Double>) {
    val steps = 120
    for (i in 0..steps) {
        val (x, y) = f(2 * PI * i / steps)
        val px = c + c * x.toFloat(); val py = c + c * y.toFloat()
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    p.close()
}
