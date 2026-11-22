package com.xenoorigin.xelauncher.wallpaper

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class LiveSkyLayerTest {
    @get:Rule val rule = createComposeRule()
    private val sun = SunTimes(6 * 60 + 15, 17 * 60 + 48)

    private fun shot(minute: Int, name: String) {
        rule.setContent { LiveSkyLayer(sun, { minute }, animate = false, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun liveSkyNight() = shot(22 * 60, "live_sky_night")
    @Test fun liveSkyDusk() = shot(17 * 60 + 40, "live_sky_dusk")
    @Test fun liveSkyDay() = shot(10 * 60, "live_sky_day")
}
