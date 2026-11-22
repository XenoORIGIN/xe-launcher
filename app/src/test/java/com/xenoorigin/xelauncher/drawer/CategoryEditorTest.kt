package com.xenoorigin.xelauncher.drawer

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.CategorySort
import com.xenoorigin.xelauncher.settings.DrawerSort
import com.xenoorigin.xelauncher.settings.XeSettings
import org.junit.Rule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class CategoryEditorTest {
    @get:Rule val rule = createComposeRule()
    private val longName = "เดินทางและท่องเที่ยวรอบโลกกับครอบครัว"
    private val cats = listOf(
        CategoryEntity(id = 1, builtIn = "TRAVEL", iconName = "map-pin", position = 0) to longName,
        CategoryEntity(id = 2, builtIn = null, name = "Work", iconName = "briefcase", position = 1) to "Work",
    )

    private fun show(actions: EditorActions, accents: Map<Long, Long> = emptyMap(), sorts: Map<Long, CategorySort> = emptyMap()) =
        rule.setContent { XeTheme { CategoryEditor(cats, actions, startAdding = false, onClose = {}, accents = accents, sorts = sorts) } }

    private fun noop() = EditorActions({ _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})

    /** A long Thai name wraps or ends in "…"; it is never silently clipped mid-word. */
    @Test fun longThaiNameIsNotCutMidWord() {
        show(noop())
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(longName).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        val r = results.single()
        assertThat(!r.hasVisualOverflow || r.isLineEllipsized(r.lineCount - 1)).isTrue()
        assertThat(r.lineCount).isAtMost(2)
    }

    @Test fun swipeLeftHidesCategory() {
        var hidden: Pair<Long, Boolean>? = null
        show(noop().copy(setHidden = { id, h -> hidden = id to h }))
        rule.onNodeWithTag("cat_row_2").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertThat(hidden).isEqualTo(2L to true)
    }

    @Test fun accentAndSortAreSaved() {
        var accent: Pair<Long, Long?>? = null
        var sort: Pair<Long, CategorySort>? = null
        show(noop().copy(setAccent = { id, c -> accent = id to c }, setSort = { id, s -> sort = id to s }))
        rule.onNodeWithTag("cat_row_2").performClick()
        // The sheet scrolls; tap through the click action so the test doesn't depend on scroll position.
        for (tag in listOf("accent_3", "sort_TIME", "category_save")) rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertThat(accent?.first).isEqualTo(2L)
        assertThat(accent?.second).isEqualTo(CategoryAccents[3])
        assertThat(sort).isEqualTo(2L to CategorySort.TIME)
    }

    @Test fun editorScreenshot() {
        show(noop(), accents = mapOf(2L to CategoryAccents[5]))
        rule.mainClock.advanceTimeBy(2_000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("src/test/screenshots/category_editor.png")
    }

    @Test fun categorySortFollowsDrawerByDefault() {
        val s = XeSettings(drawerSort = DrawerSort.RECENTLY_INSTALLED)
        assertThat(sortForCategory(5, s)).isEqualTo(DrawerSort.RECENTLY_INSTALLED)
        val t = s.copy(categorySort = mapOf(5L to CategorySort.ALPHABETICAL, 6L to CategorySort.TIME))
        assertThat(sortForCategory(5, t)).isEqualTo(DrawerSort.ALPHABETICAL)
        assertThat(sortForCategory(6, t)).isEqualTo(DrawerSort.MOST_USED)
        assertThat(sortForCategory(7, t)).isEqualTo(DrawerSort.RECENTLY_INSTALLED)
    }
}
