package com.xenoorigin.xelauncher.wallpaper

/**
 * What the live sky looks like at a minute of the day: gradient from [top] through [mid] to [bottom],
 * how visible the [stars] are (0..1), the glow of the sun or moon ([sunUp]) at ([orbX], [orbY]) in 0..1
 * screen space, and its [orbGlow] strength.
 */
data class SkyLook(
    val top: Long, val mid: Long, val bottom: Long, val stars: Float,
    val sunUp: Boolean, val orbX: Float, val orbY: Float, val orbGlow: Float,
)

private data class Key(val minute: Int, val top: Long, val mid: Long, val bottom: Long, val stars: Float)

private val Keys = listOf(
    Key(0, 0xFF01020A, 0xFF060A22, 0xFF0D1436, 1f),
    Key(4 * 60 + 40, 0xFF03051A, 0xFF0F1640, 0xFF27204F, .9f),
    Key(5 * 60 + 40, 0xFF0B1238, 0xFF3A2C68, 0xFF9A4F6B, .45f),
    Key(6 * 60 + 20, 0xFF1F3474, 0xFF7A5A96, 0xFFFF9A6B, .08f),
    Key(7 * 60 + 30, 0xFF2F62C4, 0xFF78A8E8, 0xFFF7D2A6, 0f),
    Key(12 * 60, 0xFF1F5FD0, 0xFF4F92EC, 0xFF9FD0FF, 0f),
    Key(16 * 60 + 30, 0xFF2A58B8, 0xFF6C93D8, 0xFFF2C79A, 0f),
    Key(17 * 60 + 50, 0xFF2B2F78, 0xFF9A4F8C, 0xFFFF8A4C, 0f),
    Key(18 * 60 + 40, 0xFF141A4A, 0xFF4A2A6A, 0xFFC4506A, .25f),
    Key(19 * 60 + 40, 0xFF050925, 0xFF101A48, 0xFF2A2258, .75f),
    Key(21 * 60, 0xFF02030E, 0xFF070C28, 0xFF111A40, 1f),
    Key(24 * 60, 0xFF01020A, 0xFF060A22, 0xFF0D1436, 1f),
)

private fun mix(a: Long, b: Long, t: Float): Long {
    fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toFloat()
    fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toLong().coerceIn(0, 255)
    return (0xFFL shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

private const val SUNRISE = 6 * 60
private const val SUNSET = 18 * 60 + 20

fun skyAt(minuteOfDay: Int): SkyLook {
    val m = Math.floorMod(minuteOfDay, 24 * 60)
    val i = Keys.indexOfLast { it.minute <= m }.coerceAtMost(Keys.lastIndex - 1)
    val a = Keys[i]; val b = Keys[i + 1]
    val t = (m - a.minute).toFloat() / (b.minute - a.minute)
    val sunUp = m in SUNRISE..SUNSET
    // The sun (or moon) travels an arc across the sky: low at the edges of its time, high in the middle.
    val f = if (sunUp) (m - SUNRISE).toFloat() / (SUNSET - SUNRISE)
    else Math.floorMod(m - SUNSET, 24 * 60).toFloat() / (24 * 60 - (SUNSET - SUNRISE))
    val orbX = .12f + .76f * f
    val orbY = .62f - .5f * (4f * f * (1f - f))
    val glow = if (sunUp) .55f + .45f * (4f * f * (1f - f)) else .35f
    return SkyLook(mix(a.top, b.top, t), mix(a.mid, b.mid, t), mix(a.bottom, b.bottom, t), a.stars + (b.stars - a.stars) * t, sunUp, orbX, orbY, glow)
}

/** The live sky is redrawn whenever this changes (every 20 minutes). */
fun skyBucket(minuteOfDay: Int): Int = Math.floorMod(minuteOfDay, 24 * 60) / 20

fun minuteNow(): Int = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
