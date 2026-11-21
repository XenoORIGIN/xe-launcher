package com.xenoorigin.xelauncher.badges

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BadgeCountsTest {
    private fun n(pkg: String, number: Int = 0, group: String? = null, summary: Boolean = false, clearable: Boolean = true, user: Long = 0) =
        Notif(pkg, user, number, group, summary, clearable)

    @Test fun eachNotificationCountsOne_orItsOwnNumberWhenTheAppGivesOne() {
        val c = badgeCounts(listOf(n("jp.naver.line.android"), n("jp.naver.line.android", number = 5), n("com.whatsapp")), own = "xe")
        assertThat(c[badgeKey("jp.naver.line.android", 0)]).isEqualTo(6)
        assertThat(c[badgeKey("com.whatsapp", 0)]).isEqualTo(1)
    }

    @Test fun ongoingNotifications_likeMusicOrDownloads_dontCount() {
        val c = badgeCounts(listOf(n("com.spotify.music", clearable = false)), own = "xe")
        assertThat(c).isEmpty()
    }

    @Test fun aGroupSummary_isNotCountedOnTopOfItsMessages() {
        val c = badgeCounts(listOf(n("line", group = "g", summary = true), n("line", group = "g"), n("line", group = "g")), own = "xe")
        assertThat(c[badgeKey("line", 0)]).isEqualTo(2)
    }

    @Test fun aSummaryThatKnowsTheRealTotal_wins() {
        val c = badgeCounts(listOf(n("line", number = 14, group = "g", summary = true), n("line", group = "g"), n("line", group = "g")), own = "xe")
        assertThat(c[badgeKey("line", 0)]).isEqualTo(14)
    }

    @Test fun aLoneSummary_countsAsOne() {
        assertThat(badgeCounts(listOf(n("gmail", group = "g", summary = true)), own = "xe")[badgeKey("gmail", 0)]).isEqualTo(1)
    }

    @Test fun xesOwnAlerts_andOtherProfiles_areKeptApart() {
        val c = badgeCounts(listOf(n("xe"), n("line", user = 0), n("line", user = 10)), own = "xe")
        assertThat(c.keys).containsExactly(badgeKey("line", 0), badgeKey("line", 10))
    }

    @Test fun labelsCapAt99plus() {
        assertThat(badgeLabel(7)).isEqualTo("7")
        assertThat(badgeLabel(99)).isEqualTo("99")
        assertThat(badgeLabel(250)).isEqualTo("99+")
    }
}
