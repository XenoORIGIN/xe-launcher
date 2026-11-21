package com.xenoorigin.xelauncher.icons

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppKey
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconRepositoryTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private var loads = 0
    private val loader: (AppKey) -> Drawable? = { loads++; ColorDrawable(Color.RED) }
    private val dir = File(ctx.cacheDir, "icons-test-${System.nanoTime()}")
    private val key = AppKey("com.test.app", "com.test.app.Main", 0)

    @Test fun secondRequest_afterMemoryCleared_comesFromDisk() = runBlocking<Unit> {
        val repo = IconRepository(loader, dir)
        repo.icon(key, IconStyle.GLASS_DARK, 96, 1.15f)
        repo.flushDisk()
        repo.clearMemory()
        repo.icon(key, IconStyle.GLASS_DARK, 96, 1.15f)
        assertThat(loads).isEqualTo(1)
    }

    @Test fun invalidate_forcesReRender() = runBlocking<Unit> {
        val repo = IconRepository(loader, dir)
        repo.icon(key, IconStyle.GLASS_DARK, 96, 1.15f)
        repo.invalidate("com.test.app")
        repo.icon(key, IconStyle.GLASS_DARK, 96, 1.15f)
        assertThat(loads).isEqualTo(2)
    }

    @Test fun missingIcon_returnsNullWithoutCrash() = runBlocking<Unit> {
        val repo = IconRepository({ null }, dir)
        assertThat(repo.icon(key, IconStyle.ORIGINAL, 96, 1f)).isNull()
    }

    @Test fun colorGlass_dominantColorComesFromWholeIcon_evenFromDiskCache() = runBlocking<Unit> {
        // White glyph on a blue background: the glyph alone has no colour.
        val blueIcon: (AppKey) -> Drawable? = {
            android.graphics.drawable.AdaptiveIconDrawable(ColorDrawable(Color.rgb(20, 90, 240)), ColorDrawable(Color.TRANSPARENT))
        }
        val repo = IconRepository(blueIcon, dir)
        val first = repo.icon(key, IconStyle.GLASS_COLOR, 96, 1f)!!
        assertThat(Color.blue(first.dominantColor)).isGreaterThan(200)
        repo.flushDisk(); repo.clearMemory()
        val again = repo.icon(key, IconStyle.GLASS_COLOR, 96, 1f)!!
        assertThat(again.dominantColor).isEqualTo(first.dominantColor)
    }

    @Test fun sameIconRequestedTwiceAtOnce_rendersOnce() = runBlocking<Unit> {
        val slowLoader: (AppKey) -> Drawable? = { loads++; Thread.sleep(150); ColorDrawable(Color.RED) }
        val repo = IconRepository(slowLoader, dir)
        val a = async(Dispatchers.Default) { repo.icon(key, IconStyle.GLASS_DARK, 64, 1f) }
        val b = async(Dispatchers.Default) { repo.icon(key, IconStyle.GLASS_DARK, 64, 1f) }
        assertThat(a.await()).isNotNull(); assertThat(b.await()).isNotNull()
        assertThat(loads).isEqualTo(1)
    }

    @Test fun renderedIcon_isAvailableInstantly_evenBeforeDiskWriteFinishes() = runBlocking<Unit> {
        val repo = IconRepository(loader, dir)
        repo.icon(key, IconStyle.GLASS_DARK, 64, 1f)
        assertThat(repo.peek(key, IconStyle.GLASS_DARK, 64, 1f)).isNotNull()
        repo.flushDisk()
        assertThat(dir.listFiles()!!.any { it.name.endsWith(".png") }).isTrue()
    }
}
