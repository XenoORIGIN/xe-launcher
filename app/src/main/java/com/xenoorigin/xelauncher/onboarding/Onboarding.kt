package com.xenoorigin.xelauncher.onboarding

import androidx.annotation.StringRes
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.settings.OnboardingStage

/** The first-run order: intro, language, default Home (only if needed), tutorial slides, practice, done. */
fun nextStage(stage: OnboardingStage, isDefaultHome: Boolean): OnboardingStage = when (stage) {
    OnboardingStage.INTRO -> OnboardingStage.LANGUAGE
    OnboardingStage.LANGUAGE -> if (isDefaultHome) OnboardingStage.SLIDES else OnboardingStage.DEFAULT_HOME
    OnboardingStage.DEFAULT_HOME -> OnboardingStage.SLIDES
    OnboardingStage.SLIDES -> OnboardingStage.PRACTICE
    OnboardingStage.PRACTICE, OnboardingStage.DONE -> OnboardingStage.DONE
}

/** What Home looks like right now, as far as the practice steps care. [drawer] is 0 (closed) to 1 (open). */
data class HomeState(val drawer: Float, val menuOpen: Boolean, val editMode: Boolean)

/** One thing to try on the real Home screen during the tutorial. */
enum class PracticeStep(@StringRes val text: Int) {
    OPEN_DRAWER(R.string.practice_open_drawer),
    CLOSE_DRAWER(R.string.practice_close_drawer),
    HOLD_ICON(R.string.practice_hold_icon),
    HOLD_EMPTY(R.string.practice_hold_empty),
    EXIT_EDIT(R.string.practice_exit_edit);

    fun isDone(s: HomeState): Boolean = when (this) {
        OPEN_DRAWER -> s.drawer > .9f
        CLOSE_DRAWER -> s.drawer < .05f
        HOLD_ICON -> s.menuOpen
        HOLD_EMPTY -> s.editMode
        EXIT_EDIT -> !s.editMode
    }
}

/** Edit mode steps are left out when the layout is locked (holding an empty spot does nothing then). */
fun practiceSteps(canEdit: Boolean): List<PracticeStep> =
    listOf(PracticeStep.OPEN_DRAWER, PracticeStep.CLOSE_DRAWER, PracticeStep.HOLD_ICON) +
        if (canEdit) listOf(PracticeStep.HOLD_EMPTY, PracticeStep.EXIT_EDIT) else emptyList()

/** Moves on at most one step per change, so a single state can't tick off two steps at once. */
fun advancePractice(steps: List<PracticeStep>, index: Int, state: HomeState): Int =
    if (index < steps.size && steps[index].isDone(state)) index + 1 else index
