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

    /**
     * How well [label] (or [pkg]) matches [needle]; lower is better, null = no match.
     * 0 prefix, 1 word prefix, 2 substring, 3 initials ("ytm" → YouTube Music), 4 package,
     * 5 letters in order ("wtsap"), 6 one typo ("telgram"). The same query typed on the other keyboard
     * layout (Thai Kedmanee ↔ English) also counts, a little lower.
     */
    private fun rank(label: String, pkg: String, needle: String): Int? {
        val direct = rankOne(label, pkg, needle)
        val swapped = KeyboardSwap.convert(needle)?.let { rankOne(label, pkg, it) }?.plus(1)
        return listOfNotNull(direct, swapped).minOrNull()
    }

    private fun rankOne(label: String, pkg: String, needle: String): Int? {
        val l = label.lowercase(Locale.ROOT)
        val words = words(label)
        return when {
            l.startsWith(needle) -> 0
            words.any { it.startsWith(needle) } -> 1
            l.contains(needle) -> 2
            needle.length >= 2 && words.size >= 2 && words.joinToString("") { it.take(1) }.startsWith(needle) -> 3
            pkg.isNotEmpty() && pkg.lowercase(Locale.ROOT).contains(needle) -> 4
            needle.length >= 3 && inOrder(needle, l.filter { it.isLetterOrDigit() }) && l.firstOrNull() == needle.first() -> 5
            needle.length >= 4 && (words + l.replace(" ", "")).any { w -> oneTypo(needle, w) || (w.length > needle.length && oneTypo(needle, w.take(needle.length + 1))) } -> 6
            else -> null
        }
    }

    /** Words of a label, also split where case changes ("YouTube" → you, tube). */
    private fun words(label: String): List<String> =
        label.split(' ', '-', '_', '.').filter { it.isNotEmpty() }
            .flatMap { w -> w.split(Regex("(?<=[a-z])(?=[A-Z])")) }
            .map { it.lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }

    private fun inOrder(needle: String, hay: String): Boolean {
        var i = 0
        for (ch in hay) if (i < needle.length && ch == needle[i]) i++
        return i == needle.length
    }

    /** True when [a] and [b] differ by at most one edit (insert, delete, change, or two letters swapped). */
    fun oneTypo(a: String, b: String): Boolean {
        if (a == b) return true
        val n = a.length; val m = b.length
        if (kotlin.math.abs(n - m) > 1) return false
        val d = Array(n + 1) { IntArray(m + 1) }
        for (x in 0..n) d[x][0] = x
        for (y in 0..m) d[0][y] = y
        for (x in 1..n) for (y in 1..m) {
            val cost = if (a[x - 1] == b[y - 1]) 0 else 1
            d[x][y] = minOf(d[x - 1][y] + 1, d[x][y - 1] + 1, d[x - 1][y - 1] + cost)
            if (x > 1 && y > 1 && a[x - 1] == b[y - 2] && a[x - 2] == b[y - 1]) d[x][y] = minOf(d[x][y], d[x - 2][y - 2] + 1)
        }
        return d[n][m] <= 1
    }
}

/** Thai Kedmanee ↔ English QWERTY: the same keys, for a query typed with the wrong layout on. */
object KeyboardSwap {
    private const val EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,./"
    private const val TH = "ๆไำพะัีรนยบลฟหกดเ้่าสวงผปแอิืทมใฝ"
    private val toEn = TH.indices.associate { TH[it] to EN[it] }
    private val toTh = EN.indices.associate { EN[it] to TH[it] }

    /** [q] as if typed on the other layout, or null when it isn't all from one layout. */
    fun convert(q: String): String? = when {
        q.isNotEmpty() && q.all { it in toEn } -> q.map { toEn.getValue(it) }.joinToString("")
        q.isNotEmpty() && q.all { it in toTh } && q.any { it.isLetter() } -> q.map { toTh.getValue(it) }.joinToString("")
        else -> null
    }
}
