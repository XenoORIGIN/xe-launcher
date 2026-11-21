package com.xenoorigin.xelauncher.drawer

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class CategoryBarTest {
    @get:Rule val rule = createComposeRule()

    /** The drawer reopens on a category far along the bar: that category must be in view, not cut off at the edge. */
    @Test fun openingOnALaterCategory_showsItInTheBar() {
        val cats = sampleCategories()
        val state = DrawerState(initial = 7, pageCount = { cats.size })
        rule.setContent {
            XeTheme { TestBackdrop(black = true) { CategoryBar(cats, state, onSelect = {}) } }
        }
        rule.waitForIdle()
        val bar = rule.onNodeWithTag("categoryBar").getBoundsInRoot()
        val node = rule.onNodeWithContentDescription(cats[7].title)
        node.assertIsSelected()
        val b = node.getBoundsInRoot()
        assertThat(b.left.value).isAtLeast(bar.left.value)
        assertThat(b.right.value).isAtMost(bar.right.value)
    }
}
