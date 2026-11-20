package com.xenoorigin.xelauncher.icons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.xenoorigin.xelauncher.apps.AppKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * [fillsTile]: the bitmap is a whole icon meant to fill the tile shape (no glass tile behind it),
 * either because the style asks for it or because no clean symbol could be cut out.
 */
data class RenderedIcon(val bitmap: ImageBitmap, val dominantColor: Int, val fillsTile: Boolean = false)

/** Renders app icons in each style, with a memory LRU (sized in bytes) and a PNG disk cache. */
class IconRepository(
    private val loader: (AppKey) -> Drawable?,
    private val dir: File,
    /** Optional custom icon image per app (user picked); takes priority over the app's own icon. */
    private val customIcon: (AppKey) -> File? = { null },
    private val customVersion: (AppKey) -> String? = { k -> customIcon(k)?.let { "1" } },
) {
    private val memory = object : LruCache<String, RenderedIcon>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: RenderedIcon) = value.bitmap.width * value.bitmap.height * 4
    }
    private val renderGate = Semaphore(4)

    private enum class Variant { GLYPH_GRAY, GLYPH_COLOR, FULL, FULL_GRAY }

    private fun variantOf(style: IconStyle) = when (style) {
        IconStyle.GLASS_DARK -> Variant.GLYPH_GRAY
        IconStyle.GLASS_COLOR, IconStyle.GLASS_CLEAR -> Variant.GLYPH_COLOR
        IconStyle.MONO -> Variant.FULL_GRAY
        IconStyle.ORIGINAL -> Variant.FULL
    }

    private fun cacheKey(key: AppKey, v: Variant, size: Int, contrast: Float): String {
        val custom = customVersion(key)?.let { "|c$it" }.orEmpty()
        return "v2|${key.encode()}|$v|$size|${"%.2f".format(contrast)}$custom"
    }

    private fun file(key: AppKey, ck: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(ck.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(dir, "${key.packageName}__$hash.png")
    }

    /** Memory-only lookup, safe on the main thread; lets tiles show instantly when already rendered. */
    fun peek(key: AppKey, style: IconStyle, sizePx: Int, contrast: Float): RenderedIcon? =
        memory.get(cacheKey(key, variantOf(style), sizePx, contrast))

    suspend fun icon(key: AppKey, style: IconStyle, sizePx: Int, contrast: Float): RenderedIcon? = withContext(Dispatchers.Default) {
        val v = variantOf(style)
        val ck = cacheKey(key, v, sizePx, contrast)
        memory.get(ck)?.let { return@withContext it }
        renderGate.withPermit {
            memory.get(ck)?.let { return@withPermit it }
            val f = file(key, ck)
            val meta = File(f.path.removeSuffix(".png") + ".meta")
            val cached = if (f.exists()) BitmapFactory.decodeFile(f.path) else null
            val cachedMeta = meta.takeIf { it.exists() }?.readText()?.split(';')
            if (cached != null && cachedMeta?.size == 2) {
                val color = cachedMeta[0].toIntOrNull()
                if (color != null) return@withPermit RenderedIcon(cached.asImageBitmap(), color, cachedMeta[1] == "1").also { memory.put(ck, it) }
            }
            val custom = customIcon(key)?.let { BitmapFactory.decodeFile(it.path) }
            val (rendered, fills, color) = if (custom != null) {
                val whole = IconProcessor.square(custom, sizePx)
                val out = if (v == Variant.FULL_GRAY || v == Variant.GLYPH_GRAY) IconProcessor.grayscale(whole, contrast) else whole
                Triple(out, true, IconProcessor.dominantColor(whole))
            } else {
                val d = loader(key) ?: return@withPermit null
                val whole = IconProcessor.full(d, sizePx)
                val color = IconProcessor.dominantColor(whole)
                when (v) {
                    Variant.FULL -> Triple(whole, true, color)
                    Variant.FULL_GRAY -> Triple(IconProcessor.grayscale(whole, contrast), true, color)
                    Variant.GLYPH_GRAY, Variant.GLYPH_COLOR -> {
                        val g = IconProcessor.extractGlyph(IconProcessor.foregroundOf(d, sizePx))
                        when {
                            !g.isGlyph && v == Variant.GLYPH_GRAY -> Triple(IconProcessor.grayscale(whole, contrast), true, color)
                            !g.isGlyph -> Triple(whole, true, color)
                            v == Variant.GLYPH_GRAY -> Triple(IconProcessor.grayscale(g.bitmap, contrast, lightenDarkGlyphs = true), false, color)
                            else -> Triple(g.bitmap, false, color)
                        }
                    }
                }
            }
            dir.mkdirs()
            runCatching {
                f.outputStream().use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) }
                meta.writeText("$color;${if (fills) 1 else 0}")
            }
            RenderedIcon(rendered.asImageBitmap(), color, fills).also { memory.put(ck, it) }
        }
    }

    /** Renders icons ahead of time so the drawer opens with every icon already in place. */
    suspend fun prewarm(keys: List<AppKey>, styleOf: (AppKey) -> IconStyle, sizePx: Int, contrast: Float) = coroutineScope {
        keys.map { k -> async(Dispatchers.Default) { runCatching { icon(k, styleOf(k), sizePx, contrast) } } }.awaitAll()
        Unit
    }

    fun clearMemory() = memory.evictAll()

    fun invalidate(packageName: String) {
        memory.snapshot().keys.filter { it.startsWith("v2|$packageName/") }.forEach { memory.remove(it) }
        dir.listFiles { f -> f.name.startsWith("${packageName}__") }?.forEach { it.delete() }
    }
}
