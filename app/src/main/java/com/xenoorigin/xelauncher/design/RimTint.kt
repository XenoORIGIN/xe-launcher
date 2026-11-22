package com.xenoorigin.xelauncher.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.xenoorigin.xelauncher.wallpaper.SunTimes
import kotlin.math.abs

private val Amber = Color(0xFFFFB066)
private val Night = Color(0xFF86A8FF)
private const val MAX = .35f
private const val NIGHT_ALPHA = .26f
private const val GOLDEN = 40

/**
 * Colour the glass rims pick up at [minute] of the day: amber around sunset, cool blue at night,
 * clear by day. Alpha never passes .35 so the rim stays glass, not paint.
 */
fun rimTintAt(minute: Int, sun: SunTimes): Color {
    val m = ((minute % 1440) + 1440) % 1440
    val d = m - sun.sunset
    return when {
        abs(d) <= GOLDEN -> {
            val k = abs(d) / GOLDEN.toFloat()
            // Peaks at sunset; after it the amber slides toward the night blue.
            val c = if (d > 0) lerp(Amber, Night, k) else Amber
            c.copy(alpha = MAX - (MAX - .2f) * k * k)
        }
        m > sun.sunset + GOLDEN || m < sun.sunrise - 20 -> Night.copy(alpha = NIGHT_ALPHA)
        m < sun.sunrise + 40 -> Night.copy(alpha = NIGHT_ALPHA * (1f - (m - (sun.sunrise - 20)) / 60f))
        d >= -GOLDEN * 2 -> Amber.copy(alpha = .2f * (1f - (-d - GOLDEN) / GOLDEN.toFloat()))
        else -> Color.Transparent
    }
}
