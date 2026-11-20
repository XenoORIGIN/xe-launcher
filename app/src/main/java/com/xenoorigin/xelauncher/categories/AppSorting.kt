package com.xenoorigin.xelauncher.categories

import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.data.LaunchStat
import com.xenoorigin.xelauncher.settings.DrawerSort
import java.text.Collator
import java.util.Locale

object AppSorting {
    private val thai: Collator = Collator.getInstance(Locale.forLanguageTag("th")).apply { strength = Collator.SECONDARY }
    private fun isThai(c: Char?) = c != null && c in '฀'..'๿'

    /** Latin (and others) first, Thai after; each group collated. */
    val alphabetical: Comparator<AppEntry> = Comparator { a, b ->
        val ta = isThai(a.label.firstOrNull()); val tb = isThai(b.label.firstOrNull())
        if (ta != tb) (if (ta) 1 else -1) else thai.compare(a.label, b.label)
    }

    fun sort(apps: List<AppEntry>, sort: DrawerSort, stats: Map<String, LaunchStat>): List<AppEntry> = when (sort) {
        DrawerSort.ALPHABETICAL -> apps.sortedWith(alphabetical)
        DrawerSort.MOST_USED -> apps.sortedWith(compareByDescending<AppEntry> { stats[it.key.encode()]?.count ?: 0 }.then(alphabetical))
        DrawerSort.RECENTLY_INSTALLED -> apps.sortedWith(compareByDescending<AppEntry> { it.installedAt }.then(alphabetical))
    }
}
