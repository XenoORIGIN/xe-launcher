package com.xenoorigin.xelauncher.design

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TiltTest {
    @Test fun uprightAndLevel_isCentredHorizontally() {
        assertThat(tiltFromGravity(0f, 6.9f, 6.9f).x).isWithin(.01f).of(0f)
    }

    @Test fun rightEdgeDown_tiltsRight() {
        // Android reports gravity on the axis pointing up; right edge down makes x negative.
        assertThat(tiltFromGravity(-3f, 6f, 6f).x).isGreaterThan(.3f)
        assertThat(tiltFromGravity(3f, 6f, 6f).x).isLessThan(-.3f)
    }

    @Test fun lyingFlatterMovesTheLightDown() {
        assertThat(tiltFromGravity(0f, 2f, 9.5f).y).isGreaterThan(tiltFromGravity(0f, 9.5f, 2f).y)
    }

    @Test fun valuesAreClamped() {
        val t = tiltFromGravity(-50f, 0f, 50f)
        assertThat(t.x).isAtMost(1f); assertThat(t.y).isAtMost(1f)
        val u = tiltFromGravity(50f, 50f, -50f)
        assertThat(u.x).isAtLeast(-1f); assertThat(u.y).isAtLeast(-1f)
    }

    @Test fun tapCounter_firesOnTheNthQuickTap_only() {
        val c = TapCounter(needed = 7, windowMs = 600)
        val fired = (0 until 7).map { c.tap(it * 200L) }
        assertThat(fired).isEqualTo(listOf(false, false, false, false, false, false, true))
        assertThat(c.tap(1500L)).isFalse()
    }

    @Test fun tapCounter_slowTapsStartOver() {
        val c = TapCounter(needed = 3, windowMs = 500)
        assertThat(c.tap(0)).isFalse(); assertThat(c.tap(400)).isFalse()
        assertThat(c.tap(1200)).isFalse() // too slow: counts as tap 1
        assertThat(c.tap(1500)).isFalse(); assertThat(c.tap(1800)).isTrue()
    }
}
