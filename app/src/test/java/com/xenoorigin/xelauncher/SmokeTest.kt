package com.xenoorigin.xelauncher

import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SmokeTest {
    @Test
    fun mainActivity_reachesResumed() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            assertThat(s.state).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun manifest_declaresHomeActivity() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(ctx.packageName)
        val res = ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        assertThat(res.map { it.activityInfo.name }).contains(MainActivity::class.java.name)
    }
}
