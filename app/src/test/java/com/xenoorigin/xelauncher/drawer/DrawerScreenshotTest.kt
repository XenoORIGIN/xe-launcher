package com.xenoorigin.xelauncher.drawer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleRepo
import com.xenoorigin.xelauncher.settings.XeSettings
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
class DrawerScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun drawer_black() {
        val repo = sampleRepo()
        val cats = sampleCategories()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    TestBackdrop(black = true) {
                        AppDrawer(cats, DrawerState(pageCount = { cats.size }), DrawerLook({ IconStyle.GLASS_DARK }, IconShape.SQUIRCLE, 60, true, true),
                            { _, _ -> }, { _, _ -> }, {}, {}, {})
                    }
                }
            }
        }
        rule.waitForIdle(); Thread.sleep(400); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/drawer_black.png")
    }

    @Test fun drawerLiquid() {
        val repo = sampleRepo()
        val cats = sampleCategories()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    com.xenoorigin.xelauncher.testutil.LiquidBackdrop(com.xenoorigin.xelauncher.testutil.xeWall("aurora")) {
                        AppDrawer(cats, DrawerState(pageCount = { cats.size }), DrawerLook({ IconStyle.GLASS_CLEAR }, IconShape.SQUIRCLE, 60, true, true),
                            { _, _ -> }, { _, _ -> }, {}, {}, {})
                    }
                }
            }
        }
        rule.waitForIdle(); Thread.sleep(400); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/drawer_liquid.png")
    }

    @Test fun drawerMenu() {
        rule.setContent {
            XeTheme { TestBackdrop(black = true) { DrawerMenuSheet(true, XeSettings(), {}, {}, {}, {}, {}, {}) } }
        }
        rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/drawer_menu.png")
    }
}
