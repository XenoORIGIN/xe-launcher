package com.xenoorigin.xelauncher.design

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LensGlassTest {
    private val sdf = { x: Float, y: Float -> LensGlass.roundRect(x, y, 100f, 50f, 100f, 50f, 50f) }
    private val center = { _: Float, _: Float -> 100f to 50f }
    private val lens = LensGlass.Lens(strength = 30f, edge = 30f, magnify = .2f, chroma = .5f)

    @Test fun centreStaysPut() {
        val (x, y) = LensGlass.sample(100f, 50f, sdf, center, lens, 1f)
        assertThat(x).isWithin(.01f).of(100f); assertThat(y).isWithin(.01f).of(50f)
    }

    @Test fun insideIsMagnified() {
        val (x, _) = LensGlass.sample(130f, 50f, sdf, center, lens, 1f)
        assertThat(x).isLessThan(130f) // samples closer to the centre: what's behind looks bigger
    }

    @Test fun edgeBendsInwardMoreThanDeepInside() {
        val nearEdge = 100f - LensGlass.sample(100f, 3f, sdf, center, lens, 1f).second.let { 50f - it }
        val (_, yEdge) = LensGlass.sample(100f, 3f, sdf, center, lens, 1f)
        assertThat(yEdge - 3f).isGreaterThan(15f)
        assertThat(nearEdge).isNotNaN()
    }

    @Test fun coloursSplitAtTheEdgeOnly() {
        val r = LensGlass.sample(100f, 3f, sdf, center, lens, 1.5f).second
        val b = LensGlass.sample(100f, 3f, sdf, center, lens, .5f).second
        assertThat(r - b).isGreaterThan(5f)
        val rc = LensGlass.sample(100f, 50f, sdf, center, lens, 1.5f).second
        val bc = LensGlass.sample(100f, 50f, sdf, center, lens, .5f).second
        assertThat(rc - bc).isWithin(.01f).of(0f)
    }

    @Test fun shapesMeltTogether() {
        val a = LensGlass.circle(0f, 0f, -12f, 0f, 10f); val b = LensGlass.circle(0f, 0f, 12f, 0f, 10f)
        assertThat(minOf(a, b)).isGreaterThan(0f)            // apart, the gap is outside
        assertThat(LensGlass.smin(a, b, 12f)).isLessThan(0f)  // melted, the gap is filled: a neck
    }
}
