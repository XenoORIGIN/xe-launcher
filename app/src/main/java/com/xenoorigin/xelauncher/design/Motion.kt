package com.xenoorigin.xelauncher.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf

@kotlinx.serialization.Serializable enum class AnimationSpeed(val scale: Float) { OFF(0f), FAST(0.7f), NORMAL(1f), SMOOTH(1.35f) }

object XeMotion {
    const val staggerStepMs: Int = 28

    /** Spring tuned for [speed]; OFF is effectively instant. */
    fun <T> spring(
        speed: AnimationSpeed,
        dampingRatio: Float = 0.78f,
        stiffness: Float = Spring.StiffnessMediumLow,
    ): SpringSpec<T> = if (speed == AnimationSpeed.OFF) {
        spring(stiffness = 1_000_000f)
    } else {
        spring(dampingRatio = dampingRatio, stiffness = stiffness / (speed.scale * speed.scale))
    }

    /** The one spring sheets and panels use to arrive and leave, so everything settles the same way. */
    fun <T> settle(speed: AnimationSpeed): SpringSpec<T> = spring(speed, dampingRatio = .82f, stiffness = 420f)

    fun durationMs(base: Int, speed: AnimationSpeed): Int = (base * speed.scale).toInt()
}

val LocalAnimationSpeed = staticCompositionLocalOf { AnimationSpeed.NORMAL }

/** How far one drawer row has arrived (0..1) when the drawer is [progress] open: top rows lead, the rest follow. */
fun drawerCascade(progress: Float, row: Int): Float {
    val lag = (row.coerceAtMost(7)) * .055f
    return ((progress - lag) / (1f - .055f * 7)).coerceIn(0f, 1f).let { if (progress >= 1f) 1f else it }
}

/** A Home page's lean while swiping: [offset] is how far it is from the centre (-1..1, 0 = on screen). */
data class PageDepth(val scale: Float, val rotationY: Float, val alpha: Float)

fun pageDepth(offset: Float): PageDepth {
    val o = offset.coerceIn(-1f, 1f); val a = kotlin.math.abs(o)
    return PageDepth(1f - .1f * a, o * 16f, 1f - .4f * a)
}

/** Coming back to Home from an app: the screen settles in from slightly larger. [p] runs 0 to 1. */
fun homeReturnScale(p: Float): Float = 1.07f - .07f * p.coerceIn(0f, 1f)
