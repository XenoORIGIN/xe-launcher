package com.xenoorigin.xelauncher.apps

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

/**
 * How much an app is used: minutes in the foreground over the last week when Android lets Xe read it
 * ("Usage access"), otherwise how often it was opened from Xe. Reads are cached for [REFRESH_MS].
 *
 * @param source package → foreground ms for the last 7 days, or null when access isn't granted
 */
class UsageTime(private val source: () -> Map<String, Long>?, private val clock: () -> Long = System::currentTimeMillis) {
    private var cached: Map<String, Long>? = null
    private var readAt = Long.MIN_VALUE

    /** Foreground time per package, or null without access. */
    fun usage(): Map<String, Long>? {
        val now = clock()
        if (readAt == Long.MIN_VALUE || now - readAt > REFRESH_MS) {
            cached = runCatching { source() }.getOrNull()
            readAt = now
        }
        return cached
    }

    /** True when ranking uses real time spent. */
    val usingTime: Boolean get() = usage() != null

    /** Bigger = used more. Time in ms when available, else the launch count. */
    fun score(packageName: String, launches: Int): Long = usage()?.let { it[packageName] ?: 0L } ?: launches.toLong()

    /** Forget the cached reading (e.g. right after the user grants access). */
    fun refresh() { readAt = Long.MIN_VALUE }

    companion object {
        const val REFRESH_MS = 10 * 60_000L
        private const val WEEK_MS = 7 * 24 * 60 * 60_000L

        fun hasAccess(ctx: Context): Boolean = runCatching {
            val ops = ctx.getSystemService(AppOpsManager::class.java)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)

        /** Opens Android's "Usage access" page (on Xe's own entry where the phone supports it). */
        fun requestAccess(ctx: Context) {
            val own = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
            val list = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            for (i in listOf(own, list)) if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }

        /** Foreground ms per package over the last week, or null without access. */
        fun systemSource(ctx: Context): () -> Map<String, Long>? = {
            if (!hasAccess(ctx)) null else {
                val usm = ctx.getSystemService(UsageStatsManager::class.java)
                val now = System.currentTimeMillis()
                usm.queryAndAggregateUsageStats(now - WEEK_MS, now).mapValues { it.value.totalTimeInForeground }
            }
        }
    }
}
