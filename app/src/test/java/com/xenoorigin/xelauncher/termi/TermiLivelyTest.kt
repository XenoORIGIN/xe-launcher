package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Owner, 27 Nov: Termi should keep moving its body all the time, in the widget too. */
class TermiLivelyTest {
    private fun poses(a: TermiAnim) = (0 until 96).map { TermiAnimations.frame(a, it / 96f, it).pose }

    @Test fun idleStrollsLeftAndRight() {
        val dx = poses(TermiAnim.IDLE).map { it.dx }
        assertThat(dx.max()).isGreaterThan(2f)
        assertThat(dx.min()).isLessThan(-2f)
    }

    @Test fun idleStepsAndHops() {
        val p = poses(TermiAnim.IDLE)
        assertThat(p.map { it.legs }.toSet().size).isAtLeast(2)    // feet lift while walking
        assertThat(p.minOf { it.dy }).isLessThan(-1f)               // bounces / a hop
        assertThat(p.map { it.face }.toSet().size).isAtLeast(3)     // looks around, blinks, smiles
    }

    @Test fun idleNeverHoldsStillForLong() {
        val p = poses(TermiAnim.IDLE)
        var still = 0; var longest = 0
        for (i in 1 until p.size) { if (p[i] == p[i - 1]) still++ else still = 0; longest = maxOf(longest, still) }
        assertThat(longest).isLessThan(8) // never more than ~2/3 s frozen at 12 fps over an 8 s loop
    }

    @Test fun routinesComeOften() {
        val d = AnimDirector(java.util.Random(1))
        var now = 0L; var vibes = 0
        repeat(400) { if (d.next(Mood.HAPPY, null, now) != TermiAnim.IDLE) vibes++; now += 1_000 }
        assertThat(vibes).isAtLeast(25) // one every 6–14 s across 400 s
    }
}
