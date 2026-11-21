package com.xenoorigin.xelauncher.badges

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the badge counter needs to know about one notification in the shade. */
data class Notif(
    val pkg: String, val userSerial: Long, val number: Int,
    val group: String?, val isSummary: Boolean, val clearable: Boolean,
)

fun badgeKey(pkg: String, userSerial: Long) = "$pkg#$userSerial"

/**
 * Unread counts per app (key from [badgeKey]). Each notification counts as its own number when the app sets
 * one, otherwise as 1. Ongoing ones (music, downloads) don't count, nor do Xe's own. A group summary isn't
 * added on top of its messages, but if it states a bigger total than they add up to, that total is used.
 */
fun badgeCounts(all: List<Notif>, own: String): Map<String, Int> {
    val out = HashMap<String, Int>()
    all.filter { it.clearable && it.pkg != own }
        .groupBy { Triple(it.pkg, it.userSerial, it.group ?: "\u0000${System.identityHashCode(it)}") }
        .forEach { (k, list) ->
            val children = list.filter { !it.isSummary }
            val childSum = children.sumOf { if (it.number > 0) it.number else 1 }
            val summary = list.filter { it.isSummary }.maxOfOrNull { if (it.number > 0) it.number else 1 } ?: 0
            val count = maxOf(childSum, if (children.isEmpty()) summary else if (summary > 1) summary else 0)
            if (count > 0) out.merge(badgeKey(k.first, k.second), count, Int::plus)
        }
    return out
}

fun badgeLabel(count: Int): String = if (count > 99) "99+" else count.toString()

/** Live unread counts, filled in by [BadgeListener] while Xe has notification access. */
object Badges {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()
    fun set(counts: Map<String, Int>) { _counts.value = counts }
}
