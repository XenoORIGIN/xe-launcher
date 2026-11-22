package com.xenoorigin.xelauncher.settings.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.IconGlass
import com.xenoorigin.xelauncher.settings.XeSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class EffectsSettingsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun effectsTogglesPersist() {
        var s by mutableStateOf(XeSettings())
        rule.setContent { XeTheme { AppearanceScreen(s, { f -> s = f(s) }, {}) } }
        fun tap(tag: String) { rule.onNodeWithTag(tag).performScrollTo().performClick(); rule.waitForIdle() }

        tap("iconGlass_LENS"); assertThat(s.iconGlass).isEqualTo(IconGlass.LENS)
        tap("iconGlass_OFF"); assertThat(s.iconGlass).isEqualTo(IconGlass.OFF)
        tap("effect_liquidDock"); assertThat(s.liquidDock).isFalse()
        tap("effect_depthParallax"); assertThat(s.depthParallax).isFalse()
        tap("effect_glassLaunch"); assertThat(s.glassLaunch).isFalse()
        tap("effect_timeTint"); assertThat(s.timeTint).isFalse()
        tap("effect_timeTint"); assertThat(s.timeTint).isTrue()
    }
}
