package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Store rules found by the final review (waking at night, double credit, damaged files). */
class TermiReviewFixesTest {
    @get:Rule val tmp = TemporaryFolder()
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, day, h, m).toInstant(zone).toEpochMilli()
    private val MIN = 60_000L

    private var now = at(23, 12)
    private var usage: Map<String, Long>? = null
    private var night: (Long, Long) -> Long? = { _, _ -> null }
    private fun store(file: java.io.File = tmp.root.resolve("termi.json")) = TermiStore(
        file, clock = { now }, zone = { zone as ZoneId }, usageToday = { usage }, nightScreenMs = { a, b -> night(a, b) },
        defaultCreators = { setOf("com.termux") },
    )

    // I1
    @Test fun feedingWakesTermiDuringTheNightUntilTheWindowEnds() {
        val s = store()
        now = at(24, 1)
        assertThat(s.mood()).isEqualTo(Mood.ASLEEP)
        s.feed()
        assertThat(s.mood()).isNotEqualTo(Mood.ASLEEP)
        now = at(24, 3)
        assertThat(s.mood()).isNotEqualTo(Mood.ASLEEP)
        now = at(25, 1)
        assertThat(s.mood()).isEqualTo(Mood.ASLEEP)
    }

    @Test fun wakeAndSleepToggleDuringTheNight() {
        val s = store()
        now = at(24, 2)
        s.toggleSleep()
        assertThat(s.mood()).isNotEqualTo(Mood.ASLEEP)
        s.toggleSleep()
        assertThat(s.mood()).isEqualTo(Mood.ASLEEP)
    }

    // I3
    @Test fun overlappingHomeShowsCreditOnce() {
        val s = store()
        usage = mapOf("com.termux" to 20 * MIN)
        now = at(23, 8)
        var nested = false
        night = { _, _ -> if (!nested) { nested = true; s.onHomeShown() }; 0L }
        s.onHomeShown()
        assertThat(s.current().xp).isEqualTo(10 + 40)
    }

    // I4
    @Test fun nightBonusOncePerWindow() {
        val s = store()
        s.setNight(60, 420)
        night = { _, _ -> 0L }
        now = at(23, 8)
        assertThat(s.onHomeShown().rested).isTrue()
        now = at(24, 0, 30)
        assertThat(s.onHomeShown().rested).isFalse()
        now = at(24, 8)
        assertThat(s.onHomeShown().rested).isTrue()
    }

    // I5
    @Test fun yesterdaysMinutesAreNotCreditedAgainAfterMidnight() {
        val s = store()
        now = at(23, 23)
        usage = mapOf("com.termux" to 60 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(30)
        now = at(24, 0, 5)
        usage = mapOf("com.termux" to 62 * MIN) // a daily bucket that still holds yesterday
        assertThat(s.onHomeShown().creatorXp).isEqualTo(1)
    }

    // I6
    @Test fun unreadableFileIsKeptAside() {
        val f = tmp.root.resolve("t.json").apply { writeText("{not json") }
        store(f).feed()
        assertThat(java.io.File(f.path + ".bad").readText()).isEqualTo("{not json")
    }

    @Test fun unknownItemDoesNotLoseProgress() {
        val f = tmp.root.resolve("t.json")
        f.writeText("""{"xp":300,"updatedAt":${now},"equipped":{"HEAD":"TOP_HAT","EYES":"ROUND"}}""")
        val s = store(f)
        assertThat(s.current().xp).isEqualTo(300)
        assertThat(s.current().equipped).containsExactly(Slot.EYES, Item.ROUND)
    }
}
