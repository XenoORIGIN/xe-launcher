package com.xenoorigin.xelauncher.wallpaper

import androidx.compose.ui.geometry.Offset

/** The wallpaper is drawn this much larger than the screen so it can shift without showing its edges. */
const val WALL_SCALE = 1.12f

/**
 * How far to shift the wallpaper so it seems to sit behind Home: across all pages it travels 8% of the
 * screen width the other way (slower than the icons), and tilting the phone nudges it up to [tiltPx].
 *
 * @param pagePos current page plus the swipe fraction (0 = first page)
 * @param tilt the phone's tilt, roughly -1..1 per axis
 */
fun parallaxOffset(pagePos: Float, pageCount: Int, tilt: Offset, widthPx: Float, tiltPx: Float): Offset {
    val travel = widthPx * .08f
    val x = if (pageCount <= 1) 0f else -((pagePos / (pageCount - 1)).coerceIn(0f, 1f) - .5f) * travel
    val tx = -tilt.x.coerceIn(-1f, 1f) * tiltPx
    val ty = tilt.y.coerceIn(-1f, 1f) * tiltPx
    return Offset(x + tx, ty)
}
