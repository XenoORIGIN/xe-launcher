package com.xenoorigin.xelauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.xenoorigin.xelauncher.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

enum class WallpaperTarget { HOME, LOCK, BOTH }

/** Xe owns the wallpaper: it draws it itself (so glass can blur it) and mirrors it to the system. */
class WallpaperController(
    private val context: Context,
    private val settings: SettingsStore,
    private val dir: File = File(context.filesDir, "wallpaper"),
    private val screenSize: () -> Pair<Int, Int> = { defaultScreenSize(context) },
) {
    private val _current = MutableStateFlow<ImageBitmap?>(null)
    val current: StateFlow<ImageBitmap?> = _current.asStateFlow()
    private val _backdrop = MutableStateFlow<ImageBitmap?>(null)
    /** Small pre-blurred wallpaper for full-screen panels (drawer, search, settings): no live blur needed. */
    val backdrop: StateFlow<ImageBitmap?> = _backdrop.asStateFlow()

    private fun show(bmp: Bitmap) {
        _current.value = bmp.asImageBitmap()
        _backdrop.value = runCatching { WallpaperRenderer.backdrop(bmp).asImageBitmap() }.getOrNull()
    }
    private val file get() = File(dir, "current.png")

    /** For tests: how many times the system wallpaper was changed. */
    var systemWallpaperSetCount = 0
        private set

    suspend fun restore() = withContext(Dispatchers.IO) {
        val saved = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
        if (saved != null) { show(saved); return@withContext }
        val (w, h) = screenSize()
        val preset = allWallpapers().byId(settings.current().wallpaperId) ?: XeCollection.first()
        val bmp = WallpaperRenderer.render(preset, w, h)
        save(bmp)
        show(bmp)
    }

    suspend fun apply(preset: WallpaperPreset, target: WallpaperTarget, adjust: WallpaperAdjust = WallpaperAdjust()) = withContext(Dispatchers.IO) {
        val (w, h) = screenSize()
        val raw = WallpaperRenderer.render(preset, w, h)
        val bmp = if (adjust == WallpaperAdjust()) raw else WallpaperRenderer.compose(raw, w, h, adjust)
        applyBitmap(bmp, target)
        settings.update { it.copy(wallpaperId = preset.id) }
    }

    /** Applies an already-rendered bitmap (used by Xe Studio and imports). */
    suspend fun applyBitmap(bmp: Bitmap, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        if (target != WallpaperTarget.LOCK) {
            save(bmp)
            show(bmp)
        }
        val flags = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        runCatching { WallpaperManager.getInstance(context).setBitmap(bmp, null, true, flags) }
            .onSuccess { systemWallpaperSetCount++ }
    }

    /**
     * Copies a picture from the gallery into Xe's folder at a sensible size. Big camera photos (50 MP)
     * are decoded downsampled, in sRGB, without HDR gain maps, so nothing is stretched, banded or
     * out of memory. The picture keeps its own shape; it is fitted to the screen when applied.
     */
    suspend fun importFromUri(uri: Uri): WallpaperPreset.Image = withContext(Dispatchers.IO) {
        val (w, h) = screenSize()
        val longest = (maxOf(w, h) * 2.2f).toInt()
        val src = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            d.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
            d.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
            val big = maxOf(info.size.width, info.size.height)
            if (big > longest) {
                val f = longest.toFloat() / big
                d.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1))
            }
        }
        // Redraw into a plain 8-bit bitmap: drops any HDR gain map / 16-bit config.
        val plain = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(plain).drawBitmap(src, 0f, 0f, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        dir.mkdirs()
        val out = File(dir, "imported_${System.currentTimeMillis()}.jpg")
        out.outputStream().use { plain.compress(Bitmap.CompressFormat.JPEG, 94, it) }
        pruneImports()
        WallpaperPreset.Image(out)
    }

    /** Applies a picture with the user's framing, dim and blur. */
    suspend fun applyImage(file: File, adjust: WallpaperAdjust, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        val (w, h) = screenSize()
        val src = BitmapFactory.decodeFile(file.path) ?: return@withContext
        applyBitmap(WallpaperRenderer.compose(src, w, h, adjust), target)
        settings.update { it.copy(wallpaperId = "image:${file.name}") }
    }

    /** Recently imported pictures, newest first. */
    fun imports(): List<File> = dir.listFiles { f -> f.name.startsWith("imported_") }?.sortedByDescending { it.lastModified() }.orEmpty()

    fun deleteImport(f: File) { if (f.parentFile == dir) f.delete() }

    private fun pruneImports() = imports().drop(12).forEach { it.delete() }

    fun currentFile(): File? = file.takeIf { it.exists() }

    private fun save(bmp: Bitmap) {
        dir.mkdirs()
        val tmp = File(dir, "current.tmp")
        tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tmp.renameTo(file)
    }

    companion object {
        fun defaultScreenSize(context: Context): Pair<Int, Int> {
            val wm = context.getSystemService(WindowManager::class.java)
            val (w, h) = if (android.os.Build.VERSION.SDK_INT >= 30) {
                wm.maximumWindowMetrics.bounds.let { it.width() to it.height() }
            } else {
                @Suppress("DEPRECATION")
                android.util.DisplayMetrics().also { wm.defaultDisplay.getRealMetrics(it) }.let { it.widthPixels to it.heightPixels }
            }
            return minOf(w, h) to maxOf(w, h)
        }
    }
}
