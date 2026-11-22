package com.xenoorigin.xelauncher.apps

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.categories.AppSorting
import com.xenoorigin.xelauncher.settings.DrawerSort
import org.junit.Test

class UsageTimeTest {
    private val now = 10_000_000L

    @Test fun usesForegroundTimeWhenAllowed() {
        val u = UsageTime({ mapOf("com.line" to 3_600_000L, "com.calc" to 60_000L) }, clock = { now })
        // Opened often but briefly loses to opened rarely but for long.
        assertThat(u.score("com.line", launches = 2)).isGreaterThan(u.score("com.calc", launches = 50))
        assertThat(u.usingTime).isTrue()
    }

    @Test fun fallsBackToLaunchCountsWithoutAccess() {
        val u = UsageTime({ null }, clock = { now })
        assertThat(u.score("com.line", launches = 2)).isLessThan(u.score("com.calc", launches = 50))
        assertThat(u.usingTime).isFalse()
    }

    @Test fun accessRevokedMidSessionFallsBack() {
        var allowed = true
        var t = now
        val u = UsageTime({ if (allowed) mapOf("a" to 5L) else null }, clock = { t })
        assertThat(u.usingTime).isTrue()
        allowed = false
        t += UsageTime.REFRESH_MS + 1
        assertThat(u.score("a", launches = 9)).isEqualTo(9L)
        assertThat(u.usingTime).isFalse()
    }

    @Test fun sortingByUseFollowsTimeSpent() {
        fun app(p: String) = AppEntry(AppKey(p, "$p.Main", 0), p, -1, 0L, false)
        val apps = listOf(app("com.calc"), app("com.line"))
        val sorted = AppSorting.sort(apps, DrawerSort.MOST_USED, emptyMap(), usage = mapOf("com.line" to 100L, "com.calc" to 1L))
        assertThat(sorted.map { it.key.packageName }).containsExactly("com.line", "com.calc").inOrder()
    }

    @Test fun staleReadingIsDoneInTheBackgroundNotByTheCaller() {
        val queued = mutableListOf<() -> Unit>()
        var reads = 0; var changed = 0
        val u = UsageTime({ reads++; mapOf("a" to 5L) }, clock = { now }, runner = { queued += it }, onChanged = { changed++ })
        assertThat(u.usage()).isNull() // nothing read yet, and the caller didn't wait for it
        assertThat(reads).isEqualTo(0)
        u.usage() // a second ask while the first read is pending doesn't queue another
        assertThat(queued).hasSize(1)
        queued.removeAt(0)()
        assertThat(reads).isEqualTo(1)
        assertThat(changed).isEqualTo(1)
        assertThat(u.usage()).containsExactly("a", 5L)
    }

    @Test fun emptyReadingCountsAsNoAccess() {
        // Some phones answer with nothing at all even though access is on: keep using launch counts.
        val u = UsageTime({ emptyMap() }, clock = { now })
        assertThat(u.usingTime).isFalse()
        assertThat(u.score("a", launches = 4)).isEqualTo(4L)
    }

    @Test fun refreshAfterGrantReadsAgainRightAway() {
        var allowed = false
        val u = UsageTime({ if (allowed) mapOf("a" to 7L) else null }, clock = { now })
        assertThat(u.usingTime).isFalse()
        allowed = true
        assertThat(u.usingTime).isFalse() // still cached
        u.refresh()
        assertThat(u.usingTime).isTrue()
    }
}
