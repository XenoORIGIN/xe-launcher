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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.asCoroutineDispatcher
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

    /** An app update right after an icon was drawn: the old picture still being saved must not come back. */
    @Test fun invalidate_beatsADiskWriteStillInFlight() = runBlocking<Unit> {
        repeat(20) { n ->
            val k = AppKey("com.race.app$n", "com.race.app$n.Main", 0)
            var count = 0
            val repo = IconRepository({ count++; ColorDrawable(Color.RED) }, File(dir, "race$n"))
            repo.icon(k, IconStyle.GLASS_DARK, 96, 1f)
            repo.invalidate(k.packageName)
            repo.flushDisk(); repo.clearMemory()
            repo.icon(k, IconStyle.GLASS_DARK, 96, 1f)
            assertThat(count).isEqualTo(2)
        }
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

    /** Eight worker threads, like the Galaxy A16 (this test machine may have fewer). */
    private val phone = java.util.concurrent.Executors.newFixedThreadPool(8).asCoroutineDispatcher()

    private fun keyN(i: Int) = AppKey("com.test.app$i", "com.test.app$i.Main", 0)

    /** Opening the drawer while every icon is still being prepared: the one on screen must not wait its turn. */
    @Test fun iconOnScreen_isNotStuckBehindPrewarm() = runBlocking<Unit> {
        val order = java.util.Collections.synchronizedList(mutableListOf<String>())
        val slow: (AppKey) -> Drawable? = { k -> order += k.packageName; Thread.sleep(40); ColorDrawable(Color.RED) }
        val repo = IconRepository(slow, dir, processors = 8, renderDispatcher = phone)
        val keys = (0 until 60).map(::keyN)
        val warm = launch(Dispatchers.Default) { repo.prewarm(keys, { IconStyle.GLASS_DARK }, 64, 1f) }
        delay(60)
        val last = keys.last()
        assertThat(repo.icon(last, IconStyle.GLASS_DARK, 64, 1f)).isNotNull()
        assertThat(order.indexOf(last.packageName)).isLessThan(8)
        warm.join()
    }

    /** Pre-rendering every app shares the phone with scrolling and animations, so it only uses a little of it. */
    @Test fun prewarm_rendersAtMostTwoAtATime() = runBlocking<Unit> {
        val now = java.util.concurrent.atomic.AtomicInteger(); var peak = 0
        val slow: (AppKey) -> Drawable? = {
            val n = now.incrementAndGet(); synchronized(this@IconRepositoryTest) { peak = maxOf(peak, n) }
            Thread.sleep(30); now.decrementAndGet(); ColorDrawable(Color.RED)
        }
        val repo = IconRepository(slow, dir, processors = 8, renderDispatcher = phone)
        repo.prewarm((0 until 20).map(::keyN), { IconStyle.GLASS_DARK }, 64, 1f)
        assertThat(peak).isAtMost(2)
        assertThat((0 until 20).all { repo.peek(keyN(it), IconStyle.GLASS_DARK, 64, 1f) != null }).isTrue()
    }
}
