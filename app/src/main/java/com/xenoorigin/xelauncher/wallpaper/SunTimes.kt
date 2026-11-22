package com.xenoorigin.xelauncher.wallpaper

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Sunrise and sunset as minutes of the local day. */
data class SunTimes(val sunrise: Int, val sunset: Int) {
    companion object {
        /** Used when the sun doesn't rise or set (polar day/night) or nothing is known. */
        val FALLBACK = SunTimes(6 * 60, 18 * 60)
    }
}

/**
 * NOAA's simplified sunrise/sunset equation (accurate to a few minutes), for [date] at [lat]/[lon],
 * expressed in a local time [utcOffsetMin] minutes from UTC.
 */
fun sunTimes(date: LocalDate, lat: Double, lon: Double, utcOffsetMin: Int): SunTimes {
    val gamma = 2 * Math.PI / 365 * (date.dayOfYear - 1)
    val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
    val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) +
        0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
    val latR = Math.toRadians(lat)
    val cosHa = cos(Math.toRadians(90.833)) / (cos(latR) * cos(decl)) - tan(latR) * tan(decl)
    if (cosHa.isNaN() || cosHa < -1 || cosHa > 1) return SunTimes.FALLBACK
    val ha = Math.toDegrees(acos(cosHa))
    val noonUtc = 720 - 4 * lon - eqTime
    val rise = (noonUtc - 4 * ha + utcOffsetMin).toInt()
    val set = (noonUtc + 4 * ha + utcOffsetMin).toInt()
    if (rise !in 0 until 24 * 60 || set !in 0 until 24 * 60 || set <= rise + 60) return SunTimes.FALLBACK
    return SunTimes(rise, set)
}

/** A city near the middle of each common time zone. No location permission: the zone is enough for a sky. */
private val ZoneCentres = mapOf(
    "Asia/Bangkok" to (13.75 to 100.5), "Asia/Ho_Chi_Minh" to (10.8 to 106.7), "Asia/Jakarta" to (-6.2 to 106.8),
    "Asia/Singapore" to (1.35 to 103.8), "Asia/Kuala_Lumpur" to (3.14 to 101.7), "Asia/Manila" to (14.6 to 121.0),
    "Asia/Yangon" to (16.8 to 96.2), "Asia/Vientiane" to (17.97 to 102.6), "Asia/Phnom_Penh" to (11.56 to 104.9),
    "Asia/Tokyo" to (35.7 to 139.7), "Asia/Seoul" to (37.6 to 127.0), "Asia/Shanghai" to (31.2 to 121.5),
    "Asia/Hong_Kong" to (22.3 to 114.2), "Asia/Taipei" to (25.0 to 121.5), "Asia/Kolkata" to (22.6 to 88.4),
    "Asia/Dubai" to (25.2 to 55.3), "Europe/London" to (51.5 to -0.13), "Europe/Paris" to (48.86 to 2.35),
    "Europe/Berlin" to (52.5 to 13.4), "America/New_York" to (40.7 to -74.0), "America/Los_Angeles" to (34.05 to -118.2),
    "America/Chicago" to (41.9 to -87.6), "Australia/Sydney" to (-33.9 to 151.2),
)

fun latLonFor(zone: ZoneId): Pair<Double, Double> = ZoneCentres[zone.id] ?: ZoneCentres.getValue("Asia/Bangkok")

/** Today's sun times where the phone is (by its time zone). */
fun sunToday(zone: ZoneId = ZoneId.systemDefault(), date: LocalDate = LocalDate.now(zone)): SunTimes {
    val (lat, lon) = latLonFor(zone)
    val offset = zone.rules.getOffset(date.atStartOfDay(zone).toInstant()).totalSeconds / 60
    return sunTimes(date, lat, lon, offset)
}
