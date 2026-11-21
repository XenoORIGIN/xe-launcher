package com.xenoorigin.xelauncher.play

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleRepo
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class DrawPadTest {
    @get:Rule val rule = createComposeRule()

    private fun app(i: Int, label: String) = AppEntry(AppKey("pkg.${i % 4}", "pkg.${i % 4}.Main", 700L + i), label, -1, 0, false)
    private val apps = listOf(app(0, "LINE"), app(1, "Discord"), app(2, "Notion"), app(3, "Netflix"))

    private fun show(launched: MutableList<String>) {
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    TestBackdrop(black = true) {
                        DrawPad(apps, mapOf(apps[3].key.encode() to 9), { IconStyle.GLASS_DARK }, IconShape.SQUIRCLE, { a, _ -> launched += a.label }, {})
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun drawingAnL_opensTheOnlyAppStartingWithL() {
        val launched = mutableListOf<String>()
        show(launched)
        rule.onNodeWithTag("drawPad").performTouchInput {
            down(Offset(300f, 500f))
            repeat(20) { moveBy(Offset(0f, 30f)); advanceEventTime(12) }
            repeat(12) { moveBy(Offset(30f, 0f)); advanceEventTime(12) }
            up()
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        rule.onNodeWithTag("drawnLetter", useUnmergedTree = true).assertTextEquals("L")
        assertThat(launched).containsExactly("LINE")
    }

    @Test fun drawingAnN_offersTheMatches_mostUsedFirst_withoutOpeningOne() {
        val launched = mutableListOf<String>()
        show(launched)
        rule.onNodeWithTag("drawPad").performTouchInput {
            down(Offset(300f, 1100f))
            repeat(20) { moveBy(Offset(0f, -30f)); advanceEventTime(12) }
            repeat(20) { moveBy(Offset(18f, 30f)); advanceEventTime(12) }
            repeat(20) { moveBy(Offset(0f, -30f)); advanceEventTime(12) }
            up()
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        rule.onNodeWithTag("drawnLetter", useUnmergedTree = true).assertTextEquals("N")
        assertThat(launched).isEmpty()
        assertThat(appsForLetter(apps, 'N', mapOf(apps[3].key.encode() to 9)).map { it.label }).containsExactly("Netflix", "Notion").inOrder()
    }
}
