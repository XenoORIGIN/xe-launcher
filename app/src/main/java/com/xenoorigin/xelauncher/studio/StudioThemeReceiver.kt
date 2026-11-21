package com.xenoorigin.xelauncher.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.settings.SettingsStore
import com.xenoorigin.xelauncher.xeGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Copies the newly applied theme and switches Xe's icons to it. Only a successful copy changes the
 * icon style, so a broken or missing Studio never leaves Xe without icons.
 */
suspend fun onStudioThemeApplied(store: StudioThemeStore, settings: SettingsStore, dropIcons: () -> Unit): Boolean {
    if (!store.pull()) return false
    dropIcons()
    settings.update { it.copy(iconStyle = IconStyle.STUDIO) }
    return true
}

/** Xe Studio says "a theme was applied". The manifest only lets apps signed with Xe's key send this. */
class StudioThemeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != StudioThemeStore.ACTION_THEME_APPLIED) return
        val graph = context.xeGraph
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { onStudioThemeApplied(graph.studioTheme, graph.settings, graph.icons::dropStudio) } finally { pending.finish() }
        }
    }
}
