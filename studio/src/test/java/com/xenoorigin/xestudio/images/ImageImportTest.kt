package com.xenoorigin.xestudio.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ImageImportTest {
    @Test fun normalizeShrinksHugePhoto() {
        val big = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        // Noise-ish content so compression has real work to do.
        for (y in 0 until 3000 step 7) for (x in 0 until 4000 step 5) big.setPixel(x, y, Color.rgb(x % 255, y % 255, (x * y) % 255))
        val bytes = ImageImport.normalize(big)
        assertThat(bytes.size).isAtMost(1_000_000)
        val back = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertThat(maxOf(back.width, back.height)).isEqualTo(512)
        assertThat(back.width).isEqualTo(512)
        assertThat(back.height).isEqualTo(384)
    }

    @Test fun smallPictureKeepsItsSize() {
        val small = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val back = ImageImport.normalize(small).let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        assertThat(back.width).isEqualTo(100)
        assertThat(back.height).isEqualTo(50)
    }
}
