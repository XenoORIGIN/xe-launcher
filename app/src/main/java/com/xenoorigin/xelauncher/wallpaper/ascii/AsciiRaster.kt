package com.xenoorigin.xelauncher.wallpaper.ascii

import com.xenoorigin.xelauncher.wallpaper.ascii.AsciiGlyphs.CH
import com.xenoorigin.xelauncher.wallpaper.ascii.AsciiGlyphs.CW

/** Turns cells into pixels: one 5 × 8 glyph per cell on the dark ground. Allocates nothing. */
object AsciiRaster {
    const val GROUND = 0xFF18181A.toInt()

    /** [out] is `cols*5` wide and `rows*8` tall, row-major. */
    fun rasterize(cells: CellBuffer, out: IntArray) {
        val w = cells.cols * CW
        require(out.size == w * cells.rows * CH) { "raster size" }
        out.fill(GROUND)
        for (r in 0 until cells.rows) for (c in 0 until cells.cols) {
            val i = r * cells.cols + c
            val g = cells.glyph[i].takeIf { it >= 0 } ?: AsciiGlyphs.index(cells.l[i], cells.jitter[i])
            if (g == 0) continue
            val mask = AsciiGlyphs.masks[g]
            val col = cells.color[i]
            val x0 = c * CW
            val y0 = r * CH
            for (y in 0 until CH) {
                val row = (y0 + y) * w + x0
                for (x in 0 until CW) if (mask[y * CW + x]) out[row + x] = col
            }
        }
    }
}
