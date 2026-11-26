package com.xenoorigin.xelauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.settings.XeSettings
import org.junit.Test

/** 2.1 bug hunt: with ASCII in the accent colour, changing the accent left the phone's copy in the old colour. */
class AsciiAccentTest {
    private val ascii = XeSettings(wallpaperId = "ascii_lake", asciiColor = AsciiColor.ACCENT, accent = 0xFFFFFFFF)

    @Test fun accentChangeRedrawsAccentAscii() = assertThat(asciiNeedsRedraw(ascii, ascii.copy(accent = 0xFF0A84FF))).isTrue()
    @Test fun monoAsciiIgnoresAccent() =
        assertThat(asciiNeedsRedraw(ascii.copy(asciiColor = AsciiColor.MONO), ascii.copy(asciiColor = AsciiColor.MONO, accent = 0xFF0A84FF))).isFalse()
    @Test fun otherWallpapersIgnoreAccent() =
        assertThat(asciiNeedsRedraw(ascii.copy(wallpaperId = "aurora"), ascii.copy(wallpaperId = "aurora", accent = 0xFF0A84FF))).isFalse()
    @Test fun sameAccentNothingToDo() = assertThat(asciiNeedsRedraw(ascii, ascii)).isFalse()
}
