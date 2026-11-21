package com.xenoorigin.xelauncher.drawer

import com.xenoorigin.xelauncher.categories.BuiltInCategory

/** How a category page lays out its icons: a sparse category gets fewer columns, bigger icons, centred. */
data class DrawerGrid(val columns: Int, val iconScale: Float, val sidePaddingDp: Int)

fun drawerGridFor(count: Int): DrawerGrid = when {
    count <= 3 -> {
        val n = count.coerceAtLeast(1)
        DrawerGrid(n, 1.32f, 10 + (3 - n) * 48)
    }
    count <= 6 -> DrawerGrid(3, 1.16f, 14)
    else -> DrawerGrid(4, 1f, 10)
}

/** Each category's own colour, used for the soft light behind its page. */
fun categoryHue(builtIn: BuiltInCategory?, id: Long): Long = when (builtIn) {
    BuiltInCategory.COMMUNICATION -> 0xFF30D158
    BuiltInCategory.SOCIAL -> 0xFF5E5CE6
    BuiltInCategory.MEDIA -> 0xFFFF375F
    BuiltInCategory.GAMES -> 0xFFBF5AF2
    BuiltInCategory.SHOPPING -> 0xFFFF9F0A
    BuiltInCategory.FINANCE -> 0xFFFFC53D
    BuiltInCategory.TRAVEL -> 0xFF64D2FF
    BuiltInCategory.WORK -> 0xFF0A84FF
    BuiltInCategory.TOOLS -> 0xFF5AC8B8
    BuiltInCategory.OTHER -> 0xFF8E8E93
    null -> CustomHues[Math.floorMod(id, CustomHues.size.toLong()).toInt()]
}

private val CustomHues = listOf(0xFFFF6482, 0xFF7D7AFF, 0xFF66D4CF, 0xFFFFB340, 0xFFDA8FFF, 0xFF70D7FF)

/** The colour at a continuous page position (2.4 = 40% of the way from page 2 to page 3). */
fun blendHue(hues: List<Long>, position: Float): Long {
    if (hues.isEmpty()) return 0xFF8E8E93
    val p = position.coerceIn(0f, hues.lastIndex.toFloat())
    val i = p.toInt().coerceAtMost(hues.lastIndex)
    val j = (i + 1).coerceAtMost(hues.lastIndex)
    val t = p - i
    fun ch(c: Long, s: Int) = (c shr s) and 0xFF
    fun mix(s: Int) = (ch(hues[i], s) + (ch(hues[j], s) - ch(hues[i], s)) * t).toLong().coerceIn(0, 255)
    return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}

enum class BentoSize { HERO, WIDE, SMALL }

/** One tile: [app] is the index in the page's app list; [col] (0..3) and [row] are its top-left cell. */
data class BentoCell(val app: Int, val size: BentoSize, val col: Int, val row: Int)

data class Bento(val cells: List<BentoCell>, val rows: Int)

/**
 * Bento layout on a 4-column grid, from how often each app was opened ([uses], in page order). The most used
 * app gets a 2×2 tile at the top left; the next most used get 2×1 wide tiles, as many as it takes to fill the
 * last row exactly; the rest are 1×1 in their usual order. Wide tiles only start on an even column, so no
 * gaps are left behind.
 */
fun bentoLayout(uses: List<Int>): Bento {
    val n = uses.size
    if (n == 0) return Bento(emptyList(), 0)
    val byUse = uses.indices.sortedWith(compareByDescending<Int> { uses[it] }.thenBy { it })
    val hero = byUse.first()
    if (n == 1) return Bento(listOf(BentoCell(hero, BentoSize.HERO, 1, 0)), 2)
    val wideCount = Math.floorMod(1 - n, 4).coerceAtMost(n - 1)
    val wides = ArrayDeque(byUse.drop(1).take(wideCount))
    val wideSet = wides.toSet()
    val smalls = ArrayDeque(uses.indices.filter { it != hero && it !in wideSet })
    val taken = HashSet<Pair<Int, Int>>()
    val cells = mutableListOf(BentoCell(hero, BentoSize.HERO, 0, 0))
    for (r in 0..1) for (c in 0..1) taken += r to c
    var r = 0; var c = 0
    while (wides.isNotEmpty() || smalls.isNotEmpty()) {
        while ((r to c) in taken) { c++; if (c == 4) { c = 0; r++ } }
        if (wides.isNotEmpty() && c % 2 == 0 && (r to c + 1) !in taken) {
            cells += BentoCell(wides.removeFirst(), BentoSize.WIDE, c, r); taken += r to c; taken += r to c + 1
        } else if (smalls.isNotEmpty()) {
            cells += BentoCell(smalls.removeFirst(), BentoSize.SMALL, c, r); taken += r to c
        } else {
            taken += r to c // only wide tiles left and this cell can't start one
        }
    }
    return Bento(cells, maxOf(2, cells.maxOf { it.row + if (it.size == BentoSize.HERO) 2 else 1 }))
}
