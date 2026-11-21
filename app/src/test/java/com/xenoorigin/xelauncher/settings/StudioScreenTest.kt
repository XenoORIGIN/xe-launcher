package com.xenoorigin.xelauncher.settings

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.settings.ui.StudioScreen
import com.xenoorigin.xelauncher.studio.StudioThemeStore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w390dp-h844dp-xxhdpi")
class StudioScreenTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun installStudio() {
        val pkg = StudioThemeStore.STUDIO_PACKAGE
        val pm = shadowOf(app.packageManager)
        pm.installPackage(PackageInfo().apply { packageName = pkg; applicationInfo = ApplicationInfo().apply { packageName = pkg } })
        val ri = ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = pkg; name = "$pkg.MainActivity"; applicationInfo = ApplicationInfo().apply { packageName = pkg } } }
        pm.addResolveInfoForIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_INFO).setPackage(pkg), ri)
        pm.addResolveInfoForIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg), ri)
    }

    @Test fun openStudioLaunchesAppWhenInstalled() {
        installStudio()
        rule.setContent { XeTheme { StudioScreen({}, {}) } }
        rule.onNodeWithTag("open_studio").performClick()
        val started = shadowOf(app).nextStartedActivity
        assertThat(started.component?.packageName ?: started.`package`).isEqualTo(StudioThemeStore.STUDIO_PACKAGE)
    }

    @Test fun openStudioOpensDownloadWhenMissing() {
        rule.setContent { XeTheme { StudioScreen({}, {}) } }
        rule.onNodeWithTag("open_studio").performClick()
        val started = shadowOf(app).nextStartedActivity
        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.dataString).endsWith("/releases/latest/download/xe-studio.apk")
    }
}
