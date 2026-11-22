package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import com.xenoorigin.xelauncher.widgets.GlassWidget
import com.xenoorigin.xelauncher.widgets.GlassWidgetKind
import com.xenoorigin.xelauncher.widgets.LocalWidgetSpan
import com.xenoorigin.xelauncher.widgets.Span
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi")
class WidgetEditOverlayTest {
    @get:Rule val rule = createComposeRule()
    private val placed = mutableListOf<Placement>()
    private var done = false

    private fun show(taken: Set<Pair<Int, Int>> = emptySet()) = rule.setContent {
        val px = with(LocalDensity.current) { 400.dp.toPx() }
        val grid = Rect(0f, with(LocalDensity.current) { 60.dp.toPx() }, px, with(LocalDensity.current) { 660.dp.toPx() }) // 4×6 cells of 100dp
        XeTheme {
            TestBackdrop(black = false) {
                WidgetEditOverlay(
                    WidgetCell(7, 0, 1, 2, 2, 0, "xe_clock"), grid, 4, 6, taken, Span(1, 1), Span(4, 4),
                    listOf(Span(2, 1), Span(2, 2), Span(4, 2), Span(4, 4)),
                    content = { s -> androidx.compose.runtime.CompositionLocalProvider(LocalWidgetSpan provides s) { GlassWidget(GlassWidgetKind.CLOCK) } },
                    onPlace = { placed += it }, onRemove = {}, onDone = { done = true },
                )
            }
        }
    }

    @Test fun handleDragResizesAndShowsLabel() {
        show()
        rule.onNodeWithTag("size_label", useUnmergedTree = true).assertTextEquals("2×2")
        rule.onNodeWithTag("handle_BOTTOM_RIGHT").performTouchInput {
            down(center); repeat(10) { moveBy(Offset(10.dp.toPx(), 0f)) }; up()
        }
        rule.waitForIdle()
        assertThat(placed.last()).isEqualTo(Placement(0, 1, 3, 2, true))
        rule.onNodeWithTag("size_label", useUnmergedTree = true).assertTextEquals("3×2")
    }

    @Test fun dropOnTakenCellsIsRefused() {
        show(taken = setOf(2 to 1))
        rule.onNodeWithTag("handle_BOTTOM_RIGHT").performTouchInput {
            down(center); repeat(10) { moveBy(Offset(10.dp.toPx(), 0f)) }; up()
        }
        rule.waitForIdle()
        assertThat(placed).isEmpty()
        rule.onNodeWithTag("size_label", useUnmergedTree = true).assertTextEquals("2×2")
    }

    @Test fun presetChipResizes() {
        show()
        rule.onNodeWithTag("preset_2").performClick()
        rule.waitForIdle()
        assertThat(placed.last()).isEqualTo(Placement(0, 1, 4, 2, true))
    }

    @Test fun doneFinishes() {
        show()
        rule.onNodeWithTag("widget_edit_done").performClick()
        assertThat(done).isTrue()
    }

    @Test fun overlayScreenshot() {
        show()
        rule.mainClock.advanceTimeBy(1000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/widget_edit.png")
    }
}
