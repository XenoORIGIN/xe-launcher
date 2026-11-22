package com.xenoorigin.xelauncher.home

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.widgets.Span
import org.junit.Test

class WidgetEditMathTest {
    private val start = CellRect(1, 1, 2, 2)
    private val min = Span(1, 1); private val max = Span(4, 4)
    private fun place(mode: Handle, dx: Float, dy: Float, taken: Set<Pair<Int, Int>> = emptySet()) =
        proposePlacement(start, dx, dy, mode, cols = 4, rows = 6, min = min, max = max, taken = taken)

    @Test fun moveSnapsToNearestCell() {
        assertThat(place(Handle.BODY, .6f, 1.4f)).isEqualTo(Placement(2, 2, 2, 2, true))
        assertThat(place(Handle.BODY, .4f, -.4f)).isEqualTo(Placement(1, 1, 2, 2, true))
    }

    @Test fun placementClampsToGrid() {
        assertThat(place(Handle.BODY, 9f, 9f)).isEqualTo(Placement(2, 4, 2, 2, true))
        assertThat(place(Handle.BOTTOM_RIGHT, 9f, 0f)).isEqualTo(Placement(1, 1, 3, 2, true))
    }

    @Test fun resizeFromTopLeftKeepsTheBottomRightCorner() {
        assertThat(place(Handle.TOP_LEFT, -1f, -1f)).isEqualTo(Placement(0, 0, 3, 3, true))
        assertThat(place(Handle.TOP_LEFT, 3f, 3f)).isEqualTo(Placement(2, 2, 1, 1, true)) // stops at the minimum
    }

    @Test fun resizeRespectsTheWidgetsMinimum() {
        val p = proposePlacement(start, -5f, 0f, Handle.BOTTOM_RIGHT, 4, 6, Span(2, 2), max, emptySet())
        assertThat(p).isEqualTo(Placement(1, 1, 2, 2, true))
    }

    @Test fun placementOntoOccupiedCellDoesNotFit() {
        val p = place(Handle.BOTTOM_RIGHT, 1f, 0f, taken = setOf(3 to 2))
        assertThat(p.w).isEqualTo(3)
        assertThat(p.fits).isFalse()
    }

    @Test fun presetThatCannotGrowInPlaceIsRefusedNotCrashed() {
        // A 2×2 widget in the right half of a 4-column grid, asked to become 4×2.
        val p = proposePlacement(CellRect(2, 0, 2, 2), 0f, 0f, Handle.BOTTOM_RIGHT, 4, 6, Span(4, 2), Span(4, 2), emptySet())
        assertThat(p.fits).isFalse()
        val tall = proposePlacement(CellRect(0, 5, 2, 1), 0f, 0f, Handle.BOTTOM_RIGHT, 4, 6, Span(2, 3), Span(2, 3), emptySet())
        assertThat(tall.fits).isFalse()
    }

    @Test fun cornerDragOnWidgetBelowItsMinimumAtTheEdgeIsRefused() {
        val p = proposePlacement(CellRect(3, 0, 1, 2), 1f, 0f, Handle.BOTTOM_RIGHT, 4, 6, Span(2, 2), Span(4, 4), emptySet())
        assertThat(p.fits).isFalse()
        val q = proposePlacement(CellRect(0, 0, 1, 2), -1f, 0f, Handle.TOP_LEFT, 4, 6, Span(2, 2), Span(4, 4), emptySet())
        assertThat(q.fits).isFalse()
    }

    @Test fun movingAWidgetWiderThanTheGridIsRefused() {
        val p = proposePlacement(CellRect(0, 0, 5, 2), 1f, 0f, Handle.BODY, 4, 6, Span(1, 1), Span(5, 5), emptySet())
        assertThat(p.fits).isFalse()
    }
}
