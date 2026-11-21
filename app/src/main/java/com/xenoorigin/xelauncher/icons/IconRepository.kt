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
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.asCoroutineDispatcher
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
    /** CPU cores to plan for (tests pin it to a phone's count). */
    processors: Int = Runtime.getRuntime().availableProcessors(),
    /** Where icons are drawn; by default a few low-priority threads, so scrolling and animations stay smooth. */
    private val renderDispatcher: kotlinx.coroutines.CoroutineDispatcher = backgroundPool((processors - 1).coerceIn(2, 6)),
) {
    private val memory = object : LruCache<String, RenderedIcon>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: RenderedIcon) = value.bitmap.width * value.bitmap.height * 4
    }
    private val renderGate = Semaphore((processors - 1).coerceIn(2, 6))
    /** Renders in progress, so a tile and the pre-warmer asking for the same icon share one render. */
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<RenderedIcon?>>()
    /** Disk writes happen off the render path; the icon is on screen before its PNG is saved. */
    private val diskScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO.limitedParallelism(2))
    private val pendingWrites = java.util.concurrent.ConcurrentLinkedQueue<kotlinx.coroutines.Job>()
    /** Bumped by [invalidate]; a save that started before the bump is thrown away instead of reviving the old icon. */
    private val generation = java.util.concurrent.ConcurrentHashMap<String, Int>()

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

    suspend fun icon(key: AppKey, style: IconStyle, sizePx: Int, contrast: Float): RenderedIcon? {
        val v = variantOf(style)
        val ck = cacheKey(key, v, sizePx, contrast)
        memory.get(ck)?.let { return it }
        val job = inFlight.computeIfAbsent(ck) {
            diskScope.async(renderDispatcher, start = kotlinx.coroutines.CoroutineStart.LAZY) {
                try { render(key, v, ck, sizePx, contrast) } finally { inFlight.remove(ck) }
            }
        }
        return job.await()
    }

    private suspend fun render(key: AppKey, v: Variant, ck: String, sizePx: Int, contrast: Float): RenderedIcon? =
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
            val out = RenderedIcon(rendered.asImageBitmap(), color, fills).also { memory.put(ck, it) }
            val gen = generation[key.packageName] ?: 0
            pendingWrites += diskScope.launch {
                dir.mkdirs()
                runCatching {
                    val tmp = File(f.path + ".tmp")
                    tmp.outputStream().use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    synchronized(generation) {
                        if ((generation[key.packageName] ?: 0) != gen) { tmp.delete(); return@runCatching }
                        meta.writeText("$color;${if (fills) 1 else 0}")
                        tmp.renameTo(f)
                    }
                }
            }
            out
        }

    /**
     * Renders icons ahead of time, in [keys] order, so the drawer opens with icons already in place. Only
     * [PREWARM_WORKERS] at a time: the rest of the render slots stay free, so an icon that is on screen right
     * now starts at once instead of waiting behind every app on the phone.
     */
    suspend fun prewarm(keys: List<AppKey>, styleOf: (AppKey) -> IconStyle, sizePx: Int, contrast: Float) = coroutineScope {
        val next = java.util.concurrent.atomic.AtomicInteger()
        repeat(PREWARM_WORKERS) {
            launch {
                while (true) {
                    val k = keys.getOrNull(next.getAndIncrement()) ?: break
                    runCatching { icon(k, styleOf(k), sizePx, contrast) }
                }
            }
        }
    }

    companion object {
        const val PREWARM_WORKERS = 2

        /** Fixed threads at background priority (as the stock launcher's loader), so drawing icons never steals frames. */
        fun backgroundPool(threads: Int): kotlinx.coroutines.CoroutineDispatcher {
            val n = java.util.concurrent.atomic.AtomicInteger()
            return java.util.concurrent.Executors.newFixedThreadPool(threads) { r ->
                Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); r.run() }, "xe-icons-${n.incrementAndGet()}").apply { isDaemon = true }
            }.asCoroutineDispatcher()
        }
    }

    /** Waits for queued disk writes (tests, and before clearing the cache). */
    suspend fun flushDisk() {
        while (true) { val j = pendingWrites.poll() ?: break; j.join() }
    }

    fun clearMemory() = memory.evictAll()

    fun invalidate(packageName: String) {
        synchronized(generation) {
            generation.merge(packageName, 1, Int::plus)
            memory.snapshot().keys.filter { it.startsWith("v2|$packageName/") }.forEach { memory.remove(it) }
            dir.listFiles { f -> f.name.startsWith("${packageName}__") }?.forEach { it.delete() }
        }
    }
}
