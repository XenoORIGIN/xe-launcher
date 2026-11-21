package com.xenoorigin.xelauncher.studio

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.xenoorigin.xestudio.core.IconOverride
import com.xenoorigin.xestudio.core.IconRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class StoredTheme(val recipe: IconRecipe, val version: Long)

/**
 * Xe's own copy of the icon theme applied in Xe Studio. Icons are always drawn from this copy, so the
 * theme keeps working if Studio is uninstalled; Studio is only asked for a new copy when it says so.
 */
class StudioThemeStore(
    private val dir: File,
    private val resolver: ContentResolver,
    /** True only when the theme provider really belongs to Xe Studio signed with Xe's key. */
    private val isTrusted: () -> Boolean = { true },
) {
    private val _theme = MutableStateFlow(load())
    val theme: StateFlow<StoredTheme?> = _theme.asStateFlow()
    private val images = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

    private fun load(): StoredTheme? {
        val recipe = File(dir, "icons.json").takeIf { it.isFile }?.readText()?.let(IconRecipe::decode) ?: return null
        val version = File(dir, "version").takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() ?: 1L
        return StoredTheme(recipe, version)
    }

    /** Copies the applied theme from Studio. False (and the old copy kept) when anything is missing or unreadable. */
    suspend fun pull(): Boolean = withContext(Dispatchers.IO) {
        if (!isTrusted()) return@withContext false
        runCatching {
            val (json, version) = resolver.query(Uri.parse("content://$AUTHORITY/icons"), null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) null else c.getString(c.getColumnIndexOrThrow("recipe")) to c.getLong(c.getColumnIndexOrThrow("version"))
            } ?: return@withContext false
            val recipe = IconRecipe.decode(json) ?: return@withContext false
            // Fetch every picture first; only then replace the old copy.
            val fresh = File(dir, "images.new").apply { deleteRecursively(); mkdirs() }
            for (o in recipe.overrides.values) if (o is IconOverride.Image && safeName(o.file)) {
                resolver.openInputStream(Uri.parse("content://$AUTHORITY/images/${o.file}"))?.use { input ->
                    File(fresh, o.file).outputStream().use { input.copyTo(it) }
                }
            }
            dir.mkdirs()
            val old = File(dir, "images")
            old.deleteRecursively()
            if (!fresh.renameTo(old)) { fresh.copyRecursively(old, overwrite = true); fresh.deleteRecursively() }
            write(File(dir, "icons.json"), json)
            write(File(dir, "version"), version.toString())
            images.clear()
            _theme.value = StoredTheme(recipe, version)
            true
        }.getOrDefault(false)
    }

    /** An override picture from the stored copy. */
    fun image(file: String): Bitmap? {
        if (!safeName(file)) return null
        images[file]?.let { return it }
        val f = File(File(dir, "images"), file).takeIf { it.isFile } ?: return null
        // Never decode a huge picture at full size on the home screen.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(f.path, it) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_SIDE) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val scaled = if (maxOf(bmp.width, bmp.height) > MAX_IMAGE_SIDE) {
            val k = MAX_IMAGE_SIDE.toFloat() / maxOf(bmp.width, bmp.height)
            Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true)
        } else bmp
        return scaled.also { images[file] = it }
    }

    private fun safeName(f: String) = f.isNotEmpty() && !f.contains('/') && !f.contains('\\') && !f.startsWith(".")

    private fun write(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { tmp.copyTo(f, overwrite = true); tmp.delete() }
    }

    companion object {
        const val MAX_IMAGE_SIDE = 512

        /** The provider answering for [AUTHORITY] is Xe Studio, signed with the same key as Xe. */
        fun studioIsGenuine(ctx: android.content.Context): Boolean = runCatching {
            val owner = ctx.packageManager.resolveContentProvider(AUTHORITY, 0)?.packageName
            owner == STUDIO_PACKAGE && ctx.packageManager.checkSignatures(ctx.packageName, STUDIO_PACKAGE) == android.content.pm.PackageManager.SIGNATURE_MATCH
        }.getOrDefault(false)

        const val AUTHORITY = "com.xenoorigin.xestudio.themes"
        const val ACTION_THEME_APPLIED = "com.xenoorigin.xestudio.action.THEME_APPLIED"
        const val STUDIO_PACKAGE = "com.xenoorigin.xestudio"
    }
}
