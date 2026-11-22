package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.widgets.Span
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PresetNames = listOf("S", "M", "L", "XL")

/**
 * Edits one widget right where it sits: drag it to move, drag a corner to resize (a chip shows the
 * size as it changes), or tap a size preset. Snaps to the grid; spots that are taken show red and are
 * refused. Tapping outside finishes.
 *
 * @param gridBounds the page grid in root coordinates
 * @param taken cells other items cover on this page
 * @param content draws the widget at a given size (live preview while resizing)
 * @param onPlace called when a new place fits; returns false if it couldn't be saved
 */
@Composable
fun WidgetEditOverlay(
    cell: WidgetCell, gridBounds: Rect, cols: Int, rows: Int, taken: Set<Pair<Int, Int>>,
    min: Span, max: Span, presets: List<Span>,
    content: @Composable (Span) -> Unit,
    onPlace: (Placement) -> Unit, onRemove: () -> Unit, onDone: () -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf(Offset.Zero) }
    var rect by remember(cell.id) { mutableStateOf(CellRect(cell.x, cell.y, cell.spanX, cell.spanY)) }
    var handle by remember { mutableStateOf<Handle?>(null) }
    var dragPx by remember { mutableStateOf(Offset.Zero) }
    var proposal by remember { mutableStateOf<Placement?>(null) }
    val cw = gridBounds.width / cols; val ch = gridBounds.height / rows

    fun propose(h: Handle, d: Offset) = proposePlacement(rect, d.x / cw, d.y / ch, h, cols, rows, min, max, taken)

    // The frame glides between snapped places on a spring.
    val fx = remember { Animatable(0f) }; val fy = remember { Animatable(0f) }
    val fw = remember { Animatable(0f) }; val fh = remember { Animatable(0f) }
    val shown = proposal?.takeIf { handle != null && handle != Handle.BODY }?.let { CellRect(it.x, it.y, it.w, it.h) } ?: rect
    LaunchedEffect(shown, gridBounds, origin) {
        val tx = gridBounds.left - origin.x + shown.x * cw; val ty = gridBounds.top - origin.y + shown.y * ch
        val first = fw.value == 0f
        val sp = spring<Float>(dampingRatio = .72f, stiffness = 520f)
        if (first) { fx.snapTo(tx); fy.snapTo(ty); fw.snapTo(shown.w * cw); fh.snapTo(shown.h * ch) } else {
            launch { fx.animateTo(tx, sp) }; launch { fy.animateTo(ty, sp) }
            launch { fw.animateTo(shown.w * cw, sp) }; launch { fh.animateTo(shown.h * ch, sp) }
        }
    }

    val wobble by rememberInfiniteTransition(label = "wobble").animateFloat(
        -.5f, .5f, infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse), label = "w",
    )

    Box(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }
            .background(Color.Black.copy(alpha = .38f))
            .pointerInput(Unit) { detectTapGestures { onDone() } },
    ) {
        val p = proposal
        // Where it will land while the body is being dragged: white if it fits, red if not.
        if (p != null && handle == Handle.BODY) {
            Box(
                Modifier.offset { IntOffset((gridBounds.left - origin.x + p.x * cw).roundToInt(), (gridBounds.top - origin.y + p.y * ch).roundToInt()) }
                    .size(with(density) { (p.w * cw).toDp() }, with(density) { (p.h * ch).toDp() })
                    .padding(6.dp).clip(RoundedCornerShape(26.dp))
                    .background(if (p.fits) Color.White.copy(alpha = .16f) else Color(0x66FF453A))
                    .border(2.dp, if (p.fits) Color.White.copy(alpha = .7f) else Color(0xFFFF453A), RoundedCornerShape(26.dp)),
            )
        }

        val bodyOffset = if (handle == Handle.BODY) dragPx else Offset.Zero
        val bad = p != null && !p.fits && handle != null
        val span = Span(shown.w, shown.h)
        Box(
            Modifier.offset { IntOffset((fx.value + bodyOffset.x).roundToInt(), (fy.value + bodyOffset.y).roundToInt()) }
                .size(with(density) { fw.value.toDp() }, with(density) { fh.value.toDp() })
                .graphicsLayer {
                    val lifted = handle == Handle.BODY
                    scaleX = if (lifted) 1.04f else 1f; scaleY = scaleX
                    rotationZ = if (handle == null) wobble else 0f
                    shadowElevation = if (lifted) 24f else 8f
                }
                .testTag("widget_edit_frame"),
        ) {
            Box(Modifier.fillMaxSize().padding(6.dp)) { content(span) }
            Box(
                Modifier.fillMaxSize().padding(4.dp)
                    .border(2.dp, if (bad) Color(0xFFFF453A) else Color.White.copy(alpha = .85f), RoundedCornerShape(28.dp))
                    .pointerInput(rect) {
                        detectDragGestures(
                            onDragStart = { handle = Handle.BODY; dragPx = Offset.Zero; proposal = propose(Handle.BODY, Offset.Zero); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDragEnd = { finish(handle, proposal, { rect = it }, onPlace, haptic); handle = null; proposal = null; dragPx = Offset.Zero },
                            onDragCancel = { handle = null; proposal = null; dragPx = Offset.Zero },
                        ) { ch2, d ->
                            ch2.consume(); dragPx += d
                            val np = propose(Handle.BODY, dragPx)
                            if (np != proposal) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            proposal = np
                        }
                    },
            )
            // Corner handles: big touch targets, small glass dots.
            for (h in listOf(Handle.TOP_LEFT, Handle.TOP_RIGHT, Handle.BOTTOM_LEFT, Handle.BOTTOM_RIGHT)) {
                val align = when (h) {
                    Handle.TOP_LEFT -> Alignment.TopStart; Handle.TOP_RIGHT -> Alignment.TopEnd
                    Handle.BOTTOM_LEFT -> Alignment.BottomStart; else -> Alignment.BottomEnd
                }
                Box(
                    Modifier.align(align).offset(if (align == Alignment.TopStart || align == Alignment.BottomStart) (-18).dp else 18.dp, if (align == Alignment.TopStart || align == Alignment.TopEnd) (-18).dp else 18.dp)
                        .size(48.dp).testTag("handle_${h.name}")
                        .pointerInput(rect, h) {
                            var acc = Offset.Zero
                            detectDragGestures(
                                onDragStart = { acc = Offset.Zero; handle = h; proposal = propose(h, Offset.Zero) },
                                onDragEnd = { finish(handle, proposal, { rect = it }, onPlace, haptic); handle = null; proposal = null },
                                onDragCancel = { handle = null; proposal = null },
                            ) { c2, d ->
                                c2.consume(); acc += d
                                val np = propose(h, acc)
                                if (np != proposal) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                proposal = np
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White).border(3.dp, Color.Black.copy(alpha = .25f), CircleShape))
                }
            }
            // The size, live.
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = (-34).dp).clip(RoundedCornerShape(14.dp))
                    .background(if (bad) Color(0xFFFF453A) else Color.White).padding(horizontal = 10.dp, vertical = 3.dp),
            ) { Text("${shown.w}×${shown.h}", style = XeType.Label, color = if (bad) Color.White else Color.Black, modifier = Modifier.testTag("size_label")) }
        }

        // Toolbar: below the widget when there's room, otherwise above it.
        val below = (shown.y + shown.h) <= rows - 1
        val barY = if (below) fy.value + fh.value + with(density) { 10.dp.toPx() } else fy.value - with(density) { 64.dp.toPx() }
        Row(
            Modifier.offset { IntOffset((gridBounds.left - origin.x).roundToInt().coerceAtLeast(0), barY.roundToInt().coerceAtLeast(0)) }
                .padding(horizontal = 12.dp)
                .glassTile(RoundedCornerShape(26.dp), LocalGlassStyle.current, Color.White.copy(alpha = .12f))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            // Delete and Done always keep their full size; the size chips share what's left and scroll
            // sideways on small screens or with large text.
            Row(
                Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
            presets.forEachIndexed { i, s ->
                val on = s.x == rect.w && s.y == rect.h
                Box(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(if (on) Color.White else Color.White.copy(alpha = .1f))
                        .clickable {
                            val pl = proposePlacement(rect, 0f, 0f, Handle.BOTTOM_RIGHT, cols, rows, s, s, taken)
                                .let { if (it.fits) it else fitSomewhere(rect, s, cols, rows, taken) }
                            if (pl.fits) { scope.launch { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }; rect = CellRect(pl.x, pl.y, pl.w, pl.h); onPlace(pl) }
                            else haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp).testTag("preset_$i"),
                ) { Text("${PresetNames.getOrElse(i) { "" }} ${s.x}×${s.y}", style = XeType.Label, color = if (on) Color.Black else Color.White, maxLines = 1, softWrap = false) }
            }
            }
            Box(Modifier.size(38.dp).clip(CircleShape).clickable(remember { MutableInteractionSource() }, null) { onRemove() }.testTag("widget_edit_remove"), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ti_trash), stringResource(R.string.widget_remove), Modifier.size(20.dp), tint = Color(0xFFFF6B6B))
            }
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White).clickable { onDone() }.testTag("widget_edit_done"), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ti_check), stringResource(R.string.done), Modifier.size(20.dp), tint = Color.Black)
            }
        }
    }
}

private fun finish(
    handle: Handle?, p: Placement?, setRect: (CellRect) -> Unit, onPlace: (Placement) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    if (handle == null || p == null) return
    if (p.fits) { setRect(CellRect(p.x, p.y, p.w, p.h)); onPlace(p) }
    else haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // springs back to where it was
}

/** For a size preset that doesn't fit where the widget is: the nearest free spot on the page, if any. */
private fun fitSomewhere(r: CellRect, s: Span, cols: Int, rows: Int, taken: Set<Pair<Int, Int>>): Placement {
    var best: Placement? = null; var bestD = Int.MAX_VALUE
    for (y in 0..rows - s.y) for (x in 0..cols - s.x) {
        val ok = (x until x + s.x).all { cx -> (y until y + s.y).all { cy -> (cx to cy) !in taken } }
        if (!ok) continue
        val d = kotlin.math.abs(x - r.x) + kotlin.math.abs(y - r.y)
        if (d < bestD) { bestD = d; best = Placement(x, y, s.x, s.y, true) }
    }
    return best ?: Placement(r.x, r.y, s.x, s.y, false)
}
