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

/** Hold an app, then drag it: the drop must arrive even though Home switches to edit mode mid-gesture. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class HomeDragTest {
    @get:Rule val rule = createComposeRule()
    private val look = HomeLook(4, 6, 4, 60, true, IconShape.SQUIRCLE, { IconStyle.GLASS_DARK })
    private val layout = HomeLayout(listOf(listOf(AppCell(1, 0, 0, sampleEntry(0)))), emptyList())

    @Test fun holdAndDrag_outsideEditMode_dropsOnNewCell() {
        val drops = mutableListOf<DropRequest>()
        val repo = sampleRepo()
        rule.setContent {
            var edit by remember { mutableStateOf(false) }
            var menu by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    androidx.compose.foundation.layout.Box {
                        // Same wiring as the app: holding opens the menu, dragging closes it and enters edit mode.
                        HomeScreen(
                            layout, look, rememberPagerState { 1 }, edit,
                            HomeCallbacks({ _, _ -> }, { _, _, b -> if (!edit) menu = b }, { _, _ -> }, {}, {}, {}, { drops += it }, { menu = null; edit = true }),
                        )
                        menu?.let { b -> IconMenu(b, emptyList(), { null }, {}, listOf(MenuAction(com.xenoorigin.xelauncher.R.drawable.ti_x, "x") {}), { menu = null }) }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onAllNodesWithText(sampleEntry(0).label).onFirst().performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(30f, 10f)); advanceEventTime(50)
            repeat(10) { moveBy(Offset(60f, 60f)); advanceEventTime(30) }
            up()
        }
        rule.waitForIdle()
        assertThat(drops).hasSize(1)
        val d = drops.single() as DropRequest.Move
        assertThat(d.x to d.y).isNotEqualTo(0 to 0)
    }

    @Test fun inEditMode_dragStartsWithoutHolding() {
        val drops = mutableListOf<DropRequest>()
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    HomeScreen(layout, look, rememberPagerState { 1 }, true, HomeCallbacks({ _, _ -> }, { _, _, _ -> }, { _, _ -> }, {}, {}, {}, { drops += it }, {}))
                }
            }
        }
        rule.waitForIdle()
        rule.onAllNodesWithText(sampleEntry(0).label).onFirst().performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(60f, 60f)); advanceEventTime(16) }
            up()
        }
        rule.waitForIdle()
        assertThat(drops).hasSize(1)
    }
}
