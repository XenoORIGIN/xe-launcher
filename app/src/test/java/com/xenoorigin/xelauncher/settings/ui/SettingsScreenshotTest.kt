package com.xenoorigin.xelauncher.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.design.XeTheme
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
@Config(sdk = [34], qualifiers = "th-w390dp-h844dp-xxhdpi")
class SettingsScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent { XeTheme { TestBackdrop(black = true) { content() } } }
        rule.mainClock.advanceTimeBy(800); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/settings_$name.png")
    }

    @Test fun root() = shot("root_th") { SettingsRoot(XeSettings(), {}, {}) }
    @Test fun appearance() = shot("appearance_th") { AppearanceScreen(XeSettings(), {}, {}) }
    @Test fun icons() = shot("icons_th") { IconsScreen(XeSettings(), {}, {}) }
    @Test @Config(qualifiers = "en-w390dp-h844dp-xxhdpi") fun rootEnglish() = shot("root_en") { SettingsRoot(XeSettings(), {}, {}) }
}
