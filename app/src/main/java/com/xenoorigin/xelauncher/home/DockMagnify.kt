package com.xenoorigin.xelauncher.home

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

const val DOCK_MAX_SCALE = 1.45f
private const val REACH = 1.8f

/**
 * How big a dock icon is drawn while a finger slides along the dock: [DOCK_MAX_SCALE] right under the
 * finger, easing (cosine) back to 1 about two slots away. No finger = 1.
 */
fun dockScale(fingerX: Float?, slotCenterX: Float, slotWidth: Float): Float {
    if (fingerX == null || slotWidth <= 0f) return 1f
    val d = abs(fingerX - slotCenterX) / slotWidth
    if (d >= REACH) return 1f
    val k = (cos(d / REACH * PI).toFloat() + 1f) / 2f
    return 1f + (DOCK_MAX_SCALE - 1f) * k
}
