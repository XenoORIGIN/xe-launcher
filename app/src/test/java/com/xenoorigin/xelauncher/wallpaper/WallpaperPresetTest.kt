package com.xenoorigin.xelauncher.wallpaper

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WallpaperPresetTest {
    private fun pixels(b: Bitmap) = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }

    @Test fun collection_hasTwentyTwoUniquePresets_includingTheLiveSkyAndBokeh() {
        assertThat(XeCollection).hasSize(22)
        assertThat(XeCollection.map { it.id }.toSet()).hasSize(22)
        assertThat(XeCollection.first().id).isEqualTo("xe_black")
        assertThat(XeCollection.map { it.id }).containsAtLeast("xe_blue", LIVE_SKY_ID)
    }

    @Test fun render_isDeterministic() {
        XeCollection.forEach { p ->
            assertThat(pixels(WallpaperRenderer.render(p, 90, 195))).isEqualTo(pixels(WallpaperRenderer.render(p, 90, 195)))
        }
    }

    @Test fun render_matchesRequestedSize() {
        val b = WallpaperRenderer.render(XeCollection[1], 108, 234)
        assertThat(b.width).isEqualTo(108); assertThat(b.height).isEqualTo(234)
    }

    @Test fun byId_findsPresetOrNull() {
        assertThat(XeCollection.byId("xe_blue")?.id).isEqualTo("xe_blue")
        assertThat(XeCollection.byId("nope")).isNull()
    }
}
