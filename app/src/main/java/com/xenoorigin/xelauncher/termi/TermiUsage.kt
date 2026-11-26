package com.xenoorigin.xelauncher.termi

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.xenoorigin.xelauncher.apps.UsageTime
import java.time.LocalDate
import java.time.ZoneId

/** Readings from Android's usage stats for Termi (all null without Usage access). */
object TermiUsage {
    /** Foreground ms since local midnight for [pkgs]. */
    fun today(ctx: Context): (Set<String>) -> Map<String, Long>? = { pkgs ->
        if (!UsageTime.hasAccess(ctx)) null else {
            val usm = ctx.getSystemService(UsageStatsManager::class.java)
            val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            usm.queryAndAggregateUsageStats(start, System.currentTimeMillis())
                .filterKeys { it in pkgs }.mapValues { it.value.totalTimeInForeground }
        }
    }

    /**
     * Screen-on ms between [from] and [to] from screen on/off [events] (time, on?), given whether the screen was
     * already on at [from]. A screen left on the whole time has no events at all, and counts in full.
     */
    fun screenOnTotal(events: List<Pair<Long, Boolean>>, from: Long, to: Long, onAtStart: Boolean): Long {
        var on: Long? = if (onAtStart) from else null
        var total = 0L
        for ((t, isOn) in events.sortedBy { it.first }) {
            if (isOn) { if (on == null) on = t } else { on?.let { total += t - it }; on = null }
        }
        on?.let { total += to - it }
        return total.coerceIn(0L, to - from)
    }

    private fun screenEvents(ctx: Context, from: Long, to: Long): List<Pair<Long, Boolean>> {
        val events = ctx.getSystemService(UsageStatsManager::class.java).queryEvents(from, to)
        val e = UsageEvents.Event()
        val out = mutableListOf<Pair<Long, Boolean>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> out += e.timeStamp to true
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> out += e.timeStamp to false
            }
        }
        return out
    }

    /** Screen-on ms between [from] and [to]. The state at [from] is the last screen event in the 12 hours before. */
    fun screenOn(ctx: Context): (Long, Long) -> Long? = { from, to ->
        if (!UsageTime.hasAccess(ctx)) null else runCatching {
            val before = screenEvents(ctx, from - 12 * 3_600_000L, from)
            val inside = screenEvents(ctx, from, to)
            // No event before either: fall back to the old reading (on if the first event inside switches it off).
            val onAtStart = before.lastOrNull()?.second ?: (inside.firstOrNull()?.second == false)
            screenOnTotal(inside, from, to, onAtStart)
        }.getOrNull()
    }

    /** Default creator apps that are installed. */
    fun installedDefaults(ctx: Context): () -> Set<String> = {
        CREATOR_DEFAULTS.filter { ctx.packageManager.getLaunchIntentForPackage(it) != null }.toSet()
    }
}
