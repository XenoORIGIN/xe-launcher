package com.xenoorigin.xelauncher.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.design.UiStyle
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class NothingWidgetTest {
    @get:Rule val rule = createComposeRule()
    private fun time() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun show(style: UiStyle, kind: GlassWidgetKind, span: Span, dark: Boolean = true) = rule.setContent {
        XeTheme(dark = dark, uiStyle = style) {
            TestBackdrop(black = true) {
                CompositionLocalProvider(LocalWidgetSpan provides span) {
                    Box(Modifier.size((span.x * 88).dp, (span.y * 96).dp)) { GlassWidget(kind) }
                }
            }
        }
    }

    @Test fun nothingClockIsDotMatrix() {
        show(UiStyle.NOTHING, GlassWidgetKind.CLOCK, Span(4, 2))
        rule.onNodeWithContentDescription(time()).assertExists()
    }

    @Test fun glassClockIsText() {
        show(UiStyle.GLASS, GlassWidgetKind.CLOCK, Span(4, 2))
        rule.onNodeWithText(time()).assertExists()
    }

    @Test fun nothingBatteryShowsDots() {
        show(UiStyle.NOTHING, GlassWidgetKind.BATTERY, Span(2, 2))
        rule.onNodeWithContentDescription("dot_ring").assertExists()
    }

    @Test fun nothingWidgetsShot() {
        rule.setContent {
            XeTheme(dark = false, uiStyle = UiStyle.NOTHING) {
                TestBackdrop(black = false) {
                    androidx.compose.foundation.layout.Column {
                        CompositionLocalProvider(LocalWidgetSpan provides Span(4, 2)) { Box(Modifier.size(352.dp, 190.dp)) { GlassWidget(GlassWidgetKind.CLOCK) } }
                        androidx.compose.foundation.layout.Row {
                            CompositionLocalProvider(LocalWidgetSpan provides Span(2, 2)) { Box(Modifier.size(176.dp, 190.dp)) { GlassWidget(GlassWidgetKind.BATTERY) } }
                            CompositionLocalProvider(LocalWidgetSpan provides Span(2, 2)) { Box(Modifier.size(176.dp, 190.dp)) { GlassWidget(GlassWidgetKind.STORAGE) } }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/nothing_widgets.png")
    }
}
