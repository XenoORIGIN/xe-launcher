package com.xenoorigin.xelauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A launcher never finishes on Back; screens inside handle Back themselves.
        onBackPressedDispatcher.addCallback(this) {}
        preferFastestRefresh()
        setContent { XeApp(xeGraph) }
    }

    override fun onStart() {
        super.onStart()
        runCatching { xeGraph.widgetHost.startListening() }
        // Coming back to Home is a good moment to look for a new version (at most every 10 minutes).
        xeGraph.scope.launch { xeGraph.updates.checkIfDue() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { xeGraph.widgetHost.stopListening() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) xeGraph.homePressed.tryEmit(Unit)
    }

    /**
     * Asks for the screen's fastest mode at the current resolution (90 Hz on the Galaxy A16), so motion is
     * as smooth as the panel allows. The system still has the final say (e.g. "Standard" motion smoothness).
     */
    private fun preferFastestRefresh() {
        if (android.os.Build.VERSION.SDK_INT < 30) return
        val d = runCatching { display }.getOrNull() ?: return
        val now = d.mode
        val best = d.supportedModes.filter { it.physicalWidth == now.physicalWidth && it.physicalHeight == now.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        if (best.refreshRate > now.refreshRate + .5f || window.attributes.preferredDisplayModeId != best.modeId) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
        }
    }
}
