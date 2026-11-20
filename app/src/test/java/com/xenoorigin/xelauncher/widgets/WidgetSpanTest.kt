package com.xenoorigin.xelauncher.widgets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetSpanTest {
    @Test fun span_roundsUpToWholeCells() {
        assertThat(XeWidgetHost.spanForSize(180f, 110f, 85f, 100f, 4, 6)).isEqualTo(3 to 2)
    }

    @Test fun span_neverExceedsGrid_orGoesBelowOne() {
        assertThat(XeWidgetHost.spanForSize(2000f, 2000f, 85f, 100f, 4, 6)).isEqualTo(4 to 6)
        assertThat(XeWidgetHost.spanForSize(0f, 0f, 85f, 100f, 4, 6)).isEqualTo(1 to 1)
    }
}
