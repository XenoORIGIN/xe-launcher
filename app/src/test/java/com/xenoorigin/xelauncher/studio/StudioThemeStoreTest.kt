package com.xenoorigin.xelauncher.studio

import android.content.ContentProvider
import android.content.ContentValues
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.icons.IconRepository
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xestudio.core.Fill
import com.xenoorigin.xestudio.core.IconOverride
import com.xenoorigin.xestudio.core.IconRecipe
import com.xenoorigin.xestudio.core.TileShape
import com.xenoorigin.xestudio.core.Treatment
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

/** Stands in for Xe Studio's theme provider. */
class FakeStudioProvider : ContentProvider() {
    companion object {
        var recipe: String? = null
        var version = 1L
        var images = mapOf<String, ByteArray>()
        var gone = false
    }
    override fun onCreate() = true
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?) =
        if (gone) null else MatrixCursor(arrayOf("recipe", "version")).apply { recipe?.let { addRow(arrayOf<Any>(it, version)) } }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val bytes = images[uri.lastPathSegment] ?: throw java.io.FileNotFoundException()
        val f = File.createTempFile("img", ".bin").apply { writeBytes(bytes) }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun getType(uri: Uri) = null
    override fun insert(uri: Uri, v: ContentValues?) = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class StudioThemeStoreTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dir = File(ctx.filesDir, "studio-${System.nanoTime()}")
    private val key = AppKey("com.test.app", "com.test.app.Main", 0)
    private val blue: ByteArray = ByteArrayOutputStream().also {
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }.compress(Bitmap.CompressFormat.PNG, 100, it)
    }.toByteArray()
    private val recipe = IconRecipe(
        shape = TileShape.SQUARE, background = Fill(listOf(0xFF102030.toInt())), treatment = Treatment.ORIGINAL,
        overrides = mapOf(key.encode() to IconOverride.Image("o.png")),
    )

    @Before fun setUp() {
        FakeStudioProvider.recipe = IconRecipe.encode(recipe); FakeStudioProvider.version = 5L
        FakeStudioProvider.images = mapOf("o.png" to blue); FakeStudioProvider.gone = false
        Robolectric.setupContentProvider(FakeStudioProvider::class.java, StudioThemeStore.AUTHORITY)
    }

    private fun store() = StudioThemeStore(dir, ctx.contentResolver)

    @Test fun pullCopiesRecipeAndImages() = runBlocking<Unit> {
        val s = store()
        assertThat(s.pull()).isTrue()
        assertThat(s.theme.value).isEqualTo(StoredTheme(recipe, 5L))
        assertThat(s.image("o.png")!!.getPixel(1, 1)).isEqualTo(Color.BLUE)
        // Survives a restart.
        assertThat(store().theme.value).isEqualTo(StoredTheme(recipe, 5L))
    }

    @Test fun pullKeepsOldThemeWhenProviderMissing() = runBlocking<Unit> {
        val s = store(); s.pull()
        FakeStudioProvider.gone = true
        assertThat(s.pull()).isFalse()
        assertThat(s.theme.value!!.recipe).isEqualTo(recipe)
    }

    @Test fun pullRejectsNewerSchema() = runBlocking<Unit> {
        val s = store(); s.pull()
        FakeStudioProvider.recipe = IconRecipe.encode(recipe).replace("\"schemaVersion\":1", "\"schemaVersion\":2")
        FakeStudioProvider.version = 9L
        assertThat(s.pull()).isFalse()
        assertThat(s.theme.value!!.version).isEqualTo(5L)
    }

    private fun repo(s: StudioThemeStore, custom: File? = null) = IconRepository(
        { ColorDrawable(Color.RED) }, File(dir, "cache-${System.nanoTime()}"), { custom },
        studioTheme = { s.theme.value }, studioImage = s::image,
    )

    @Test fun studioStyleUsesStoredThemeAfterStudioRemoved() = runBlocking<Unit> {
        store().pull()
        FakeStudioProvider.gone = true
        val r = repo(store()).icon(key, IconStyle.STUDIO, 96, 1.15f)!!
        assertThat(r.fillsTile).isTrue()
        assertThat(r.bitmap.asAndroidBitmap().getPixel(48, 48)).isEqualTo(Color.BLUE)
    }

    @Test fun customPictureBeatsStudioTheme() = runBlocking<Unit> {
        val s = store(); s.pull()
        val pic = File(dir, "custom.png").apply {
            parentFile!!.mkdirs()
            outputStream().use { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val r = repo(s, custom = pic).icon(key, IconStyle.STUDIO, 96, 1.15f)!!
        assertThat(r.bitmap.asAndroidBitmap().getPixel(48, 48)).isEqualTo(Color.GREEN)
    }

    @Test fun noThemeFallsBackToGlassDark() = runBlocking<Unit> {
        val empty = StudioThemeStore(File(dir, "none"), ctx.contentResolver)
        val r = repo(empty).icon(key, IconStyle.STUDIO, 96, 1.15f)!!
        val dark = repo(empty).icon(key, IconStyle.GLASS_DARK, 96, 1.15f)!!
        assertThat(r.fillsTile).isEqualTo(dark.fillsTile)
        assertThat(r.bitmap.asAndroidBitmap().getPixel(48, 48)).isEqualTo(dark.bitmap.asAndroidBitmap().getPixel(48, 48))
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class StudioThemeAppliedTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun themeAppliedSwitchesIconsToStudioStyle() = runBlocking<Unit> {
        FakeStudioProvider.recipe = IconRecipe.encode(IconRecipe()); FakeStudioProvider.version = 3L
        FakeStudioProvider.images = emptyMap(); FakeStudioProvider.gone = false
        Robolectric.setupContentProvider(FakeStudioProvider::class.java, StudioThemeStore.AUTHORITY)
        val sc = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        val settings = com.xenoorigin.xelauncher.settings.SettingsStore(ctx, sc, "studio-${System.nanoTime()}")
        val store = StudioThemeStore(File(ctx.filesDir, "st-${System.nanoTime()}"), ctx.contentResolver)
        var dropped = 0
        assertThat(onStudioThemeApplied(store, settings) { dropped++ }).isTrue()
        assertThat(settings.settings.first().iconStyle).isEqualTo(IconStyle.STUDIO)
        assertThat(dropped).isEqualTo(1)
        sc.cancel()
    }

    @Test fun failedPullLeavesIconStyleAlone() = runBlocking<Unit> {
        FakeStudioProvider.gone = true
        Robolectric.setupContentProvider(FakeStudioProvider::class.java, StudioThemeStore.AUTHORITY)
        val sc = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        val settings = com.xenoorigin.xelauncher.settings.SettingsStore(ctx, sc, "studio2-${System.nanoTime()}")
        val store = StudioThemeStore(File(ctx.filesDir, "st2-${System.nanoTime()}"), ctx.contentResolver)
        assertThat(onStudioThemeApplied(store, settings) {}).isFalse()
        assertThat(settings.settings.first().iconStyle).isEqualTo(IconStyle.GLASS_DARK)
        sc.cancel()
    }
}
