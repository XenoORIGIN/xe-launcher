package com.xenoorigin.xelauncher.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Xe's dot-matrix type for Nothing style: 5 × 7 dots per character, drawn as round dots so every phone
 * shows the same thing. Covers digits, Latin letters (upper case) and a few marks; anything else can't be dotted.
 */
object DotFont {
    const val W = 5
    const val H = 7

    private val rows: Map<Char, String> = mapOf(
        '0' to "01110 10001 10011 10101 11001 10001 01110", '1' to "00100 01100 00100 00100 00100 00100 01110",
        '2' to "01110 10001 00001 00110 01000 10000 11111", '3' to "11110 00001 00001 01110 00001 00001 11110",
        '4' to "00010 00110 01010 10010 11111 00010 00010", '5' to "11111 10000 11110 00001 00001 10001 01110",
        '6' to "00110 01000 10000 11110 10001 10001 01110", '7' to "11111 00001 00010 00100 01000 01000 01000",
        '8' to "01110 10001 10001 01110 10001 10001 01110", '9' to "01110 10001 10001 01111 00001 00010 01100",
        'A' to "01110 10001 10001 11111 10001 10001 10001", 'B' to "11110 10001 10001 11110 10001 10001 11110",
        'C' to "01110 10001 10000 10000 10000 10001 01110", 'D' to "11100 10010 10001 10001 10001 10010 11100",
        'E' to "11111 10000 10000 11110 10000 10000 11111", 'F' to "11111 10000 10000 11110 10000 10000 10000",
        'G' to "01110 10001 10000 10111 10001 10001 01111", 'H' to "10001 10001 10001 11111 10001 10001 10001",
        'I' to "01110 00100 00100 00100 00100 00100 01110", 'J' to "00111 00010 00010 00010 00010 10010 01100",
        'K' to "10001 10010 10100 11000 10100 10010 10001", 'L' to "10000 10000 10000 10000 10000 10000 11111",
        'M' to "10001 11011 10101 10101 10001 10001 10001", 'N' to "10001 10001 11001 10101 10011 10001 10001",
        'O' to "01110 10001 10001 10001 10001 10001 01110", 'P' to "11110 10001 10001 11110 10000 10000 10000",
        'Q' to "01110 10001 10001 10001 10101 10010 01101", 'R' to "11110 10001 10001 11110 10100 10010 10001",
        'S' to "01111 10000 10000 01110 00001 00001 11110", 'T' to "11111 00100 00100 00100 00100 00100 00100",
        'U' to "10001 10001 10001 10001 10001 10001 01110", 'V' to "10001 10001 10001 10001 10001 01010 00100",
        'W' to "10001 10001 10001 10101 10101 10101 01010", 'X' to "10001 10001 01010 00100 01010 10001 10001",
        'Y' to "10001 10001 01010 00100 00100 00100 00100", 'Z' to "11111 00001 00010 00100 01000 10000 11111",
        ':' to "00000 00100 00100 00000 00100 00100 00000", '.' to "00000 00000 00000 00000 00000 01100 01100",
        '-' to "00000 00000 00000 11111 00000 00000 00000", '/' to "00001 00001 00010 00100 01000 10000 10000",
        '%' to "11001 11010 00010 00100 01000 01011 10011", ' ' to "00000 00000 00000 00000 00000 00000 00000",
        '+' to "00000 00100 00100 11111 00100 00100 00000", '!' to "00100 00100 00100 00100 00100 00000 00100",
        '?' to "01110 10001 00001 00110 00100 00000 00100", '°' to "01100 10010 10010 01100 00000 00000 00000",
    )
    private val glyphs: Map<Char, BooleanArray> = rows.mapValues { (_, r) -> r.replace(" ", "").map { it == '1' }.toBooleanArray() }

    /** The 5 × 7 mask for [c] (row-major), case-insensitive; null when Xe has no dotted form for it. */
    fun glyph(c: Char): BooleanArray? = glyphs[c.uppercaseChar()]

    fun canDraw(text: String): Boolean = text.isNotEmpty() && text.all { glyph(it) != null }

    /** A grid of lit dots, [cols] × [rows], row-major. */
    class Grid(val cols: Int, val rows: Int, val on: BooleanArray)

    /**
     * Any text (Thai with its vowels and tone marks included) as a dot grid [rows] high: the text is drawn with
     * the system font into a tiny bitmap that tall, and every pixel that is mostly ink becomes a lit dot.
     */
    fun rasterize(text: String, rows: Int, bold: Boolean = true): Grid {
        if (text.isEmpty() || rows <= 0) return Grid(0, rows, BooleanArray(0))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            color = android.graphics.Color.WHITE
        }
        // Size the font so its full height (marks above and below included) fills the rows.
        paint.textSize = 100f
        val fm = paint.fontMetrics
        paint.textSize = 100f * rows / (fm.descent - fm.ascent)
        val m = paint.fontMetrics
        val cols = kotlin.math.ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
        val bmp = android.graphics.Bitmap.createBitmap(cols, rows, android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bmp).drawText(text, 0f, -m.ascent, paint)
        val px = IntArray(cols * rows).also { bmp.getPixels(it, 0, cols, 0, 0, cols, rows) }
        bmp.recycle()
        return Grid(cols, rows, BooleanArray(px.size) { (px[it] ushr 24) > 96 })
    }
}

/** [text] as dots from [DotFont.rasterize] (for text the 5 × 7 font can't draw, such as Thai). */
@Composable
fun DotRasterText(text: String, rows: Int, dot: Dp, color: Color, modifier: Modifier = Modifier, ghost: Float = .08f) {
    val g = remember(text, rows) { DotFont.rasterize(text, rows) }
    Canvas(modifier.size(dot * g.cols, dot * g.rows).semantics { contentDescription = text }) {
        val step = dot.toPx(); val r = step * .38f
        for (y in 0 until g.rows) for (x in 0 until g.cols) {
            val on = g.on[y * g.cols + x]
            if (!on && ghost <= 0f) continue
            drawCircle(if (on) color else color.copy(alpha = color.alpha * ghost), r, Offset((x + .5f) * step, (y + .5f) * step))
        }
    }
}

/**
 * [text] in dot-matrix type, [dot] being the spacing of the dot grid. Unlit dots are drawn faintly
 * ([ghost]) like the panel of a Nothing display; text that can't be dotted falls back to nothing, so
 * callers check [DotFont.canDraw] first.
 */
@Composable
fun DotText(text: String, dot: Dp, color: Color, modifier: Modifier = Modifier, ghost: Float = .10f, accentLast: Color? = null) {
    val n = text.length
    val w = dot * (n * (DotFont.W + 1) - 1)
    val h = dot * DotFont.H
    val masks = remember(text) { text.map { DotFont.glyph(it) } }
    Canvas(modifier.size(w, h).semantics { contentDescription = text }) {
        val step = dot.toPx()
        val r = step * .38f
        masks.forEachIndexed { i, m ->
            val ox = i * (DotFont.W + 1) * step
            val c = if (accentLast != null && i == n - 1) accentLast else color
            for (y in 0 until DotFont.H) for (x in 0 until DotFont.W) {
                val on = m?.get(y * DotFont.W + x) == true
                if (!on && ghost <= 0f) continue
                drawCircle(if (on) c else c.copy(alpha = c.alpha * ghost), r, Offset(ox + (x + .5f) * step, (y + .5f) * step))
            }
        }
    }
}
