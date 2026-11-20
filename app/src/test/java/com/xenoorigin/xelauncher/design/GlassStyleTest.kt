package com.xenoorigin.xelauncher.design

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlassStyleTest {
    @Test fun saver_hasNoBlurAndNoRefraction() {
        val s = glassStyleFor(GlassQuality.SAVER, dark = true, sdk = 34)
        assertThat(s.blurRadius).isEqualTo(0.dp)
        assertThat(s.refraction).isEqualTo(0f)
    }

    @Test fun normal_onApi31_blursWithoutRefraction() {
        val s = glassStyleFor(GlassQuality.NORMAL, dark = true, sdk = 31)
        assertThat(s.blurRadius).isEqualTo(20.dp)
        assertThat(s.refraction).isEqualTo(0f)
    }

    @Test fun high_onApi33_refracts() {
        assertThat(glassStyleFor(GlassQuality.HIGH, dark = true, sdk = 33).refraction).isGreaterThan(0f)
    }

    @Test fun high_onApi32_doesNotRefract() {
        assertThat(glassStyleFor(GlassQuality.HIGH, dark = true, sdk = 32).refraction).isEqualTo(0f)
    }

    @Test fun anyQuality_onApi30_hasNoBlur() {
        GlassQuality.entries.forEach {
            assertThat(glassStyleFor(it, dark = false, sdk = 30).blurRadius).isEqualTo(0.dp)
        }
    }
}
