package com.xenoorigin.xelauncher.termi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.FileOutputStream
import java.io.File
import java.time.Instant
import java.time.ZoneId

enum class TermiEvent { FED, PLAYED, SLEPT, WOKE, CODED, RESTED, LEVEL_UP, STAGE_UP }

data class Credit(val creatorXp: Int, val rested: Boolean)

/** Apps counted as "creator" apps when Kantaphon hasn't chosen yet (only those installed are used). */
val CREATOR_DEFAULTS = setOf(
    "com.anthropic.claude", "com.openai.chatgpt", "com.google.android.apps.bard", "com.github.android",
    "com.termux", "com.foxdebug.acode", "com.foxdebug.acodefree", "io.spck", "com.paprbit.dcoder",
    "com.replit.app", "com.sonelli.juicessh", "com.pterodactyl.app",
)

/**
 * Termi's saved state and everything that changes it. Saves to [file] after each change.
 * [usageToday] = foreground ms since local midnight for the given packages (null without Usage access);
 * [nightScreenMs] = screen-on ms between two times (null without Usage access).
 */
class TermiStore(
    private val file: File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val usageToday: (Set<String>) -> Map<String, Long>? = { null },
    private val nightScreenMs: (Long, Long) -> Long? = { _, _ -> null },
    private val defaultCreators: () -> Set<String> = { emptySet() },
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }
    private val _state = MutableStateFlow(load())
    /** Saved state (not decayed); use [current] for the needs right now. */
    val state: StateFlow<TermiState> = _state
    private val listeners = mutableListOf<(TermiEvent) -> Unit>()

    fun listen(l: (TermiEvent) -> Unit) { synchronized(listeners) { listeners += l } }
    fun unlisten(l: (TermiEvent) -> Unit) { synchronized(listeners) { listeners -= l } }

    private fun load(): TermiState {
        val text = (if (file.exists()) runCatching { file.readText() }.getOrNull() else null) ?: return TermiState(updatedAt = clock())
        return decode(text) ?: decode(withoutUnknownItems(text)) ?: run {
            // Never overwrite progress we couldn't read: keep the file aside, then start fresh.
            runCatching { file.copyTo(File(file.path + ".bad"), overwrite = true) }
            TermiState(updatedAt = clock())
        }
    }

    private fun decode(text: String?): TermiState? = text?.let { runCatching { json.decodeFromString(TermiState.serializer(), it) }.getOrNull() }

    /** The saved state with any slot or item this version doesn't know left out of `equipped`. */
    private fun withoutUnknownItems(text: String): String? = runCatching {
        val o = json.parseToJsonElement(text).jsonObject
        val eq = o["equipped"]?.jsonObject ?: return@runCatching null
        val slots = Slot.entries.map { it.name }.toSet(); val items = Item.entries.map { it.name }.toSet()
        val kept = eq.filter { (k, v) -> k in slots && runCatching { v.jsonPrimitive.content }.getOrNull() in items }
        JsonObject(o + ("equipped" to JsonObject(kept))).toString()
    }.getOrNull()

    private fun minuteOf(ts: Long): Int = Instant.ofEpochMilli(ts).atZone(zone()).let { it.hour * 60 + it.minute }
    private fun dayOf(ts: Long): Long = Instant.ofEpochMilli(ts).atZone(zone()).toLocalDate().toEpochDay()

    fun current(): TermiState = TermiRules.decay(_state.value, clock(), ::minuteOf)
    fun mood(): Mood = clock().let { TermiRules.mood(current(), it, minuteOf(it)) }

    private fun change(vararg events: TermiEvent, f: (TermiState) -> TermiState) = changeWith { f(it) to events.toList() }

    /** Reads, changes and saves under one lock, so overlapping callers never both use the same old state. */
    @Synchronized
    private fun changeWith(f: (TermiState) -> Pair<TermiState, List<TermiEvent>>) {
        val before = current()
        val (after, events) = f(before)
        _state.value = after
        save(after)
        val out = events.toMutableList()
        if (TermiRules.level(after.xp) > TermiRules.level(before.xp)) out += TermiEvent.LEVEL_UP
        if (TermiRules.stage(after.xp) > TermiRules.stage(before.xp)) out += TermiEvent.STAGE_UP
        val ls = synchronized(listeners) { listeners.toList() }
        out.forEach { e -> ls.forEach { it(e) } }
    }

    private fun save(s: TermiState) {
        runCatching {
            file.parentFile?.mkdirs()
            val t = File(file.path + ".tmp")
            FileOutputStream(t).use { out -> out.write(json.encodeToString(TermiState.serializer(), s).toByteArray()); out.fd.sync() }
            if (!t.renameTo(file)) { file.delete(); t.renameTo(file) }
        }
    }

    fun feed() = clock().let { now -> change(TermiEvent.FED) { TermiRules.feed(it, now, minuteOf(now), nightEndIfInside(it, now)) } }
    fun play() = clock().let { now -> change(TermiEvent.PLAYED) { TermiRules.play(it, now, minuteOf(now), nightEndIfInside(it, now)) } }

    /** The end of the night window [now] is in, or 0 when it isn't in one. */
    private fun nightEndIfInside(s: TermiState, now: Long): Long {
        val minute = minuteOf(now)
        if (!TermiRules.inNight(minute, s.nightStart, s.nightEnd)) return 0L
        val today = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
        val endDay = if (minute < s.nightEnd) today else today.plusDays(1)
        return endDay.atStartOfDay(zone()).toInstant().toEpochMilli() + s.nightEnd * 60_000L
    }

    /** Sleep when awake, wake when napping (or sleeping through the night window). */
    fun toggleSleep() {
        val now = clock()
        if (mood() == Mood.ASLEEP) change(TermiEvent.WOKE) { TermiRules.wake(it, now, nightEndIfInside(it, now)) }
        else change(TermiEvent.SLEPT) { TermiRules.sleep(it, now, minuteOf(now), dayOf(now)) }
    }

    fun equip(item: Item?, slot: Slot) = change { TermiRules.equip(it, item, slot) }

    fun creatorApps(): Set<String> = _state.value.creatorApps ?: defaultCreators()

    fun setCreator(pkg: String, on: Boolean) {
        val base = creatorApps()
        change { it.copy(creatorApps = if (on) base + pkg else base - pkg) }
    }

    fun setNight(start: Int, end: Int) = change { it.copy(nightStart = Math.floorMod(start, 1440), nightEnd = Math.floorMod(end, 1440)) }

    internal fun addXpForTest(xp: Int) = change { it.copy(xp = it.xp + xp) }

    /** Developer mode: jump straight to [level] (1–15); the stage follows. */
    fun setLevel(level: Int) = change { it.copy(xp = (level.coerceIn(1, TermiRules.MAX_LEVEL) - 1) * TermiRules.XP_PER_LEVEL) }

    /** Developer mode: let every item be worn whatever the level. */
    fun setUnlockAll(on: Boolean) = change { s ->
        // Turning it off takes off what the level doesn't allow yet.
        val kept = if (on) s.equipped else s.equipped.filterValues { it.level <= TermiRules.level(s.xp) }
        s.copy(unlockAll = on, equipped = kept)
    }

    /** Developer mode: a brand-new Termi, keeping the owner's choices (creator apps, bedtime). */
    fun reset() = clock().let { now -> change { s -> TermiState(updatedAt = now, creatorApps = s.creatorApps, nightStart = s.nightStart, nightEnd = s.nightEnd) } }

    /**
     * Called when Home is shown: credits new minutes in creator apps (1 XP per 2 minutes, at most 60 a day)
     * and, once per night window after it ends, the night-rest bonus. The slow usage queries run first;
     * the credit is then worked out from the latest state inside the lock, so overlapping calls credit once.
     * Credited minutes are kept across days (only the daily cap resets), so a usage bucket that still holds
     * yesterday after midnight adds only its new minutes.
     */
    fun onHomeShown(): Credit {
        val now = clock()
        val today = dayOf(now)
        val apps = creatorApps()
        val usage = if (apps.isEmpty()) null else runCatching { usageToday(apps) }.getOrNull()
        val s0 = _state.value
        val window = lastNightWindow(s0, now)
        val screen = if (window != null && !nightChecked(s0, window.second, today)) runCatching { nightScreenMs(window.first, window.second) }.getOrNull() else null
        if (usage == null && screen == null) return Credit(0, false)
        var credit = Credit(0, false)
        changeWith { s ->
            var units = 0
            var credited = s.creditedMs
            var capUsed = if (s.creditDay == today) s.creatorXpToday else 0
            var xpAdd = 0
            if (usage != null) {
                val next = credited.toMutableMap()
                for (pkg in apps) {
                    val total = usage[pkg] ?: continue
                    var prev = credited[pkg] ?: 0L
                    // Smaller total: a new day counts from zero; the same day (reinstall) takes it as the baseline.
                    if (total < prev) { if (s.creditDay != today) prev = 0L else { next[pkg] = total; continue } }
                    val u = ((total - prev) / 120_000L).toInt()
                    units += u
                    next[pkg] = prev + u * 120_000L
                }
                credited = next
                xpAdd = minOf(units, (60 - capUsed).coerceAtLeast(0))
                capUsed += xpAdd
            }
            val checkNight = screen != null && window != null && !nightChecked(s, window.second, today)
            val rested = checkNight && screen!! < 15 * 60_000L
            credit = Credit(xpAdd, rested)
            val events = buildList { if (units > 0) add(TermiEvent.CODED); if (rested) add(TermiEvent.RESTED) }
            s.copy(
                xp = s.xp + xpAdd + if (rested) 40 else 0,
                `fun` = (s.`fun` + units).coerceAtMost(100f),
                energy = if (rested) 100f else s.energy,
                creditDay = if (usage != null) today else s.creditDay,
                creditedMs = credited,
                creatorXpToday = if (usage != null) capUsed else s.creatorXpToday,
                lastNightEnd = if (checkNight) window!!.second else s.lastNightEnd,
                lastNightDay = if (checkNight) today else s.lastNightDay,
            ) to events
        }
        return credit
    }

    /** The night window (start, end) that ended most recently, or null while inside one or with no window. */
    private fun lastNightWindow(s: TermiState, now: Long): Pair<Long, Long>? {
        val minute = minuteOf(now)
        if (s.nightStart == s.nightEnd || TermiRules.inNight(minute, s.nightStart, s.nightEnd)) return null
        val endAt = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
            .let { d -> if (minute >= s.nightEnd) d else d.minusDays(1) }
            .atStartOfDay(zone()).toInstant().toEpochMilli() + s.nightEnd * 60_000L
        val len = Math.floorMod(s.nightEnd - s.nightStart, 1440) * 60_000L
        return (endAt - len) to endAt
    }

    /** Already checked: this window's end is recorded (or, from older saves, today's check was done). */
    private fun nightChecked(s: TermiState, endAt: Long, today: Long) =
        s.lastNightEnd >= endAt || (s.lastNightEnd == 0L && s.lastNightDay == today)
}
