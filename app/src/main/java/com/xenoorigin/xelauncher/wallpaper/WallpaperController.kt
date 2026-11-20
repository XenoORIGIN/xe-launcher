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
    private val file get() = File(dir, "current.png")

    /** For tests: how many times the system wallpaper was changed. */
    var systemWallpaperSetCount = 0
        private set

    suspend fun restore() = withContext(Dispatchers.IO) {
        val saved = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
        if (saved != null) { _current.value = saved.asImageBitmap(); return@withContext }
        val (w, h) = screenSize()
        val preset = XeCollection.byId(settings.current().wallpaperId) ?: XeCollection.first()
        val bmp = WallpaperRenderer.render(preset, w, h)
        save(bmp)
        _current.value = bmp.asImageBitmap()
    }

    suspend fun apply(preset: WallpaperPreset, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        val (w, h) = screenSize()
        val bmp = WallpaperRenderer.render(preset, w, h)
        applyBitmap(bmp, target)
        settings.update { it.copy(wallpaperId = preset.id) }
    }

    /** Applies an already-rendered bitmap (used by Xe Studio and imports). */
    suspend fun applyBitmap(bmp: Bitmap, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        if (target != WallpaperTarget.LOCK) {
            save(bmp)
            _current.value = bmp.asImageBitmap()
        }
        val flags = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        runCatching { WallpaperManager.getInstance(context).setBitmap(bmp, null, true, flags) }
            .onSuccess { systemWallpaperSetCount++ }
    }

    suspend fun importFromUri(uri: Uri): WallpaperPreset.Image = withContext(Dispatchers.IO) {
        val (w, h) = screenSize()
        val src = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val out = File(dir, "imported_${System.currentTimeMillis()}.png")
        dir.mkdirs()
        out.outputStream().use { WallpaperRenderer.centerCrop(src, w, h).compress(Bitmap.CompressFormat.PNG, 100, it) }
        WallpaperPreset.Image(out)
    }

    fun currentFile(): File? = file.takeIf { it.exists() }

    private fun save(bmp: Bitmap) {
        dir.mkdirs()
        val tmp = File(dir, "current.tmp")
        tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tmp.renameTo(file)
    }

    companion object {
        fun defaultScreenSize(context: Context): Pair<Int, Int> {
            val b = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            return minOf(b.width(), b.height()) to maxOf(b.width(), b.height())
        }
    }
}
