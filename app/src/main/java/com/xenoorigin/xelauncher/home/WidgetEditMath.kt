package com.xenoorigin.xelauncher.home

import com.xenoorigin.xelauncher.widgets.Span
import kotlin.math.roundToInt

/** A widget's place on the grid, in cells. */
data class CellRect(val x: Int, val y: Int, val w: Int, val h: Int)

/** Where a widget would land, and whether it can (inside the grid, no other item underneath). */
data class Placement(val x: Int, val y: Int, val w: Int, val h: Int, val fits: Boolean)

/** What the finger is holding while editing a widget. */
enum class Handle { BODY, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * The placement for a drag of ([dx], [dy]) cells on [handle], starting from [start]. Moves keep the size;
 * corner drags move that corner and keep the opposite one fixed. Sizes stay within [min]..[max] and the grid.
 * [taken] holds the cells other items cover on this page.
 */
fun proposePlacement(
    start: CellRect, dx: Float, dy: Float, handle: Handle,
    cols: Int, rows: Int, min: Span, max: Span, taken: Set<Pair<Int, Int>>,
): Placement {
    val mx = dx.roundToInt(); val my = dy.roundToInt()
    val minW = min.x.coerceAtMost(cols); val minH = min.y.coerceAtMost(rows)
    val maxW = max.x.coerceAtMost(cols); val maxH = max.y.coerceAtMost(rows)
    var left = start.x; var top = start.y; var right = start.x + start.w; var bottom = start.y + start.h
    val refused = Placement(start.x, start.y, start.w, start.h, false)
    // A range with no room (widget at an edge, below its minimum, or bigger than the grid) can't be satisfied.
    fun Int.clamp(lo: Int, hi: Int): Int? = if (lo > hi) null else coerceIn(lo, hi)
    when (handle) {
        Handle.BODY -> {
            left = (start.x + mx).clamp(0, cols - start.w) ?: return refused
            top = (start.y + my).clamp(0, rows - start.h) ?: return refused
            right = left + start.w; bottom = top + start.h
        }
        Handle.TOP_LEFT -> {
            left = (left + mx).clamp(maxOf(0, right - maxW), right - minW) ?: return refused
            top = (top + my).clamp(maxOf(0, bottom - maxH), bottom - minH) ?: return refused
        }
        Handle.TOP_RIGHT -> {
            right = (right + mx).clamp(left + minW, minOf(cols, left + maxW)) ?: return refused
            top = (top + my).clamp(maxOf(0, bottom - maxH), bottom - minH) ?: return refused
        }
        Handle.BOTTOM_LEFT -> {
            left = (left + mx).clamp(maxOf(0, right - maxW), right - minW) ?: return refused
            bottom = (bottom + my).clamp(top + minH, minOf(rows, top + maxH)) ?: return refused
        }
        Handle.BOTTOM_RIGHT -> {
            right = (right + mx).clamp(left + minW, minOf(cols, left + maxW)) ?: return refused
            bottom = (bottom + my).clamp(top + minH, minOf(rows, top + maxH)) ?: return refused
        }
    }
    val w = right - left; val h = bottom - top
    val fits = left >= 0 && top >= 0 && right <= cols && bottom <= rows &&
        (left until right).all { x -> (top until bottom).all { y -> (x to y) !in taken } }
    return Placement(left, top, w, h, fits)
}
