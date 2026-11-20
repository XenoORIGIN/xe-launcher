package com.xenoorigin.xelauncher.apps

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Keeps the list of launchable apps for every profile up to date. */
class AppCatalog(
    private val platform: AppPlatform,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val ownPackage: String? = null,
) {
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()
    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 64)
    /** Package names that were added, removed or updated. */
    val packageChanges: SharedFlow<String> = _changes.asSharedFlow()
    private var unregister: (() -> Unit)? = null

    fun start() {
        if (unregister != null) return
        unregister = platform.listen(
            onPackageChanged = { pkg, serial -> scope.launch(io) { refreshPackage(pkg, serial) } },
            onProfilesChanged = { scope.launch(io) { refreshAll() } },
        )
        scope.launch(io) { refreshAll() }
    }

    fun stop() { unregister?.invoke(); unregister = null }

    fun refreshAll() {
        _apps.value = platform.profiles().flatMap { p -> platform.activities(p).toEntries(p) }
    }

    private fun refreshPackage(pkg: String, serial: Long) {
        val profile = platform.profiles().firstOrNull { it.serial == serial } ?: return refreshAll()
        val fresh = platform.activities(profile, pkg).toEntries(profile)
        _apps.update { cur -> cur.filterNot { it.key.packageName == pkg && it.key.userSerial == serial } + fresh }
        _changes.tryEmit(pkg)
    }

    private fun List<RawActivity>.toEntries(p: Profile) = filter { it.packageName != ownPackage }.map {
        AppEntry(AppKey(it.packageName, it.activity, p.serial), it.label, it.androidCategory, it.installedAt, isClone = !p.isMain)
    }
}
