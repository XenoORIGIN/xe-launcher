package com.xenoorigin.xelauncher.play

private const val DAY = 86_400_000L
private const val START_DAYS = 21f
private const val FULL_DAYS = 60f

/** How dusty an app is (0 clean .. 1 thick) when it was last touched at [lastUse] (0 = unknown: clean). */
fun dustLevel(now: Long, lastUse: Long): Float {
    if (lastUse <= 0L) return 0f
    val days = (now - lastUse) / DAY.toFloat()
    return ((days - START_DAYS) / (FULL_DAYS - START_DAYS)).coerceIn(0f, 1f)
}

/** When the app was last touched: opened, installed, wiped clean, or when dust was switched on. */
fun lastUse(lastLaunch: Long, installedAt: Long, wipedAt: Long, dustSince: Long): Long =
    maxOf(lastLaunch, installedAt, wipedAt, dustSince)

/** Rubbing a dusty icon: the finger has to travel about three icon widths to wipe it clean. */
class RubProgress(private val iconSizePx: Float) {
    private var travelled = 0f
    val progress: Float get() = (travelled / (iconSizePx * 3f)).coerceIn(0f, 1f)

    /** Adds finger travel; true once, the moment the icon becomes clean. */
    fun add(distancePx: Float): Boolean {
        val before = progress
        travelled += distancePx
        return before < 1f && progress >= 1f
    }
}
