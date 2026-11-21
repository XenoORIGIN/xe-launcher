package com.xenoorigin.xelauncher.update

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class UpdateNotifierTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val rel = ReleaseInfo("v1.0.9", "https://x/xe-launcher.apk", "")

    @Test fun newRelease_notifiesOnce_perVersion() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "upd1-${System.nanoTime()}")
        val posted = mutableListOf<ReleaseInfo>()
        val n = UpdateNotifier(settings, check = { UpdateResult.Available(rel) }, post = { posted += it })
        n.checkNow(); n.checkNow()
        assertThat(posted).containsExactly(rel)
        assertThat(n.available.value).isEqualTo(rel)
        assertThat(settings.settings.first().lastNotifiedTag).isEqualTo("v1.0.9")
        sc.cancel()
    }

    @Test fun notificationsOff_stillShowsInApp_butDoesNotPost() = runBlocking<Unit> {
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(ctx, sc, "upd2-${System.nanoTime()}")
        settings.update { it.copy(updateNotify = false) }
        val posted = mutableListOf<ReleaseInfo>()
        val n = UpdateNotifier(settings, check = { UpdateResult.Available(rel) }, post = { posted += it })
        n.checkNow()
        assertThat(posted).isEmpty()
        assertThat(n.available.value).isEqualTo(rel)
        sc.cancel()
    }
}
