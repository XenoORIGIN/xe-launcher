package com.xenoorigin.xelauncher.wallpaper.ascii

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class AsciiScenesTest {
    private val c = 72
    private val r = 98
    private fun paint(s: AsciiScene, t: Float = 1f) = AsciiField(c, r).apply { fill(s, t) }
    private fun AsciiField.rowsOf(tint: Int, from: Float, to: Float) =
        (0 until size).count { highlight[it] == tint && it / cols in (rows * from).toInt()..(rows * to).toInt() }

    @Test fun rainHasStreaksAndNoMoon() {
        val f = paint(AsciiScene.RAIN)
        assertThat(f.cells.glyph.count { it == AsciiGlyphs.RAIN }).isAtLeast(c * r * 3 / 100)
        assertThat(f.highlight.count { it == AsciiField.MOON_TINT }).isEqualTo(0)
    }

    @Test fun dawnHasALowSunAndNoStars() {
        val f = paint(AsciiScene.DAWN)
        assertThat(f.highlight.count { it == AsciiScenes.DAWN_SUN }).isGreaterThan(10)
        assertThat(f.discCell.second).isIn(com.google.common.collect.Range.closed((f.horizonRow * .6f).toInt(), f.horizonRow - 1))
        assertThat(f.lastStars).isEqualTo(0)
    }

    @Test fun pinesHaveTreesAndFireflies() {
        val f = paint(AsciiScene.PINES)
        assertThat(f.trees).isAtLeast(6)
        assertThat(f.cells.glyph.count { it == AsciiGlyphs.SPARK }).isIn(com.google.common.collect.Range.closed(8, 14))
        assertThat(f.highlight.count { it == AsciiScenes.FIREFLY }).isAtLeast(8)
    }

    @Test fun cityHasLitWindows() {
        val f = paint(AsciiScene.CITY)
        assertThat(f.rowsOf(AsciiScenes.WINDOW, .4f, .75f)).isAtLeast(20)
        assertThat(f.buildings).isIn(com.google.common.collect.Range.closed(10, 16))
    }

    @Test fun spaceHasPlanetRingStarsAndMeteor() {
        val f = paint(AsciiScene.SPACE)
        assertThat(f.lastStars).isAtLeast(c * r * 4 / 100)
        assertThat(f.highlight.count { it == AsciiScenes.RING }).isAtLeast(20)
        assertThat(f.meteorCells).isAtLeast(6)
    }

    @Test fun lighthouseHasABeam() {
        val f = paint(AsciiScene.LIGHTHOUSE)
        assertThat(f.highlight.count { it == AsciiScenes.BEAM }).isAtLeast(40)
    }

    @Test fun beamSweepsOverTime() {
        val a = paint(AsciiScene.LIGHTHOUSE, 0f).highlight.toList()
        val b = paint(AsciiScene.LIGHTHOUSE, 3f).highlight.toList()
        assertThat(a).isNotEqualTo(b)
    }

    @Test fun everySceneDrawsSomethingOnBothGridShapes() {
        for (s in AsciiScene.entries) for ((cc, rr) in listOf(72 to 98, 96 to 30)) {
            val f = AsciiField(cc, rr); f.fill(s, 0f)
            assertWithMessage("$s $cc×$rr").that(f.cells.l.count { it > .3f }).isGreaterThan(cc * rr / 50)
        }
    }
}
