package com.xenoorigin.xelauncher.search

import android.content.pm.ShortcutInfo
import com.xenoorigin.xelauncher.apps.AppEntry
import java.util.Locale

sealed interface SearchResult {
    data class App(val entry: AppEntry) : SearchResult
    data class Shortcut(val info: ShortcutInfo, val label: String) : SearchResult
    data class Calc(val expr: String, val value: String) : SearchResult
    data class Web(val query: String) : SearchResult
}

object SearchEngine {
    private val operators = Regex("[+\\-−*/×÷^%]")

    fun search(
        q: String, apps: List<AppEntry>, shortcuts: List<ShortcutInfo>,
        calculator: Boolean = true, web: Boolean = true,
    ): List<SearchResult> {
        val query = q.trim()
        if (query.isEmpty()) return emptyList()
        val needle = query.lowercase(Locale.ROOT)
        val out = mutableListOf<SearchResult>()
        if (calculator && operators.containsMatchIn(query)) {
            Calculator.eval(query)?.let { out += SearchResult.Calc(query, Calculator.format(it)) }
        }
        out += apps.mapNotNull { a -> rank(a.label, a.key.packageName, needle)?.let { it to a } }
            .sortedWith(compareBy({ it.first }, { it.second.label.length }))
            .map { SearchResult.App(it.second) }
        out += shortcuts.mapNotNull { s ->
            val label = (s.shortLabel ?: s.longLabel)?.toString() ?: return@mapNotNull null
            rank(label, "", needle)?.takeIf { it < 3 }?.let { it to SearchResult.Shortcut(s, label) }
        }.sortedBy { it.first }.take(6).map { it.second }
        if (web) out += SearchResult.Web(query)
        return out
    }

    /** 0 label prefix, 1 word prefix, 2 substring, 3 package substring, null no match. */
    private fun rank(label: String, pkg: String, needle: String): Int? {
        val l = label.lowercase(Locale.ROOT)
        return when {
            l.startsWith(needle) -> 0
            l.split(' ', '-', '_', '.').any { it.startsWith(needle) } -> 1
            l.contains(needle) -> 2
            pkg.isNotEmpty() && pkg.lowercase(Locale.ROOT).contains(needle) -> 3
            else -> null
        }
    }
}
