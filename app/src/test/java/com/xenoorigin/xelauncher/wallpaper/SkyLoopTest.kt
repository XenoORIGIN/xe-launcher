package com.xenoorigin.xelauncher.wallpaper

import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SkyLoopTest {
    @Test fun skyStopsTickingWhenXeIsInTheBackground() {
        assertThat(skyLoopRuns(animate = true, state = Lifecycle.State.RESUMED)).isTrue()
        assertThat(skyLoopRuns(animate = true, state = Lifecycle.State.STARTED)).isTrue()
        assertThat(skyLoopRuns(animate = true, state = Lifecycle.State.CREATED)).isFalse()
        assertThat(skyLoopRuns(animate = false, state = Lifecycle.State.RESUMED)).isFalse()
    }
}
