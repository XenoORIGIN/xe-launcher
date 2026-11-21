package com.xenoorigin.xestudio.link

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xestudio.core.IconOverride
import com.xenoorigin.xestudio.core.IconRecipe
import com.xenoorigin.xestudio.core.TileShape
import com.xenoorigin.xestudio.projects.ProjectKind
import com.xenoorigin.xestudio.projects.ProjectStore
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileNotFoundException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ThemeProviderTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val store = ProjectStore(File(ctx.filesDir, "projects-test"))
    private val recipe = IconRecipe(shape = TileShape.CIRCLE, overrides = mapOf("a/b#0" to IconOverride.Image("o.webp")))

    private fun installXe() = shadowOf(ctx.packageManager).installPackage(PackageInfo().apply { packageName = XeLink.XE_PACKAGE })

    private fun applied() = runBlocking {
        installXe()
        val p = store.create(ProjectKind.ICONS, "A", IconRecipe.encode(recipe))
        File(store.imageDir(p.id).apply { mkdirs() }, "o.webp").writeBytes(byteArrayOf(7, 8, 9))
        assertThat(XeLink.applyIcons(ctx, p, store)).isEqualTo(ApplyResult.APPLIED)
    }

    private fun provider() = Robolectric.buildContentProvider(ThemeProvider::class.java).create(XeLink.AUTHORITY).get()

    @Test fun providerReturnsAppliedRecipe() {
        applied()
        provider().query(Uri.parse("content://${XeLink.AUTHORITY}/icons"), null, null, null, null)!!.use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(IconRecipe.decode(c.getString(c.getColumnIndexOrThrow("recipe")))).isEqualTo(recipe)
            assertThat(c.getLong(c.getColumnIndexOrThrow("version"))).isGreaterThan(0L)
        }
    }

    @Test fun providerIsEmptyBeforeAnythingIsApplied() {
        provider().query(Uri.parse("content://${XeLink.AUTHORITY}/icons"), null, null, null, null)!!.use { c ->
            assertThat(c.count).isEqualTo(0)
        }
    }

    @Test fun providerServesOverrideImage() {
        applied()
        provider().openFile(Uri.parse("content://${XeLink.AUTHORITY}/images/o.webp"), "r")!!.use { pfd ->
            assertThat(android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).readBytes()).isEqualTo(byteArrayOf(7, 8, 9))
        }
    }

    @Test(expected = FileNotFoundException::class) fun providerRejectsPathTraversal() {
        applied()
        provider().openFile(Uri.parse("content://${XeLink.AUTHORITY}/images/..%2Ficons.json"), "r")
    }

    @Test fun applyWithoutXeReturnsXeMissing() = runBlocking<Unit> {
        val p = store.create(ProjectKind.ICONS, "A", IconRecipe.encode(recipe))
        assertThat(XeLink.applyIcons(ctx, p, store)).isEqualTo(ApplyResult.XE_MISSING)
    }

    @Test fun applySendsBroadcastToXe() {
        applied()
        val sent = shadowOf(ctx).broadcastIntents.single { it.action == XeLink.ACTION_THEME_APPLIED }
        assertThat(sent.`package`).isEqualTo(XeLink.XE_PACKAGE)
    }

    @Test fun manifestProviderRequiresSignaturePermission() {
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS)
        val prov = info.providers!!.single { it.authority == XeLink.AUTHORITY }
        assertThat(prov.readPermission).isEqualTo(XeLink.PERMISSION)
        assertThat(prov.exported).isTrue()
        val perm = info.permissions!!.single { it.name == XeLink.PERMISSION }
        assertThat(perm.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE).isEqualTo(PermissionInfo.PROTECTION_SIGNATURE)
    }
}
