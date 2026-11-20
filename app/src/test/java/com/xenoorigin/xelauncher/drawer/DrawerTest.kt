package com.xenoorigin.xelauncher.drawer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.XeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class DrawerTest {
    @get:Rule val rule = createComposeRule()

    private fun pagerWith(state: DrawerState, speed: AnimationSpeed, scopeOut: (CoroutineScope) -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalAnimationSpeed provides speed) {
                scopeOut(rememberCoroutineScope())
                HorizontalPager(state.pager, Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize()) }
            }
        }
    }

    @Test fun animateTo_landsOnPage_andBubbleFollows() {
        val state = DrawerState(pageCount = { 6 })
        lateinit var scope: CoroutineScope
        pagerWith(state, AnimationSpeed.NORMAL) { scope = it }
        rule.runOnIdle { scope.launch { state.animateTo(3, AnimationSpeed.NORMAL) } }
        rule.waitForIdle()
        assertThat(state.pager.currentPage).isEqualTo(3)
        assertThat(state.bubbleOffsetFraction).isWithin(0.001f).of(3f)
    }

    @Test fun animationOff_snapsInOneFrame() {
        val state = DrawerState(pageCount = { 6 })
        lateinit var scope: CoroutineScope
        pagerWith(state, AnimationSpeed.OFF) { scope = it }
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { scope.launch { state.animateTo(4, AnimationSpeed.OFF) } }
        rule.mainClock.advanceTimeByFrame(); rule.mainClock.advanceTimeByFrame()
        assertThat(state.pager.currentPage).isEqualTo(4)
    }

    @Test fun stretch_isZeroAtRest_andPeaksMidway() {
        assertThat(bubbleStretch(2f)).isWithin(0.001f).of(0f)
        assertThat(bubbleStretch(2.5f)).isWithin(0.001f).of(0.35f)
    }

    private fun barStaysInside(fontScale: Float) {
        val cats = sampleCategories()
        rule.setContent {
            XeTheme(fontScale = fontScale) {
                CategoryBar(cats, DrawerState(pageCount = { cats.size }), onSelect = {}, modifier = Modifier)
            }
        }
        val bar = rule.onNodeWithTag("categoryBar").getBoundsInRoot()
        val buttons = rule.onAllNodesWithTag("categoryButton", useUnmergedTree = true).fetchSemanticsNodes()
        assertThat(buttons).hasSize(cats.size)
        buttons.forEach { n ->
            val b = n.boundsInRoot
            val d = rule.density
            assertThat(b.left).isAtLeast(with(d) { bar.left.toPx() } - 0.5f)
            assertThat(b.right).isAtMost(with(d) { bar.right.toPx() } + 0.5f)
            assertThat(b.top).isAtLeast(with(d) { bar.top.toPx() } - 0.5f)
            assertThat(b.bottom).isAtMost(with(d) { bar.bottom.toPx() } + 0.5f)
        }
    }

    @Test fun categoryBar_buttonsStayInsideBar() = barStaysInside(1f)
    @Test fun categoryBar_buttonsStayInsideBar_largeFont() = barStaysInside(1.3f)
}
