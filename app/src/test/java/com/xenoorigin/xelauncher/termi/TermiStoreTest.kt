package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class TermiStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, day, h, m).toInstant(zone).toEpochMilli()

    private var now = at(23, 12)
    private var usage: Map<String, Long>? = null
    private var night: Long? = null
    private fun store(file: java.io.File = tmp.root.resolve("termi.json")) = TermiStore(
        file, clock = { now }, zone = { zone as ZoneId }, usageToday = { usage }, nightScreenMs = { _, _ -> night },
        defaultCreators = { setOf("com.termux", "com.github.android") },
    )
    private val MIN = 60_000L

    @Test fun persistsAndReloads() {
        val f = tmp.root.resolve("t.json")
        store(f).apply { feed() }
        assertThat(store(f).current().xp).isEqualTo(10)
    }

    @Test fun corruptFileGivesAFreshTermi() {
        val f = tmp.root.resolve("bad.json").apply { writeText("{not json") }
        assertThat(store(f).current().xp).isEqualTo(0)
    }

    @Test fun creatorMinutesCountOnceAndOnlyForChosenApps() {
        val s = store()
        usage = mapOf("com.termux" to 10 * MIN, "com.instagram.android" to 120 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(5)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(0)
        usage = mapOf("com.termux" to 14 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(2)
        assertThat(s.current().xp).isEqualTo(7)
    }

    @Test fun usageGoingDownIsANewBaseline() {
        val s = store()
        usage = mapOf("com.termux" to 20 * MIN); s.onHomeShown()
        usage = mapOf("com.termux" to 4 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(0)
        usage = mapOf("com.termux" to 8 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(2)
        assertThat(s.current().xp).isAtLeast(0)
    }

    @Test fun creatorXpCappedPerDayAndResetsNextDay() {
        val s = store()
        usage = mapOf("com.termux" to 180 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(60)
        usage = mapOf("com.termux" to 240 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(0)
        now = at(24, 12); usage = mapOf("com.termux" to 10 * MIN)
        assertThat(s.onHomeShown().creatorXp).isEqualTo(5)
    }

    @Test fun choosingCreators() {
        val s = store()
        assertThat(s.creatorApps()).containsExactly("com.termux", "com.github.android")
        s.setCreator("com.termux", false); s.setCreator("com.anthropic.claude", true)
        assertThat(s.creatorApps()).containsExactly("com.github.android", "com.anthropic.claude")
    }

    @Test fun nightRestOncePerDay() {
        now = at(24, 8); night = 10 * MIN
        val s = store()
        val c = s.onHomeShown()
        assertThat(c.rested).isTrue()
        assertThat(s.current().xp).isEqualTo(40)
        assertThat(s.current().energy).isWithin(1f).of(100f)
        assertThat(s.onHomeShown().rested).isFalse()
    }

    @Test fun lateNightGivesNoBonusButCostsNothing() {
        now = at(24, 8); night = 30 * MIN
        val s = store()
        assertThat(s.onHomeShown().rested).isFalse()
        assertThat(s.current().xp).isEqualTo(0)
    }

    @Test fun noUsageAccessNoNightBonus() {
        now = at(24, 8); night = null
        assertThat(store().onHomeShown().rested).isFalse()
    }

    @Test fun duringTheNightNoBonusYet() {
        now = at(24, 3); night = 0
        assertThat(store().onHomeShown().rested).isFalse()
    }

    @Test fun levelAndStageEvents() {
        val s = store()
        val seen = mutableListOf<TermiEvent>()
        s.listen { seen += it }
        s.addXpForTest(55); s.feed()
        assertThat(seen).contains(TermiEvent.LEVEL_UP)
        s.addXpForTest(340)
        now += 3 * 3_600_000L; s.play()
        assertThat(seen).contains(TermiEvent.STAGE_UP)
    }
}
