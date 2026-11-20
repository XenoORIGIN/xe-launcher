package com.xenoorigin.xelauncher.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.data.AppCategoryOverride
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.data.FolderEntity
import com.xenoorigin.xelauncher.data.HomeItemEntity
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupManagerTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun db() = Room.inMemoryDatabaseBuilder(ctx, XeDatabase::class.java).allowMainThreadQueries().build()
    private fun store(name: String) = SettingsStore(ctx, CoroutineScope(SupervisorJob() + Dispatchers.IO), name)

    @Test fun exportThenImport_reproducesEverything() = runBlocking<Unit> {
        val srcDb = db(); val srcSettings = store("bk_src")
        srcDb.categories().insert(CategoryEntity(builtIn = "OTHER", iconName = "category", position = 0))
        val mine = srcDb.categories().insert(CategoryEntity(name = "งาน", iconName = "briefcase", position = 1))
        srcDb.overrides().upsert(AppCategoryOverride("a/b#0", mine))
        val fid = srcDb.home().insertFolder(FolderEntity(name = "Folder"))
        srcDb.home().insert(HomeItemEntity(type = "APP", appKey = "a/b#0", container = "DOCK", position = 0))
        srcDb.home().insert(HomeItemEntity(type = "FOLDER", folderId = fid, container = "PAGE", x = 1))
        srcSettings.update { it.copy(iconStyle = IconStyle.ORIGINAL, homeColumns = 5) }
        val wp = File(ctx.cacheDir, "wp.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val out = ByteArrayOutputStream()
        BackupManager(srcDb, srcSettings, { wp }, {}).export(out)

        val dstDb = db(); val dstSettings = store("bk_dst")
        var restoredWallpaper: ByteArray? = null
        val r = BackupManager(dstDb, dstSettings, { null }, { restoredWallpaper = it.readBytes() }).import(ByteArrayInputStream(out.toByteArray()))
        assertThat(r.isSuccess).isTrue()
        assertThat(dstDb.categories().all().map { it.name ?: it.builtIn }).containsExactly("OTHER", "งาน").inOrder()
        assertThat(dstDb.overrides().all().single().appKey).isEqualTo("a/b#0")
        assertThat(dstDb.home().all()).hasSize(2)
        assertThat(dstDb.home().folders().single().name).isEqualTo("Folder")
        assertThat(dstSettings.current().homeColumns).isEqualTo(5)
        assertThat(dstSettings.current().iconStyle).isEqualTo(IconStyle.ORIGINAL)
        assertThat(restoredWallpaper).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test fun corruptFile_fails_andChangesNothing() = runBlocking<Unit> {
        val dstDb = db(); val dstSettings = store("bk_corrupt")
        dstDb.categories().insert(CategoryEntity(builtIn = "OTHER", iconName = "category", position = 0))
        val r = BackupManager(dstDb, dstSettings, { null }, {}).import(ByteArrayInputStream("not a zip".toByteArray()))
        assertThat(r.isFailure).isTrue()
        assertThat(dstDb.categories().all()).hasSize(1)
    }
}
