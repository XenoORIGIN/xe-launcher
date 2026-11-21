package com.xenoorigin.xelauncher.play

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DustTest {
    private val day = 86_400_000L
    private val now = 1_000 * day

    @Test fun recentlyUsedApps_areClean() {
        assertThat(dustLevel(now, lastUse = now - 3 * day)).isEqualTo(0f)
        assertThat(dustLevel(now, lastUse = now - 20 * day)).isEqualTo(0f)
    }

    @Test fun dustBuildsUpAfterThreeWeeks_andMaxesOutAroundTwoMonths() {
        val early = dustLevel(now, now - 25 * day)
        val later = dustLevel(now, now - 45 * day)
        assertThat(early).isGreaterThan(0f)
        assertThat(later).isGreaterThan(early)
        assertThat(dustLevel(now, now - 60 * day)).isEqualTo(1f)
        assertThat(dustLevel(now, now - 900 * day)).isEqualTo(1f)
    }

    @Test fun lastUse_isTheLatestOfLaunchInstallWipeAndWhenDustWasTurnedOn() {
        // Turning dust on today must not bury every app that hasn't been opened through Xe yet.
        assertThat(lastUse(lastLaunch = 0, installedAt = 5, wipedAt = 0, dustSince = now)).isEqualTo(now)
        assertThat(lastUse(lastLaunch = now - day, installedAt = 0, wipedAt = now - 2 * day, dustSince = 0)).isEqualTo(now - day)
        assertThat(lastUse(0, 0, 0, 0)).isEqualTo(0)
        assertThat(dustLevel(now, 0)).isEqualTo(0f)
    }

    @Test fun rubbing_wipesInProportionToDistance() {
        val r = RubProgress(iconSizePx = 100f)
        assertThat(r.add(60f)).isFalse()
        assertThat(r.progress).isGreaterThan(0f)
        assertThat(r.progress).isLessThan(1f)
        var done = false
        repeat(20) { if (r.add(40f)) done = true }
        assertThat(done).isTrue()
        assertThat(r.progress).isEqualTo(1f)
    }
}
