package com.xenoorigin.xelauncher.widgets

import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.home.HomeCallbacks
import com.xenoorigin.xelauncher.home.HomeLayout
import com.xenoorigin.xelauncher.home.HomeLook
import com.xenoorigin.xelauncher.home.HomeScreen
import com.xenoorigin.xelauncher.home.LocalWidgetContent
import com.xenoorigin.xelauncher.home.WidgetCell
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleRepo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class WidgetLongPressTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    /**
     * A press on a part of a widget that doesn't take touches: the view says "not mine" to the down, so neither
     * Android nor Compose send it the rest of the gesture (moves, lift). Whatever the finger does next, e.g. a
     * swipe to the next page, the view itself must not decide later that it was a long press.
     */
    @Test fun pressTheWidgetDoesNotTake_neverOpensMenuByItself() {
        val v = XeWidgetHostView(ctx)
        v.isHapticFeedbackEnabled = true
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0)
        val taken = v.dispatchTouchEvent(down)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout() + 200L))
        assertThat(taken).isFalse()
        assertThat(v.isPressed).isFalse()
        assertThat(v.childCount).isEqualTo(0)
    }

    private val look = HomeLook(4, 6, 4, 60, true, IconShape.SQUIRCLE, { IconStyle.GLASS_DARK })
    private val widget = WidgetCell(7, 0, 0, 4, 2, 42, null)

    private class Counts { var menus = 0; var clicks = 0; var edits = 0 }

    /** Home with one widget whose whole face is a button, the usual shape of other apps' widgets. */
    private fun setHome(counts: Counts) {
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(
                    LocalIconRepository provides repo,
                    LocalWidgetContent provides { _ ->
                        AndroidView(
                            factory = { c ->
                                XeWidgetHostView(c).apply {
                                    addView(View(c).apply { isClickable = true; setOnClickListener { counts.clicks++ } }, ViewGroup.LayoutParams(-1, -1))
                                }
                            },
                            modifier = Modifier.fillMaxSize().testTag("widget"),
                        )
                    },
                ) {
                    Box(Modifier.fillMaxSize()) {
                        HomeScreen(
                            HomeLayout(listOf(listOf(widget)), emptyList()), look, rememberPagerState { 2 }, false,
                            HomeCallbacks({ _, _ -> }, { _, _, _ -> counts.menus++ }, { _, _ -> }, {}, {}, {}, {}, {}, onWidgetEdit = { counts.edits++ }),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun holdingAWidget_liftsItForEditingOnce_andDoesNotAlsoTapIt() {
        val counts = Counts(); setHome(counts)
        rule.onNodeWithTag("widget").performTouchInput { down(center); advanceEventTime(900); up() }
        rule.waitForIdle()
        assertThat(counts.edits).isEqualTo(1)
        assertThat(counts.menus).isEqualTo(0)
        assertThat(counts.clicks).isEqualTo(0)
    }

    @Test fun slowSwipeAcrossAWidget_neitherOpensMenuNorTapsIt() {
        val counts = Counts(); setHome(counts)
        rule.onNodeWithTag("widget").performTouchInput {
            down(center)
            // A slow swipe up that stays inside the widget and takes longer than a long press.
            repeat(20) { moveBy(Offset(0f, -6f)); advanceEventTime(40) }
            up()
        }
        rule.waitForIdle()
        assertThat(counts.menus + counts.edits).isEqualTo(0)
        assertThat(counts.clicks).isEqualTo(0)
    }

    @Test fun tappingAWidget_stillTapsIt() {
        val counts = Counts(); setHome(counts)
        rule.onNodeWithTag("widget").performTouchInput { click(center) }
        rule.waitForIdle()
        assertThat(counts.clicks).isEqualTo(1)
        assertThat(counts.menus).isEqualTo(0)
    }
}
