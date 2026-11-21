package com.xenoorigin.xelauncher.play

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleGameTest {
    private fun run(g: BubbleGameSim, seconds: Float) = repeat((seconds * 60).toInt()) { g.step(1f / 60f) }

    @Test fun bubblesKeepComing_fasterAsTheClockRunsDown() {
        val g = BubbleGameSim(1080f, 2340f, unit = 2.6f, seed = 5)
        run(g, 3f); val early = g.spawned
        run(g, 22f); val beforeEnd = g.spawned
        val g2 = BubbleGameSim(1080f, 2340f, unit = 2.6f, seed = 5)
        run(g2, 3f)
        assertThat(early).isAtLeast(3)
        assertThat(beforeEnd - early).isGreaterThan(early * 4)
    }

    @Test fun poppingScores_goldenBubblesScoreFive() {
        val g = BubbleGameSim(1080f, 2340f, unit = 2.6f, seed = 9)
        run(g, 2f)
        val b = g.bubbles.first()
        val gain = if (b.golden) 5 else 1
        assertThat(g.tap(b.x, b.y)).isTrue()
        assertThat(g.score).isEqualTo(gain)
        assertThat(g.tap(-500f, -500f)).isFalse()
    }

    @Test fun theRoundEndsAfterThirtySeconds_andIgnoresTapsAfterwards() {
        val g = BubbleGameSim(1080f, 2340f, unit = 2.6f, seed = 1)
        run(g, 29f)
        assertThat(g.over).isFalse()
        run(g, 1.5f)
        assertThat(g.over).isTrue()
        assertThat(g.timeLeft).isEqualTo(0f)
        val score = g.score
        g.bubbles.firstOrNull()?.let { g.tap(it.x, it.y) }
        assertThat(g.score).isEqualTo(score)
    }
}
