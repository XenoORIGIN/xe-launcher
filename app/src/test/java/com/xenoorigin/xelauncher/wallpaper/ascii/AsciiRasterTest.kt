package com.xenoorigin.xelauncher.wallpaper.ascii

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AsciiRasterTest {
    private val white = 0xFFFFFFFF.toInt()

    @Test fun emptyCellsStayGround() {
        val b = CellBuffer(2, 1, 1); b.clear()
        val px = IntArray(10 * 8)
        AsciiRaster.rasterize(b, px)
        assertThat(px.toSet()).containsExactly(AsciiRaster.GROUND)
    }

    @Test fun brightCellStampsItsGlyphInItsOwnCellOnly() {
        val b = CellBuffer(2, 1, 1); b.clear()
        b.l[1] = 1f; b.color[1] = white; b.jitter[1] = 0f
        val px = IntArray(10 * 8)
        AsciiRaster.rasterize(b, px)
        assertThat((0 until 8).flatMap { y -> (0 until 5).map { px[y * 10 + it] } }.toSet()).containsExactly(AsciiRaster.GROUND)
        val lit = (0 until 8).sumOf { y -> (5 until 10).count { px[y * 10 + it] == white } }
        assertThat(lit).isEqualTo(20) // glyph 7 = four 5-px lines
    }

    @Test fun glyphOverrideWins() {
        val b = CellBuffer(1, 1, 1); b.clear()
        b.glyph[0] = AsciiGlyphs.RAIN; b.color[0] = white
        val px = IntArray(5 * 8)
        AsciiRaster.rasterize(b, px)
        assertThat(px.count { it == white }).isEqualTo(6)
    }

    @Test fun indexIgnoresDimCells() {
        assertThat(AsciiGlyphs.index(0.08f, 0.3f)).isEqualTo(0)
        assertThat(AsciiGlyphs.index(1f, 0.3f)).isEqualTo(7)
    }

    @Test fun jitterIsStablePerSeed() {
        assertThat(CellBuffer(4, 4, 7).jitter.toList()).isEqualTo(CellBuffer(4, 4, 7).jitter.toList())
    }
}
