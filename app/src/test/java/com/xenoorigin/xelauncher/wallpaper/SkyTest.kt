package com.xenoorigin.xelauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SkyTest {
    private fun lum(c: Long) = (((c shr 16) and 0xFF) + ((c shr 8) and 0xFF) + (c and 0xFF)) / 3

    @Test fun nightHasStars_dayHasNone() {
        assertThat(skyAt(23 * 60).stars).isGreaterThan(.8f)
        assertThat(skyAt(2 * 60).stars).isGreaterThan(.8f)
        assertThat(skyAt(12 * 60).stars).isEqualTo(0f)
    }

    @Test fun noonIsBrighterThanMidnight_andDuskIsWarm() {
        assertThat(lum(skyAt(12 * 60).bottom)).isGreaterThan(lum(skyAt(0).bottom) + 60)
        val dusk = skyAt(18 * 60 + 10).bottom
        assertThat((dusk shr 16) and 0xFF).isGreaterThan(dusk and 0xFF) // more red than blue near the horizon
    }

    @Test fun colours_changeGradually_includingAcrossMidnight() {
        for (m in 0 until 24 * 60) {
            val a = skyAt(m); val b = skyAt((m + 1) % (24 * 60))
            assertThat(kotlin.math.abs(lum(a.top) - lum(b.top))).isAtMost(6)
            assertThat(kotlin.math.abs(a.stars - b.stars)).isAtMost(.05f)
        }
    }

    @Test fun theSunIsUpByDay_andTheMoonByNight() {
        assertThat(skyAt(12 * 60).sunUp).isTrue()
        assertThat(skyAt(1 * 60).sunUp).isFalse()
        assertThat(skyAt(12 * 60).orbY).isLessThan(skyAt(17 * 60).orbY)
    }

    @Test fun skyRedrawsEveryTwentyMinutes() {
        assertThat(skyBucket(10 * 60 + 5)).isEqualTo(skyBucket(10 * 60 + 19))
        assertThat(skyBucket(10 * 60 + 19)).isNotEqualTo(skyBucket(10 * 60 + 21))
    }
}
