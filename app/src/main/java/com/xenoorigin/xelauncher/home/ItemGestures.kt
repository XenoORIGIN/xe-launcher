package com.xenoorigin.xelauncher.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

/** Callbacks for an icon on Home: tap, long-press (menu) and long-press-then-drag (move). Positions are in root coordinates. */
class ItemGestureCallbacks(
    val toRoot: (Offset) -> Offset,
    val onTap: () -> Unit,
    val onLongPress: () -> Unit,
    val onDragStart: (Offset) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onDragEnd: (Offset) -> Unit,
    val onDragCancel: () -> Unit,
)

/**
 * Tap → onTap. Hold → onLongPress, and if the finger then moves → drag. A quick move before the hold is
 * left alone so the pager/drawer gestures still work. While [editMode] is on, dragging starts right away.
 *
 * The gesture is keyed only on [key] and reads [editMode]/[callbacks] fresh each time, so switching to
 * edit mode in the middle of a drag (which the app does) doesn't cancel the drag.
 */
fun Modifier.homeItemGestures(key: Any?, editMode: () -> Boolean, callbacks: () -> ItemGestureCallbacks): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = down.position
        val slop = viewConfiguration.touchSlop

        suspend fun AwaitPointerEventScope.dragLoop(alreadyDragging: Boolean) {
            var dragging = alreadyDragging
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val c = ev.changes.firstOrNull { it.id == down.id }
                val cb = callbacks()
                if (c == null) { if (dragging) cb.onDragCancel(); break }
                if (c.changedToUp()) { if (dragging) cb.onDragEnd(cb.toRoot(c.position)); c.consume(); break }
                if (!dragging && (c.position - start).getDistance() > slop) { dragging = true; cb.onDragStart(cb.toRoot(start)) }
                if (dragging) cb.onDrag(cb.toRoot(c.position))
                c.consume()
            }
        }

        if (editMode()) {
            // Edit mode: move as soon as the finger moves; a tap is still a tap.
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val c = ev.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (c.changedToUp()) { callbacks().onTap(); return@awaitEachGesture }
                if ((c.position - start).getDistance() > slop) {
                    val cb = callbacks()
                    cb.onDragStart(cb.toRoot(start)); cb.onDrag(cb.toRoot(c.position)); c.consume()
                    dragLoop(alreadyDragging = true)
                    return@awaitEachGesture
                }
            }
        }

        val phase1 = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val ev = awaitPointerEvent()
                val c = ev.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull "gone"
                if (c.changedToUp()) return@withTimeoutOrNull "up"
                if ((c.position - start).getDistance() > slop || c.isConsumed) return@withTimeoutOrNull "moved"
            }
            @Suppress("UNREACHABLE_CODE") "gone"
        }
        when (phase1) {
            "up" -> callbacks().onTap()
            "moved", "gone" -> Unit
            else -> {
                callbacks().onLongPress()
                dragLoop(alreadyDragging = false)
            }
        }
    }
}
