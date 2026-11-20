package com.xenoorigin.xelauncher.icons

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppKey
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CustomIconStoreTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val key = AppKey("com.example", "com.example.Main", 0)
    private fun pic() = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

    @Test fun set_survivesNewInstance_andClearRemoves() {
        val dir = File(ctx.filesDir, "ci-${System.nanoTime()}")
        CustomIconStore(dir).set(key, pic())
        val again = CustomIconStore(dir)
        assertThat(again.get(key)).isNotNull()
        again.clear(key)
        assertThat(CustomIconStore(dir).get(key)).isNull()
    }

    @Test fun customPicture_isUsedByRepository_filledAndSquare() = kotlinx.coroutines.runBlocking<Unit> {
        val dir = File(ctx.filesDir, "ci-${System.nanoTime()}")
        val store = CustomIconStore(dir).apply { set(key, pic()) }
        val repo = IconRepository({ null }, File(dir, "cache"), store::get)
        val r = repo.icon(key, IconStyle.ORIGINAL, 64, 1f)!!
        assertThat(r.fillsTile).isTrue()
        assertThat(r.bitmap.width).isEqualTo(64)
        assertThat(r.bitmap.height).isEqualTo(64)
    }
}
