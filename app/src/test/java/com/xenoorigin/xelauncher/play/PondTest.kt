package com.xenoorigin.xelauncher.play

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PondTest {
    private fun run(p: PondSim, seconds: Float) = repeat((seconds * 60).toInt()) { p.step(1f / 60f) }

    @Test fun aTap_makesARipple_andReleasesBubblesThatRise() {
        val p = PondSim(seed = 1)
        p.splash(200f, 800f, unit = 3f)
        assertThat(p.ripples).hasSize(1)
        assertThat(p.bubbles.size).isIn(3..5)
        val y0 = p.bubbles.map { it.y }
        run(p, .5f)
        p.bubbles.forEachIndexed { i, b -> assertThat(b.y).isLessThan(y0[i]) }
        assertThat(p.alive).isTrue()
    }

    @Test fun everythingFadesAway_andThePondGoesQuiet() {
        val p = PondSim(seed = 2)
        p.splash(200f, 800f, unit = 3f)
        run(p, 7f)
        assertThat(p.bubbles).isEmpty(); assertThat(p.ripples).isEmpty(); assertThat(p.pops).isEmpty()
        assertThat(p.alive).isFalse()
    }

    @Test fun tappingABubble_popsIt_insteadOfMakingANewSplash() {
        val p = PondSim(seed = 3)
        p.splash(200f, 800f, unit = 3f)
        val b = p.bubbles.first()
        val before = p.bubbles.size
        assertThat(p.popAt(b.x + b.r * .5f, b.y)).isTrue()
        assertThat(p.bubbles.size).isEqualTo(before - 1)
        assertThat(p.pops).hasSize(1)
        assertThat(p.popAt(5000f, 5000f)).isFalse()
    }

    @Test fun thePondNeverFillsUp() {
        val p = PondSim(seed = 4)
        repeat(40) { p.splash(100f + it, 600f, unit = 3f) }
        assertThat(p.bubbles.size).isAtMost(PondSim.MAX_BUBBLES)
    }
}
