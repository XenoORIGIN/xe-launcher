package com.xenoorigin.xelauncher.design

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onLayoutRectChanged
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Xe 2.0 Liquid Glass, drawn as a real lens (no shader needed, so it looks the same on every phone and in
 * tests): the wallpaper behind is magnified a little, bent hard inward near the edge with the colours split
 * (rainbow streaks), then a crisp bright rim, a soft inner lip and faint cyan/pink fringes go on top.
 * Shapes are signed distance functions (negative inside), so blobs can melt together like the reference clip.
 */
object LensGlass {
    /** Signed distance to a rounded rectangle centred at ([cx], [cy]) with half size ([hw], [hh]) and corner [r]. */
    fun roundRect(x: Float, y: Float, cx: Float, cy: Float, hw: Float, hh: Float, r: Float): Float {
        val rr = min(r, min(hw, hh))
        val qx = abs(x - cx) - hw + rr; val qy = abs(y - cy) - hh + rr
        return hypot(max(qx, 0f), max(qy, 0f)) + min(max(qx, qy), 0f) - rr
    }

    fun circle(x: Float, y: Float, cx: Float, cy: Float, r: Float): Float = hypot(x - cx, y - cy) - r

    /** Smooth union: the two shapes melt into one with a neck of size about [k] where they come close. */
    fun smin(a: Float, b: Float, k: Float): Float {
        if (k <= 0f) return min(a, b)
        val h = (.5f + .5f * (b - a) / k).coerceIn(0f, 1f)
        return b * (1 - h) + a * h - k * h * (1 - h)
    }

    /** Lens settings, all lengths in px. */
    class Lens(val strength: Float, val edge: Float, val magnify: Float, val chroma: Float)

    /**
     * Where to sample the backdrop for the point ([x], [y]) inside the shape: pulled toward [center] to magnify,
     * and pushed inward along the edge normal inside the [Lens.edge] band (strongest at the rim).
     * [k] scales the bend for one colour channel (1 = green). Returns (sx, sy).
     */
    fun sample(x: Float, y: Float, sdf: (Float, Float) -> Float, center: (Float, Float) -> Pair<Float, Float>, lens: Lens, k: Float): Pair<Float, Float> {
        val d = -sdf(x, y)
        if (d <= 0f) return x to y
        val t = (d / lens.edge).coerceIn(0f, 1f)
        val prof = (1 - t) * (1 - t)
        val e = 1f
        var nx = sdf(x + e, y) - sdf(x - e, y); var ny = sdf(x, y + e) - sdf(x, y - e)
        val nl = max(hypot(nx, ny), 1e-4f); nx /= nl; ny /= nl
        val (cx, cy) = center(x, y)
        val ch = 1f + (k - 1f) * prof
        val ox = (-nx * lens.strength * prof - (x - cx) * lens.magnify) * ch
        val oy = (-ny * lens.strength * prof - (y - cy) * lens.magnify) * ch
        return (x + ox) to (y + oy)
    }

    /**
     * Renders the lens on the CPU into a [w]×[h] bitmap at one sample per [res] px (drawn scaled up), reading
     * [pixels] (the backdrop, [bw]×[bh]); [src] is the backdrop area behind this bitmap. Outside the shape is
     * clear and the edge is anti-aliased. Meant for a background thread; cached by the caller.
     */
    fun render(
        w: Int, h: Int, res: Float, pixels: IntArray, bw: Int, bh: Int, src: RectF,
        sdf: (Float, Float) -> Float, center: (Float, Float) -> Pair<Float, Float>, lens: Lens, dp: Float,
    ): Bitmap {
        val ow = max(1, (w / res).toInt()); val oh = max(1, (h / res).toInt())
        val out = IntArray(ow * oh)
        val sx = src.width() / w; val sy = src.height() / h
        fun fetch(x: Float, y: Float, shift: Int): Int {
            // Bilinear sample of one channel of the backdrop at layer point (x, y).
            val bx = (src.left + x * sx - .5f).coerceIn(0f, bw - 1.001f); val by = (src.top + y * sy - .5f).coerceIn(0f, bh - 1.001f)
            val x0 = bx.toInt(); val y0 = by.toInt(); val fx = bx - x0; val fy = by - y0
            val x1 = min(x0 + 1, bw - 1); val y1 = min(y0 + 1, bh - 1)
            fun c(px: Int, py: Int) = (pixels[py * bw + px] shr shift) and 255
            val top = c(x0, y0) * (1 - fx) + c(x1, y0) * fx
            val bot = c(x0, y1) * (1 - fx) + c(x1, y1) * fx
            return (top * (1 - fy) + bot * fy).toInt().coerceIn(0, 255)
        }
        for (yy in 0 until oh) for (xx in 0 until ow) {
            val x = (xx + .5f) * res; val y = (yy + .5f) * res
            val sd = sdf(x, y)
            val a = ((.6f - sd / dp) / 1.2f).coerceIn(0f, 1f)
            if (a <= 0f) continue
            val d = -sd
            val t = (d / lens.edge).coerceIn(0f, 1f)
            val prof = (1 - t) * (1 - t)
            var nx = sdf(x + 1f, y) - sdf(x - 1f, y); var ny = sdf(x, y + 1f) - sdf(x, y - 1f)
            val nl = max(hypot(nx, ny), 1e-4f); nx /= nl; ny /= nl
            val (cx, cy) = center(x, y)
            val ox = -nx * lens.strength * prof - (x - cx) * lens.magnify
            val oy = -ny * lens.strength * prof - (y - cy) * lens.magnify
            val kr = 1f + lens.chroma * prof; val kb = 1f - lens.chroma * prof
            val r = fetch(x + ox * kr, y + oy * kr, 16)
            val g = fetch(x + ox, y + oy, 8)
            val b = fetch(x + ox * kb, y + oy * kb, 0)
            out[yy * ow + xx] = ((a * 255).toInt() shl 24) or (r shl 16) or (g shl 8) or b
        }
        return Bitmap.createBitmap(out, ow, oh, Bitmap.Config.ARGB_8888)
    }

    /**
     * The shape's smooth-edged mask and its light: a crisp rim, a soft lip just inside it (brighter at the top),
     * faint cyan and pink fringes, and a light film. [res] = px per sample, [dp] = px per dp.
     */
    fun buildOverlay(w: Int, h: Int, res: Float, dp: Float, sdf: (Float, Float) -> Float, film: Float): Pair<Bitmap, Bitmap> {
        val bw = max(1, (w / res).toInt()); val bh = max(1, (h / res).toInt())
        val mask = IntArray(bw * bh); val over = IntArray(bw * bh)
        for (yy in 0 until bh) for (xx in 0 until bw) {
            val x = (xx + .5f) * res; val y = (yy + .5f) * res
            val sd = sdf(x, y) / dp // in dp, negative inside
            val a = ((.6f - sd) / 1.2f).coerceIn(0f, 1f) // ~1dp anti-aliased edge
            if (a <= 0f) continue
            mask[yy * bw + xx] = (a * 255).toInt() shl 24
            val top = 1f - (y / h) * .55f
            val rim = exp(-((sd + .7f) * (sd + .7f)) / .35f) * .95f
            val lip = if (sd < 0f) exp(sd / 3.2f) * .30f * top else 0f
            val cyan = exp(-((sd + 2.4f) * (sd + 2.4f)) / .5f) * .20f
            val pink = exp(-((sd + 3.6f) * (sd + 3.6f)) / .6f) * .16f
            // Mix white light with the two tints (un-premultiplied colour + alpha).
            val wA = (film + lip + rim).coerceAtMost(1f)
            val tA = (cyan + pink).coerceAtMost(1f)
            val total = (wA + tA * (1 - wA)).coerceIn(0f, 1f)
            if (total <= 0f) continue
            val mix = if (total > 0f) tA * (1 - wA) / total else 0f
            val cr = 255f * (1 - mix) + mix * (if (cyan > pink) 127f else 255f)
            val cg = 255f * (1 - mix) + mix * (if (cyan > pink) 232f else 138f)
            val cb = 255f * (1 - mix) + mix * (if (cyan > pink) 255f else 216f)
            over[yy * bw + xx] = ((total * a * 255).toInt() shl 24) or (cr.toInt() shl 16) or (cg.toInt() shl 8) or cb.toInt()
        }
        return Bitmap.createBitmap(mask, bw, bh, Bitmap.Config.ARGB_8888) to Bitmap.createBitmap(over, bw, bh, Bitmap.Config.ARGB_8888)
    }

    fun sqr(v: Float) = v * v
    fun len(x: Float, y: Float) = sqrt(x * x + y * y)
}

/** Where lens pictures are worked out; tests swap in an immediate one. */
var lensDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default

/**
 * A rounded panel of 2.0 Liquid Glass (dock, widgets, folders): the wallpaper behind is bent by a real lens,
 * worked out off the main thread whenever the panel moves, with the rim and light drawn over [content].
 * Without a wallpaper copy it falls back to a light film.
 */
@androidx.compose.runtime.Composable
fun LensPanel(corner: androidx.compose.ui.unit.Dp, modifier: androidx.compose.ui.Modifier, content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val d = androidx.compose.ui.platform.LocalDensity.current
    val dp = d.density
    val backdrop = LocalGlassBackdrop.current
    val img = backdrop?.image()
    var at by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val w = at.width.toInt(); val h = at.height.toInt()
    val rPx = with(d) { corner.toPx() }
    val sdf = { x: Float, y: Float -> LensGlass.roundRect(x, y, w / 2f, h / 2f, w / 2f, h / 2f, rPx) }
    val center = { _: Float, _: Float -> w / 2f to h / 2f }
    val looks = androidx.compose.runtime.remember(w, h, rPx) { if (w > 2 && h > 2) LensGlass.buildOverlay(w, h, dp / 2f, dp, sdf, .05f) else null }
    // Big panels (widgets) bend gently: the zoom never shifts the picture more than ~6 dp, the edge band stays thin.
    val lens = androidx.compose.runtime.remember(dp, w, h) {
        val half = (minOf(w, h) / 2f).coerceAtLeast(1f)
        LensGlass.Lens(strength = minOf(16f * dp, half * .35f), edge = minOf(18f * dp, half * .45f), magnify = minOf(.12f, 6f * dp / half), chroma = .6f)
    }
    val pixels = androidx.compose.runtime.remember(img) { img?.asAndroidBitmap()?.let { b -> IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) } } }
    var picture by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Bitmap?>(null) }
    val key = Triple((at.left / 6).toInt(), (at.top / 6).toInt(), w * 31 + h)
    androidx.compose.runtime.LaunchedEffect(img, key) {
        val b = img?.asAndroidBitmap() ?: return@LaunchedEffect
        val px = pixels ?: return@LaunchedEffect
        if (w <= 2 || h <= 2) return@LaunchedEffect
        val src = backdropSource(at, androidx.compose.ui.unit.IntSize(b.width, b.height), backdrop.screen(), backdrop.parallaxPx())
        val area = at
        picture = kotlinx.coroutines.withContext(lensDispatcher) {
            LensGlass.render(area.width.toInt(), area.height.toInt(), dp / 1.5f, px, b.width, b.height, RectF(src.left, src.top, src.right, src.bottom), sdf, center, lens, dp)
        }
    }
    val paint = androidx.compose.runtime.remember { android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG) }
    val smoke = androidx.compose.runtime.remember { android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply { colorFilter = android.graphics.PorterDuffColorFilter(0x2E000000, android.graphics.PorterDuff.Mode.SRC_IN) } }
    androidx.compose.foundation.layout.Box(
        modifier
            .androidx_onRect { at = it }
            .then(androidx.compose.ui.Modifier.drawWithContent {
                val dst = RectF(0f, 0f, size.width, size.height)
                drawIntoCanvasCompat { nc ->
                    val p = picture
                    if (p != null) {
                        nc.drawBitmap(p, null, dst, paint)
                        // A light smoke over the bent picture so white text and icons on the glass stay readable.
                        looks?.first?.let { m -> nc.drawBitmap(m, null, dst, smoke) }
                    }
                    else looks?.first?.let { m -> nc.drawBitmap(m, null, dst, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply { colorFilter = android.graphics.PorterDuffColorFilter(0x2EFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN) }) }
                }
                drawContent()
                drawIntoCanvasCompat { nc -> looks?.second?.let { o -> nc.drawBitmap(o, null, dst, paint) } }
            }),
        content = content,
    )
}

private fun androidx.compose.ui.Modifier.androidx_onRect(f: (androidx.compose.ui.geometry.Rect) -> Unit) =
    this.then(androidx.compose.ui.Modifier.onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) { b ->
        val r = b.boundsInRoot; f(androidx.compose.ui.geometry.Rect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat()))
    })

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoCanvasCompat(block: (android.graphics.Canvas) -> Unit) =
    drawContext.canvas.nativeCanvas.let(block)
