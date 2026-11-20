package com.xenoorigin.xelauncher.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

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

    private inline fun render(size: Int, draw: (Canvas, Int) -> Unit): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { draw(Canvas(it), size) }
}
