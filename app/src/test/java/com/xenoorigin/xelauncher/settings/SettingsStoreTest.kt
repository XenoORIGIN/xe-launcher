package com.xenoorigin.xelauncher.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.GlassQuality
import com.xenoorigin.xelauncher.icons.IconStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsStoreTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test fun freshStore_hasSpecDefaults() = runBlocking<Unit> {
        val s = SettingsStore(ctx, scope(), "t_defaults").settings.first()
        assertThat(s.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(s.iconStyle).isEqualTo(IconStyle.GLASS_DARK)
        assertThat(s.glassQuality).isEqualTo(GlassQuality.NORMAL)
        assertThat(s.animationSpeed).isEqualTo(AnimationSpeed.NORMAL)
        assertThat(s.wallpaperId).isEqualTo("xe_black")
        assertThat(s.swipeDown).isEqualTo(GestureAction.NOTIFICATIONS)
    }

    @Test fun update_survivesNewInstance() = runBlocking<Unit> {
        val job = SupervisorJob()
        SettingsStore(ctx, CoroutineScope(job + Dispatchers.IO), "t_persist").update { it.copy(homeColumns = 5, hiddenApps = setOf("a/b#0")) }
        job.cancelAndJoin()
        val again = SettingsStore(ctx, scope(), "t_persist").settings.first()
        assertThat(again.homeColumns).isEqualTo(5)
        assertThat(again.hiddenApps).containsExactly("a/b#0")
    }

    @Test fun export_import_roundTrip() = runBlocking<Unit> {
        val a = SettingsStore(ctx, scope(), "t_export")
        a.update { it.copy(iconStyle = IconStyle.GLASS_CLEAR, language = AppLanguage.TH) }
        val json = a.exportJson()
        val b = SettingsStore(ctx, scope(), "t_import")
        b.importJson(json)
        assertThat(b.settings.first()).isEqualTo(a.settings.first())
    }

    @Test fun import_unknownEnum_fallsBackToDefault() = runBlocking<Unit> {
        val b = SettingsStore(ctx, scope(), "t_unknown")
        b.importJson("""{"iconStyle":"HOLOGRAM","homeColumns":6,"someFutureKey":1}""")
        val s = b.settings.first()
        assertThat(s.iconStyle).isEqualTo(IconStyle.GLASS_DARK)
        assertThat(s.homeColumns).isEqualTo(6)
    }
}
