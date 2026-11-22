package com.xenoorigin.xelauncher.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
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

/** A small Android 11 phone with big text (like the owner's dad's): the edit toolbar must still fit. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-xhdpi")
class WidgetEditNarrowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun removeAndDoneStayOnScreenWithLargeText() {
        var removed = false
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 1.6f)) {
                val px = with(LocalDensity.current) { 320.dp.toPx() }
                val grid = Rect(0f, with(LocalDensity.current) { 60.dp.toPx() }, px, with(LocalDensity.current) { 560.dp.toPx() })
                XeTheme {
                    TestBackdrop(black = false) {
                        // A 1×1 battery widget in the middle, as in the photo.
                        WidgetEditOverlay(
                            WidgetCell(7, 2, 3, 1, 1, 0, "xe_battery"), grid, 4, 6, emptySet(), Span(1, 1), Span(4, 4),
                            listOf(Span(1, 1), Span(2, 2), Span(4, 2)),
                            content = { s -> CompositionLocalProvider(LocalWidgetSpan provides s) { GlassWidget(GlassWidgetKind.BATTERY) } },
                            onPlace = {}, onRemove = { removed = true }, onDone = {},
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        val screen = rule.onRoot().fetchSemanticsNode().boundsInRoot
        listOf("widget_edit_remove", "widget_edit_done").forEach { tag ->
            val r = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertThat(r.right).isAtMost(screen.right)
            assertThat(r.left).isAtLeast(screen.left)
            // Full size, not squeezed by the size chips.
            assertThat(r.width).isWithin(1f).of(with(rule.density) { 38.dp.toPx() })
            assertThat(r.height).isWithin(1f).of(with(rule.density) { 38.dp.toPx() })
        }
        rule.onNodeWithTag("widget_edit_remove").performClick()
        assertThat(removed).isTrue()
    }
}
