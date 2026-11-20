package com.xenoorigin.xelauncher.home

import android.annotation.SuppressLint
import android.content.Context
import com.xenoorigin.xelauncher.settings.GestureAction
import com.xenoorigin.xelauncher.settings.XeSettings
import kotlin.math.abs

object GestureResolver {
    /** Maps a finished vertical gesture on Home to the configured action, or null. */
    fun resolve(dx: Float, dy: Float, velocityY: Float, pointers: Int, s: XeSettings, thresholdPx: Float, flingPx: Float): GestureAction? {
        if (abs(dx) > abs(dy) * 1.2f) return null
        val down = dy > thresholdPx || velocityY > flingPx
        val up = dy < -thresholdPx || velocityY < -flingPx
        val action = when {
            pointers >= 2 && down -> s.twoFingerDown
            up -> s.swipeUp
            down -> s.swipeDown
            else -> return null
        }
        return action.takeUnless { it == GestureAction.NONE }
    }
}

/** Opens the notification shade (best effort; silently does nothing if the OEM blocks it). */
@SuppressLint("WrongConstant")
fun expandNotificationShade(context: Context) {
    runCatching {
        val sbm = context.getSystemService("statusbar") ?: return
        sbm.javaClass.getMethod("expandNotificationsPanel").invoke(sbm)
    }
}
