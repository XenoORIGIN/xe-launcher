package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScreenOnTest {
    private val from = 1_000_000L; private val to = from + 6 * 3_600_000L

    // 2.1 bug hunt: a screen left on all night has no on/off events inside the night: that is 6 h on, not 0.
    @Test fun onAllNightWithNoEvents() = assertThat(TermiUsage.screenOnTotal(emptyList(), from, to, onAtStart = true)).isEqualTo(to - from)

    @Test fun offAllNight() = assertThat(TermiUsage.screenOnTotal(emptyList(), from, to, onAtStart = false)).isEqualTo(0L)

    @Test fun onThenOff() = assertThat(TermiUsage.screenOnTotal(listOf(from + 100L to false), from, to, onAtStart = true)).isEqualTo(100L)

    @Test fun briefCheckInTheNight() =
        assertThat(TermiUsage.screenOnTotal(listOf(from + 1000L to true, from + 61_000L to false), from, to, onAtStart = false)).isEqualTo(60_000L)
}
