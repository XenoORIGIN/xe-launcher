package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TermiRulesTest {
    private val H = 3_600_000L
    private val t0 = 1_000_000_000_000L
    /** Minute of day for a timestamp: the test day starts at t0 = 12:00. */
    private val minuteAt: (Long) -> Int = { ((720 + (it - t0) / 60_000) % 1440).toInt().let { m -> if (m < 0) m + 1440 else m } }
    private fun s(vararg f: Pair<String, Float>) = TermiState(updatedAt = t0).let { st ->
        f.fold(st) { a, (k, v) -> when (k) { "food" -> a.copy(food = v); "fun" -> a.copy(`fun` = v); else -> a.copy(energy = v) } }
    }

    @Test fun levels() {
        assertThat(TermiRules.level(0)).isEqualTo(1)
        assertThat(TermiRules.level(59)).isEqualTo(1)
        assertThat(TermiRules.level(60)).isEqualTo(2)
        assertThat(TermiRules.level(10_000)).isEqualTo(15)
    }

    @Test fun stages() {
        assertThat(TermiRules.stage(399)).isEqualTo(Stage.JUNIOR)
        assertThat(TermiRules.stage(400)).isEqualTo(Stage.SENIOR)
        assertThat(TermiRules.stage(839)).isEqualTo(Stage.SENIOR)
        assertThat(TermiRules.stage(840)).isEqualTo(Stage.LEAD)
    }

    @Test fun decayWhileAwake() {
        val d = TermiRules.decay(s("food" to 80f, "fun" to 80f, "energy" to 80f), t0 + 10 * H, minuteAt)
        assertThat(d.food).isWithin(.01f).of(20f)
        assertThat(d.`fun`).isEqualTo(0f)
        assertThat(d.energy).isWithin(.01f).of(30f)
        assertThat(d.updatedAt).isEqualTo(t0 + 10 * H)
    }

    @Test fun energyRefillsWhileNapping() {
        val st = s("energy" to 20f).copy(napUntil = t0 + 3 * H)
        assertThat(TermiRules.decay(st, t0 + 2 * H, minuteAt).energy).isWithin(.01f).of(60f)
    }

    @Test fun clockGoingBackwardsChangesNothing() {
        val st = s("food" to 50f)
        assertThat(TermiRules.decay(st, t0 - 5 * H, minuteAt)).isEqualTo(st)
    }

    @Test fun longAbsenceClampsToZero() {
        val d = TermiRules.decay(s(), t0 + 30L * 24 * H, minuteAt)
        assertThat(listOf(d.food, d.`fun`, d.energy).all { it in 0f..100f }).isTrue()
        assertThat(d.food).isEqualTo(0f)
        assertThat(d.xp).isEqualTo(0)
    }

    @Test fun feedGivesXpOncePerTwoHours() {
        var st = s("food" to 10f)
        st = TermiRules.feed(st, t0, 720)
        st = TermiRules.feed(st, t0 + 60_000, 721)
        assertThat(st.food).isWithin(.1f).of(80f)
        assertThat(st.xp).isEqualTo(10)
        st = TermiRules.feed(st, t0 + 2 * H + 1, 841)
        assertThat(st.xp).isEqualTo(20)
    }

    @Test fun playCostsALittleEnergy() {
        val st = TermiRules.play(s("fun" to 10f, "energy" to 50f), t0, 720)
        assertThat(st.`fun`).isWithin(.1f).of(45f)
        assertThat(st.energy).isWithin(.1f).of(45f)
        assertThat(st.xp).isEqualTo(10)
    }

    @Test fun feedingAtNightWakesButGivesNoXp() {
        val st = TermiRules.feed(s().copy(napUntil = t0 + H), t0, 60)
        assertThat(st.xp).isEqualTo(0)
        assertThat(st.napUntil).isEqualTo(0L)
    }

    @Test fun sleepXpOncePerDay() {
        var st = TermiRules.sleep(s(), t0, 720, day = 5)
        assertThat(st.xp).isEqualTo(10)
        assertThat(st.napUntil).isEqualTo(t0 + 45 * 60_000L)
        st = TermiRules.wake(st, t0 + 1)
        st = TermiRules.sleep(st, t0 + 2, 720, day = 5)
        assertThat(st.xp).isEqualTo(10)
    }

    @Test fun moods() {
        assertThat(TermiRules.mood(s("food" to 20f), t0, 720)).isEqualTo(Mood.HUNGRY)
        assertThat(TermiRules.mood(s("fun" to 20f), t0, 720)).isEqualTo(Mood.LONELY)
        assertThat(TermiRules.mood(s("energy" to 20f), t0, 720)).isEqualTo(Mood.SLEEPY)
        assertThat(TermiRules.mood(s(), t0, 720)).isEqualTo(Mood.HAPPY)
        assertThat(TermiRules.mood(s("food" to 50f), t0, 720)).isEqualTo(Mood.OKAY)
        assertThat(TermiRules.mood(s(), t0, 60)).isEqualTo(Mood.ASLEEP)
        assertThat(TermiRules.mood(s().copy(napUntil = t0 + 1), t0, 720)).isEqualTo(Mood.ASLEEP)
    }

    @Test fun nightWindow() {
        assertThat(TermiRules.inNight(23 * 60, 0, 360)).isFalse()
        assertThat(TermiRules.inNight(60, 0, 360)).isTrue()
        assertThat(TermiRules.inNight(23 * 60, 1320, 360)).isTrue()
        assertThat(TermiRules.inNight(7 * 60, 1320, 360)).isFalse()
        assertThat(TermiRules.inNight(800, 780, 840)).isTrue()
        assertThat(TermiRules.inNight(900, 780, 840)).isFalse()
        assertThat(TermiRules.inNight(100, 100, 100)).isFalse()
    }

    @Test fun unlocksAndEquip() {
        assertThat(TermiRules.unlocked(240)).containsExactly(Item.CAP, Item.COFFEE, Item.SUNGLASSES, Item.BEANIE).inOrder()
        val st = s().copy(xp = 240)
        assertThat(TermiRules.equip(st, Item.WIZARD, Slot.HEAD)).isEqualTo(st)
        assertThat(TermiRules.equip(st, Item.CAP, Slot.HEAD).equipped).containsExactly(Slot.HEAD, Item.CAP)
        assertThat(TermiRules.equip(st, Item.CAP, Slot.EYES)).isEqualTo(st)
        assertThat(TermiRules.equip(TermiRules.equip(st, Item.CAP, Slot.HEAD), null, Slot.HEAD).equipped).isEmpty()
    }
}
