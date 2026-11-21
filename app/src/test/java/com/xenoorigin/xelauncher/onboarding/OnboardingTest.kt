package com.xenoorigin.xelauncher.onboarding

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.OnboardingStage
import com.xenoorigin.xelauncher.settings.OnboardingStage.DEFAULT_HOME
import com.xenoorigin.xelauncher.settings.OnboardingStage.DONE
import com.xenoorigin.xelauncher.settings.OnboardingStage.INTRO
import com.xenoorigin.xelauncher.settings.OnboardingStage.LANGUAGE
import com.xenoorigin.xelauncher.settings.OnboardingStage.PRACTICE
import com.xenoorigin.xelauncher.settings.OnboardingStage.SLIDES
import com.xenoorigin.xelauncher.settings.XeSettings
import org.junit.Test

class OnboardingTest {
    @Test fun firstRun_goesAnimationLanguageHomeSlidesPractice() {
        val seen = generateSequence(INTRO) { s -> nextStage(s, isDefaultHome = false).takeIf { it != s } }.toList()
        assertThat(seen).containsExactly(INTRO, LANGUAGE, DEFAULT_HOME, SLIDES, PRACTICE, DONE).inOrder()
    }

    @Test fun alreadyTheHomeApp_skipsThatQuestion() {
        assertThat(nextStage(LANGUAGE, isDefaultHome = true)).isEqualTo(SLIDES)
    }

    @Test fun newInstall_startsWithTheIntro() {
        assertThat(XeSettings().onboarding).isEqualTo(INTRO)
    }

    @Test fun practice_withLayoutLocked_leavesOutEditMode() {
        assertThat(practiceSteps(canEdit = true)).containsExactly(
            PracticeStep.OPEN_DRAWER, PracticeStep.CLOSE_DRAWER, PracticeStep.HOLD_ICON, PracticeStep.HOLD_EMPTY, PracticeStep.EXIT_EDIT,
        ).inOrder()
        assertThat(practiceSteps(canEdit = false)).containsNoneOf(PracticeStep.HOLD_EMPTY, PracticeStep.EXIT_EDIT)
    }

    @Test fun eachPracticeStep_isDoneByTheMatchingGesture() {
        val idle = HomeState(drawer = 0f, menuOpen = false, editMode = false)
        assertThat(PracticeStep.OPEN_DRAWER.isDone(idle)).isFalse()
        assertThat(PracticeStep.OPEN_DRAWER.isDone(idle.copy(drawer = 1f))).isTrue()
        assertThat(PracticeStep.CLOSE_DRAWER.isDone(idle.copy(drawer = .5f))).isFalse()
        assertThat(PracticeStep.CLOSE_DRAWER.isDone(idle)).isTrue()
        assertThat(PracticeStep.HOLD_ICON.isDone(idle.copy(menuOpen = true))).isTrue()
        assertThat(PracticeStep.HOLD_EMPTY.isDone(idle.copy(editMode = true))).isTrue()
        assertThat(PracticeStep.EXIT_EDIT.isDone(idle.copy(editMode = true))).isFalse()
        assertThat(PracticeStep.EXIT_EDIT.isDone(idle)).isTrue()
    }

    @Test fun practice_walksForwardOneStepAtATime() {
        val steps = practiceSteps(canEdit = true)
        var i = 0
        fun feed(s: HomeState) { i = advancePractice(steps, i, s) }
        feed(HomeState(1f, false, false)); assertThat(i).isEqualTo(1)
        feed(HomeState(1f, false, false)); assertThat(i).isEqualTo(1) // still open: not closed yet
        feed(HomeState(0f, false, false)); assertThat(i).isEqualTo(2)
        feed(HomeState(0f, true, false)); assertThat(i).isEqualTo(3)
        feed(HomeState(0f, false, true)); assertThat(i).isEqualTo(4)
        feed(HomeState(0f, false, false)); assertThat(i).isEqualTo(5)
        assertThat(i).isEqualTo(steps.size)
    }
}
