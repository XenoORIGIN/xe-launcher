package com.xenoorigin.xelauncher

import android.app.Application
import com.xenoorigin.xelauncher.di.AppGraph

class XeApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

val android.content.Context.xeGraph: AppGraph
    get() = (applicationContext as XeApplication).graph
