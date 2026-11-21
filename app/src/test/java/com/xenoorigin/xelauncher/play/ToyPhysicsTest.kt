package com.xenoorigin.xelauncher.play

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class ToyPhysicsTest {
    private fun run(p: ToyPhysics, seconds: Float, fx: Float = 0f, fy: Float = 0f) {
        repeat((seconds * 90).toInt()) { p.step(1f / 90f, fx, fy) }
    }

    @Test fun noForce_iconsStayInPlace_andCountAsResting() {
        val p = ToyPhysics(8)
        run(p, 1f)
        (0 until 8).forEach { assertThat(p.x(it)).isEqualTo(0f); assertThat(p.y(it)).isEqualTo(0f) }
        assertThat(p.resting).isTrue()
    }

    @Test fun tiltingRight_slidesIconsRight_thenTheySettleBackWhenTheTiltStops() {
        val p = ToyPhysics(6)
        run(p, .4f, fx = 4f)
        (0 until 6).forEach { assertThat(p.x(it)).isGreaterThan(4f) }
        assertThat(p.resting).isFalse()
        run(p, 3f)
        (0 until 6).forEach { assertThat(abs(p.x(it))).isLessThan(.5f) }
        assertThat(p.resting).isTrue()
    }

    @Test fun iconsDoNotAllMoveInLockstep() {
        val p = ToyPhysics(6)
        run(p, .25f, fy = -5f)
        val ys = (0 until 6).map { p.y(it) }
        assertThat(ys.toSet().size).isGreaterThan(1)
        ys.forEach { assertThat(it).isLessThan(0f) }
    }

    @Test fun travelIsCapped_evenUnderAHugeForce() {
        val p = ToyPhysics(4)
        run(p, 2f, fx = 500f, fy = 500f)
        (0 until 4).forEach { assertThat(hypot(p.x(it), p.y(it))).isAtMost(ToyPhysics.MAX_TRAVEL_DP + .01f) }
    }

    @Test fun shake_scattersAndSpins_thenEverythingReturns() {
        val p = ToyPhysics(10)
        p.shake(seed = 3)
        run(p, .15f)
        assertThat((0 until 10).count { hypot(p.x(it), p.y(it)) > 6f }).isAtLeast(7)
        assertThat((0 until 10).count { abs(p.rotation(it)) > 3f }).isAtLeast(5)
        run(p, 4f)
        (0 until 10).forEach { assertThat(hypot(p.x(it), p.y(it))).isLessThan(.6f); assertThat(abs(p.rotation(it))).isLessThan(.6f) }
    }

    @Test fun splash_pushesNearbyIconsAway_andLeavesFarOnesAlone() {
        val p = ToyPhysics(3)
        p.setCenter(0, 100f, 100f); p.setCenter(1, 140f, 100f); p.setCenter(2, 900f, 900f)
        p.push(120f, 100f, radiusDp = 120f)
        run(p, .12f)
        assertThat(p.x(0)).isLessThan(-2f)
        assertThat(p.x(1)).isGreaterThan(2f)
        assertThat(p.x(2)).isEqualTo(0f)
    }

    @Test fun growing_keepsExistingIconsWhereTheyAre() {
        val p = ToyPhysics(2)
        run(p, .3f, fx = 4f)
        val x0 = p.x(0)
        p.ensure(40)
        assertThat(p.x(0)).isEqualTo(x0)
        assertThat(p.x(39)).isEqualTo(0f)
    }
}

class SloshFilterTest {
    @Test fun holdingStillAtAnAngle_fadesToNoForce() {
        val f = SloshFilter()
        repeat(200) { f.update(-3f, 8f, 1f / 50f) }
        assertThat(f.fx).isEqualTo(0f); assertThat(f.fy).isEqualTo(0f)
    }

    @Test fun dippingTheRightEdge_pushesRight_tippingTheTopAway_pushesUp() {
        val f = SloshFilter()
        repeat(50) { f.update(0f, 9f, 1f / 50f) }
        f.update(-4f, 6f, 1f / 50f)
        assertThat(f.fx).isGreaterThan(2f)
        assertThat(f.fy).isLessThan(-2f)
    }
}

class ShakeDetectorTest {
    @Test fun aJoltCountsOnce_gentleMovesNever() {
        val s = ShakeDetector()
        assertThat(s.sample(2f, 3f, 1f, 0)).isFalse()
        assertThat(s.sample(15f, 4f, 0f, 100)).isTrue()
        assertThat(s.sample(15f, 4f, 0f, 300)).isFalse()
        assertThat(s.sample(15f, 4f, 0f, 1200)).isTrue()
    }
}
