package com.xenoorigin.xelauncher.settings

import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.ui.AboutScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w390dp-h844dp-xxhdpi")
class FunScreensTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tappingTheNameSevenTimes_unlocksSupernova() {
        var s = XeSettings()
        rule.setContent { XeTheme { AboutScreen({}, s, { f -> s = f(s) }) } }
        repeat(6) { rule.onNodeWithText("Xe Launcher").performClick() }
        assertThat(s.novaUnlocked).isFalse()
        rule.onNodeWithText("Xe Launcher").performClick()
        rule.waitForIdle()
        assertThat(s.novaUnlocked).isTrue()
    }

    @Test fun bugReportIntent_opensAnEmailToTheDeveloper() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        app.startActivity(com.xenoorigin.xelauncher.bugs.BugReport.emailIntent("S", "B").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val started = shadowOf(app).nextStartedActivity
        assertThat(started.getStringArrayExtra(Intent.EXTRA_EMAIL)).asList().contains("xenodevg@gmail.com")
    }
}
