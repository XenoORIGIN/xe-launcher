package com.xenoorigin.xelauncher.bugs

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BugReportTest {
    private val device = DeviceInfo("samsung", "SM-A166B", "15", 35, "1.0.5", "1080x2340", "th-TH")

    @Test fun reportsGoToTheOwnersInbox() {
        assertThat(BugReport.EMAIL).isEqualTo("xenodevg@gmail.com")
    }

    @Test fun body_hasDescriptionDeviceAndOptionalParts() {
        val b = BugReport.body("Icons vanish", device, "style=GLASS_DARK", crash = "java.lang.X: boom", log = "line1")
        assertThat(b).contains("Icons vanish")
        assertThat(b).contains("SM-A166B"); assertThat(b).contains("Android 15 (API 35)"); assertThat(b).contains("1.0.5")
        assertThat(b).contains("style=GLASS_DARK"); assertThat(b).contains("java.lang.X: boom"); assertThat(b).contains("line1")
        val plain = BugReport.body("Hi", device, null, null, null)
        assertThat(plain).doesNotContain("Crash"); assertThat(plain).doesNotContain("Log")
    }

    @Test fun subject_marksCrashes() {
        assertThat(BugReport.subject("1.0.5", crash = false)).isEqualTo("Xe Launcher 1.0.5 — bug report")
        assertThat(BugReport.subject("1.0.5", crash = true)).isEqualTo("Xe Launcher 1.0.5 — crash report")
    }

    @Test fun intent_isAnEmailToTheOwner() {
        val i = BugReport.emailIntent("S", "B")
        assertThat(i.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(i.getStringArrayExtra(Intent.EXTRA_EMAIL)).asList().containsExactly("xenodevg@gmail.com")
        assertThat(i.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("S")
        assertThat(i.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo("B")
        assertThat(i.selector?.data.toString()).isEqualTo("mailto:")
    }
}
