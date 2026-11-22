package com.xenoorigin.xelauncher.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
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
@Config(sdk = [34], qualifiers = "w390dp-h1400dp-xxhdpi")
class GlassWidgetsScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun allWidgets() {
        rule.setContent {
            XeTheme {
                TestBackdrop(black = false) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(GlassWidgetKind.CLOCK, GlassWidgetKind.SEARCH, GlassWidgetKind.MONTH).forEach { k ->
                            Box(Modifier.fillMaxWidth().aspectRatio(k.spanX / (k.spanY * 1.05f))) { GlassWidget(k) }
                        }
                        GlassWidgetKind.entries.filter { it.spanX == 2 }.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { k -> Box(Modifier.weight(1f).aspectRatio(1f / 1.05f)) { GlassWidget(k) } }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/widgets_all.png")
    }

    @Test fun allWidgetsLiquid() {
        rule.setContent {
            XeTheme {
                com.xenoorigin.xelauncher.testutil.LiquidBackdrop(com.xenoorigin.xelauncher.testutil.xeWall("violet_glass", 1170, 4200)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(GlassWidgetKind.CLOCK, GlassWidgetKind.SEARCH, GlassWidgetKind.MONTH).forEach { k ->
                            Box(Modifier.fillMaxWidth().aspectRatio(k.spanX / (k.spanY * 1.05f))) { GlassWidget(k) }
                        }
                        GlassWidgetKind.entries.filter { it.spanX == 2 }.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { k -> Box(Modifier.weight(1f).aspectRatio(1f / 1.05f)) { GlassWidget(k) } }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/widgets_liquid.png")
    }
}
