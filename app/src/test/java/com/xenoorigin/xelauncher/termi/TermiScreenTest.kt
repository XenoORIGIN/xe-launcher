package com.xenoorigin.xelauncher.termi

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.AsciiColor
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class TermiScreenTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun show(store: TermiStore, usage: Boolean = true, apps: List<Pair<String, String>> = listOf("com.termux" to "Termux", "com.instagram.android" to "Instagram")) {
        rule.setContent {
            XeTheme { TermiScreen(store, apps, usage, {}, {}, 0xFFFFFFFF, AsciiColor.MONO, animate = false) }
        }
        rule.waitForIdle()
    }

    private fun store() = TermiStore(tmp.root.resolve("t${System.nanoTime()}.json"), defaultCreators = { setOf("com.termux") })

    @Test fun lockedItemCannotBeWorn() {
        val s = store()
        show(s)
        rule.onNodeWithTag("wear_WIZARD", useUnmergedTree = true).performScrollTo().performClick()
        assertThat(s.current().equipped).isEmpty()
    }

    @Test fun unlockedItemIsWorn() {
        val s = store().apply { addXpForTest(120) }
        show(s)
        rule.onNodeWithTag("wear_CAP", useUnmergedTree = true).performScrollTo().performClick()
        assertThat(s.current().equipped[Slot.HEAD]).isEqualTo(Item.CAP)
    }

    @Test fun creatorSwitchesPersist() {
        val s = store()
        show(s)
        rule.onNodeWithTag("creator_com.instagram.android", useUnmergedTree = true).performScrollTo().performClick()
        assertThat(s.creatorApps()).containsExactly("com.termux", "com.instagram.android")
        rule.onNodeWithTag("creator_com.termux", useUnmergedTree = true).performScrollTo().performClick()
        assertThat(s.creatorApps()).containsExactly("com.instagram.android")
    }

    @Test fun usageRowOnlyWithoutAccess() {
        show(store(), usage = true)
        assertThat(rule.onAllNodesWithTag("termi_usage").fetchSemanticsNodes()).isEmpty()
    }

    @Test fun usageRowShownWithoutAccess() {
        show(store(), usage = false)
        rule.onNodeWithTag("termi_usage", useUnmergedTree = true).performScrollTo()
    }

    @Test fun nightSteppersMoveByHalfHours() {
        val s = store()
        show(s)
        rule.onNodeWithTag("night_start_plus", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithTag("night_end_minus", useUnmergedTree = true).performScrollTo().performClick()
        assertThat(s.current().nightStart).isEqualTo(30)
        assertThat(s.current().nightEnd).isEqualTo(330)
        rule.onNodeWithTag("night_start_minus", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithTag("night_start_minus", useUnmergedTree = true).performScrollTo().performClick()
        assertThat(s.current().nightStart).isEqualTo(1410)
    }
}
