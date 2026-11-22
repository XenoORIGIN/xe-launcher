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

/**
 * How a picture is fitted to the screen. [zoom] >= 1 enlarges beyond "fill"; [offsetX]/[offsetY] in -1..1
 * pick which part of the overflow is shown (0 = centre). [dim] 0..0.8 darkens, [blur] 0..1 softens.
 */
data class WallpaperAdjust(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val dim: Float = 0f,
    val blur: Float = 0f,
)

object WallpaperRenderer {
    fun render(p: WallpaperPreset, w: Int, h: Int): Bitmap = when (p) {
        is WallpaperPreset.Mesh -> mesh(p, w, h)
        is WallpaperPreset.Abstract -> abstract(p, w, h)
        is WallpaperPreset.Image -> image(p, w, h)
        is WallpaperPreset.Sky -> sky(skyAt(p.minute ?: minuteNow(), sunToday()), w, h)
    }

    /** Gradient sky with the sun's or moon's glow and, at night, stars. */
    fun sky(look: SkyLook, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(look.top.argb(), look.mid.argb(), look.bottom.argb()), floatArrayOf(0f, .58f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        val ox = look.orbX * w; val oy = look.orbY * h
        if (look.stars > 0f) {
            val rnd = Random(2026)
            val unit = w / 400f
            repeat(260) {
                val x = rnd.nextFloat() * w
                val y = (rnd.nextFloat() * rnd.nextFloat()) * h * .85f
                val big = rnd.nextFloat() < .06f
                val r = (if (big) 1.5f + rnd.nextFloat() else .4f + rnd.nextFloat() * .8f) * unit
                val a = (look.stars * (.35f + rnd.nextFloat() * .65f) * 255).toInt().coerceIn(0, 255)
                val tint = if (rnd.nextFloat() < .2f) 0xFFDDE6 else if (rnd.nextFloat() < .2f) 0xD8E4FF else 0xFFFFFF
                paint.shader = null
                if (big) {
                    paint.shader = RadialGradient(x, y, r * 5f, intArrayOf((a / 3 shl 24) or tint, tint), null, Shader.TileMode.CLAMP)
                    c.drawCircle(x, y, r * 5f, paint); paint.shader = null
                }
                paint.color = (a shl 24) or tint
                c.drawCircle(x, y, r, paint)
            }
        }
        // Glow first, then the disc itself.
        val warm = if (look.sunUp) 0xFFE0A0 else 0xD6E2FF
        val glowR = w * (if (look.sunUp) .75f else .5f)
        val ga = ((if (look.sunUp) .5f else .22f) * look.orbGlow * 255).toInt()
        paint.shader = RadialGradient(ox, oy, glowR, intArrayOf((ga shl 24) or warm, ((ga / 4) shl 24) or warm, warm), floatArrayOf(0f, .35f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(ox, oy, glowR, paint)
        val discR = w * (if (look.sunUp) .055f else .04f)
        paint.shader = RadialGradient(ox, oy, discR, intArrayOf(if (look.sunUp) 0xFFFFF6E0.toInt() else 0xFFF2F5FF.toInt(), if (look.sunUp) 0xF0FFE2A8.toInt() else 0xE0DCE4F5.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(ox, oy, discR, paint)
        if (!look.sunUp) {
            // A crescent: the disc's shadow side.
            paint.shader = null; paint.color = (0xD0 shl 24) or (look.top.argb() and 0x00FFFFFF)
            c.drawCircle(ox + discR * .45f, oy - discR * .25f, discR * .9f, paint)
        }
        grain(out, .02f, 7)
        return out
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

    /** Fits [src] to w×h without stretching, then applies pan, zoom, blur and dim. */
    fun compose(src: Bitmap, w: Int, h: Int, a: WallpaperAdjust): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val scale = max(w.toFloat() / src.width, h.toFloat() / src.height) * a.zoom.coerceIn(1f, 4f)
        val dx = src.width * scale - w; val dy = src.height * scale - h
        val tx = -dx * (a.offsetX.coerceIn(-1f, 1f) + 1f) / 2f
        val ty = -dy * (a.offsetY.coerceIn(-1f, 1f) + 1f) / 2f
        val m = Matrix().apply { setScale(scale, scale); postTranslate(tx, ty) }
        val c = Canvas(out)
        c.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG))
        if (a.blur > 0.01f) {
            val small = Bitmap.createScaledBitmap(out, (w / 4).coerceAtLeast(1), (h / 4).coerceAtLeast(1), true)
            boxBlur(small, (a.blur * 6f).toInt().coerceAtLeast(1))
            c.drawBitmap(small, null, android.graphics.Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
        }
        if (a.dim > 0.005f) c.drawColor(android.graphics.Color.argb((a.dim.coerceIn(0f, .8f) * 255).toInt(), 0, 0, 0))
        return out
    }

    /** A tiny, heavily blurred copy of the wallpaper, drawn behind full-screen panels instead of live blur. */
    fun backdrop(src: Bitmap): Bitmap {
        val w = 120; val h = (src.height * w / src.width.toFloat()).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
        boxBlur(small, 5)
        return small
    }

    /** Three-pass box blur (close to Gaussian), in place. */
    fun boxBlur(b: Bitmap, radius: Int) {
        val w = b.width; val h = b.height
        val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(px.size)
        repeat(3) {
            pass(px, tmp, w, h, radius, horizontal = true)
            pass(tmp, px, w, h, radius, horizontal = false)
        }
        b.setPixels(px, 0, w, 0, 0, w, h)
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val div = 2 * r + 1
        for (line in 0 until lines) {
            fun idx(i: Int) = if (horizontal) line * w + i.coerceIn(0, len - 1) else i.coerceIn(0, len - 1) * w + line
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) { val p = src[idx(i)]; sa += p ushr 24; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF }
            for (i in 0 until len) {
                dst[idx(i)] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = src[idx(i + r + 1)]; val rem = src[idx(i - r)]
                sa += (add ushr 24) - (rem ushr 24)
                sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                sb += (add and 0xFF) - (rem and 0xFF)
            }
        }
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
