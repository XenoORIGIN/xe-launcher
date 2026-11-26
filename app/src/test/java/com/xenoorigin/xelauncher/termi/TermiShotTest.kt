package com.xenoorigin.xelauncher.termi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import com.xenoorigin.xelauncher.widgets.LocalWidgetActions
import com.xenoorigin.xelauncher.widgets.LocalWidgetSpan
import com.xenoorigin.xelauncher.widgets.Span
import com.xenoorigin.xelauncher.widgets.WidgetActions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Review pictures of the Termi widget at each Home size and of its page (written to SHOT_DIR). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class TermiShotTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun store() = TermiStore(tmp.root.resolve("t${System.nanoTime()}.json"), defaultCreators = { setOf("com.termux") }).also {
        it.addXpForTest(150) // level 3: cap and coffee unlocked
        it.equip(Item.CAP, Item.CAP.slot)
    }

    private fun widget(span: Span, name: String) {
        val s = store()
        rule.setContent {
            XeTheme {
                TestBackdrop(black = true) {
                    CompositionLocalProvider(
                        LocalWidgetSpan provides span,
                        LocalWidgetActions provides WidgetActions(termi = s, openTermi = {}, animate = false),
                    ) { Box(Modifier.padding(12.dp)) { TermiWidget(span, Modifier.size((span.x * 88).dp, (span.y * 96).dp)) } }
                }
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/$name")
    }

    @Test fun widget2x2() = widget(Span(2, 2), "termi_widget_2x2.png")
    @Test fun widget4x2() = widget(Span(4, 2), "termi_widget_4x2.png")
    @Test fun widget4x4() = widget(Span(4, 4), "termi_widget_4x4.png")

    @Test fun page() {
        val s = store()
        rule.setContent {
            XeTheme {
                TestBackdrop(black = true) {
                    Box(Modifier.fillMaxSize()) {
                        TermiScreen(s, listOf("com.termux" to "Termux", "com.instagram.android" to "Instagram"), false, {}, {}, 0xFFFFFFFF, AsciiColor.MONO, animate = false)
                    }
                }
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/termi_page.png")
    }
}
