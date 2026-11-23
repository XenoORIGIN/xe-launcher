package com.xenoorigin.xelauncher.termi

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.widgets.GlassWidgetKind
import com.xenoorigin.xelauncher.widgets.LocalWidgetActions
import com.xenoorigin.xelauncher.widgets.LocalWidgetSpan
import com.xenoorigin.xelauncher.widgets.Span
import com.xenoorigin.xelauncher.widgets.WidgetActions
import com.xenoorigin.xelauncher.widgets.clamp
import com.xenoorigin.xelauncher.widgets.presetSpans
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class TermiWidgetTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private var opened = 0
    private fun show(span: Span, store: TermiStore = TermiStore(tmp.root.resolve("t${System.nanoTime()}.json"))): TermiStore {
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(
                    LocalWidgetSpan provides span,
                    LocalWidgetActions provides WidgetActions(termi = store, openTermi = { opened++ }, animate = false),
                ) { TermiWidget(span, Modifier.size((span.x * 80).dp, (span.y * 90).dp)) }
            }
        }
        rule.waitForIdle()
        return store
    }

    @Test fun layoutBySize() {
        assertThat(termiLayout(Span(2, 2))).isEqualTo(TermiLayout.SMALL)
        assertThat(termiLayout(Span(2, 4))).isEqualTo(TermiLayout.SMALL)
        assertThat(termiLayout(Span(3, 2))).isEqualTo(TermiLayout.BUTTONS)
        assertThat(termiLayout(Span(4, 2))).isEqualTo(TermiLayout.BUTTONS)
        assertThat(termiLayout(Span(3, 3))).isEqualTo(TermiLayout.BUTTONS)
        assertThat(termiLayout(Span(4, 4))).isEqualTo(TermiLayout.FULL)
    }

    @Test fun sizesOnHome() {
        assertThat(GlassWidgetKind.TERMI.presetSpans(4, 6)).containsExactly(Span(2, 2), Span(4, 2), Span(4, 4)).inOrder()
        assertThat(GlassWidgetKind.TERMI.clamp(Span(1, 1))).isEqualTo(Span(2, 2))
        assertThat(GlassWidgetKind.of("xe_termi")).isEqualTo(GlassWidgetKind.TERMI)
    }

    @Test fun smallShowsOnlyTermi() {
        show(Span(2, 2))
        assertThat(rule.onAllNodesWithTag("termi_feed").fetchSemanticsNodes()).isEmpty()
        rule.onNodeWithTag("termi_view").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test fun mediumHasButtonsNoBars() {
        val s = show(Span(4, 2))
        assertThat(rule.onAllNodesWithTag("termi_bars").fetchSemanticsNodes()).isEmpty()
        val before = s.current().food
        rule.onNodeWithTag("termi_feed").performClick()
        assertThat(s.current().food).isGreaterThan(before)
        rule.onNodeWithTag("termi_play").assertExists()
    }

    @Test fun largeHasBarsAndButtons() {
        show(Span(4, 4))
        rule.onNodeWithTag("termi_bars").assertExists()
        rule.onNodeWithTag("termi_sleep").assertExists()
    }

    @Test fun sleepButtonBecomesWake() {
        show(Span(4, 2))
        rule.onNodeWithTag("termi_sleep").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Wake").assertExists()
    }
}
