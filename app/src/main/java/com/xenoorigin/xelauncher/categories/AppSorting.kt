package com.xenoorigin.xelauncher.categories

import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.data.LaunchStat
import com.xenoorigin.xelauncher.settings.DrawerSort
import java.text.Collator
import java.util.Locale

object AppSorting {
    private val thai: Collator = Collator.getInstance(Locale.forLanguageTag("th")).apply { strength = Collator.SECONDARY }
    private fun isThai(c: Char?) = c != null && c in '฀'..'๿'

    /** Label without leading symbols/emoji/spaces, so "  Notes" and "✨Notes" sort under N. */
    private fun sortKey(label: String) = label.dropWhile { !it.isLetterOrDigit() }.ifEmpty { label }

    /** Latin (and others) first, Thai after; each group collated, case-insensitive. */
    val alphabetical: Comparator<AppEntry> = Comparator { a, b ->
        val ka = sortKey(a.label); val kb = sortKey(b.label)
        val ta = isThai(ka.firstOrNull()); val tb = isThai(kb.firstOrNull())
        if (ta != tb) (if (ta) 1 else -1) else thai.compare(ka, kb).takeIf { it != 0 } ?: a.key.encode().compareTo(b.key.encode())
    }

    /**
     * @param usage foreground time per package when Usage access is granted; "most used" then means
     *   "used longest", otherwise it falls back to how often the app was opened from Xe.
     */
    fun sort(
        apps: List<AppEntry>, sort: DrawerSort, stats: Map<String, LaunchStat>, order: Map<String, Int> = emptyMap(),
        usage: Map<String, Long>? = null,
    ): List<AppEntry> = when (sort) {
        DrawerSort.CUSTOM -> apps.sortedWith(compareBy<AppEntry> { order[it.key.encode()] ?: Int.MAX_VALUE }.then(alphabetical))
        DrawerSort.ALPHABETICAL -> apps.sortedWith(alphabetical)
        DrawerSort.MOST_USED -> if (usage != null) apps.sortedWith(compareByDescending<AppEntry> { usage[it.key.packageName] ?: 0L }.then(alphabetical))
            else apps.sortedWith(compareByDescending<AppEntry> { stats[it.key.encode()]?.count ?: 0 }.then(alphabetical))
        DrawerSort.RECENTLY_INSTALLED -> apps.sortedWith(compareByDescending<AppEntry> { it.installedAt }.then(alphabetical))
    }
}
