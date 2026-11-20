package com.xenoorigin.xelauncher.apps

data class Profile(val serial: Long, val isMain: Boolean)

data class RawActivity(
    val packageName: String,
    val activity: String,
    val label: String,
    val androidCategory: Int,
    val installedAt: Long,
)

/** Thin seam over LauncherApps/UserManager so the catalog logic is testable. */
interface AppPlatform {
    fun profiles(): List<Profile>
    fun activities(profile: Profile, packageName: String? = null): List<RawActivity>
    /** Returns an unregister function. */
    fun listen(onPackageChanged: (packageName: String, userSerial: Long) -> Unit, onProfilesChanged: () -> Unit): () -> Unit
}
