package com.xenoorigin.xelauncher.update

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class UpdateCheckerTest {
    @Test fun newer() { assertThat(UpdateChecker.isNewer("v1.0.12", "1.0.9")).isTrue() }
    @Test fun same() { assertThat(UpdateChecker.isNewer("v1.0.9", "1.0.9")).isFalse() }
    @Test fun older() { assertThat(UpdateChecker.isNewer("v1.0.8", "1.0.9")).isFalse() }
    @Test fun garbage() { assertThat(UpdateChecker.isNewer("latest", "1.0.9")).isFalse() }

    @Test fun parsesGithubRelease() = runBlocking<Unit> {
        val json = """{"tag_name":"v1.0.20","body":"Notes","assets":[{"name":"other.txt","browser_download_url":"x"},
            {"name":"xe-launcher.apk","browser_download_url":"https://github.com/XenoORIGIN/xe-launcher/releases/download/v1.0.20/xe-launcher.apk"}]}"""
        val r = (UpdateChecker.check("1.0.3") { HttpResult(200, json) } as UpdateResult.Available).release
        assertThat(r.tag).isEqualTo("v1.0.20")
        assertThat(r.apkUrl).endsWith("/v1.0.20/xe-launcher.apk")
        assertThat(r.notes).isEqualTo("Notes")
    }

    @Test fun networkFailure_isFailed() = runBlocking<Unit> {
        assertThat(UpdateChecker.check("1.0.3") { null }).isEqualTo(UpdateResult.Failed)
    }

    @Test fun noReleasePublishedYet_isUpToDate_notAnError() = runBlocking<Unit> {
        assertThat(UpdateChecker.check("1.0.3") { HttpResult(404, """{"message":"Not Found"}""") }).isEqualTo(UpdateResult.UpToDate)
    }

    @Test fun sameVersion_isUpToDate() = runBlocking<Unit> {
        assertThat(UpdateChecker.check("1.0.20") { HttpResult(200, """{"tag_name":"v1.0.20","assets":[]}""") }).isEqualTo(UpdateResult.UpToDate)
    }

    @Test fun rateLimited_isFailed() = runBlocking<Unit> {
        assertThat(UpdateChecker.check("1.0.3") { HttpResult(403, "{}") }).isEqualTo(UpdateResult.Failed)
    }

    // A beta of a version is older than the version itself.
    @Test fun releaseIsNewerThanItsBeta() {
        assertThat(UpdateChecker.isNewer("v2.0.0", "2.0.0-beta1")).isTrue()
        assertThat(UpdateChecker.isNewer("v2.0.0", "2.0.0")).isFalse()
        assertThat(UpdateChecker.isNewer("v1.0.18", "2.0.0-beta1")).isFalse()
    }
}
