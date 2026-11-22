package com.xenoorigin.xelauncher.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/** A glyph cut out of an icon; [isGlyph] is false when the icon had to be kept whole. */
class Glyph(val bitmap: Bitmap, val isGlyph: Boolean)

object IconProcessor {
    private const val FALLBACK = 0xFF8E8E93.toInt()

    /**
     * Desaturates [src] and applies [contrast] around mid-grey. With [lightenDarkGlyphs], an icon whose
     * visible pixels are mostly dark is inverted so it stays visible on dark glass.
     */
    fun grayscale(src: Bitmap, contrast: Float, lightenDarkGlyphs: Boolean = false): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val t = (1f - contrast) * 128f
        cm.postConcat(ColorMatrix(floatArrayOf(contrast, 0f, 0f, 0f, t, 0f, contrast, 0f, 0f, t, 0f, 0f, contrast, 0f, t, 0f, 0f, 0f, 1f, 0f)))
        if (lightenDarkGlyphs && averageLuminance(src) < 0.3f) {
            cm.postConcat(ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)))
        }
        Canvas(out).drawBitmap(src, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) })
        // Make channels exactly equal (filter rounding can differ by 1).
        val px = IntArray(out.width * out.height); out.getPixels(px, 0, out.width, 0, 0, out.width, out.height)
        for (i in px.indices) { val p = px[i]; val g = Color.green(p); px[i] = Color.argb(Color.alpha(p), g, g, g) }
        out.setPixels(px, 0, out.width, 0, 0, out.width, out.height)
        return out
    }

    /**
     * Liquid Glass symbol: every pixel turned near-white, keeping its alpha and a hint of its shading
     * ([shade] of the darkness) so the symbol isn't flat.
     */
    fun milkyWhite(src: Bitmap, shade: Float = .15f): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h); src.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val c = px[i]; val a = Color.alpha(c)
            if (a == 0) { px[i] = 0; continue }
            val lum = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000
            val v = (255 - (255 - lum) * shade).toInt().coerceIn(0, 255)
            px[i] = Color.argb(a, v, v, v)
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, w, 0, 0, w, h) }
    }

    /** A whole-tile icon (no separate symbol) as milky white: light parts solid, dark parts fade into the glass. */
    fun milkyFromFull(src: Bitmap): Bitmap {
        val out = milkyWhite(src)
        val w = src.width; val h = src.height
        val s = IntArray(w * h); src.getPixels(s, 0, w, 0, 0, w, h)
        val o = IntArray(w * h); out.getPixels(o, 0, w, 0, 0, w, h)
        for (i in s.indices) {
            val c = s[i]
            val lum = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000
            val a = (Color.alpha(c) * (.25f + .75f * lum / 255f)).toInt().coerceIn(0, 255)
            o[i] = (o[i] and 0x00FFFFFF) or (a shl 24)
        }
        out.setPixels(o, 0, w, 0, 0, w, h)
        return out
    }

    /** Average luminance (0..1) of pixels with alpha >= 128; 1 when none. */
    fun averageLuminance(src: Bitmap): Float {
        var sum = 0.0; var n = 0
        forEachSample(src) { p ->
            if (Color.alpha(p) >= 128) { sum += (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)) / 255.0; n++ }
        }
        return if (n == 0) 1f else (sum / n).toFloat()
    }

    /** Most common saturated hue bucket, ignoring transparent and near-white/near-black pixels. */
    fun dominantColor(src: Bitmap): Int {
        val buckets = HashMap<Int, IntArray>() // key -> [count, r, g, b]
        forEachSample(src) { p ->
            if (Color.alpha(p) < 128) return@forEachSample
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            val max = maxOf(r, g, b); val min = minOf(r, g, b)
            if (max < 40 || (min > 215) || max - min < 24) return@forEachSample
            val k = (r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)
            val a = buckets.getOrPut(k) { IntArray(4) }
            a[0]++; a[1] += r; a[2] += g; a[3] += b
        }
        val best = buckets.values.maxByOrNull { it[0] } ?: return FALLBACK
        return Color.rgb(best[1] / best[0], best[2] / best[0], best[3] / best[0])
    }

    private inline fun forEachSample(src: Bitmap, f: (Int) -> Unit) {
        val step = if (src.width > 64) 2 else 1
        var y = 0
        while (y < src.height) { var x = 0; while (x < src.width) { f(src.getPixel(x, y)); x += step }; y += step }
    }

    /** The glyph layer only (adaptive foreground), cropped to the visible 72dp area. */
    fun foregroundOf(d: Drawable, sizePx: Int): Bitmap =
        if (d is AdaptiveIconDrawable && d.foreground != null) render(sizePx) { c, s ->
            val inset = s / 4; d.foreground.setBounds(-inset, -inset, s + inset, s + inset); d.foreground.draw(c)
        } else full(d, sizePx)

    /** The complete icon (background + foreground for adaptive icons), unmasked. */
    fun full(d: Drawable, sizePx: Int): Bitmap = render(sizePx) { c, s ->
        if (d is AdaptiveIconDrawable) {
            val inset = s / 4
            d.background?.let { it.setBounds(-inset, -inset, s + inset, s + inset); it.draw(c) }
            d.foreground?.let { it.setBounds(-inset, -inset, s + inset, s + inset); it.draw(c) }
        } else { d.setBounds(0, 0, s, s); d.draw(c) }
    }

    /** Centre-crops any picture to a square icon of [size] px (for user-picked icon images). */
    fun square(src: Bitmap, size: Int): Bitmap {
        val side = minOf(src.width, src.height)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val sx = (src.width - side) / 2; val sy = (src.height - side) / 2
        Canvas(out).drawBitmap(src, android.graphics.Rect(sx, sy, sx + side, sy + side), android.graphics.Rect(0, 0, size, size), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private inline fun render(size: Int, draw: (Canvas, Int) -> Unit): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { draw(Canvas(it), size) }

    /**
     * Returns just the symbol of an icon layer. When the layer carries its own flat plate (legacy icons
     * wrapped by the system), the plate colour is keyed out. Icons without one flat plate (photos,
     * gradients) come back unchanged with isGlyph = false so the caller can show them whole.
     * Small symbols are enlarged so every tile looks equally full.
     */
    fun extractGlyph(src: Bitmap): Glyph {
        val w = src.width; val h = src.height
        val px = IntArray(w * h); src.getPixels(px, 0, w, 0, 0, w, h)
        var opaque = 0; var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) for (x in 0 until w) if (Color.alpha(px[y * w + x]) > 200) {
            opaque++; if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y
        }
        if (opaque == 0) return Glyph(src, true)
        if (opaque < w * h * 0.45f) return Glyph(normalize(px, w, h), true)

        // Sample a ring just inside the opaque area to find the plate colour.
        val bw = maxX - minX; val bh = maxY - minY
        val inset = (minOf(bw, bh) * 0.1f).toInt().coerceAtLeast(1)
        val ring = ArrayList<Int>()
        val x0 = minX + inset; val x1 = maxX - inset; val y0 = minY + inset; val y1 = maxY - inset
        val step = (minOf(bw, bh) / 24).coerceAtLeast(1)
        var t = x0; while (t <= x1) { ring += px[y0 * w + t]; ring += px[y1 * w + t]; t += step }
        t = y0; while (t <= y1) { ring += px[t * w + x0]; ring += px[t * w + x1]; t += step }
        val ringOpaque = ring.filter { Color.alpha(it) > 200 }
        if (ringOpaque.size < ring.size / 2) return Glyph(src, false)
        val pr = ringOpaque.map(Color::red).sorted()[ringOpaque.size / 2]
        val pg = ringOpaque.map(Color::green).sorted()[ringOpaque.size / 2]
        val pb = ringOpaque.map(Color::blue).sorted()[ringOpaque.size / 2]
        fun dist(c: Int) = maxOf(kotlin.math.abs(Color.red(c) - pr), kotlin.math.abs(Color.green(c) - pg), kotlin.math.abs(Color.blue(c) - pb))
        if (ringOpaque.count { dist(it) < 36 } < ringOpaque.size * 0.75f) return Glyph(src, false)

        val out = IntArray(px.size)
        var kept = 0
        for (i in px.indices) {
            val c = px[i]; val a = Color.alpha(c)
            if (a == 0) continue
            val d = dist(c)
            val keep = ((d - 26f) / 40f).coerceIn(0f, 1f)
            val na = (a * keep).toInt()
            if (na > 128) kept++
            out[i] = (c and 0x00FFFFFF) or (na shl 24)
        }
        if (kept < opaque * 0.01f || kept > opaque * 0.6f) return Glyph(src, false)
        return Glyph(normalize(out, w, h), true)
    }

    /** Scales the visible content so its larger side is between 50% and 80% of the bitmap, centred. */
    private fun normalize(px: IntArray, w: Int, h: Int): Bitmap {
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) for (x in 0 until w) if (Color.alpha(px[y * w + x]) > 24) {
            if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y
        }
        val src = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, w, 0, 0, w, h) }
        if (maxX < 0) return src
        val cw = maxX - minX + 1; val ch = maxY - minY + 1
        val big = maxOf(cw, ch).toFloat() / maxOf(w, h)
        val target = when { big < 0.5f -> 0.62f; big > 0.8f -> 0.8f; else -> big }
        val scale = target / big
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val m = android.graphics.Matrix().apply {
            postTranslate(-(minX + cw / 2f), -(minY + ch / 2f)); postScale(scale, scale); postTranslate(w / 2f, h / 2f)
        }
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }
}
