package com.xenoorigin.xelauncher.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.util.Random
import kotlin.math.max

object WallpaperRenderer {
    fun render(p: WallpaperPreset, w: Int, h: Int): Bitmap = when (p) {
        is WallpaperPreset.Mesh -> mesh(p, w, h)
        is WallpaperPreset.Abstract -> abstract(p, w, h)
        is WallpaperPreset.Image -> image(p, w, h)
    }

    private fun Long.argb() = toInt()

    private fun mesh(p: WallpaperPreset.Mesh, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val rnd = Random(p.seed.toLong())
        c.drawColor(p.colors.first().argb())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val blobs = 7
        repeat(blobs) { i ->
            val color = p.colors[1 + i % (p.colors.size - 1)].argb()
            val cx = rnd.nextFloat() * w; val cy = rnd.nextFloat() * h
            val r = (0.45f + rnd.nextFloat() * 0.5f) * max(w, h) * 0.6f * (0.6f + 0.4f * p.glow)
            val alpha = (0.35f + rnd.nextFloat() * 0.35f) * p.glow
            val core = (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
            paint.shader = RadialGradient(cx, cy, r, intArrayOf(core, core and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, paint)
        }
        grain(out, p.grain, p.seed)
        return out
    }

    private fun abstract(p: WallpaperPreset.Abstract, w: Int, h: Int): Bitmap {
        // Bands are drawn small then scaled up: a cheap, smooth blur.
        val sw = max(8, w / 10); val sh = max(16, h / 10)
        val small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val sc = Canvas(small)
        val rnd = Random(p.seed.toLong())
        sc.drawColor(p.colors.first().argb())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        repeat(p.shapes) { i ->
            val top = sh * (i.toFloat() / p.shapes) - sh * .1f
            val amp = sh * (.08f + rnd.nextFloat() * .1f)
            val path = Path().apply {
                moveTo(-sw * .3f, top + amp)
                cubicTo(sw * .2f, top - amp * 1.5f, sw * .6f, top + amp * 2.2f, sw * 1.3f, top - amp * .5f)
                lineTo(sw * 1.3f, top + sh * .44f)
                cubicTo(sw * .75f, top + sh * .55f, sw * .25f, top + sh * .3f, -sw * .3f, top + sh * .48f)
                close()
            }
            val a = p.colors[1 + i % (p.colors.size - 1)].argb()
            val b = p.colors[1 + (i + 1) % (p.colors.size - 1)].argb()
            paint.shader = LinearGradient(0f, top, sw.toFloat(), top + sh * .3f, a, (b and 0x00FFFFFF) or 0x66000000, Shader.TileMode.CLAMP)
            sc.drawPath(path, paint)
        }
        paint.shader = LinearGradient(0f, 0f, sw.toFloat(), sh.toFloat(), 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        sc.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), paint)
        val mid = Bitmap.createScaledBitmap(small, sw * 2, sh * 2, true)
        val out = Bitmap.createScaledBitmap(mid, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        // Glass lenses: magnify what's under them a little and add a rim.
        val snapshot = out.copy(Bitmap.Config.ARGB_8888, false)
        repeat(p.lenses) {
            val r = w * (.14f + rnd.nextFloat() * .14f)
            val cx = r + rnd.nextFloat() * (w - 2 * r); val cy = r + rnd.nextFloat() * (h - 2 * r)
            val m = Matrix().apply { setScale(1.22f, 1.22f, cx, cy) }
            paint.shader = android.graphics.BitmapShader(snapshot, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(m) }
            c.drawCircle(cx, cy, r, paint)
            paint.shader = RadialGradient(cx - r * .35f, cy - r * .45f, r * 1.3f, intArrayOf(0x38FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, paint)
            paint.shader = android.graphics.SweepGradient(cx, cy, intArrayOf(0x20FFFFFF, 0xB0FFFFFF.toInt(), 0x20FFFFFF, 0x70FFFFFF, 0x20FFFFFF), null)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = max(1f, w / 300f)
            c.drawCircle(cx, cy, r, paint); paint.style = Paint.Style.FILL
        }
        paint.shader = null
        grain(out, .025f, p.seed)
        return out
    }

    private fun image(p: WallpaperPreset.Image, w: Int, h: Int): Bitmap {
        val src = BitmapFactory.decodeFile(p.file.path) ?: return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        return centerCrop(src, w, h)
    }

    fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val scale = max(w.toFloat() / src.width, h.toFloat() / src.height)
        val m = Matrix().apply { setScale(scale, scale); postTranslate((w - src.width * scale) / 2f, (h - src.height * scale) / 2f) }
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** Fine monochrome noise; prevents banding in smooth gradients. */
    private fun grain(b: Bitmap, amount: Float, seed: Int) {
        if (amount <= 0f) return
        val rnd = Random(seed * 31L + 7)
        val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        val amp = (amount * 255).toInt().coerceAtLeast(1)
        for (i in px.indices) {
            val n = rnd.nextInt(amp * 2 + 1) - amp
            val p = px[i]
            val r = ((p shr 16) and 0xFF) + n; val g = ((p shr 8) and 0xFF) + n; val bl = (p and 0xFF) + n
            px[i] = (p and -0x1000000) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or bl.coerceIn(0, 255)
        }
        b.setPixels(px, 0, b.width, 0, 0, b.width, b.height)
    }
}
