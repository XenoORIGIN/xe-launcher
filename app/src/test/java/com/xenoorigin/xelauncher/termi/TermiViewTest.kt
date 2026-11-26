package com.xenoorigin.xelauncher.termi

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.AsciiColor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class TermiViewTest {
    @get:Rule val rule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val blue = 0xFF2F6BFF.toInt()

    @Before fun reset() { TermiViewProbe.frames = 0 }

    private fun content(animate: Boolean) = rule.setContent {
        TermiView(Stage.JUNIOR, emptyMap(), Mood.HAPPY, null, blue, AsciiColor.MONO, 0xFFFFFFFF, animate, Modifier.size(300.dp, 150.dp))
    }

    @Test fun drawsTermiInTheBodyColour() {
        content(false)
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888).also { v.draw(android.graphics.Canvas(it)) }
        var n = 0
        for (y in 0 until 450 step 2) for (x in 0 until 900 step 2) if (b.getPixel(x, y) == blue) n++
        assertThat(n).isGreaterThan(200)
    }

    @Test fun stillWhenAnimationIsOff() {
        rule.mainClock.autoAdvance = false
        content(false)
        rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        assertThat(TermiViewProbe.frames).isEqualTo(1)
    }

    @Test fun movesWhenAllowed() {
        rule.mainClock.autoAdvance = false
        content(true)
        rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        assertThat(TermiViewProbe.frames).isAtLeast(8)
    }

    @Test fun stopsWhileHidden() {
        rule.mainClock.autoAdvance = false
        content(true)
        rule.mainClock.advanceTimeBy(300); rule.waitForIdle()
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        rule.waitForIdle()
        val before = TermiViewProbe.frames
        rule.mainClock.advanceTimeBy(5_000); rule.waitForIdle()
        assertThat(TermiViewProbe.frames).isEqualTo(before)
    }

    // C1: tiny sizes (a widget mid-resize, huge fonts) must not crash the ASCII grid.
    @Test fun tinySizesDoNotCrash() {
        rule.setContent {
            androidx.compose.foundation.layout.Column {
                TermiView(Stage.JUNIOR, emptyMap(), Mood.HAPPY, null, 0xFFFFFFFF.toInt(), AsciiColor.MONO, 0xFFFFFFFF, false, Modifier.size(100.dp, 6.dp))
                TermiView(Stage.JUNIOR, emptyMap(), Mood.HAPPY, null, 0xFFFFFFFF.toInt(), AsciiColor.MONO, 0xFFFFFFFF, false, Modifier.size(3.dp, 100.dp))
                TermiView(Stage.JUNIOR, emptyMap(), Mood.HAPPY, null, 0xFFFFFFFF.toInt(), AsciiColor.MONO, 0xFFFFFFFF, false, Modifier.size(1.dp, 1.dp))
            }
        }
        rule.waitForIdle()
    }
}
