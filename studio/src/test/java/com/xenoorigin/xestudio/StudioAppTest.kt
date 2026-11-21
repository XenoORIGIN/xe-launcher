package com.xenoorigin.xestudio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StudioAppTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun s(id: Int) = ctx.getString(id)

    /** Lets work posted to the main thread (applying a theme) and the following animations finish. */
    private fun settle() = repeat(20) {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        rule.mainClock.advanceTimeBy(100); rule.waitForIdle()
    }

    @Test fun fourTabsShowAndSwitch() {
        rule.setContent { StudioApp(StudioGraph(ctx)) }
        for (t in listOf("tab_create", "tab_store", "tab_mine", "tab_profile")) rule.onNodeWithTag(t).assertExists()
        rule.onNodeWithTag("tab_mine").performClick()
        rule.onNodeWithText(s(R.string.mine_empty)).assertExists()
        rule.onNodeWithTag("tab_store").performClick()
        rule.onNodeWithText(s(R.string.coming_soon)).assertExists()
    }

    @Test fun mineListsProjects() {
        val g = StudioGraph(ctx)
        kotlinx.coroutines.runBlocking { g.projects.create(com.xenoorigin.xestudio.projects.ProjectKind.ICONS, "Midnight set", "{}") }
        rule.setContent { StudioApp(g) }
        rule.onNodeWithTag("tab_mine").performClick()
        rule.onNodeWithText("Midnight set").assertExists()
    }

    @Test fun createOffersIconTheme() {
        rule.setContent { StudioApp(StudioGraph(ctx)) }
        rule.onNodeWithText(s(R.string.new_icon_theme)).assertExists()
    }

    @Test fun newIconThemeCreatesProjectAndOpensEditor() {
        val g = StudioGraph(ctx)
        rule.setContent { StudioApp(g) }
        rule.onNodeWithText(s(R.string.new_icon_theme)).performClick()
        rule.waitUntil(3_000) { g.projects.projects.value.isNotEmpty() }
        rule.onNodeWithTag("apply_xe").assertExists()
        rule.onNodeWithTag("editor_back").performClick()
        rule.onNodeWithTag("tab_create").assertExists()
    }

    @Test fun applyWithoutXeExplainsHowToGetIt() {
        val g = StudioGraph(ctx)
        rule.setContent { StudioApp(g) }
        rule.onNodeWithText(s(R.string.new_icon_theme)).performClick()
        rule.waitUntil(3_000) { g.projects.projects.value.isNotEmpty() }
        rule.onNodeWithTag("apply_xe").performClick()
        settle()
        org.junit.Assert.assertEquals(com.xenoorigin.xestudio.link.ApplyResult.XE_MISSING, g.applyResult.value)
        settle()
        rule.onNodeWithTag("xe_missing", useUnmergedTree = true).assertExists()
        rule.onNodeWithText(s(R.string.get_xe)).assertExists()
    }

    @Test fun shellScreenshot() {
        rule.setContent { StudioApp(StudioGraph(ctx)) }
        rule.onRoot().captureRoboImage("src/test/screenshots/studio_shell.png")
    }
}
