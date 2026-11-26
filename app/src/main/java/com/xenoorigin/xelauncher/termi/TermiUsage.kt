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

    /** Screen-on ms between [from] and [to]. */
    fun screenOn(ctx: Context): (Long, Long) -> Long? = { from, to ->
        if (!UsageTime.hasAccess(ctx)) null else {
            val events = ctx.getSystemService(UsageStatsManager::class.java).queryEvents(from, to)
            val e = UsageEvents.Event()
            var on: Long? = null
            var total = 0L
            var first = true
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                when (e.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE -> { on = e.timeStamp; first = false }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        total += e.timeStamp - (on ?: if (first) from else e.timeStamp); on = null; first = false
                    }
                }
            }
            on?.let { total += to - it }
            total
        }
    }

    /** Default creator apps that are installed. */
    fun installedDefaults(ctx: Context): () -> Set<String> = {
        CREATOR_DEFAULTS.filter { ctx.packageManager.getLaunchIntentForPackage(it) != null }.toSet()
    }
}
