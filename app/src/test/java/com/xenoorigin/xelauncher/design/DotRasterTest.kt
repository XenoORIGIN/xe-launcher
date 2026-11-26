package com.xenoorigin.xelauncher.design

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 2.1: any text (Thai included) can be drawn as dots by sampling a real font. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DotRasterTest {
    @Test fun thaiTextBecomesADotGrid() {
        val g = DotFont.rasterize("ตั้งค่า", rows = 13)
        assertThat(g.rows).isEqualTo(13)
        assertThat(g.cols).isGreaterThan(10)
        assertThat(g.on.count { it }).isGreaterThan(15)
    }

    @Test fun longerTextIsWider() {
        assertThat(DotFont.rasterize("การตั้งค่าทั้งหมด", 13).cols).isGreaterThan(DotFont.rasterize("ตั้งค่า", 13).cols)
    }

    @Test fun emptyTextIsEmpty() {
        assertThat(DotFont.rasterize("", 13).cols).isEqualTo(0)
    }
}
