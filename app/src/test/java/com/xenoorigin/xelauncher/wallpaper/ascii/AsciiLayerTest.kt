package com.xenoorigin.xelauncher.wallpaper.ascii

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.wallpaper.AsciiCollection
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class AsciiLayerTest {
    @get:Rule val rule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val still = AsciiCollection.first { it.id == "ascii_lake" }
    private val live = AsciiCollection.first().copy(minute = 0)

    private fun windowBitmap(): Bitmap { val v = rule.activity.window.decorView; return Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888).also { v.draw(android.graphics.Canvas(it)) } }

    @Before fun reset() { AsciiLayerProbe.frames = 0 }

    @Test fun drawsCrispBlocks() {
        rule.setContent { AsciiLayer(still, { Offset.Zero }, animate = false, modifier = Modifier.fillMaxSize()) }
        rule.waitForIdle()
        val b = windowBitmap()
        var runs = 0
        for (y in 200 until b.height - 200 step 37) {
            var x = 1
            while (x < b.width - 1) {
                val c = b.getPixel(x, y)
                var e = x
                while (e < b.width - 1 && b.getPixel(e, y) == c) e++
                if (c != AsciiRaster.GROUND && x > 1 && e < b.width - 1) {
                    assertThat((e - x) % 3).isEqualTo(0); runs++
                }
                x = e
            }
        }
        assertThat(runs).isGreaterThan(50)
    }

    @Test fun stillPresetDrawsOnce() {
        rule.mainClock.autoAdvance = false
        rule.setContent { AsciiLayer(still, { Offset.Zero }, animate = true, modifier = Modifier.fillMaxSize(), compute = kotlinx.coroutines.Dispatchers.Unconfined) }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertThat(AsciiLayerProbe.frames).isEqualTo(1)
    }

    @Test fun livePresetAnimatesWhenAllowed() {
        rule.mainClock.autoAdvance = false
        rule.setContent { AsciiLayer(live, { Offset.Zero }, animate = true, modifier = Modifier.fillMaxSize(), compute = kotlinx.coroutines.Dispatchers.Unconfined) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertThat(AsciiLayerProbe.frames).isAtLeast(4)
    }

    @Test fun livePresetStaysStillWhenAnimationIsOff() {
        rule.mainClock.autoAdvance = false
        rule.setContent { AsciiLayer(live, { Offset.Zero }, animate = false, modifier = Modifier.fillMaxSize(), compute = kotlinx.coroutines.Dispatchers.Unconfined) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertThat(AsciiLayerProbe.frames).isEqualTo(1)
    }

    @Test fun parallaxMovesByWholePixels() {
        var shift by mutableStateOf(Offset.Zero)
        rule.setContent { AsciiLayer(still, { shift }, animate = false, modifier = Modifier.fillMaxSize()) }
        rule.waitForIdle()
        val a = windowBitmap()
        shift = Offset(7.4f, 0f)
        rule.waitForIdle()
        val b = windowBitmap()
        val y = a.height / 2
        val same = (300 until 700).count { b.getPixel(it + 7, y) == a.getPixel(it, y) }
        assertThat(same).isEqualTo(400)
    }
    @Test fun stopsDrawingWhileXeIsHidden() {
        rule.mainClock.autoAdvance = false
        rule.setContent { AsciiLayer(live, { Offset.Zero }, animate = true, modifier = Modifier.fillMaxSize(), compute = kotlinx.coroutines.Dispatchers.Unconfined) }
        rule.mainClock.advanceTimeBy(500); rule.waitForIdle()
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        rule.waitForIdle()
        val before = AsciiLayerProbe.frames
        rule.mainClock.advanceTimeBy(130_000); rule.waitForIdle()
        assertThat(AsciiLayerProbe.frames).isEqualTo(before)
    }

    @Test fun framesAreComputedOffTheMainThread() {
        rule.setContent { AsciiLayer(live, { Offset.Zero }, animate = true, modifier = Modifier.fillMaxSize()) }
        rule.waitUntil(5_000) { AsciiLayerProbe.frames >= 3 }
        assertThat(AsciiLayerProbe.computedOnMain).isFalse()
    }
}
