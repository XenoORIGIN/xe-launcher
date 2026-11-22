package com.xenoorigin.xelauncher.home

import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.AnimationSpeed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LaunchBubbleTest {
    @get:Rule val rule = createComposeRule()
    private val view = View(ApplicationProvider.getApplicationContext())
    private val icon = Rect(100f, 200f, 260f, 360f)

    @Test fun launchPassesScaleUpOptions() {
        // The zoom-from-icon open is kept whether or not the glass bubble is on.
        assertThat(launchOptions(view, icon)).isNotNull()
        assertThat(launchOptions(view, Rect.Zero)).isNull()
    }

    @Test fun bubbleOnlyWhenGlassLaunchOn() {
        assertThat(showBubble(glassLaunch = true, speed = AnimationSpeed.FAST, icon)).isTrue()
        assertThat(showBubble(glassLaunch = false, speed = AnimationSpeed.FAST, icon)).isFalse()
        assertThat(showBubble(glassLaunch = true, speed = AnimationSpeed.OFF, icon)).isFalse()
    }

    @Test fun bubbleClearsWhenLauncherStops() {
        val owner = object : LifecycleOwner { val reg = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }; override val lifecycle get() = reg }
        var done = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                LaunchBubble(icon, AnimationSpeed.SMOOTH, Color.White) { done++ }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        assertThat(done).isEqualTo(0)
        rule.runOnIdle { owner.reg.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }
        rule.mainClock.advanceTimeByFrame()
        assertThat(done).isEqualTo(1)
    }
}
