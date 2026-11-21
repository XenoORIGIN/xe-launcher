package com.xenoorigin.xelauncher.badges

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.home.FolderCell
import com.xenoorigin.xelauncher.home.FolderIcon
import com.xenoorigin.xelauncher.home.HomeLook
import com.xenoorigin.xelauncher.icons.AppIcon
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleRepo
import com.xenoorigin.xelauncher.settings.BadgeStyle
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class BadgeUiTest {
    @get:Rule val rule = createComposeRule()

    private val line = AppEntry(AppKey("pkg.1", "pkg.1.Main", 0), "LINE", -1, 0, false)
    private val mail = AppEntry(AppKey("pkg.2", "pkg.2.Main", 0), "Gmail", -1, 0, false)
    private val quiet = AppEntry(AppKey("pkg.3", "pkg.3.Main", 0), "Notes", -1, 0, false)
    private val counts = mapOf(badgeKey("pkg.1", 0) to 12, badgeKey("pkg.2", 0) to 250)

    private fun show(style: BadgeStyle, shot: String? = null) {
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo, LocalBadgeCounts provides counts, LocalBadgeStyle provides style) {
                    TestBackdrop(black = true) {
                        Row(androidx.compose.ui.Modifier.padding(top = 16.dp)) {
                            listOf(line, mail, quiet).forEach { AppIcon(it, 60.dp, IconStyle.GLASS_DARK, IconShape.SQUIRCLE, true) }
                            FolderIcon(FolderCell(1, 0, 0, 1, "Chat", listOf(line, quiet)), HomeLook(4, 6, 4, 60, true, IconShape.SQUIRCLE, { IconStyle.GLASS_DARK }), true)
                        }
                    }
                }
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1000); rule.waitForIdle()
        shot?.let { rule.onRoot().captureRoboImage("$SHOT_DIR/$it") }
    }

    @Test fun red_showsTheNumber_cappedAt99plus_andFoldersAddUpTheirApps() {
        show(BadgeStyle.RED, "badges_red.png")
        rule.onAllNodesWithTag("badge", useUnmergedTree = true).assertCountEquals(3) // LINE, Gmail, the folder
        rule.onNodeWithText("99+", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("12", useUnmergedTree = true).assertCountEquals(2) // LINE itself and the folder holding it
    }

    @Test fun whiteGlass_showsNumbersToo() {
        show(BadgeStyle.WHITE, "badges_white.png")
        rule.onNodeWithText("99+", useUnmergedTree = true).assertExists()
    }

    @Test fun dot_hasNoNumbers() {
        show(BadgeStyle.DOT)
        rule.onAllNodesWithTag("badge", useUnmergedTree = true).assertCountEquals(3)
        rule.onNodeWithText("99+", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun off_showsNothing() {
        show(BadgeStyle.OFF)
        rule.onAllNodesWithTag("badge", useUnmergedTree = true).assertCountEquals(0)
    }
}
