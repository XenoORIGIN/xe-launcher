package com.xenoorigin.xelauncher.icons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.xenoorigin.xelauncher.apps.AppKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class RenderedIcon(val bitmap: ImageBitmap, val dominantColor: Int)

/** Renders app icons in each style, with a memory LRU and a PNG disk cache. */
class IconRepository(
    private val loader: (AppKey) -> Drawable?,
    private val dir: File,
) {
    private val memory = LruCache<String, RenderedIcon>(96)

    private enum class Variant { GRAY, FOREGROUND, FULL }

    private fun variantOf(style: IconStyle) = when (style) {
        IconStyle.GLASS_DARK -> Variant.GRAY
        IconStyle.GLASS_COLOR, IconStyle.GLASS_CLEAR -> Variant.FOREGROUND
        IconStyle.ORIGINAL -> Variant.FULL
    }

    private fun cacheKey(key: AppKey, v: Variant, size: Int, contrast: Float) =
        "${key.encode()}|$v|$size|${"%.2f".format(contrast)}"

    private fun file(key: AppKey, ck: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(ck.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(dir, "${key.packageName}__$hash.png")
    }

    suspend fun icon(key: AppKey, style: IconStyle, sizePx: Int, contrast: Float): RenderedIcon? = withContext(Dispatchers.Default) {
        val v = variantOf(style)
        val ck = cacheKey(key, v, sizePx, contrast)
        memory.get(ck)?.let { return@withContext it }
        val f = file(key, ck)
        val colorFile = File(f.path.removeSuffix(".png") + ".color")
        val cached = if (f.exists()) BitmapFactory.decodeFile(f.path) else null
        val cachedColor = colorFile.takeIf { it.exists() }?.readText()?.toIntOrNull()
        if (cached != null && cachedColor != null) {
            return@withContext RenderedIcon(cached.asImageBitmap(), cachedColor).also { memory.put(ck, it) }
        }
        val d = loader(key) ?: return@withContext null
        val whole = IconProcessor.full(d, sizePx)
        val rendered = when (v) {
            Variant.GRAY -> IconProcessor.grayscale(IconProcessor.foregroundOf(d, sizePx), contrast, lightenDarkGlyphs = true)
            Variant.FOREGROUND -> IconProcessor.foregroundOf(d, sizePx)
            Variant.FULL -> whole
        }
        val color = IconProcessor.dominantColor(whole)
        dir.mkdirs()
        runCatching {
            f.outputStream().use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) }
            colorFile.writeText(color.toString())
        }
        RenderedIcon(rendered.asImageBitmap(), color).also { memory.put(ck, it) }
    }

    fun clearMemory() = memory.evictAll()

    fun invalidate(packageName: String) {
        memory.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach { memory.remove(it) }
        dir.listFiles { f -> f.name.startsWith("${packageName}__") }?.forEach { it.delete() }
    }
}
