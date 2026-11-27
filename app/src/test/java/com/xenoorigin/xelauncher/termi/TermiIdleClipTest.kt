package com.xenoorigin.xelauncher.termi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Writes the idle loop and two routines as frames (build/termi_clip/) for a review clip. Not an assertion. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TermiIdleClipTest {
    @Test fun frames() {
        val dir = java.io.File("build/termi_clip").apply { deleteRecursively(); mkdirs() }
        val scale = 5
        val paint = Paint()
        var n = 0
        for ((anim, stage) in listOf(TermiAnim.IDLE to Stage.JUNIOR, TermiAnim.CODING to Stage.SENIOR, TermiAnim.IDLE to Stage.LEAD)) {
            val count = (anim.seconds * 12).toInt()
            for (i in 0 until count) {
                val f = TermiAnimations.frame(anim, i / count.toFloat(), i)
                val bmp = Bitmap.createBitmap(TermiSprite.BOX_W * scale, TermiSprite.BOX_H * scale, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp); c.drawColor(0xFF101014.toInt())
                paint.color = 0xFF2A2A34.toInt(); c.drawRect(0f, (TermiSprite.FEET_Y * scale).toFloat(), bmp.width.toFloat(), bmp.height.toFloat(), paint)
                (TermiSprite.build(f.pose, stage, emptyMap(), TermiSprite.bodyColor(0xFF9FD6FF)) + f.props).forEach { p ->
                    paint.color = p.argb
                    c.drawRect((p.x * scale).toFloat(), (p.y * scale).toFloat(), ((p.x + p.w) * scale).toFloat(), ((p.y + p.h) * scale).toFloat(), paint)
                }
                java.io.File(dir, "f%04d.png".format(n++)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
