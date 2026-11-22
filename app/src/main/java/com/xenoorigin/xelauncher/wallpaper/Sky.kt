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

private data class Look(val top: Long, val mid: Long, val bottom: Long, val stars: Float)

// The day's looks, each placed relative to sunrise or sunset so dawn and dusk come when the sun really moves.
private val Night = Look(0xFF01020A, 0xFF060A22, 0xFF0D1436, 1f)
private val PreDawn = Look(0xFF03051A, 0xFF0F1640, 0xFF27204F, .9f)
private val Dawn = Look(0xFF0B1238, 0xFF3A2C68, 0xFF9A4F6B, .45f)
private val Sunrise = Look(0xFF1F3474, 0xFF7A5A96, 0xFFFF9A6B, .08f)
private val Morning = Look(0xFF2F62C4, 0xFF78A8E8, 0xFFF7D2A6, 0f)
private val Noon = Look(0xFF1F5FD0, 0xFF4F92EC, 0xFF9FD0FF, 0f)
private val Afternoon = Look(0xFF2A58B8, 0xFF6C93D8, 0xFFF2C79A, 0f)
private val Golden = Look(0xFF2B2F78, 0xFF9A4F8C, 0xFFFF8A4C, 0f)
private val AfterSunset = Look(0xFF141A4A, 0xFF4A2A6A, 0xFFC4506A, .25f)
private val Nightfall = Look(0xFF050925, 0xFF101A48, 0xFF2A2258, .75f)
private val DeepNight = Look(0xFF02030E, 0xFF070C28, 0xFF111A40, 1f)

private fun keysFor(sun: SunTimes): List<Pair<Int, Look>> {
    val r = sun.sunrise; val s = sun.sunset
    val raw = listOf(
        r - 150 to Night, r - 60 to PreDawn, r - 20 to Dawn, r to Sunrise, r + 70 to Morning,
        (r + s) / 2 to Noon, s - 110 to Afternoon, s - 30 to Golden, s + 20 to AfterSunset,
        s + 80 to Nightfall, s + 160 to DeepNight,
    )
    // Keep the order of the day even on very long or short days, then wrap onto 0..1440.
    var last = Int.MIN_VALUE
    return raw.map { (m, l) -> val mm = maxOf(m, last + 1); last = mm; mm to l }
}

private fun mix(a: Long, b: Long, t: Float): Long {
    fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toFloat()
    fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toLong().coerceIn(0, 255)
    return (0xFFL shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

/** How the sky looks at [minuteOfDay] on a day whose sun rises and sets at [sun]. */
fun skyAt(minuteOfDay: Int, sun: SunTimes = SunTimes(6 * 60, 18 * 60 + 20)): SkyLook {
    val m = Math.floorMod(minuteOfDay, 24 * 60)
    val keys = keysFor(sun)
    val first = keys.first().first
    // Position on the cyclic timeline starting at the first key.
    val x = first + Math.floorMod(m - first, 24 * 60)
    val cyc = keys + (keys.first().first + 24 * 60 to keys.first().second)
    val i = cyc.indexOfLast { it.first <= x }.coerceIn(0, cyc.lastIndex - 1)
    val (am, a) = cyc[i]; val (bm, b) = cyc[i + 1]
    val t = ((x - am).toFloat() / (bm - am).coerceAtLeast(1)).coerceIn(0f, 1f)
    val sunUp = m in sun.sunrise..sun.sunset
    // The sun (or moon) travels an arc across the sky: low at the edges of its time, high in the middle.
    val f = if (sunUp) (m - sun.sunrise).toFloat() / (sun.sunset - sun.sunrise)
    else Math.floorMod(m - sun.sunset, 24 * 60).toFloat() / (24 * 60 - (sun.sunset - sun.sunrise))
    val orbX = .12f + .76f * f
    val orbY = .62f - .5f * (4f * f * (1f - f))
    val glow = if (sunUp) .55f + .45f * (4f * f * (1f - f)) else .35f
    return SkyLook(mix(a.top, b.top, t), mix(a.mid, b.mid, t), mix(a.bottom, b.bottom, t), a.stars + (b.stars - a.stars) * t, sunUp, orbX, orbY, glow)
}

/** The phone-wallpaper copy of the live sky is redrawn whenever this changes (every 15 minutes). */
fun skyBucket(minuteOfDay: Int): Int = Math.floorMod(minuteOfDay, 24 * 60) / 15

fun minuteNow(): Int = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
