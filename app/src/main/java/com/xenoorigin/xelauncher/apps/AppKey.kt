package com.xenoorigin.xelauncher.apps

/** Identifies one launchable activity for one user profile (clones have different serials). */
data class AppKey(val packageName: String, val activity: String, val userSerial: Long) {
    fun encode(): String = "$packageName/$activity#$userSerial"

    companion object {
        private val pattern = Regex("^([^/#]+)/([^/#]+)#(-?\\d+)$")
        fun decode(s: String): AppKey? = pattern.matchEntire(s)?.let { m ->
            AppKey(m.groupValues[1], m.groupValues[2], m.groupValues[3].toLong())
        }
    }
}

data class AppEntry(
    val key: AppKey,
    val label: String,
    val androidCategory: Int,
    val installedAt: Long,
    val isClone: Boolean,
)
