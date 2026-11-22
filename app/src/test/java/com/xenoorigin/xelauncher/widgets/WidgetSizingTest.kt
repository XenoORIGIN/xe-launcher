package com.xenoorigin.xelauncher.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w520dp-h1700dp-xxhdpi")
class WidgetSizingTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyWidgetFitsEveryAllowedSize() {
        for (k in GlassWidgetKind.entries) {
            assertThat(k.clamp(Span(1, 1))).isEqualTo(Span(k.minX, k.minY))
            assertThat(k.clamp(Span(9, 9))).isEqualTo(Span(4, 4))
        }
        assertThat(GlassWidgetKind.CLOCK.clamp(Span(1, 1))).isEqualTo(Span(1, 1))
        assertThat(GlassWidgetKind.MONTH.clamp(Span(1, 1))).isEqualTo(Span(2, 2))
    }

    @Test fun presetsFitTheGridAndGrow() {
        for (k in GlassWidgetKind.entries) for (cols in listOf(4, 5)) {
            val p = k.presetSpans(cols, rows = 6)
            assertThat(p.size).isIn(2..4)
            assertThat(p.toSet()).hasSize(p.size)
            p.forEach { assertThat(it.x).isAtMost(cols); assertThat(it.x).isAtLeast(k.minX); assertThat(it.y).isAtLeast(k.minY) }
            assertThat(p.zipWithNext().all { (a, b) -> a.x * a.y < b.x * b.y }).isTrue()
        }
    }

    /** Each widget at small, wide, square and big sizes, for review. */
    @Test fun sizesScreenshot() {
        val cell = 88.dp
        val sizes = listOf(Span(1, 1), Span(2, 1), Span(2, 2), Span(4, 2))
        rule.setContent {
            XeTheme {
                TestBackdrop(black = false) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(GlassWidgetKind.CLOCK, GlassWidgetKind.DATE, GlassWidgetKind.BATTERY, GlassWidgetKind.SUGGESTED, GlassWidgetKind.NOTE).forEach { k ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                sizes.take(3).forEach { s ->
                                    val c = k.clamp(s)
                                    CompositionLocalProvider(LocalWidgetSpan provides c) { Box(Modifier.size(cell * c.x, cell * c.y)) { GlassWidget(k) } }
                                }
                            }
                        }
                        listOf(GlassWidgetKind.CLOCK, GlassWidgetKind.BATTERY).forEach { k ->
                            CompositionLocalProvider(LocalWidgetSpan provides Span(4, 2)) { Box(Modifier.size(cell * 4, cell * 2)) { GlassWidget(k) } }
                        }
                    }
                }
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/widgets_sizes.png")
    }
}
