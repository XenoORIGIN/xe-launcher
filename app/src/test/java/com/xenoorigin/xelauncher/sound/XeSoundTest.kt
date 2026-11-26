package com.xenoorigin.xelauncher.sound

import android.media.AudioManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class XeSoundTest {
    @Test fun playsOnlyWhenOnAndRingerIsNormal() {
        assertThat(XeSound.shouldPlay(enabled = true, ringerMode = AudioManager.RINGER_MODE_NORMAL)).isTrue()
        assertThat(XeSound.shouldPlay(enabled = true, ringerMode = AudioManager.RINGER_MODE_VIBRATE)).isFalse()
        assertThat(XeSound.shouldPlay(enabled = true, ringerMode = AudioManager.RINGER_MODE_SILENT)).isFalse()
        assertThat(XeSound.shouldPlay(enabled = false, ringerMode = AudioManager.RINGER_MODE_NORMAL)).isFalse()
    }

    @Test fun everyKindHasItsOwnSound() {
        val ids = XeSound.Kind.entries.map { it.raw }
        assertThat(ids).containsNoDuplicates()
        assertThat(ids).doesNotContain(0)
    }

    @Test fun rapidTapsAreThinned() {
        val t = XeSound.Throttle(minGapMs = 40)
        assertThat(t.allow(1000)).isTrue()
        assertThat(t.allow(1010)).isFalse()
        assertThat(t.allow(1050)).isTrue()
    }

    @Test fun playingBeforeInitIsHarmless() {
        XeSound.play(XeSound.Kind.TAP) // no crash without a context
    }
}
