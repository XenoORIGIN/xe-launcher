package com.xenoorigin.xelauncher

import android.app.Application
import com.xenoorigin.xelauncher.di.AppGraph

class XeApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        com.xenoorigin.xelauncher.bugs.CrashStore.install(
            com.xenoorigin.xelauncher.bugs.CrashStore(java.io.File(filesDir, "crashes")), BuildConfig.VERSION_NAME,
        )
        graph = AppGraph(this)
    }
}

val android.content.Context.xeGraph: AppGraph
    get() = (applicationContext as XeApplication).graph
