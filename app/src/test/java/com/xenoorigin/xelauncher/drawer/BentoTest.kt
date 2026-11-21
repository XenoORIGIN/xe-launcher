package com.xenoorigin.xelauncher.drawer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BentoTest {
    private fun occupied(b: Bento): List<Pair<Int, Int>> = b.cells.flatMap { c ->
        val w = if (c.size == BentoSize.SMALL) 1 else 2
        val h = if (c.size == BentoSize.HERO) 2 else 1
        (0 until h).flatMap { dr -> (0 until w).map { dc -> (c.row + dr) to (c.col + dc) } }
    }

    @Test fun theMostUsedApp_getsTheBigTile_theNextOnesGetWideTiles() {
        val b = bentoLayout(listOf(1, 9, 0, 4, 7, 0, 0, 2, 0, 0, 0, 0))
        val hero = b.cells.single { it.size == BentoSize.HERO }
        assertThat(hero.app).isEqualTo(1)
        assertThat(hero.col to hero.row).isEqualTo(0 to 0)
        assertThat(b.cells.filter { it.size == BentoSize.WIDE }.map { it.app }).containsExactly(4)
        // The rest keep their order.
        assertThat(b.cells.filter { it.size == BentoSize.SMALL }.map { it.app }).containsExactly(0, 2, 3, 5, 6, 7, 8, 9, 10, 11).inOrder()
    }

    @Test fun everyAppAppearsOnce_andNoTilesOverlapOrLeaveTheGrid() {
        for (n in 1..40) {
            val b = bentoLayout(List(n) { (it * 37) % 11 })
            assertThat(b.cells.map { it.app }.sorted()).isEqualTo((0 until n).toList())
            val cells = occupied(b)
            assertThat(cells.toSet().size).isEqualTo(cells.size)
            cells.forEach { (r, c) -> assertThat(c).isIn(0..3); assertThat(r).isIn(0 until b.rows) }
        }
    }

    @Test fun pagesWithThreeOrMoreApps_fillANeatRectangle() {
        for (n in 3..40) {
            val b = bentoLayout(List(n) { 0 })
            assertThat(occupied(b).size).isEqualTo(b.rows * 4)
        }
    }

    @Test fun withNoUsageYet_theFirstAppIsTheHero() {
        val b = bentoLayout(List(6) { 0 })
        assertThat(b.cells.single { it.size == BentoSize.HERO }.app).isEqualTo(0)
    }

    @Test fun aSingleApp_sitsInTheMiddle() {
        val b = bentoLayout(listOf(0))
        assertThat(b.cells.single().col).isEqualTo(1)
        assertThat(b.rows).isEqualTo(2)
    }
}
