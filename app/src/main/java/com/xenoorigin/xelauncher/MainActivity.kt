package com.xenoorigin.xelauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A launcher never finishes on Back; screens inside handle Back themselves.
        onBackPressedDispatcher.addCallback(this) {}
        setContent { XeApp(xeGraph) }
    }

    override fun onStart() {
        super.onStart()
        runCatching { xeGraph.widgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { xeGraph.widgetHost.stopListening() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) xeGraph.homePressed.tryEmit(Unit)
    }
}
