package com.xenoorigin.xelauncher.design

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex

/** Moves the item at [from] by [steps] places (negative = up), clamped to the list. Pure, for tests. */
fun <T> List<T>.moved(from: Int, steps: Int): List<T> {
    if (from !in indices) return this
    val to = (from + steps).coerceIn(0, lastIndex)
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * A column whose rows can be dragged into a new order by the handle each row places with the given
 * modifier. Rows must all be [rowHeight] tall. [onMoved] gets the new order when the finger lifts.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    rowHeight: Dp,
    spacing: Dp,
    onMoved: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, handle: Modifier, dragging: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val step = with(LocalDensity.current) { (rowHeight + spacing).toPx() }
    val order = remember(items.map(key)) { mutableStateListOf<T>().apply { addAll(items) } }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var moves by remember { mutableIntStateOf(0) }
    // If the list is replaced from outside mid-drag, nothing may stay lifted.
    androidx.compose.runtime.LaunchedEffect(items.map(key)) { if (draggingKey != null && order.none { key(it) == draggingKey }) { draggingKey = null; offset = 0f } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        order.forEachIndexed { index, item -> androidx.compose.runtime.key(key(item)) {
            val k = key(item)
            val dragging = draggingKey == k
            // Rows that get pushed aside glide to their new place on a spring instead of jumping.
            val slide = remember { androidx.compose.animation.core.Animatable(0f) }
            var lastIndex by remember { mutableIntStateOf(index) }
            androidx.compose.runtime.LaunchedEffect(index) {
                if (index != lastIndex && !dragging) {
                    slide.snapTo((lastIndex - index) * step)
                    slide.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = .72f, stiffness = 520f))
                }
                lastIndex = index
            }
            val handle = Modifier.pointerInput(k) {
                detectDragGestures(
                    onDragStart = { draggingKey = k; offset = 0f; moves = 0; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDragEnd = { draggingKey = null; offset = 0f; if (moves != 0) onMoved(order.toList()) },
                    onDragCancel = { draggingKey = null; offset = 0f },
                ) { change, drag ->
                    change.consume()
                    offset += drag.y
                    val i = order.indexOfFirst { key(it) == k }
                    if (offset > step / 2 && i < order.lastIndex) {
                        order.add(i + 1, order.removeAt(i)); offset -= step; moves++
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (offset < -step / 2 && i > 0) {
                        order.add(i - 1, order.removeAt(i)); offset += step; moves++
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth().height(rowHeight)
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        if (dragging) { translationY = offset; scaleX = 1.03f; scaleY = 1.03f; shadowElevation = 12f }
                        else translationY = slide.value
                    },
            ) { row(item, handle, dragging) }
        } }
    }
}
