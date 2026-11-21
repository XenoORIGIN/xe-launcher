package com.xenoorigin.xelauncher.bugs

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Keeps the last crash on disk so the next launch can offer to report it. */
class CrashStore(dir: File) {
    private val file = File(dir, "last-crash.txt")

    fun record(t: Throwable, version: String, now: Long = System.currentTimeMillis()) {
        val trace = StringWriter().also { t.printStackTrace(PrintWriter(it)) }.toString()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(now))
        val text = "Version $version · $time\n$trace"
        runCatching { file.parentFile?.mkdirs(); file.writeText(text.take(MAX_CHARS)) }
    }

    fun pending(): String? = runCatching { file.takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

    fun clear() { runCatching { file.delete() } }

    companion object {
        const val MAX_CHARS = 12_000

        /** Records uncaught crashes, then lets Android handle them as before. */
        fun install(store: CrashStore, version: String) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, e ->
                store.record(e, version)
                previous?.uncaughtException(thread, e)
            }
        }
    }
}
