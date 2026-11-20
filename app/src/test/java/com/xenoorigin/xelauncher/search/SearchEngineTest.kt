package com.xenoorigin.xelauncher.search

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import org.junit.Test

private fun a(label: String, pkg: String = "p.${label.hashCode()}") = AppEntry(AppKey(pkg, "$pkg.M", 0), label, -1, 0, false)

class SearchEngineTest {
    private val apps = listOf(a("ไลน์"), a("Hotel Finder"), a("Telegram"), a("กล้อง"), a("Google", "com.google.android.googlequicksearchbox"))
    private fun labels(q: String) = SearchEngine.search(q, apps, emptyList()).filterIsInstance<SearchResult.App>().map { it.entry.label }

    @Test fun thaiSubstring_matches() { assertThat(labels("ไล")).containsExactly("ไลน์") }
    @Test fun prefixBeatsSubstring_caseInsensitive() { assertThat(labels("TEL")).containsExactly("Telegram", "Hotel Finder").inOrder() }
    @Test fun packageName_matchesLast() { assertThat(labels("quicksearch")).containsExactly("Google") }
    @Test fun blank_returnsNothing() = assertThat(SearchEngine.search("  ", apps, emptyList())).isEmpty()

    @Test fun calculator_firstWhenExpression() {
        val r = SearchEngine.search("2+3×4", apps, emptyList())
        assertThat(r.first()).isEqualTo(SearchResult.Calc("2+3×4", "14"))
    }

    @Test fun web_alwaysLast() {
        val r = SearchEngine.search("tel", apps, emptyList())
        assertThat(r.last()).isEqualTo(SearchResult.Web("tel"))
    }

    @Test fun calcAndWeb_canBeTurnedOff() {
        val r = SearchEngine.search("1+1", apps, emptyList(), calculator = false, web = false)
        assertThat(r).isEmpty()
    }
}
