package com.xenoorigin.xelauncher.design

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/** Hands out increasing indexes so items on one screen can enter one after another. */
class Stagger { var next = 0 }

val LocalStagger = compositionLocalOf<Stagger?> { null }

/**
 * Fades and springs content up into place the first time it is shown.
 * [index] staggers items ([stepMs] apart); [distance] is how far below it starts, in px.
 * Respects the user's animation speed (off = no animation).
 */
fun Modifier.appear(index: Int = 0, distance: Float = 70f, stepMs: Long = 40, scaleFrom: Float = .96f): Modifier = composed {
    val speed = LocalAnimationSpeed.current
    val off = speed == AnimationSpeed.OFF
    val p = remember { Animatable(if (off) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!off) {
            delay((index * stepMs).coerceAtMost(500))
            p.animateTo(1f, XeMotion.spring(speed, dampingRatio = .78f, stiffness = 260f))
        }
    }
    graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * distance
        val sc = scaleFrom + (1f - scaleFrom) * v
        scaleX = sc; scaleY = sc
    }
}
