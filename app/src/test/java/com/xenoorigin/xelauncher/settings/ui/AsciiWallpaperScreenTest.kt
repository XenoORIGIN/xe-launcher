package com.xenoorigin.xelauncher.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.wallpaper.AsciiCollection
import com.xenoorigin.xelauncher.wallpaper.WallpaperPreset
import com.xenoorigin.xelauncher.wallpaper.WallpaperTarget
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class AsciiWallpaperScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun asciiGroupShowsEightThumbnails() {
        rule.setContent { XeTheme { Column(Modifier.verticalScroll(rememberScrollState())) { AsciiGroup(XeSettings(), {}, {}) } } }
        AsciiCollection.forEach { rule.onNodeWithTag("thumb_${it.id}").performScrollTo() }
        assertThat(AsciiCollection.count { p -> rule.onAllNodesWithTag("thumb_${p.id}").fetchSemanticsNodes().size == 1 }).isEqualTo(8)
    }

    @Test fun colourChipSavesImmediately() {
        var s by mutableStateOf(XeSettings())
        rule.setContent { XeTheme { Column(Modifier.verticalScroll(rememberScrollState())) { AsciiGroup(s, { c -> s = s.copy(asciiColor = c) }, {}) } } }
        rule.onNodeWithTag("ascii_color_ACCENT").performScrollTo().performClick()
        rule.waitForIdle()
        assertThat(s.asciiColor).isEqualTo(AsciiColor.ACCENT)
    }

    @Test fun editorColourOnlyPreviewsUntilApply() {
        var applied: WallpaperPreset? = null
        rule.setContent { XeTheme { WallpaperEditor(AsciiCollection[1], onApply = { p, _, _ -> applied = p }, onDelete = null, onClose = {}) } }
        rule.onNodeWithTag("editor_ascii_color_SKY", useUnmergedTree = true).performScrollTo().performClick()
        rule.waitForIdle()
        assertThat(applied).isNull()
        rule.onNodeWithTag("set_wallpaper", useUnmergedTree = true).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("target_${WallpaperTarget.HOME}", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        val p = applied as WallpaperPreset.Ascii
        assertThat(p.color).isEqualTo(AsciiColor.SKY)
        assertThat(settingsAfterApply(XeSettings(), p).asciiColor).isEqualTo(AsciiColor.SKY)
        assertThat(settingsAfterApply(XeSettings(), com.xenoorigin.xelauncher.wallpaper.XeCollection.first()).asciiColor).isEqualTo(AsciiColor.MONO)
    }

    @Test fun editorHidesDimAndBlurForAscii() {
        rule.setContent { XeTheme { WallpaperEditor(AsciiCollection[1], onApply = { _, _, _ -> }, onDelete = null, onClose = {}) } }
        rule.waitForIdle()
        assertThat(rule.onAllNodesWithTag("slider_dim").fetchSemanticsNodes()).isEmpty()
    }
}
