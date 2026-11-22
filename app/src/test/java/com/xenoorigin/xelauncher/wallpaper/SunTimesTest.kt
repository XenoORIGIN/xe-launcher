package com.xenoorigin.xelauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SunTimesTest {
    @Test fun bangkokSunriseIsAroundSixFifteenInNovember() {
        val (lat, lon) = latLonFor(ZoneId.of("Asia/Bangkok"))
        val t = sunTimes(LocalDate.of(2026, 11, 22), lat, lon, utcOffsetMin = 7 * 60)
        assertThat(t.sunrise).isIn(6 * 60 + 10..6 * 60 + 25)
        assertThat(t.sunset).isIn(17 * 60 + 40..17 * 60 + 55)
    }

    @Test fun londonSummerDayIsLong() {
        val (lat, lon) = latLonFor(ZoneId.of("Europe/London"))
        val t = sunTimes(LocalDate.of(2026, 6, 21), lat, lon, utcOffsetMin = 60)
        assertThat(t.sunset - t.sunrise).isGreaterThan(16 * 60)
    }

    @Test fun polarNightFallsBack() {
        val t = sunTimes(LocalDate.of(2026, 12, 21), 78.2, 15.6, utcOffsetMin = 60)
        assertThat(t).isEqualTo(SunTimes.FALLBACK)
    }

    @Test fun unknownZoneUsesBangkok() {
        assertThat(latLonFor(ZoneId.of("Pacific/Galapagos")).first).isWithin(.5).of(13.75)
    }

    @Test fun skyIsDarkAfterDuskAndBlueAtNoon() {
        val sun = SunTimes(6 * 60 + 15, 17 * 60 + 48)
        assertThat(skyAt(sun.sunset + 90, sun).stars).isGreaterThan(.7f)
        val noon = skyAt(12 * 60, sun).top
        assertThat(noon and 0xFF).isGreaterThan((noon shr 16) and 0xFF) // more blue than red
        // Dusk arrives with the real sunset: 30 min before sunset is warm, 2 h before is still day.
        assertThat(skyAt(sun.sunset - 120, sun).stars).isEqualTo(0f)
        val golden = skyAt(sun.sunset - 10, sun).bottom
        assertThat((golden shr 16) and 0xFF).isGreaterThan(golden and 0xFF)
    }

    @Test fun oneMinuteChangesAreSmallForAnyDay() {
        fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toInt()
        for (sun in listOf(SunTimes(5 * 60, 20 * 60 + 30), SunTimes(6 * 60 + 15, 17 * 60 + 48), SunTimes.FALLBACK)) {
            for (m in 0 until 24 * 60) {
                val a = skyAt(m, sun); val b = skyAt((m + 1) % (24 * 60), sun)
                for (sh in listOf(0, 8, 16)) for ((x, y) in listOf(a.top to b.top, a.mid to b.mid, a.bottom to b.bottom))
                    assertThat(kotlin.math.abs(ch(x, sh) - ch(y, sh))).isAtMost(6)
            }
        }
    }
}
