package com.xenoorigin.xelauncher.onboarding

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xenoorigin.xelauncher.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the app icon to PNG for review and for the website (record mode only). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AppIconShotTest {
    @Test fun renderIcon() {
        if (System.getProperty("roborazzi.test.record") != "true") return
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = ContextCompat.getDrawable(ctx, R.drawable.ic_launcher_background)!!
        val fg = ContextCompat.getDrawable(ctx, R.drawable.ic_launcher_foreground)!!
        // Same crop a launcher uses: the middle 72/108 of the layers.
        val inset = -512 / 4
        bg.setBounds(inset, inset, 512 - inset, 512 - inset); bg.draw(c)
        fg.setBounds(inset, inset, 512 - inset, 512 - inset); fg.draw(c)
        File("src/test/screenshots").mkdirs()
        File("src/test/screenshots/app_icon.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
