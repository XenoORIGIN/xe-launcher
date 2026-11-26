package com.xenoorigin.xelauncher.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.design.UiStyle
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.ui.SettingsRoot
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Review pictures of the new settings page in both styles. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class SettingsStyleShotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(style: UiStyle, name: String) {
        rule.setContent { XeTheme(uiStyle = style) { TestBackdrop(black = style == UiStyle.NOTHING) { SettingsRoot(XeSettings(uiStyle = style), {}, {}) } } }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/$name")
    }

    @Test fun glass() = shot(UiStyle.GLASS, "settings_glass.png")
    @Test fun nothing() = shot(UiStyle.NOTHING, "settings_nothing.png")
}
