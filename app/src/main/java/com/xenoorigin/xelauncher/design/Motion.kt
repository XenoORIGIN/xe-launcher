package com.xenoorigin.xelauncher.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf

enum class AnimationSpeed(val scale: Float) { OFF(0f), FAST(0.7f), NORMAL(1f), SMOOTH(1.35f) }

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

    fun durationMs(base: Int, speed: AnimationSpeed): Int = (base * speed.scale).toInt()
}

val LocalAnimationSpeed = staticCompositionLocalOf { AnimationSpeed.NORMAL }
