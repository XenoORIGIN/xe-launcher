package com.xenoorigin.xelauncher.drawer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.categories.BuiltInCategory
import com.xenoorigin.xelauncher.categories.CategoryWithApps
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleRepo
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class DrawerPlayTest {
    @get:Rule val rule = createComposeRule()

    private val kplus = AppEntry(AppKey("pkg.1", "pkg.1.Main", 900), "K PLUS", -1, 0, false)
    private val truemoney = AppEntry(AppKey("pkg.3", "pkg.3.Main", 901), "TrueMoney", -1, 0, false)

    private fun cats(): List<CategoryWithApps> = sampleCategories().mapIndexed { i, c ->
        if (c.builtIn == BuiltInCategory.FINANCE) c.copy(apps = listOf(kplus, truemoney)) else c
    }

    private fun show(state: DrawerState, play: DrawerPlay, launched: MutableList<String> = mutableListOf()) {
        val repo = sampleRepo()
        val cats = cats()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    TestBackdrop(black = true) {
                        AppDrawer(cats, state, DrawerLook({ IconStyle.GLASS_DARK }, IconShape.SQUIRCLE, 60, true, true),
                            { a, _ -> launched += a.label }, { _, _ -> }, {}, {}, {}, play = play)
                    }
                }
            }
        }
        rule.waitForIdle(); Thread.sleep(300); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
    }

    private val financeIndex = BuiltInCategory.entries.indexOf(BuiltInCategory.FINANCE)

    @Test fun sparseCategory_bigCentredIcons_glow_andDust() {
        val state = DrawerState(initial = financeIndex, pageCount = { cats().size })
        show(state, DrawerPlay(glow = true, dust = { if (it.label == "TrueMoney") .8f else 0f }))
        rule.onRoot().captureRoboImage("$SHOT_DIR/drawer_sparse_play.png")
        // Two apps: they share the width instead of hugging the left edge.
        val b = rule.onNodeWithContentDescription("K PLUS").getBoundsInRoot()
        val t = rule.onNodeWithContentDescription("TrueMoney").getBoundsInRoot()
        assertThat(b.left.value).isGreaterThan(40f)
        assertThat((t.right - t.left).value).isGreaterThan(70f) // 60dp × 1.32
        rule.onNodeWithTag("appCount", useUnmergedTree = true).assertExists()
    }

    @Test fun draggingAlongTheBar_scrubsThroughCategories() {
        val state = DrawerState(initial = 0, pageCount = { cats().size })
        show(state, DrawerPlay(magnify = true))
        val bar = rule.onNodeWithTag("categoryBar").getBoundsInRoot()
        rule.onNodeWithTag("categoryBar").performTouchInput {
            val y = height / 2f
            down(Offset(60f, y))
            repeat(12) { moveBy(Offset(28f, 0f)); advanceEventTime(16) }
            up()
        }
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        assertThat(state.pager.targetPage).isAtLeast(2)
        assertThat((bar.right - bar.left).value).isGreaterThan(0f)
    }

    @Test fun tappingACategory_stillPicksIt() {
        val state = DrawerState(initial = 0, pageCount = { cats().size })
        show(state, DrawerPlay(magnify = true))
        rule.onNodeWithContentDescription(cats()[2].title).performClick()
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        assertThat(state.pager.targetPage).isEqualTo(2)
    }

    @Test fun holdAndRub_wipesDust_withoutOpeningTheApp_orTurningThePage() {
        val state = DrawerState(initial = financeIndex, pageCount = { cats().size })
        val wiped = mutableListOf<String>(); val launched = mutableListOf<String>()
        show(state, DrawerPlay(dust = { if (it.label == "K PLUS") 1f else 0f }, onWiped = { wiped += it.label }), launched)
        rule.onAllNodesWithContentDescription("K PLUS").onFirst().performTouchInput {
            down(center)
            advanceEventTime(220)
            repeat(16) { k -> moveBy(Offset(if (k % 2 == 0) 90f else -90f, 6f)); advanceEventTime(30) }
            up()
        }
        rule.mainClock.advanceTimeBy(800); rule.waitForIdle()
        assertThat(wiped).containsExactly("K PLUS")
        assertThat(launched).isEmpty()
        assertThat(state.pager.targetPage).isEqualTo(financeIndex)
    }

    @Test fun aQuickSwipeOnADustyIcon_stillTurnsThePage() {
        val state = DrawerState(initial = financeIndex, pageCount = { cats().size })
        val wiped = mutableListOf<String>()
        show(state, DrawerPlay(dust = { 1f }, onWiped = { wiped += it.label }))
        rule.onAllNodesWithContentDescription("K PLUS").onFirst().performTouchInput {
            down(center)
            repeat(8) { moveBy(Offset(-60f, 0f)); advanceEventTime(10) }
            up()
        }
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        assertThat(wiped).isEmpty()
        assertThat(state.pager.targetPage).isEqualTo(financeIndex + 1)
    }

    @Test fun tappingADustyIcon_opensIt() {
        val state = DrawerState(initial = financeIndex, pageCount = { cats().size })
        val launched = mutableListOf<String>()
        show(state, DrawerPlay(dust = { 1f }), launched)
        rule.onAllNodesWithContentDescription("TrueMoney").onFirst().performClick()
        rule.waitForIdle()
        assertThat(launched).containsExactly("TrueMoney")
    }

    @Test fun withThePondOn_swipesStillTurnPages_andIconsStillOpen() {
        val state = DrawerState(initial = financeIndex, pageCount = { cats().size })
        val launched = mutableListOf<String>()
        show(state, DrawerPlay(pond = true), launched)
        val grid = rule.onAllNodesWithTag("drawerGrid").onFirst()
        // A tap on empty water: ripples, nothing opens.
        rule.onNodeWithTag("categoryBar").assertExists()
        rule.onAllNodesWithContentDescription("K PLUS").onFirst().performClick()
        rule.waitForIdle()
        assertThat(launched).containsExactly("K PLUS")
        rule.onRoot().performTouchInput { click(Offset(width / 2f, height * .7f)) }
        rule.waitForIdle()
        assertThat(launched).containsExactly("K PLUS")
        rule.onRoot().performTouchInput {
            down(Offset(width * .8f, height * .6f))
            repeat(8) { moveBy(Offset(-70f, 0f)); advanceEventTime(10) }
            up()
        }
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        assertThat(state.pager.targetPage).isEqualTo(financeIndex + 1)
        grid.assertExists()
    }
}
