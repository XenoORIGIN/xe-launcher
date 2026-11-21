package com.xenoorigin.xelauncher.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.AppLanguage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class OnboardingUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun intro_finishesByItself() {
        var done = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { XeTheme { IntroAnimation({ done++ }) } }
        rule.mainClock.advanceTimeBy(1000)
        assertThat(done).isEqualTo(0)
        rule.mainClock.advanceTimeBy(INTRO_MS + 500L)
        assertThat(done).isEqualTo(1)
    }

    @Test fun intro_tapSkips() {
        var done = 0
        rule.setContent { XeTheme { IntroAnimation({ done++ }) } }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("intro").performClick()
        assertThat(done).isEqualTo(1)
    }

    @Test fun language_showsBothNames_andPicksEnglish() {
        var picked: AppLanguage? = null
        rule.setContent { XeTheme { LanguageStep({ picked = it }) } }
        rule.onNodeWithText("ไทย").assertIsDisplayed()
        rule.onNodeWithText("English").performClick()
        assertThat(picked).isEqualTo(AppLanguage.EN)
    }

    @Test fun slides_nextThroughAll_thenTryIt() {
        var practice = 0; var skipped = 0
        rule.setContent { XeTheme { TutorialSlides(onPractice = { practice++ }, onSkip = { skipped++ }) } }
        repeat(TUTORIAL_SLIDES - 1) { rule.onNodeWithTag("slides_next").performClick(); rule.waitForIdle() }
        assertThat(practice).isEqualTo(0)
        rule.onNodeWithTag("slides_next").performClick()
        assertThat(practice).isEqualTo(1)
        assertThat(skipped).isEqualTo(0)
    }

    @Test fun slides_skip() {
        var skipped = 0
        rule.setContent { XeTheme { TutorialSlides(onPractice = {}, onSkip = { skipped++ }) } }
        rule.onNodeWithTag("slides_skip").performClick()
        assertThat(skipped).isEqualTo(1)
    }

    @Test fun practice_showsTheStep_andCanBeSkipped() {
        var skipped = 0
        rule.setContent { XeTheme { PracticeOverlay(PracticeStep.HOLD_ICON, index = 2, total = 5, finished = false, onSkip = { skipped++ }) } }
        rule.onNodeWithText("3/5").assertIsDisplayed()
        rule.onNodeWithTag("practice_skip").performClick()
        assertThat(skipped).isEqualTo(1)
    }
}
