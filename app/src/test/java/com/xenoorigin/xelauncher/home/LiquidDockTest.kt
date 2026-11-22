package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleEntry
import com.xenoorigin.xelauncher.icons.sampleRepo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click

class DockMagnifyMathTest {
    @Test fun scaleIsMaxUnderFinger() {
        assertThat(dockScale(100f, 100f, 80f)).isWithin(1e-4f).of(DOCK_MAX_SCALE)
        assertThat(dockScale(null, 100f, 80f)).isEqualTo(1f)
    }

    @Test fun scaleFallsOffWithDistance() {
        val near = dockScale(140f, 100f, 80f); val far = dockScale(220f, 100f, 80f)
        assertThat(near).isLessThan(DOCK_MAX_SCALE); assertThat(far).isLessThan(near)
        assertThat(dockScale(100f + 80f * 1.8f, 100f, 80f)).isEqualTo(1f)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class LiquidDockTest {
    @get:Rule val rule = createComposeRule()
    private val look = HomeLook(4, 6, 4, 60, false, IconShape.SQUIRCLE, { IconStyle.GLASS_DARK })
    private val dock = (0 until 4).map { AppCell(10L + it, it, 0, sampleEntry(it)) }

    @Test fun slidingAlongTheDockAndLiftingOpensTheAppUnderTheFinger() {
        val launched = mutableListOf<String>()
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    HomeScreen(
                        HomeLayout(listOf(emptyList()), dock), look, rememberPagerState { 1 }, false,
                        HomeCallbacks({ a, _ -> launched += a.label }, { _, _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {}),
                    )
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("dock").performTouchInput {
            val y = centerY
            down(Offset(width * .125f, y))
            repeat(12) { moveBy(Offset(width * .5f / 12, 0f)); advanceEventTime(16) }
            up()
        }
        rule.waitForIdle()
        assertThat(launched).containsExactly(sampleEntry(2).label)
    }

    @Test fun aPlainTapOnADockIconStillOpensIt() {
        val launched = mutableListOf<String>()
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    HomeScreen(
                        HomeLayout(listOf(emptyList()), dock), look, rememberPagerState { 1 }, false,
                        HomeCallbacks({ a, _ -> launched += a.label }, { _, _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {}),
                    )
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("dock").performTouchInput { click(Offset(width * .375f, centerY)) }
        rule.waitForIdle()
        assertThat(launched).containsExactly(sampleEntry(1).label)
    }
}
