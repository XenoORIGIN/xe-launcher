package com.xenoorigin.xelauncher.apps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppKeyTest {
    @Test fun encode_decode_roundTrip() {
        val k = AppKey("com.discord", "com.discord.main.MainActivity", 10)
        assertThat(AppKey.decode(k.encode())).isEqualTo(k)
    }

    @Test fun decode_garbage_isNull() {
        assertThat(AppKey.decode("nonsense")).isNull()
        assertThat(AppKey.decode("a/b#x")).isNull()
        assertThat(AppKey.decode("")).isNull()
    }
}
