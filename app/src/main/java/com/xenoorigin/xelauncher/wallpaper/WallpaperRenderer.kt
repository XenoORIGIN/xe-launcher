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

/** Whole screen pixels per ASCII glyph pixel for a picture [drawnWidth] wide (about 72 glyphs across). */
fun asciiPixelScale(drawnWidth: Float): Int = maxOf(1, kotlin.math.round(drawnWidth / 360f).toInt())

object WallpaperRenderer {
    fun render(p: WallpaperPreset, w: Int, h: Int): Bitmap = when (p) {
        is WallpaperPreset.Mesh -> mesh(p, w, h)
        is WallpaperPreset.Bokeh -> bokeh(p, w, h)
        is WallpaperPreset.Abstract -> abstract(p, w, h)
        is WallpaperPreset.Image -> image(p, w, h)
        is WallpaperPreset.Liquid -> liquid(p, w, h)
        is WallpaperPreset.Sky -> sky(skyAt(p.minute ?: minuteNow(), sunToday()), w, h)
        is WallpaperPreset.Ascii -> ascii(p, w, h)
    }

    /**
     * A still frame of an ASCII wallpaper. Each glyph pixel becomes a whole [asciiPixelScale] block of
     * screen pixels (never blurred); the grid is centred and any leftover edge shows the ground colour.
     * Small sizes (thumbnails) are drawn at phone resolution and scaled down, so they keep the detail.
     */
    fun ascii(p: WallpaperPreset.Ascii, w: Int, h: Int, t: Float = 0f): Bitmap {
        if (w < 360) {
            val big = ascii(p, 360, (h * 360f / w).toInt().coerceAtLeast(8), t)
            return Bitmap.createScaledBitmap(big, w, h, true)
        }
        val k = asciiPixelScale(w.toFloat())
        val cols = (w + 5 * k - 1) / (5 * k)
        val rows = (h + 8 * k - 1) / (8 * k)
        val f = com.xenoorigin.xelauncher.wallpaper.ascii.AsciiField(cols, rows)
        val look = if (p.live) skyAt(p.minute ?: minuteNow(), sunToday()) else null
        f.fill(p.scene, t, look, p.color, p.accent)
        val rw = cols * 5; val rh = rows * 8
        val px = IntArray(rw * rh)
        com.xenoorigin.xelauncher.wallpaper.ascii.AsciiRaster.rasterize(f.cells, px)
        val raster = Bitmap.createBitmap(px, rw, rh, Bitmap.Config.ARGB_8888)
        val scaled = Bitmap.createScaledBitmap(raster, rw * k, rh * k, false)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(com.xenoorigin.xelauncher.wallpaper.ascii.AsciiRaster.GROUND)
        val (ox, oy) = asciiOrigin(w, h)
        c.drawBitmap(scaled, ox.toFloat(), oy.toFloat(), null)
        return out
    }

    /** Where the scaled ASCII grid's top-left corner lands for a [w] × [h] picture (≤ 0: centred overflow). */
    fun asciiOrigin(w: Int, h: Int): Pair<Int, Int> {
        val k = asciiPixelScale(w.toFloat())
        val cols = (w + 5 * k - 1) / (5 * k)
        val rows = (h + 8 * k - 1) / (8 * k)
        return (w - cols * 5 * k) / 2 to (h - rows * 8 * k) / 2
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

    /**
     * Light through leaves: a dark-to-light base, soft foliage, then three layers of bokeh discs from very
     * soft and big to small and crisp. Soft layers are drawn small, blurred and scaled up, so it stays quick.
     * colors = [top, bottom, glow, then the light colours].
     */
    private fun bokeh(p: WallpaperPreset.Bokeh, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val rnd = Random(p.seed.toLong())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val top = p.colors[0].argb(); val bottom = p.colors[1].argb(); val glow = p.colors[2].argb()
        val lights = p.colors.drop(3).map { it.argb() }
        paint.shader = android.graphics.LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = RadialGradient(w * .7f, h * .3f, max(w, h) * .55f, intArrayOf(alpha(glow, .55f), alpha(glow, 0f)), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
        val unit = w / 1080f
        fun layer(scale: Int, blur: Int, count: Int, rMin: Float, rMax: Float, a0: Float, a1: Float, ring: Boolean, ovals: Boolean, shade: Int = 0, shadeT: Float = 0f) {
            val lw = max(1, w / scale); val lh = max(1, h / scale)
            val lb = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888)
            val lc = Canvas(lb)
            val pp = Paint(Paint.ANTI_ALIAS_FLAG)
            val k = unit / scale
            repeat(count) {
                val x = rnd.nextFloat() * lw; val y = rnd.nextFloat() * lh * (if (ovals) 1f else .88f)
                val r = (rMin + rnd.nextFloat() * (rMax - rMin)) * k
                val col = lights[rnd.nextInt(lights.size)]
                val a = a0 + rnd.nextFloat() * (a1 - a0)
                pp.style = Paint.Style.FILL
                pp.color = if (ovals) alpha(blend(col, shade, shadeT), a) else alpha(blend(col, 0xFFFFFFFF.toInt(), .45f), a)
                if (ovals) lc.drawOval(x - r, y - r * .65f, x + r, y + r * .65f, pp) else lc.drawCircle(x, y, r, pp)
                if (ring && r > 2f) {
                    pp.style = Paint.Style.STROKE; pp.strokeWidth = max(1f, r * .1f); pp.color = alpha(0xFFFFFFFF.toInt(), a * .6f)
                    lc.drawCircle(x, y, r, pp)
                }
            }
            if (blur > 0) boxBlur(lb, blur)
            c.drawBitmap(lb, null, android.graphics.Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
            lb.recycle()
        }
        layer(6, 2, 200, 50f, 170f, .35f, .75f, ring = false, ovals = true, shade = top, shadeT = .62f) // dark foliage
        layer(6, 3, 30, 80f, 170f, .14f, .3f, ring = false, ovals = false)   // big soft bokeh
        layer(2, 1, 55, 22f, 70f, .4f, .78f, ring = true, ovals = false)      // medium bokeh with bright rims
        layer(1, 0, 110, 3f, 12f, .5f, .95f, ring = false, ovals = false)     // crisp sparkles
        paint.shader = RadialGradient(w / 2f, h * .45f, max(w, h) * .75f, intArrayOf(0, alpha(top, .55f)), floatArrayOf(.55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
        grain(out, .025f, p.seed)
        return out
    }

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
        depth(c, w, h, rnd, p.colors, p.glow)
        grain(out, p.grain, p.seed)
        return out
    }

    /**
     * Finishing touches that give flat colour fields depth: a soft light leak from a corner, a few
     * out-of-focus glass orbs, and a gentle vignette.
     */
    private fun depth(c: Canvas, w: Int, h: Int, rnd: Random, colors: List<Long>, strength: Float = 1f) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val leak = colors.getOrElse(1) { 0xFFFFFFFF }.argb()
        val lx = if (rnd.nextBoolean()) 0f else w.toFloat()
        paint.shader = RadialGradient(lx, 0f, max(w, h) * .7f, intArrayOf((leak and 0x00FFFFFF) or ((60 * strength).toInt().coerceIn(0, 255) shl 24), leak and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        repeat(5) {
            val r = w * (.03f + rnd.nextFloat() * .07f)
            val cx = rnd.nextFloat() * w; val cy = rnd.nextFloat() * h
            val tint = colors[1 + rnd.nextInt(colors.size - 1)].argb() and 0x00FFFFFF
            paint.shader = RadialGradient(cx, cy, r, intArrayOf(tint or 0x10000000, tint or 0x2A000000, tint or 0x08000000), floatArrayOf(0f, .8f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, paint)
        }
        paint.shader = RadialGradient(w / 2f, h * .45f, max(w, h) * .8f, intArrayOf(0x00000000, 0x00000000, 0x55000000), floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    /** Liquid glass art; soft shapes are painted small and scaled up (a cheap, smooth blur), crisp light on top. */
    private fun liquid(p: WallpaperPreset.Liquid, w: Int, h: Int): Bitmap {
        val rnd = Random(p.seed.toLong())
        val sw = max(24, w / 9); val sh = max(48, h / 9)
        val small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val sc = Canvas(small)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val bg = p.colors.first().argb()
        val accents = p.colors.drop(1).map { it.argb() }
        // A deep base that is a little lighter at the top, so the art floats on something.
        paint.shader = LinearGradient(0f, 0f, 0f, sh.toFloat(), intArrayOf(blend(bg, accents[0], .18f), bg, bg), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        sc.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
        when (p.style) {
            LiquidStyle.RIBBONS -> repeat(3) { i ->
                // Wide, soft ribbons that cross each other, each with colour flowing along it.
                val y0 = sh * (.18f + i * .26f + rnd.nextFloat() * .06f)
                val thick = sh * (.16f + rnd.nextFloat() * .10f)
                val bend = sh * (.18f + rnd.nextFloat() * .14f) * (if (i % 2 == 0) 1 else -1)
                val path = Path().apply {
                    moveTo(-sw * .3f, y0 + bend * .4f)
                    cubicTo(sw * .25f, y0 - bend, sw * .75f, y0 + bend, sw * 1.3f, y0 - bend * .3f)
                    lineTo(sw * 1.3f, y0 - bend * .3f + thick * .6f)
                    cubicTo(sw * .75f, y0 + bend + thick, sw * .25f, y0 - bend + thick * .7f, -sw * .3f, y0 + bend * .4f + thick)
                    close()
                }
                val a = accents[i % accents.size]; val b = accents[(i + 1) % accents.size]
                paint.shader = LinearGradient(0f, 0f, sw.toFloat(), 0f, intArrayOf(alpha(a, .55f), alpha(b, .85f), alpha(a, .45f)), null, Shader.TileMode.CLAMP)
                sc.drawPath(path, paint)
            }
            LiquidStyle.AURORA -> {
                // Curtains: vertical rays rising from a wavy line, brighter at their foot, fading upwards.
                repeat(3) { band ->
                    val col = accents[band % accents.size]
                    val base = sh * (.42f + band * .1f)
                    val amp = sh * (.06f + rnd.nextFloat() * .05f)
                    val ph = rnd.nextFloat() * 6.28f
                    val height = sh * (.30f + rnd.nextFloat() * .12f)
                    for (x in 0 until sw) {
                        val t = x.toFloat() / sw
                        val y = base + amp * kotlin.math.sin(t * 6.28f * 1.1f + ph) + amp * .4f * kotlin.math.sin(t * 6.28f * 3.3f + ph * 2)
                        val strength = (.35f + .65f * ((kotlin.math.sin(t * 6.28f * 2.4f + ph * 3) + 1f) / 2f)) * .75f
                        // Bright at the foot, fading up into the night and softly down below the foot.
                        val foot = sh * .05f
                        paint.shader = LinearGradient(0f, y + foot, 0f, y - height, intArrayOf(alpha(col, 0f), alpha(col, strength), alpha(col, strength * .5f), alpha(col, 0f)), floatArrayOf(0f, foot / (foot + height), .45f, 1f), Shader.TileMode.CLAMP)
                        sc.drawRect(x.toFloat(), y - height, x + 1.2f, y + foot, paint)
                    }
                }
            }
            LiquidStyle.CHROME -> repeat(4) { i ->
                // Polished metal swells: wide bands with one narrow bright highlight each.
                val top = sh * (i / 4f) - sh * .04f
                val amp = sh * (.05f + rnd.nextFloat() * .04f)
                val band = sh * .3f
                val path = Path().apply {
                    moveTo(-sw * .2f, top)
                    cubicTo(sw * .3f, top - amp * 2, sw * .7f, top + amp * 2, sw * 1.2f, top - amp * .5f)
                    lineTo(sw * 1.2f, top + band)
                    cubicTo(sw * .7f, top + band + amp * 2, sw * .3f, top + band - amp * 2, -sw * .2f, top + band)
                    close()
                }
                val hi = accents[0]; val mid = accents[1 + i % (accents.size - 1)]
                paint.shader = LinearGradient(0f, top, 0f, top + band, intArrayOf(alpha(mid, .0f), alpha(mid, .55f), alpha(hi, .95f), alpha(mid, .45f), alpha(mid, 0f)), floatArrayOf(0f, .35f, .5f, .62f, 1f), Shader.TileMode.CLAMP)
                sc.drawPath(path, paint)
            }
        }
        paint.xfermode = null
        val mid = Bitmap.createScaledBitmap(small, sw * 3, sh * 3, true)
        val out = Bitmap.createScaledBitmap(mid, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        // A couple of crisp specular glints over the soft shapes: the "glass" in Liquid Glass.
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        repeat(2) {
            val y = h * (.25f + rnd.nextFloat() * .5f)
            paint.strokeWidth = max(1f, w / 300f)
            paint.shader = LinearGradient(0f, y, w.toFloat(), y, intArrayOf(0x00FFFFFF, 0x55FFFFFF, 0x00FFFFFF), floatArrayOf(.1f, .45f, .8f), Shader.TileMode.CLAMP)
            val path = Path().apply { moveTo(-w * .1f, y); cubicTo(w * .3f, y - h * .06f, w * .7f, y + h * .06f, w * 1.1f, y - h * .02f) }
            c.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL; paint.shader = null
        depth(c, w, h, rnd, p.colors, .6f)
        grain(out, .028f, p.seed)
        return out
    }

    private fun alpha(c: Int, a: Float) = (c and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
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

    /** The wallpaper as Liquid Glass sees it: 360 px wide, softly blurred and a little brighter. */
    fun glassBackdrop(src: Bitmap): Bitmap {
        // 2.0: sharper and larger, so the lens edge visibly bends what's behind (as in the reference clip).
        val w = 540; val h = (src.height * w / src.width.toFloat()).toInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
        boxBlur(out, 1)
        val px = IntArray(w * h); out.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val c = px[i]
            fun up(v: Int) = v + ((255 - v) * 12) / 100
            px[i] = (c and 0xFF000000.toInt()) or (up((c shr 16) and 255) shl 16) or (up((c shr 8) and 255) shl 8) or up(c and 255)
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
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
