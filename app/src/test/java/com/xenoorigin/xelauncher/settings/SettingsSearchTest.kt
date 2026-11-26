package com.xenoorigin.xelauncher.settings

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.ui.settingsMatches
import org.junit.Test

class SettingsSearchTest {
    @Test fun matchesTitleIgnoringCase() {
        assertThat(settingsMatches("Wallpaper", "", "wall")).isTrue()
        assertThat(settingsMatches("Wallpaper", "", "WALLP")).isTrue()
    }

    @Test fun matchesKeywordsInEitherLanguage() {
        assertThat(settingsMatches("Appearance", "theme dark ธีม มืด", "มืด")).isTrue()
        assertThat(settingsMatches("Appearance", "theme dark ธีม มืด", "dark")).isTrue()
    }

    @Test fun everyWordMustMatchSomewhere() {
        assertThat(settingsMatches("Icons", "shape size", "icon size")).isTrue()
        assertThat(settingsMatches("Icons", "shape size", "icon sound")).isFalse()
    }

    @Test fun blankQueryMatchesNothing() {
        assertThat(settingsMatches("Icons", "", "  ")).isFalse()
    }
}
