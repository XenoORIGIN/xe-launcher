package com.xenoorigin.xelauncher.drawer

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.categories.BuiltInCategory
import org.junit.Test

class DrawerLayoutMathTest {
    @Test fun fewApps_getBiggerIcons_inFewerColumns() {
        val two = drawerGridFor(2)
        assertThat(two.columns).isEqualTo(2)
        assertThat(two.iconScale).isGreaterThan(1.2f)
        assertThat(two.sidePaddingDp).isGreaterThan(drawerGridFor(3).sidePaddingDp)
        assertThat(drawerGridFor(1).columns).isEqualTo(1)
        val five = drawerGridFor(5)
        assertThat(five.columns).isEqualTo(3)
        assertThat(five.iconScale).isGreaterThan(1f)
        assertThat(five.iconScale).isLessThan(two.iconScale)
    }

    @Test fun fullCategories_keepTheNormalFourColumnGrid() {
        listOf(7, 12, 80).forEach {
            val g = drawerGridFor(it)
            assertThat(g.columns).isEqualTo(4); assertThat(g.iconScale).isEqualTo(1f)
        }
        assertThat(drawerGridFor(0).columns).isAtLeast(1)
    }

    @Test fun everyBuiltInCategory_hasItsOwnHue_andCustomOnesAreStable() {
        val hues = BuiltInCategory.entries.map { categoryHue(it, 99) }
        assertThat(hues.toSet().size).isEqualTo(hues.size)
        assertThat(categoryHue(null, 42)).isEqualTo(categoryHue(null, 42))
    }

    @Test fun glowBlendsBetweenNeighbouringCategories() {
        val a = 0xFFFF0000; val b = 0xFF0000FF
        assertThat(blendHue(listOf(a, b), 0f)).isEqualTo(a)
        assertThat(blendHue(listOf(a, b), 1f)).isEqualTo(b)
        val mid = blendHue(listOf(a, b), .5f)
        assertThat((mid shr 16) and 0xFF).isIn(120L..135L)
        assertThat(mid and 0xFF).isIn(120L..135L)
        assertThat(blendHue(listOf(a, b), 5f)).isEqualTo(b)
        assertThat(blendHue(emptyList(), 0f)).isEqualTo(0xFF8E8E93)
    }
}
