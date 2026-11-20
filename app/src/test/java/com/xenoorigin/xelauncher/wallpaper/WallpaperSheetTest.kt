package com.xenoorigin.xelauncher.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Writes a contact sheet of the Xe Collection for visual review (not an assertion). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WallpaperSheetTest {
    @Test fun writeContactSheet() {
        if (System.getProperty("roborazzi.test.record") != "true") return
        val w = 270; val h = 585; val cols = 6
        val sheet = Bitmap.createBitmap(cols * (w + 10), 2 * (h + 10), Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet)
        XeCollection.forEachIndexed { i, p ->
            c.drawBitmap(WallpaperRenderer.render(p, w, h), (i % cols) * (w + 10f), (i / cols) * (h + 10f), null)
        }
        File("src/test/screenshots").mkdirs()
        File("src/test/screenshots/wallpapers_sheet.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
