package com.xenoorigin.xelauncher.icons

import android.graphics.Bitmap
import com.xenoorigin.xelauncher.apps.AppKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest

/** Pictures the user picked as app icons, one PNG per app. Lookups never touch the disk. */
class CustomIconStore(private val dir: File) {
    private val _files = MutableStateFlow(load())
    /** AppKey.encode() -> file; changes whenever an icon is set or cleared. */
    val files: StateFlow<Map<String, File>> = _files.asStateFlow()

    private fun name(key: String) = MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) } + ".png"

    private fun load(): Map<String, File> {
        val index = File(dir, "index.txt")
        if (!index.exists()) return emptyMap()
        return index.readLines().mapNotNull { line ->
            val k = line.substringBeforeLast('\t', ""); val f = File(dir, line.substringAfterLast('\t'))
            if (k.isNotEmpty() && f.exists()) k to f else null
        }.toMap()
    }

    private fun saveIndex(map: Map<String, File>) {
        dir.mkdirs()
        File(dir, "index.txt").writeText(map.entries.joinToString("\n") { "${it.key}\t${it.value.name}" })
    }

    fun get(key: AppKey): File? = _files.value[key.encode()]

    /** Changes every time the picture for [key] changes (part of the icon cache key; no disk access). */
    fun version(key: AppKey): String? = _files.value[key.encode()]?.let { versions[key.encode()] ?: "0" }
    private val versions = HashMap<String, String>()

    /** Stores [picture] (any size) as the icon for [key], centre-cropped to a 256 px square. */
    fun set(key: AppKey, picture: Bitmap) {
        dir.mkdirs()
        val f = File(dir, name(key.encode()))
        f.outputStream().use { IconProcessor.square(picture, 256).compress(Bitmap.CompressFormat.PNG, 100, it) }
        versions[key.encode()] = System.currentTimeMillis().toString()
        val next = _files.value + (key.encode() to f)
        saveIndex(next); _files.value = next
    }

    fun clear(key: AppKey) {
        _files.value[key.encode()]?.delete()
        val next = _files.value - key.encode()
        saveIndex(next); _files.value = next
    }
}
