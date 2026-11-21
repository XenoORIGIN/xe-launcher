package com.xenoorigin.xelauncher.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.design.XeTheme
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
class OnboardingScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, at: Long = 1500, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { XeTheme { TestBackdrop(black = true) { content() } } }
        rule.mainClock.advanceTimeBy(at)
        rule.onRoot().captureRoboImage("$SHOT_DIR/onboarding_$name.png")
    }

    @Test fun intro_shards() = shot("intro_1", 500) { IntroAnimation({}) }
    @Test fun intro_gathered() = shot("intro_2", 1200) { IntroAnimation({}) }
    @Test fun intro_logo() = shot("intro_3", 2000) { IntroAnimation({}) }
    @Test fun language() = shot("language") { LanguageStep({}) }
    @Test fun slides() = shot("slides", 1100) { TutorialSlides({}, {}) }
    @Test fun practice() = shot("practice") {
        Box(Modifier.fillMaxSize()) { PracticeOverlay(PracticeStep.OPEN_DRAWER, 0, 5, false, {}) }
    }
}
