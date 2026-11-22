package com.xenoorigin.xelauncher.wallpaper.ascii

import kotlin.math.roundToInt

/**
 * Xe's own text-mode glyphs: 5 × 8 pixel masks ordered by how much "ink" they carry, so a brighter cell
 * gets a denser glyph. Drawn from bitmaps, never from a font, so every phone shows the same picture.
 */
object AsciiGlyphs {
    const val CW = 5
    const val CH = 8
    const val RAIN = 8
    const val SPARK = 9

    private val points: List<List<Pair<Int, Int>>> = listOf(
        emptyList(),
        listOf(2 to 5),                                                       // .
        listOf(2 to 2, 2 to 5),                                               // :
        listOf(1 to 4, 2 to 4, 3 to 4),                                       // -
        listOf(1 to 3, 2 to 3, 3 to 3, 1 to 5, 2 to 5, 3 to 5),               // =
        (0..4).flatMap { x -> listOf(x to 3, x to 5) },                      // two lines
        (0..4).flatMap { x -> listOf(x to 2, x to 4, x to 6) },              // three lines
        (0..4).flatMap { x -> listOf(x to 1, x to 3, x to 5, x to 7) },      // four lines
        listOf(3 to 1, 3 to 2, 2 to 3, 2 to 4, 1 to 5, 1 to 6),               // rain /
        listOf(2 to 2, 1 to 3, 2 to 3, 3 to 3, 2 to 4, 0 to 1, 4 to 1, 0 to 5, 4 to 5), // sparkle *
    )

    /** One mask per glyph, row-major (`y * 5 + x`). */
    val masks: Array<BooleanArray> = Array(points.size) { g ->
        BooleanArray(CW * CH).also { m -> points[g].forEach { (x, y) -> m[y * CW + x] = true } }
    }

    /** Glyph for brightness [l] (0..1) with the cell's fixed [jitter]; very dim cells stay empty. */
    fun index(l: Float, jitter: Float): Int = if (l < 0.09f) 0 else (l * 7 + jitter).roundToInt().coerceIn(0, 7)
}

/**
 * One frame's worth of cells: brightness [l], final colour [color] (ARGB), a glyph override [glyph]
 * (-1 = pick from [l]) and a fixed per-cell [jitter] that gives the dithered texture.
 */
class CellBuffer(val cols: Int, val rows: Int, seed: Int) {
    val size = cols * rows
    val l = FloatArray(size)
    val color = IntArray(size)
    val glyph = IntArray(size)
    val jitter = FloatArray(size).also { j ->
        val r = java.util.Random(seed.toLong())
        for (i in j.indices) j[i] = (r.nextFloat() - .5f) * .7f
    }

    fun clear() {
        l.fill(0f); color.fill(0); glyph.fill(-1)
    }
}
