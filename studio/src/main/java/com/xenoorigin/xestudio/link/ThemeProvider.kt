package com.xenoorigin.xestudio.link

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Lets Xe Launcher read the theme the user applied. Protected by a signature permission in the
 * manifest, so only apps signed with the same key (Xe) can read it.
 *
 * content://com.xenoorigin.xestudio.themes/icons → one row: recipe (JSON), version (time applied)
 * content://com.xenoorigin.xestudio.themes/images/<file> → an override picture
 */
class ThemeProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? {
        if (uri.pathSegments != listOf("icons")) return null
        val dir = XeLink.appliedDir(context!!)
        val cursor = MatrixCursor(arrayOf("recipe", "version"))
        val recipe = File(dir, "icons.json").takeIf { it.isFile }?.readText() ?: return cursor
        val version = File(dir, "version").takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() ?: 1L
        cursor.addRow(arrayOf<Any>(recipe, version))
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val seg = uri.pathSegments
        if (mode != "r" || seg.size != 2 || seg[0] != "images") throw FileNotFoundException(uri.toString())
        val name = seg[1]
        // A bare file name only: no folders, no "..".
        if (name.isEmpty() || name.contains('/') || name.contains('\\') || name.startsWith(".")) throw FileNotFoundException(uri.toString())
        val images = File(XeLink.appliedDir(context!!), "images")
        val f = File(images, name)
        if (!f.isFile || f.canonicalFile.parentFile != images.canonicalFile) throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? = if (uri.pathSegments.firstOrNull() == "images") "image/webp" else null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
}
