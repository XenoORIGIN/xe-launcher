package com.xenoorigin.xelauncher.design

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.wallpaper.SunTimes
import androidx.compose.ui.graphics.Color
import org.junit.Test

class RimTintTest {
    private val sun = SunTimes(6 * 60 + 10, 18 * 60 + 20)

    @Test fun rimTintWarmAtSunset() {
        val c = rimTintAt(sun.sunset, sun)
        assertThat(c.alpha).isGreaterThan(.2f)
        assertThat(c.alpha).isAtMost(.35f)
        assertThat(c.red).isGreaterThan(c.blue)
        // Still warm 30 minutes before.
        assertThat(rimTintAt(sun.sunset - 30, sun).red).isGreaterThan(rimTintAt(sun.sunset - 30, sun).blue)
    }

    @Test fun rimTintNeutralAtNoon() {
        assertThat(rimTintAt(12 * 60, sun).alpha).isEqualTo(0f)
    }

    @Test fun rimTintCoolAtMidnight() {
        val c = rimTintAt(0, sun)
        assertThat(c.alpha).isGreaterThan(0f)
        assertThat(c.alpha).isAtMost(.35f)
        assertThat(c.blue).isGreaterThan(c.red)
        assertThat(rimTintAt(23 * 60, sun).blue).isGreaterThan(rimTintAt(23 * 60, sun).red)
    }

    @Test fun rimTintStaysWithinLimitAllDay() {
        for (m in 0 until 24 * 60) assertThat(rimTintAt(m, sun).alpha).isAtMost(.35f)
    }
}

class RimColorTest {
    @Test fun rimColoursPickUpTint() {
        val plain = rimColors(Color.Transparent)
        val warm = rimColors(Color(0xFFFFB066).copy(alpha = .35f))
        assertThat(plain[2]).isEqualTo(Color.White)
        assertThat(warm[2].blue).isLessThan(plain[2].blue)
        assertThat(warm[2].red).isEqualTo(1f)
    }
}
