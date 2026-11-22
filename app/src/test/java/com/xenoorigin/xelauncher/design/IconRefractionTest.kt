package com.xenoorigin.xelauncher.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.IconGlass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class IconRefractionTest {
    @Test @Config(sdk = [34]) fun highlightMovesWithScroll() {
        val w = 1080f
        val a = highlightShift(0f, 0f, w); val b = highlightShift(w * .3f, 0f, w)
        assertThat(b).isNotEqualTo(a)
        for (o in listOf(-5000f, 0f, 777f, 99999f)) assertThat(highlightShift(o, 1f, w)).isIn(com.google.common.collect.Range.closed(-1f, 1f))
        assertThat(highlightShift(0f, .5f, w)).isGreaterThan(highlightShift(0f, -.5f, w)) // tilt moves it too
    }

    @Test @Config(sdk = [34]) fun lensStrengthGrowsWithSpeedAndIsBounded() {
        assertThat(lensStrength(0f)).isEqualTo(LENS_REST)
        assertThat(lensStrength(3000f)).isGreaterThan(lensStrength(500f))
        assertThat(lensStrength(1e7f)).isEqualTo(LENS_MAX)
    }

    @Test @Config(sdk = [31], application = android.app.Application::class) fun lensFallsBackBelowApi33() {
        val m = Modifier.iconLens(RoundedCornerShape(12.dp)) { 6f }
        assertThat(m).isSameInstanceAs(Modifier)
    }

    @Test @Config(sdk = [34]) fun offDrawsNoSheen() {
        assertThat(sheenAlpha(IconGlass.OFF)).isEqualTo(0f)
        assertThat(sheenAlpha(IconGlass.LIGHT)).isGreaterThan(0f)
        assertThat(sheenAlpha(IconGlass.LENS)).isGreaterThan(0f)
    }
}
