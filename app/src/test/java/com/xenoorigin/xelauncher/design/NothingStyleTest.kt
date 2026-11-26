package com.xenoorigin.xelauncher.design

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.XeSettings
import org.junit.Test

class NothingStyleTest {
    @Test fun glassIsTheDefaultStyle() {
        assertThat(XeSettings().uiStyle).isEqualTo(UiStyle.GLASS)
    }

    @Test fun nothingPanelsAreFlat() {
        val s = glassStyleFor(GlassQuality.HIGH, dark = true, sdk = 34, ui = UiStyle.NOTHING)
        assertThat(s.flat).isTrue()
        assertThat(s.blurRadius).isEqualTo(0.dp)
        assertThat(s.refraction).isEqualTo(0f)
    }

    @Test fun glassPanelsStayGlass() {
        val s = glassStyleFor(GlassQuality.HIGH, dark = true, sdk = 34, ui = UiStyle.GLASS)
        assertThat(s.flat).isFalse()
        assertThat(s.blurRadius).isGreaterThan(0.dp)
    }

    @Test fun dotFontHasDigitsLettersAndClockMarks() {
        for (c in "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ:%.-/") assertThat(DotFont.glyph(c)).isNotNull()
        assertThat(DotFont.glyph('a')).isEqualTo(DotFont.glyph('A'))
    }

    @Test fun dotGlyphsAreFiveBySeven() {
        val g = DotFont.glyph('8')!!
        assertThat(g.size).isEqualTo(DotFont.W * DotFont.H)
        assertThat(g.count { it }).isAtLeast(12)
        assertThat(DotFont.glyph('1')!!.count { it }).isLessThan(g.count { it })
    }

    @Test fun onlyLatinTextCanBeDotted() {
        assertThat(DotFont.canDraw("12:45")).isTrue()
        assertThat(DotFont.canDraw("SETTINGS")).isTrue()
        assertThat(DotFont.canDraw("ตั้งค่า")).isFalse()
    }
}
