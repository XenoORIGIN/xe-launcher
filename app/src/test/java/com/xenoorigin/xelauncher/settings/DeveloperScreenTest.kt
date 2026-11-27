package com.xenoorigin.xelauncher.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.ui.AboutScreen
import com.xenoorigin.xelauncher.settings.ui.DeveloperScreen
import com.xenoorigin.xelauncher.termi.TermiRules
import com.xenoorigin.xelauncher.termi.TermiStore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DeveloperScreenTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    @Test fun sevenTapsOnTheVersionTurnOnDeveloperMode() {
        var s = XeSettings()
        rule.setContent { XeTheme { AboutScreen({}, s, { f -> s = f(s) }) } }
        repeat(7) { rule.onNodeWithTag("about_version", useUnmergedTree = true).performClick() }
        assertThat(s.devMode).isTrue()
    }

    @Test fun plusRaisesTermisLevel_andResetNeedsTwoTaps() {
        val store = TermiStore(tmp.root.resolve("t.json"), defaultCreators = { emptySet() })
        var s = XeSettings(devMode = true)
        rule.setContent { XeTheme { DeveloperScreen({}, s, { f -> s = f(s) }, store) } }
        repeat(3) { rule.onNodeWithTag("dev_level_plus").performClick() }
        assertThat(TermiRules.level(store.current().xp)).isEqualTo(4)
        rule.onNodeWithTag("dev_reset").performClick()
        assertThat(TermiRules.level(store.current().xp)).isEqualTo(4) // first tap only asks
        rule.onNodeWithText("Tap again to reset").assertExists()
        rule.onNodeWithTag("dev_reset").performClick()
        assertThat(TermiRules.level(store.current().xp)).isEqualTo(1)
    }

    @Test fun turningDeveloperModeOffHidesItAgain() {
        val store = TermiStore(tmp.root.resolve("t2.json"), defaultCreators = { emptySet() })
        var s = XeSettings(devMode = true)
        rule.setContent { XeTheme { DeveloperScreen({}, s, { f -> s = f(s) }, store) } }
        rule.onNodeWithTag("dev_mode_switch").performClick()
        assertThat(s.devMode).isFalse()
    }
}
