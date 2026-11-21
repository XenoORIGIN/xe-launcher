package com.xenoorigin.xelauncher.bugs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import com.xenoorigin.xelauncher.BuildConfig
import com.xenoorigin.xelauncher.settings.XeSettings
import java.util.Locale

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val android: String,
    val sdk: Int,
    val appVersion: String,
    val screen: String,
    val locale: String,
) {
    companion object {
        fun collect(context: Context): DeviceInfo {
            val dm = context.resources.displayMetrics
            return DeviceInfo(
                Build.MANUFACTURER.orEmpty(), Build.MODEL.orEmpty(), Build.VERSION.RELEASE.orEmpty(), Build.VERSION.SDK_INT,
                BuildConfig.VERSION_NAME, "${dm.widthPixels}x${dm.heightPixels}", Locale.getDefault().toLanguageTag(),
            )
        }
    }
}

/** Builds the bug report email sent to the developer. */
object BugReport {
    const val EMAIL = "xenodevg@gmail.com"

    fun subject(version: String, crash: Boolean) = "Xe Launcher $version — ${if (crash) "crash" else "bug"} report"

    fun body(description: String, device: DeviceInfo, settings: String?, crash: String?, log: String?): String = buildString {
        appendLine(description.trim().ifEmpty { "(no description)" })
        appendLine(); appendLine("—— Device ——")
        appendLine("${device.manufacturer} ${device.model}")
        appendLine("Android ${device.android} (API ${device.sdk})")
        appendLine("Xe Launcher ${device.appVersion}")
        appendLine("Screen ${device.screen} · ${device.locale}")
        if (!settings.isNullOrBlank()) { appendLine(); appendLine("—— Settings ——"); appendLine(settings) }
        if (!crash.isNullOrBlank()) { appendLine(); appendLine("—— Crash ——"); appendLine(crash.trim()) }
        if (!log.isNullOrBlank()) { appendLine(); appendLine("—— Log ——"); appendLine(log.trim()) }
    }

    fun settingsSummary(s: XeSettings): String =
        "icons=${s.iconStyle}/${s.iconShape} size=${s.iconSizeDp} glass=${s.glassQuality} theme=${s.themeMode} " +
            "wallpaper=${s.wallpaperId} grid=${s.homeColumns}x${s.homeRows} dock=${s.dockSize} anim=${s.animationSpeed} lang=${s.language}"

    /** The last lines this app wrote to the system log (apps may read their own). */
    fun recentLog(lines: Int = 150): String? = runCatching {
        val p = ProcessBuilder("logcat", "-d", "-t", lines.toString(), "--pid", Process.myPid().toString()).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().use { it.readText() }.takeLast(8_000)
    }.getOrNull()

    /** An email to [EMAIL]; the mailto selector keeps the chooser to email apps. */
    fun emailIntent(subject: String, body: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    }
}
