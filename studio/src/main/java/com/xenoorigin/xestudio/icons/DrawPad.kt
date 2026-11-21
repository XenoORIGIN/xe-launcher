package com.xenoorigin.xestudio.icons

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Strokes drawn with a finger, in 0..1 coordinates so they can be redrawn at any size. */
class Sketch {
    val strokes = mutableStateListOf<List<Offset>>()
    private var current: MutableList<Offset>? = null

    fun start(p: Offset) { current = mutableListOf(p); strokes += current!!.toList() }
    fun extend(p: Offset) {
        val c = current ?: return
        c += p
        strokes[strokes.lastIndex] = c.toList()
    }
    fun end() { current = null }
    fun clear() = strokes.clear()
    val isEmpty get() = strokes.isEmpty()

    /** Paints the sketch on a [size]-px square: [background] then [ink] strokes. */
    fun render(size: Int, background: Int, ink: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(background)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; style = android.graphics.Paint.Style.STROKE; strokeWidth = size * STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
        }
        for (s in strokes) {
            if (s.size == 1) { c.drawPoint(s[0].x * size, s[0].y * size, paint); continue }
            val p = android.graphics.Path()
            s.forEachIndexed { i, o -> if (i == 0) p.moveTo(o.x * size, o.y * size) else p.lineTo(o.x * size, o.y * size) }
            c.drawPath(p, paint)
        }
        return bmp
    }

    companion object { const val STROKE = .055f }
}

@Composable
fun DrawPad(sketch: Sketch, background: Brush, ink: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(background).testTag("draw_pad")
            .pointerInput(sketch) {
                detectDragGestures(
                    onDragStart = { sketch.start(Offset(it.x / size.width, it.y / size.height)) },
                    onDragEnd = { sketch.end() },
                    onDragCancel = { sketch.end() },
                ) { change, _ -> sketch.extend(Offset(change.position.x / size.width, change.position.y / size.height)) }
            },
    ) {
        val w = size.width
        for (s in sketch.strokes) {
            val path = Path().apply { s.forEachIndexed { i, o -> if (i == 0) moveTo(o.x * w, o.y * w) else lineTo(o.x * w, o.y * w) } }
            drawPath(path, ink, style = Stroke(width = w * Sketch.STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (s.size == 1) drawCircle(ink, w * Sketch.STROKE / 2, Offset(s[0].x * w, s[0].y * w))
        }
    }
}

