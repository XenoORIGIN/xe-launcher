package com.xenoorigin.xelauncher.wallpaper.ascii

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.wallpaper.SunTimes
import com.xenoorigin.xelauncher.wallpaper.skyAt
import org.junit.Test

class AsciiFieldTest {
    private fun field() = AsciiField(72, 98)
    private val noon = skyAt(12 * 60)
    private val midnight = skyAt(0)

    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF

    @Test fun sameSeedSameCells() {
        val a = field().apply { fill(AsciiScene.LAKE, 1f) }
        val b = field().apply { fill(AsciiScene.LAKE, 1f) }
        assertThat(a.cells.l.toList()).isEqualTo(b.cells.l.toList())
    }

    @Test fun moonIsBrighterThanTheSkyAroundIt() {
        val f = field().apply { fill(AsciiScene.LAKE, 0f) }
        val (mx, my) = f.discCell
        val disc = (-1..1).flatMap { dy -> (-1..1).map { dx -> f.cells.l[(my + dy) * 72 + mx + dx] } }.average()
        val farSky = (60 until 72).map { f.cells.l[2 * 72 + it] }.average()
        assertThat(disc).isAtLeast(0.6)
        assertThat(farSky).isAtMost(0.2)
    }

    @Test fun nightHasStarsDayHasNone() {
        val f = field()
        f.fill(AsciiScene.LAKE, 0f, midnight)
        assertThat(f.lastStars).isGreaterThan(15)
        f.fill(AsciiScene.LAKE, 0f, noon)
        assertThat(f.lastStars).isEqualTo(0)
    }

    @Test fun dayUsesSunNightUsesMoon() {
        val f = field()
        f.fill(AsciiScene.LAKE, 0f, noon)
        assertThat(f.highlight.count { it == AsciiField.SUN_TINT }).isGreaterThan(0)
        f.fill(AsciiScene.LAKE, 0f, midnight)
        assertThat(f.highlight.count { it == AsciiField.SUN_TINT }).isEqualTo(0)
        assertThat(f.highlight.count { it == AsciiField.MOON_TINT }).isGreaterThan(0)
    }

    @Test fun daytimeStaysDarkEnoughForLabels() {
        val f = field().apply { fill(AsciiScene.LAKE, 0f, noon) }
        val px = IntArray(72 * 5 * 98 * 8)
        AsciiRaster.rasterize(f.cells, px)
        val mean = px.map { .2126 * r(it) + .7152 * g(it) + .0722 * b(it) }.average()
        assertThat(mean).isLessThan(70.0)
    }

    @Test fun monoLeavesPlainCellsGrey() {
        val f = field().apply { fill(AsciiScene.LAKE, 0f, midnight, AsciiColor.MONO) }
        val plain = f.cells.l.indices.filter { f.cells.l[it] >= .09f && f.amount[it] == 0f }
        assertThat(plain.size).isGreaterThan(100)
        plain.forEach { val c = f.cells.color[it]; assertThat(r(c) == g(c) && g(c) == b(c)).isTrue() }
    }

    @Test fun accentTintsPlainCells() {
        val f = field().apply { fill(AsciiScene.LAKE, 1f, midnight, AsciiColor.ACCENT, 0xFF2F8BFF) }
        val water = (f.horizonRow + 2) * 72
        val lit = (water until water + 72 * 4).filter { f.cells.l[it] >= .09f && f.amount[it] == 0f }
        assertThat(lit).isNotEmpty()
        lit.forEach { assertThat(b(f.cells.color[it])).isGreaterThan(r(f.cells.color[it])) }
    }

    @Test fun darkAccentStillVisible() {
        val f = field().apply { fill(AsciiScene.LAKE, 0f, midnight, AsciiColor.ACCENT, 0xFF101010) }
        val brightest = f.cells.l.indices.maxBy { if (f.amount[it] == 0f) f.cells.l[it] else 0f }
        val c = f.cells.color[brightest]
        assertThat(maxOf(r(c), g(c), b(c))).isAtLeast(120)
    }

    @Test fun skyModeFollowsTheLook() {
        val sun = SunTimes(6 * 60, 18 * 60 + 20)
        fun warmth(look: com.xenoorigin.xelauncher.wallpaper.SkyLook): Double {
            val f = field().apply { fill(AsciiScene.LAKE, 0f, look, AsciiColor.SKY) }
            return (0 until f.horizonRow * 72).filter { f.cells.l[it] >= .09f && f.amount[it] == 0f }
                .map { (r(f.cells.color[it]) - b(f.cells.color[it])).toDouble() }.average()
        }
        assertThat(warmth(skyAt(sun.sunset + 20, sun))).isGreaterThan(warmth(midnight))
    }

    @Test fun waterShimmersOverTime() {
        val f = field()
        f.fill(AsciiScene.LAKE, 0f); val a = f.cells.l.copyOfRange(f.horizonRow * 72, f.groundRow * 72)
        f.fill(AsciiScene.LAKE, 1f); val b = f.cells.l.copyOfRange(f.horizonRow * 72, f.groundRow * 72)
        assertThat(a.toList()).isNotEqualTo(b.toList())
    }

    @Test fun landscapeGridWorks() {
        val f = AsciiField(96, 30)
        f.fill(AsciiScene.LAKE, 0f)
        assertThat(f.groundRow).isIn(Range.closed(20, 29))
        assertThat(f.cells.l.count { it > .5f }).isGreaterThan(10)
    }

    @Test fun fillIsFast() {
        val f = field()
        val px = IntArray(360 * 784)
        repeat(5) { f.fill(AsciiScene.LAKE, it.toFloat(), midnight); AsciiRaster.rasterize(f.cells, px) }
        val t0 = System.nanoTime()
        repeat(10) { f.fill(AsciiScene.LAKE, it.toFloat(), midnight); AsciiRaster.rasterize(f.cells, px) }
        assertThat((System.nanoTime() - t0) / 10 / 1e6).isLessThan(15.0)
    }

    // 2.1 bug hunt: grids smaller than a scene needs (4 rows, 3 cols) used to throw; they now draw what fits.
    @Test fun tinyGridsDoNotCrash() {
        for ((c, r) in listOf(1 to 1, 2 to 2, 3 to 3, 40 to 2, 2 to 40)) {
            val f = AsciiField(c, r)
            AsciiScene.entries.forEach { f.fill(it, 0f) }
        }
    }
}
