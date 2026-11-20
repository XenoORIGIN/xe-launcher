package com.xenoorigin.xelauncher.drawer

import androidx.compose.foundation.pager.PagerState
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.XeMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

class DrawerState(initial: Int = 0, pageCount: () -> Int) {
    val pager: PagerState = PagerState(currentPage = initial, pageCount = pageCount)

    /** Continuous page position (e.g. 2.4 while moving from 2 to 3); drives the category bubble. */
    val bubbleOffsetFraction: Float get() = pager.currentPage + pager.currentPageOffsetFraction

    suspend fun animateTo(index: Int, speed: AnimationSpeed) {
        if (speed == AnimationSpeed.OFF) pager.scrollToPage(index)
        else pager.animateScrollToPage(index, animationSpec = XeMotion.spring(speed, dampingRatio = .82f, stiffness = 300f))
    }
}

/** How much the liquid bubble stretches: 0 at rest, up to 35% midway between two categories. */
fun bubbleStretch(fraction: Float): Float {
    val f = fraction - floor(fraction)
    return (abs(sin(PI * f)) * 0.35).toFloat()
}
