package com.xenoorigin.xelauncher.apps

import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import kotlinx.coroutines.flow.StateFlow

interface AppSource {
    val apps: StateFlow<List<AppEntry>>
    val packageChanges: kotlinx.coroutines.flow.Flow<String>
    fun launch(key: AppKey, sourceBounds: Rect?, opts: Bundle?)
    fun openAppInfo(key: AppKey)
    fun uninstall(key: AppKey)
    fun shortcuts(key: AppKey): List<ShortcutInfo>
    fun allShortcuts(): List<ShortcutInfo>
    fun startShortcut(s: ShortcutInfo, bounds: Rect?)
    fun icon(key: AppKey, densityDpi: Int): Drawable?
    fun shortcutIcon(s: ShortcutInfo, densityDpi: Int): Drawable?
}
