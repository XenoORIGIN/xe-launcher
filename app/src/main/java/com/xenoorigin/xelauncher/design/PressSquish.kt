package com.xenoorigin.xelauncher.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

val LocalPressSquish = androidx.compose.runtime.staticCompositionLocalOf { true }

/** Liquid "squish" on press: shrinks a little wider than tall, then springs back. */
fun Modifier.pressSquish(interaction: MutableInteractionSource, haptics: Boolean = true): Modifier = composed {
    val enabled = LocalPressSquish.current
    val speed = LocalAnimationSpeed.current
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressed) { if (pressed && haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val active = pressed && enabled
    val sx by animateFloatAsState(if (active) 0.94f else 1f, XeMotion.spring(speed, dampingRatio = 0.5f), label = "sx")
    val sy by animateFloatAsState(if (active) 0.96f else 1f, XeMotion.spring(speed, dampingRatio = 0.5f), label = "sy")
    graphicsLayer { scaleX = sx; scaleY = sy }
}
