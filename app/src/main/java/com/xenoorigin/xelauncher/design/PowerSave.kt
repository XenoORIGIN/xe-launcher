package com.xenoorigin.xelauncher.design

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** True while the phone's battery saver is on; decorative motion pauses then. */
@Composable
fun rememberPowerSave(): Boolean {
    val ctx = LocalContext.current
    val pm = remember { ctx.getSystemService(PowerManager::class.java) }
    var on by remember { mutableStateOf(pm?.isPowerSaveMode == true) }
    DisposableEffect(pm) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { on = pm?.isPowerSaveMode == true }
        }
        runCatching { ctx.registerReceiver(r, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)) }
        onDispose { runCatching { ctx.unregisterReceiver(r) } }
    }
    return on
}
