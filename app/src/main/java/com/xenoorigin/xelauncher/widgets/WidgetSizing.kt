package com.xenoorigin.xelauncher.widgets

import androidx.compose.runtime.compositionLocalOf

/** A widget's size in Home grid cells. */
data class Span(val x: Int, val y: Int) {
    val isSmall get() = x == 1 && y == 1
    val isWide get() = x >= 3 && y <= 2
    val isTall get() = y >= 3
    val isRow get() = y == 1
}

/** The size the widget being drawn occupies; widgets pick a layout that fits it. */
val LocalWidgetSpan = compositionLocalOf<Span?> { null }

/** Smallest size each widget still makes sense at. */
val GlassWidgetKind.minX: Int get() = if (this == GlassWidgetKind.MONTH || this == GlassWidgetKind.TERMI) 2 else 1
val GlassWidgetKind.minY: Int get() = if (this == GlassWidgetKind.MONTH || this == GlassWidgetKind.TERMI) 2 else 1
const val WIDGET_MAX = 4

fun GlassWidgetKind.clamp(s: Span): Span = Span(s.x.coerceIn(minX, WIDGET_MAX), s.y.coerceIn(minY, WIDGET_MAX))

/** Small, Medium, Large, Extra-large shortcuts for this widget, smallest first, all within the grid. */
fun GlassWidgetKind.presetSpans(cols: Int, rows: Int): List<Span> {
    val wish = when (this) {
        GlassWidgetKind.CLOCK -> listOf(Span(2, 1), Span(2, 2), Span(4, 2), Span(4, 4))
        GlassWidgetKind.MONTH -> listOf(Span(2, 2), Span(4, 3), Span(4, 4))
        GlassWidgetKind.SEARCH -> listOf(Span(2, 1), Span(4, 1), Span(4, 2))
        GlassWidgetKind.SUGGESTED -> listOf(Span(2, 1), Span(4, 1), Span(4, 2))
        GlassWidgetKind.TERMI -> listOf(Span(2, 2), Span(4, 2), Span(4, 4))
        else -> listOf(Span(1, 1), Span(2, 2), Span(4, 2), Span(4, 4))
    }
    return wish.map { Span(it.x.coerceAtMost(cols), it.y.coerceAtMost(rows)) }.map(::clamp).distinct()
        .fold(mutableListOf<Span>()) { acc, s -> if (acc.isEmpty() || s.x * s.y > acc.last().x * acc.last().y) acc += s; acc }
}
