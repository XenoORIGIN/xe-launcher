package com.xenoorigin.xelauncher.wallpaper

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParallaxTest {
    private val w = 1080f; private val tiltPx = 36f

    @Test fun firstAndLastPageSpanAtMostEightPercent() {
        val first = parallaxOffset(0f, 5, Offset.Zero, w, tiltPx)
        val last = parallaxOffset(4f, 5, Offset.Zero, w, tiltPx)
        assertThat(kotlin.math.abs(last.x - first.x)).isWithin(1f).of(w * .08f)
        assertThat(first.x).isGreaterThan(last.x) // wallpaper moves the other way, slower: depth
    }

    @Test fun onePageStaysCentred() {
        assertThat(parallaxOffset(0f, 1, Offset.Zero, w, tiltPx)).isEqualTo(Offset.Zero)
    }

    @Test fun tiltIsBounded() {
        val o = parallaxOffset(0f, 1, Offset(5f, -5f), w, tiltPx)
        assertThat(kotlin.math.abs(o.x)).isAtMost(tiltPx)
        assertThat(kotlin.math.abs(o.y)).isAtMost(tiltPx)
        assertThat(parallaxOffset(0f, 1, Offset(.5f, 0f), w, tiltPx).x).isLessThan(0f) // tilt right, scene shifts left
    }
}
