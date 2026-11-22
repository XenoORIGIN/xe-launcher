package com.xenoorigin.xelauncher.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class ReorderableTest {
    @get:Rule val rule = createComposeRule()

    /** Dragging a row past its neighbour reorders it, and once the finger lifts no row stays lifted. */
    @Test fun dragStateClearsAfterARowMoves() {
        var items by mutableStateOf(listOf("A", "B", "C"))
        var moved: List<String>? = null
        rule.setContent {
            ReorderableColumn(items, key = { it }, rowHeight = 60.dp, spacing = 10.dp, onMoved = { moved = it; items = it }) { item, handle, dragging ->
                Row(Modifier.fillMaxSize().testTag(if (dragging) "lifted" else "row_$item")) {
                    Box(handle.size(48.dp).testTag("handle_$item"))
                    Text(item)
                }
            }
        }
        rule.onNodeWithTag("handle_A").performTouchInput {
            down(center)
            repeat(20) { moveBy(androidx.compose.ui.geometry.Offset(0f, 12.dp.toPx())) }
            up()
        }
        rule.waitForIdle()
        assertThat(moved).isNotNull()
        assertThat(moved!!.indexOf("A")).isGreaterThan(0)
        assertThat(rule.onAllNodesWithTag("lifted").fetchSemanticsNodes()).isEmpty()
    }
}
