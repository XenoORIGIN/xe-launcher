package com.xenoorigin.xelauncher.termi

import kotlinx.serialization.Serializable

enum class Stage { JUNIOR, SENIOR, LEAD }
enum class Slot { HEAD, EYES, HAND }

/** Wearable things, each unlocked at a level. */
enum class Item(val slot: Slot, val level: Int) {
    CAP(Slot.HEAD, 2), COFFEE(Slot.HAND, 3), SUNGLASSES(Slot.EYES, 4), BEANIE(Slot.HEAD, 5),
    KEYBOARD(Slot.HAND, 6), ROUND(Slot.EYES, 7), PARTY(Slot.HEAD, 8), DUCK(Slot.HAND, 9),
    VR(Slot.EYES, 10), PHONE(Slot.HAND, 11), WIZARD(Slot.HEAD, 12),
}

enum class Mood { HAPPY, OKAY, HUNGRY, LONELY, SLEEPY, ASLEEP }

/** Everything Xe remembers about Termi. Needs are 0..100 and decay from [updatedAt]. */
@Serializable
data class TermiState(
    val food: Float = 80f,
    val `fun`: Float = 80f,
    val energy: Float = 80f,
    val xp: Int = 0,
    val updatedAt: Long = 0,
    val lastFeedXpAt: Long = 0,
    val lastPlayXpAt: Long = 0,
    val lastSleepXpDay: Long = -1,
    val napUntil: Long = 0,
    val equipped: Map<Slot, Item> = emptyMap(),
    /** Chosen creator apps; null = not chosen yet (use the installed defaults). */
    val creatorApps: Set<String>? = null,
    val nightStart: Int = 0,
    val nightEnd: Int = 360,
    val creditDay: Long = -1,
    val creditedMs: Map<String, Long> = emptyMap(),
    val creatorXpToday: Int = 0,
    val lastNightDay: Long = -1,
    /** Woken during the night window: stays awake until this time (the window's end). */
    val awakeUntil: Long = 0,
    /** End of the last night window whose rest bonus was checked. */
    val lastNightEnd: Long = 0,
    /** Developer mode: every item can be worn whatever the level. */
    val unlockAll: Boolean = false,
)

/**
 * Termi's care rules. Fair to people who live on their phone: nothing here looks at how much the phone is
 * used, Termi never dies and never loses a level.
 */
object TermiRules {
    const val XP_PER_LEVEL = 60
    const val MAX_LEVEL = 15
    const val SENIOR_XP = 400
    const val LEAD_XP = 840
    const val ACTION_XP = 10
    const val XP_COOLDOWN = 2 * 3_600_000L
    const val NAP_MS = 45 * 60_000L
    private const val H = 3_600_000f

    fun level(xp: Int): Int = (xp / XP_PER_LEVEL + 1).coerceIn(1, MAX_LEVEL)
    fun stage(xp: Int): Stage = when { xp >= LEAD_XP -> Stage.LEAD; xp >= SENIOR_XP -> Stage.SENIOR; else -> Stage.JUNIOR }

    /** Inside the night window [start, end) in minutes of the day; the window may cross midnight. */
    fun inNight(minute: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minute in start until end
        else -> minute >= start || minute < end
    }

    private fun asleep(s: TermiState, at: Long, minute: Int) = at < s.napUntil || (inNight(minute, s.nightStart, s.nightEnd) && at >= s.awakeUntil)

    /** Needs after time has passed (in 15-minute steps, so the night window is honoured). */
    fun decay(s: TermiState, now: Long, minuteOfDay: (Long) -> Int): TermiState {
        // Clock set back (after being set ahead): start counting from now, and let Feed/Play earn XP again.
        if (now < s.updatedAt) return s.copy(updatedAt = now, lastFeedXpAt = if (s.lastFeedXpAt > now) now - XP_COOLDOWN else s.lastFeedXpAt)
        if (now == s.updatedAt) return s
        if (s.updatedAt <= 0L) return s.copy(updatedAt = now)
        var food = s.food; var fn = s.`fun`; var en = s.energy
        // A week away empties every need anyway; never walk more steps than that (a clock set years ahead).
        var at = maxOf(s.updatedAt, now - 7L * 24 * 3_600_000L)
        val step = 15 * 60_000L
        while (at < now) {
            val dt = minOf(step, now - at)
            val h = dt / H
            food = (food - 6f * h).coerceIn(0f, 100f)
            if (asleep(s, at, minuteOfDay(at))) en = (en + 20f * h).coerceIn(0f, 100f)
            else { fn = (fn - 8f * h).coerceIn(0f, 100f); en = (en - 5f * h).coerceIn(0f, 100f) }
            at += dt
        }
        return s.copy(food = food.coerceIn(0f, 100f), `fun` = fn.coerceIn(0f, 100f), energy = en.coerceIn(0f, 100f), updatedAt = now)
    }

    fun mood(s: TermiState, now: Long, minute: Int): Mood = when {
        asleep(s, now, minute) -> Mood.ASLEEP
        s.food < 30f -> Mood.HUNGRY
        s.`fun` < 30f -> Mood.LONELY
        s.energy < 25f -> Mood.SLEEPY
        s.food >= 60f && s.`fun` >= 60f && s.energy >= 60f -> Mood.HAPPY
        else -> Mood.OKAY
    }

    private fun gain(xp: Int, add: Int) = (xp + add).coerceAtMost(LEAD_XP * 10)

    /** [wakeUntil]: when woken inside the night window, the window's end (0 otherwise). */
    fun feed(s: TermiState, now: Long, minute: Int, wakeUntil: Long = 0L): TermiState {
        val night = inNight(minute, s.nightStart, s.nightEnd)
        val xp = !night && now - s.lastFeedXpAt >= XP_COOLDOWN
        return s.copy(
            food = (s.food + 35f).coerceAtMost(100f), napUntil = 0, awakeUntil = maxOf(s.awakeUntil, wakeUntil),
            xp = if (xp) gain(s.xp, ACTION_XP) else s.xp, lastFeedXpAt = if (xp) now else s.lastFeedXpAt,
        )
    }

    fun play(s: TermiState, now: Long, minute: Int, wakeUntil: Long = 0L): TermiState {
        val night = inNight(minute, s.nightStart, s.nightEnd)
        val xp = !night && now - s.lastPlayXpAt >= XP_COOLDOWN
        return s.copy(
            `fun` = (s.`fun` + 35f).coerceAtMost(100f), energy = (s.energy - 5f).coerceAtLeast(0f), napUntil = 0, awakeUntil = maxOf(s.awakeUntil, wakeUntil),
            xp = if (xp) gain(s.xp, ACTION_XP) else s.xp, lastPlayXpAt = if (xp) now else s.lastPlayXpAt,
        )
    }

    fun sleep(s: TermiState, now: Long, minute: Int, day: Long): TermiState {
        val xp = s.lastSleepXpDay != day && !inNight(minute, s.nightStart, s.nightEnd)
        return s.copy(napUntil = now + NAP_MS, awakeUntil = 0, xp = if (xp) gain(s.xp, ACTION_XP) else s.xp, lastSleepXpDay = if (xp) day else s.lastSleepXpDay)
    }

    fun wake(s: TermiState, now: Long, wakeUntil: Long = 0L): TermiState = s.copy(napUntil = 0, awakeUntil = maxOf(s.awakeUntil, wakeUntil))

    fun unlocked(xp: Int): List<Item> = Item.entries.filter { it.level <= level(xp) }.sortedBy { it.level }

    /** Whether [item] can be worn now: its level is reached, or developer mode unlocked everything. */
    fun isUnlocked(s: TermiState, item: Item): Boolean = s.unlockAll || item.level <= level(s.xp)

    fun equip(s: TermiState, item: Item?, slot: Slot): TermiState = when {
        item == null -> s.copy(equipped = s.equipped - slot)
        item.slot != slot || !isUnlocked(s, item) -> s
        else -> s.copy(equipped = s.equipped + (slot to item))
    }
}
