package com.xenoorigin.xelauncher.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Swipe the row left past a threshold to run [onToggle]; the row follows the finger (with resistance)
 * and springs back. A tick of haptics marks the moment it will trigger.
 */
fun Modifier.swipeToToggle(enabled: Boolean = true, onToggle: () -> Unit): Modifier = if (!enabled) this else composed {
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    this
        .graphicsLayer { translationX = x.value }
        .pointerInput(Unit) {
            val threshold = 72.dp.toPx()
            var armed = false
            detectHorizontalDragGestures(
                onDragStart = { armed = false },
                onDragEnd = {
                    if (armed) onToggle()
                    scope.launch { x.animateTo(0f, XeMotion.spring(AnimationSpeed.NORMAL, dampingRatio = .6f, stiffness = 420f)) }
                },
                onDragCancel = { scope.launch { x.animateTo(0f) } },
            ) { change, dx ->
                change.consume()
                // Left only, and heavier the further it goes.
                val next = (x.value + dx * (if (x.value < -threshold) .35f else 1f)).coerceIn(-threshold * 1.8f, 0f)
                scope.launch { x.snapTo(next) }
                val nowArmed = next < -threshold
                if (nowArmed != armed) { armed = nowArmed; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            }
        }
}
