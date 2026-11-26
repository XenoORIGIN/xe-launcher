package com.xenoorigin.xelauncher.search

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import org.junit.Test

/** 2.0: easier app search — initials, letters in order, small typos, and the other keyboard layout. */
class SmartSearchTest {
    private fun a(label: String, pkg: String = "p.${label.hashCode()}") = AppEntry(AppKey(pkg, "$pkg.M", 0), label, -1, 0, false)
    private val apps = listOf(a("Google Maps"), a("Messenger"), a("Telegram"), a("WhatsApp"), a("YouTube Music"), a("Settings"), a("ไลน์"), a("Microsoft Outlook"))
    private fun labels(q: String) = SearchEngine.search(q, apps, emptyList(), web = false).filterIsInstance<SearchResult.App>().map { it.entry.label }

    @Test fun initialsFindMultiWordApps() {
        assertThat(labels("gm")).contains("Google Maps")
        assertThat(labels("ytm").first()).isEqualTo("YouTube Music")
    }

    @Test fun lettersInOrderFindTheApp() {
        assertThat(labels("wtsap")).contains("WhatsApp")
    }

    @Test fun oneTypoStillFinds() {
        assertThat(labels("telgram")).contains("Telegram")
        assertThat(labels("setings")).contains("Settings")
    }

    @Test fun typedOnTheThaiLayoutByMistake() {
        // "telegram" typed with the Thai Kedmanee keyboard on
        assertThat(labels("ะำสำเพฟท")).contains("Telegram")
    }

    @Test fun exactPrefixStillComesFirst() {
        assertThat(labels("me").first()).isEqualTo("Messenger")
    }

    @Test fun nonsenseFindsNothing() {
        assertThat(labels("zzqx")).isEmpty()
    }
}
