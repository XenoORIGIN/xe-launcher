package com.xenoorigin.xelauncher.apps

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Test

private class FakePlatform : AppPlatform {
    val profiles = mutableListOf(Profile(0, isMain = true))
    val activities = mutableMapOf<Long, MutableList<RawActivity>>(0L to mutableListOf())
    var pkgListener: ((String, Long) -> Unit)? = null
    var profileListener: (() -> Unit)? = null
    override fun profiles() = profiles.toList()
    override fun activities(profile: Profile, packageName: String?) =
        activities[profile.serial].orEmpty().filter { packageName == null || it.packageName == packageName }
    override fun listen(onPackageChanged: (String, Long) -> Unit, onProfilesChanged: () -> Unit): () -> Unit {
        pkgListener = onPackageChanged; profileListener = onProfilesChanged
        return { pkgListener = null; profileListener = null }
    }
}

private fun raw(pkg: String, label: String) = RawActivity(pkg, "$pkg.Main", label, -1, 0L)

@OptIn(ExperimentalCoroutinesApi::class)
class AppCatalogTest {
    @Test fun sameAppInTwoProfiles_givesTwoEntries_secondIsClone() = runTest(UnconfinedTestDispatcher()) {
        val p = FakePlatform()
        p.profiles += Profile(95, isMain = false)
        p.activities[0]!! += raw("com.samsung.android.messaging", "Messages")
        p.activities[95] = mutableListOf(raw("com.samsung.android.messaging", "Messages"))
        val c = AppCatalog(p, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        c.start()
        val apps = c.apps.value
        assertThat(apps).hasSize(2)
        assertThat(apps.single { it.key.userSerial == 95L }.isClone).isTrue()
        assertThat(apps.single { it.key.userSerial == 0L }.isClone).isFalse()
    }

    @Test fun packageRemoved_disappears() = runTest(UnconfinedTestDispatcher()) {
        val p = FakePlatform()
        p.activities[0]!! += listOf(raw("a.b", "Alpha"), raw("c.d", "Charlie"))
        val c = AppCatalog(p, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        c.start()
        p.activities[0]!!.removeAll { it.packageName == "a.b" }
        p.pkgListener!!.invoke("a.b", 0)
        assertThat(c.apps.value.map { it.label }).containsExactly("Charlie")
    }

    @Test fun packageAdded_appears() = runTest(UnconfinedTestDispatcher()) {
        val p = FakePlatform()
        val c = AppCatalog(p, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        c.start()
        p.activities[0]!! += raw("new.app", "New")
        p.pkgListener!!.invoke("new.app", 0)
        assertThat(c.apps.value.map { it.label }).containsExactly("New")
    }

    @Test fun ownPackage_isExcluded() = runTest(UnconfinedTestDispatcher()) {
        val p = FakePlatform()
        p.activities[0]!! += listOf(raw("com.xenoorigin.xelauncher", "Xe Launcher"), raw("x.y", "Other"))
        val c = AppCatalog(p, backgroundScope, UnconfinedTestDispatcher(testScheduler), ownPackage = "com.xenoorigin.xelauncher")
        c.start()
        assertThat(c.apps.value.map { it.label }).containsExactly("Other")
    }

    @Test fun packageChange_isAnnounced() = runTest(UnconfinedTestDispatcher()) {
        val p = FakePlatform()
        val c = AppCatalog(p, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val seen = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { c.packageChanges.collect { seen += it } }
        c.start()
        p.pkgListener!!.invoke("x.y", 0)
        assertThat(seen).containsExactly("x.y")
    }
}
