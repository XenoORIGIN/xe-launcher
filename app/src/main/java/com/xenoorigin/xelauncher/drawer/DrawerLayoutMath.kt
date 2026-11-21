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
