package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.design.rimDegrees
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleEntry
import com.xenoorigin.xelauncher.icons.sampleRepo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class GlassLensTest {
    @get:Rule val rule = createComposeRule()
    private val look = HomeLook(4, 6, 4, 60, true, IconShape.SQUIRCLE, { IconStyle.GLASS_DARK })
    private val layout = HomeLayout(listOf(listOf(AppCell(1, 0, 0, sampleEntry(0)))), emptyList())
    private var emptyHolds = 0
    private var itemMenus = 0
    private val lens = LensState({ null }, { Offset.Zero })

    private fun show() {
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo, LocalLens provides lens) {
                    HomeScreen(
                        layout, look, rememberPagerState { 1 }, false,
                        HomeCallbacks({ _, _ -> }, { _, _, _ -> itemMenus++ }, { _, _ -> }, { emptyHolds++ }, {}, {}, {}, {}),
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun holdEmptySpotAndLift_runsTheNormalLongPress() {
        show()
        rule.onRoot().performTouchInput { down(center); advanceEventTime(900); up() }
        rule.waitForIdle()
        assertThat(emptyHolds).isEqualTo(1)
        assertThat(lens.active).isFalse()
    }

    @Test fun holdEmptySpotAndDrag_movesTheLensInstead() {
        show()
        rule.onRoot().performTouchInput {
            down(center); advanceEventTime(900)
            repeat(8) { moveBy(Offset(25f, -30f)); advanceEventTime(30) }
        }
        rule.waitForIdle()
        assertThat(lens.active).isTrue()
        assertThat(lens.target.x).isGreaterThan(0f)
        rule.onRoot().performTouchInput { up() }
        rule.waitForIdle()
        assertThat(lens.active).isFalse()
        assertThat(emptyHolds).isEqualTo(0)
    }

    @Test fun holdingAnIcon_showsItsMenu_notTheLens() {
        show()
        rule.onAllNodesWithText(sampleEntry(0).label).onFirst().performTouchInput { down(center); advanceEventTime(900); up() }
        rule.waitForIdle()
        assertThat(itemMenus).isEqualTo(1)
        assertThat(lens.active).isFalse()
        assertThat(lens.target).isEqualTo(Offset.Zero) // never started
    }

    @Test fun lensMapsScreenToTheCroppedWallpaper() {
        val m = wallMapping(IntSize(100, 200), Size(300f, 600f), parallaxPx = Offset.Zero)
        assertThat(m.scale).isWithin(.001f).of(3.36f)
        assertThat(m.offset.x).isWithin(.01f).of(-18f)
        assertThat(m.offset.y).isWithin(.01f).of(-36f)
        val shifted = wallMapping(IntSize(100, 200), Size(300f, 600f), parallaxPx = Offset(10f, 4f)).offset
        assertThat(shifted.x).isWithin(.01f).of(-8f)
        assertThat(shifted.y).isWithin(.01f).of(-32f)
    }

    @Test fun rimTurnsWithTilt_inThreeDegreeSteps() {
        assertThat(rimDegrees(Offset.Zero)).isEqualTo(0)
        assertThat(rimDegrees(Offset(1f, 0f))).isEqualTo(69)
        assertThat(rimDegrees(Offset(-1f, 0f))).isEqualTo(-69)
    }
}
