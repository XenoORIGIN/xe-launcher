package com.xenoorigin.xelauncher.design

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MotionMathTest {
    @Test fun drawerCascade_topRowArrivesFirst_andAllLandWhenOpen() {
        val half = .45f
        assertThat(drawerCascade(half, row = 0)).isGreaterThan(drawerCascade(half, row = 4))
        assertThat(drawerCascade(0f, 0)).isEqualTo(0f)
        (0..20).forEach { assertThat(drawerCascade(1f, it)).isEqualTo(1f) }
    }

    @Test fun pageDepth_centredPageIsFlat_neighboursLeanAway() {
        val c = pageDepth(0f)
        assertThat(c.scale).isEqualTo(1f); assertThat(c.rotationY).isEqualTo(0f); assertThat(c.alpha).isEqualTo(1f)
        val left = pageDepth(-.5f); val right = pageDepth(.5f)
        assertThat(left.scale).isLessThan(1f)
        assertThat(left.rotationY).isEqualTo(-right.rotationY)
        assertThat(left.alpha).isLessThan(1f)
    }

    @Test fun returnToHome_startsZoomedAndFaded_endsNormal() {
        assertThat(homeReturnScale(0f)).isGreaterThan(1f)
        assertThat(homeReturnScale(1f)).isEqualTo(1f)
    }

    @Test fun settleSpringIsSharedAndScalesWithSpeed() {
        val n = XeMotion.settle<Float>(AnimationSpeed.NORMAL)
        val slow = XeMotion.settle<Float>(AnimationSpeed.SMOOTH)
        assertThat(n.dampingRatio).isEqualTo(.82f)
        assertThat(slow.stiffness).isLessThan(n.stiffness)
        assertThat(XeMotion.settle<Float>(AnimationSpeed.OFF).stiffness).isAtLeast(100_000f)
    }
}
