package com.xenoorigin.xestudio.icons

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xestudio.core.IconRecipe
import com.xenoorigin.xestudio.core.TileShape
import com.xenoorigin.xestudio.core.Treatment
import com.xenoorigin.xestudio.design.StudioTheme
import com.xenoorigin.xestudio.projects.ProjectKind
import com.xenoorigin.xestudio.projects.ProjectStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class IconEditorScreenTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private val apps = AppSource {
        listOf(
            InstalledApp("com.a/.Main#0", "Camera", ColorDrawable(Color.rgb(220, 60, 60))),
            InstalledApp("com.b/.Main#0", "Music", ColorDrawable(Color.rgb(60, 160, 220))),
            InstalledApp("com.c/.Main#0", "Maps", ColorDrawable(Color.rgb(60, 200, 120))),
        )
    }

    private fun state(): IconEditorState {
        val store = ProjectStore(tmp.root)
        val p = runBlocking { store.create(ProjectKind.ICONS, "Night", IconRecipe.encode(IconRecipe())) }
        return IconEditorState(p, store, CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }

    @Test fun tappingShapeChangesRecipe() {
        val st = state()
        rule.setContent { StudioTheme { IconEditorScreen(st, apps, onBack = {}, onApply = {}) } }
        rule.onNodeWithTag("ctl_shape_CIRCLE").performScrollTo().performClick()
        rule.waitForIdle()
        assertThat(st.recipe.value.shape).isEqualTo(TileShape.CIRCLE)
        rule.onNodeWithTag("ctl_treatment_TINT").performScrollTo().performClick()
        rule.waitForIdle()
        assertThat(st.recipe.value.treatment).isEqualTo(Treatment.TINT)
    }

    @Test fun tappingIconOpensOverrideSheet() {
        val st = state()
        rule.setContent { StudioTheme { IconEditorScreen(st, apps, onBack = {}, onApply = {}) } }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("icon_com.b/.Main#0").assertExists() }.isSuccess }
        rule.onNodeWithTag("icon_com.b/.Main#0").performClick()
        rule.onNodeWithTag("override_sheet").assertExists()
    }

    @Test fun manyAppsAreLaidOutLazily() {
        val many = AppSource { (0 until 200).map { InstalledApp("com.x$it/.M#0", "App $it", ColorDrawable(Color.GRAY)) } }
        val st = state()
        rule.setContent { StudioTheme { IconEditorScreen(st, many, onBack = {}, onApply = {}) } }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("icon_com.x0/.M#0").assertExists() }.isSuccess }
        val tiles = rule.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("icon tile") { n ->
            androidx.compose.ui.semantics.SemanticsProperties.TestTag in n.config && n.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag].startsWith("icon_")
        }).fetchSemanticsNodes().size
        assertThat(tiles).isLessThan(100)
    }

    @Test fun editorScreenshot() {
        val st = state()
        rule.setContent { StudioTheme { IconEditorScreen(st, apps, onBack = {}, onApply = {}) } }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("icon_com.c/.Main#0").assertExists() }.isSuccess }
        Thread.sleep(400); rule.mainClock.advanceTimeBy(1_500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("src/test/screenshots/icon_editor.png")
    }
}
