package com.xenoorigin.xelauncher.wallpaper.ascii

import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.wallpaper.SkyLook
import com.xenoorigin.xelauncher.wallpaper.skyAt
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class AsciiScene { LAKE, RAIN, DAWN, PINES, CITY, SPACE, LIGHTHOUSE }

/**
 * The picture behind an ASCII wallpaper, one value per glyph cell: brightness, colour and glyph overrides.
 * Everything is placed in fractions of the grid, so the same scene works on a tall phone screen and on a
 * small wide widget. Noise is made once; [fill] allocates nothing.
 */
class AsciiField(val cols: Int, val rows: Int, seed: Int = 11) {
    val cells = CellBuffer(cols, rows, seed)
    val size = cols * rows
    /** Too small for a scene (under 6 rows or 4 columns): [fill] leaves it dark instead of failing. */
    val tooSmall = rows < MIN_ROWS || cols < MIN_COLS
    val horizonRow = if (tooSmall) (rows / 2).coerceAtLeast(0) else (rows * .58f).roundToInt().coerceIn(2, rows - 2)
    /** The row a character can stand on (top of the shore). */
    val groundRow = if (tooSmall) (rows - 1).coerceAtLeast(0) else (rows * .86f).roundToInt().coerceIn(horizonRow + 1, rows - 1)

    /** Highlight colour per cell (0 = none) and how strongly it colours the glyph. */
    internal val highlight = IntArray(size)
    internal val amount = FloatArray(size)
    /** Stars shown in the last frame (for tests). */
    internal var lastStars = 0
    /** Centre cell of the sun or moon in the last frame (for tests). */
    internal var discCell = 0 to 0
    /** Counts from the last frame, for tests. */
    internal var trees = 0
    internal var buildings = 0
    internal var meteorCells = 0

    internal var look: SkyLook = skyAt(0)
    private var live = false

    // Scale factors keep noise features the same size on any grid (the concept was 128 × 45).
    internal val sx = cols / 128f
    internal val sy = rows / 45f

    private val rnd = java.util.Random(seed.toLong())
    internal val far = IntArray(cols)
    internal val near = IntArray(cols)
    internal val mt = vnoise(rows, cols, 7f * sy, 1.2f * sx)
    internal val mt2 = vnoise(rows, cols, 4f * sy, 2f * sx)
    private val cloudW = cols * 7
    private val cloud = fbm(rows, cloudW, 2.2f * sy, 22f * sx)
    private val crater = fbm(rows, cols, 1.5f * sy, 2.5f * sx)
    internal val star = BooleanArray(size) { rnd.nextFloat() < .018f }
    private val sph = FloatArray(size) { rnd.nextFloat() * 6.2832f }
    private val ssp = FloatArray(size) { 1.5f + rnd.nextFloat() * 3f }
    internal val gnd = fbm(rows, cols, 1.5f * sy, 4f * sx)
    /** General-purpose noise for scenes (0..1). */
    internal val n1 = fbm(rows, cols, 3f * sy, 3f * sx)
    internal val rand = FloatArray(size) { rnd.nextFloat() }

    init {
        val r1 = fbm(1, cols, 1f, .3f * cols).normalized()
        val r2 = fbm(1, cols, 1f, .16f * cols).normalized()
        for (c in 0 until cols) {
            far[c] = (horizonRow - horizonRow * (.08f + .32f * r1[c].pow(1.3f))).roundToInt()
            near[c] = (horizonRow - horizonRow * (.03f + .12f * r2[c])).roundToInt()
        }
    }

    /**
     * Draws [scene] at time [t] (seconds, drives motion). [look] = the sky right now (live wallpaper);
     * null = the scene's own fixed time. [color] picks the colour mode; [accent] is used by ACCENT.
     */
    fun fill(scene: AsciiScene, t: Float, look: SkyLook? = null, color: AsciiColor = AsciiColor.MONO, accent: Long = 0xFFFFFFFF) {
        if (tooSmall) { cells.l.fill(0f); highlight.fill(0); amount.fill(0f); return }
        cells.clear(); highlight.fill(0); amount.fill(0f); lastStars = 0; trees = 0; buildings = 0; meteorCells = 0
        live = look != null
        this.look = look ?: defaultLook(scene)
        when (scene) {
            AsciiScene.LAKE -> lake(t)
            AsciiScene.RAIN -> paintRain(t)
            AsciiScene.DAWN -> paintDawn(t)
            AsciiScene.PINES -> paintPines(t)
            AsciiScene.CITY -> paintCity(t)
            AsciiScene.SPACE -> paintSpace(t)
            AsciiScene.LIGHTHOUSE -> paintLighthouse(t)
        }
        finishColors(color, accent)
    }

    private fun lake(t: Float) {
        val lk = look
        val rx = .11f * cols
        val sun = live && lk.sunUp
        val ox: Float
        val oy: Float
        if (live) {
            ox = lk.orbX * cols
            oy = min(lk.orbY / .62f * horizonRow * .8f, horizonRow - rx * .4f)
        } else { ox = .24f * cols; oy = .2f * horizonRow }
        sky(if (live && lk.sunUp) dayBright() else 0f)
        orb(ox, oy, rx, sun)
        clouds(t, 0f)
        stars(t, lk.stars)
        ridges()
        water(t, ox, if (sun) 1.1f else 1f, sun)
        shore()
    }

    private fun dayBright(): Float {
        val m = look.mid
        val lum = (.2126f * ((m shr 16) and 0xFF) + .7152f * ((m shr 8) and 0xFF) + .0722f * (m and 0xFF)) / 255f
        return .3f * lum
    }

    // ------------------------------------------------------------------ shared parts

    internal fun idx(c: Int, r: Int) = r * cols + c

    internal fun sky(extra: Float, glowBand: Float = 0f) {
        for (r in 0 until horizonRow) {
            val base = .035f + .05f * r / horizonRow + extra + glowBand * .5f * exp(-(horizonRow - r) / (5f * sy))
            val v = if (extra > 0f) min(base, .35f) else base
            for (c in 0 until cols) cells.l[idx(c, r)] = v
        }
    }

    internal fun orb(x: Float, y: Float, rx: Float, sun: Boolean, strength: Float = 1f, tintColor: Int = if (sun) SUN_TINT else MOON_TINT) {
        val ry = rx * 5f / 8f
        discCell = x.toInt().coerceIn(1, cols - 2) to y.toInt().coerceIn(1, rows - 2)
        for (r in 0 until horizonRow) for (c in 0 until cols) {
            val d = hypot((c + .5f - x) / rx, (r + .5f - y) / ry)
            val i = idx(c, r)
            if (d < 1f) {
                cells.l[i] = max(cells.l[i], (if (sun) .92f else .8f - .22f * crater[i]) * strength)
                tint(i, tintColor, if (sun) .6f else .15f)
            } else {
                val halo = (if (sun) .34f else .24f) * exp(-(d - 1f) * rx * 5f / 16f) * strength
                if (halo > .02f) {
                    cells.l[i] = max(cells.l[i], cells.l[i] + halo)
                    if (halo > .08f) tint(i, tintColor, if (sun) .5f else .25f)
                }
            }
        }
    }

    internal fun clouds(t: Float, cover: Float, maxRow: Int = (horizonRow * .78f).toInt()) {
        val off = ((t * 1.4f * sx).toInt() % (cloudW - cols)).coerceAtLeast(0)
        val thr = .47f - .15f * cover
        for (r in 0 until min(maxRow, horizonRow)) for (c in 0 until cols) {
            val cl = ((cloud[r * cloudW + c + off] - thr) * 2.8f).coerceIn(0f, 1f) * (.85f + .5f * cover)
            if (cl <= 0f) continue
            val i = idx(c, r)
            cells.l[i] = max(cells.l[i] * (1f - .75f * min(cl, 1f)), cl * .5f)
            if (cl > .3f) amount[i] *= .3f
        }
    }

    internal fun stars(t: Float, amountOf: Float, density: Float = 1f, maxRow: Int = horizonRow, overRidges: Boolean = false) {
        if (amountOf <= 0f) return
        for (r in 0 until maxRow) for (c in 0 until cols) {
            val i = idx(c, r)
            if (!star[i] && !(density > 1f && rand[i] < .018f * (density - 1f))) continue
            if ((!overRidges && r >= far[c]) || cells.l[i] > .2f) continue
            val v = amountOf * (.28f + .25f * sin(t * ssp[i] + sph[i]))
            if (v > .09f && v > cells.l[i]) { cells.l[i] = v; lastStars++ }
        }
    }

    internal fun ridges(dark: Float = 1f) {
        for (c in 0 until cols) {
            val f = far[c]
            for (r in max(0, f) until horizonRow) {
                val k = (r - f).toFloat()
                cells.l[idx(c, r)] = ((.24f + .42f * mt[idx(c, r)]) * (.55f + .45f * exp(-k / (6f * sy))) + .25f * exp(-k / (1.2f * sy))) * dark
                amount[idx(c, r)] = 0f
            }
            for (r in max(0, near[c]) until horizonRow) {
                cells.l[idx(c, r)] = .06f + .1f * mt2[idx(c, r)]
                amount[idx(c, r)] = 0f
            }
        }
    }

    /** Water from the horizon down to [bottom]: wobbling mirror of the land and sky above, shimmer, light path. */
    internal fun water(t: Float, discX: Float, discStrength: Float, sun: Boolean, bottom: Int = groundRow) {
        for (r in horizonRow until bottom) {
            val k = r - horizonRow
            val ym = (horizonRow - 1 - k * 1.25f).toInt().coerceIn(0, horizonRow - 1)
            val xo = 1.6f * sin(r * .9f / sy + t * 2.2f) + .8f * sin(r * 2.3f / sy - t * 3.1f)
            val width = max((1.5f + k * .45f / sy) * sx * 1.8f, .5f)
            for (c in 0 until cols) {
                val xm = (c + xo).roundToInt().coerceIn(0, cols - 1)
                val cc = c / sx
                val rr = r / sy
                val sh = sin(cc * .23f + t * 1.1f + sin(r * 1.3f + t * .5f) * 2f) * sin(cc * .071f - t * .6f + r * .8f)
                val shim = ((sh - .5f) * 3f).coerceIn(0f, 1f) * .42f
                val dx = (c - discX - xo) / width
                val mc = exp(-dx * dx) * (.5f + .5f * sin(rr * 2.1f + t * 3f + cc * .4f)) * .65f * discStrength
                val i = idx(c, r)
                cells.l[i] = cells.l[idx(xm, ym)] * .5f + shim + mc
                if (sun && mc > .2f) tint(i, SUN_TINT, .4f)
            }
        }
    }

    internal fun shore(from: Int = groundRow) {
        for (r in from until rows) for (c in 0 until cols) {
            val i = idx(c, r)
            cells.l[i] = .05f + .12f * gnd[i] + if (r == from && gnd[i] > .35f) .25f else 0f
            amount[i] = 0f
        }
        if (from - 1 >= 0) for (c in 0 until cols) if (gnd[idx(c, from)] > .62f) cells.l[idx(c, from - 1)] = .22f
    }

    internal fun tint(i: Int, argb: Int, strength: Float) {
        if (strength >= amount[i]) { highlight[i] = argb; amount[i] = strength }
    }

    // ------------------------------------------------------------------ colour

    private fun finishColors(mode: AsciiColor, accent: Long) {
        val acc = lift(accent.toInt(), 160)
        for (r in 0 until rows) {
            val rowColor = when (mode) {
                AsciiColor.SKY -> lift(skyRowColor(r), 150)
                AsciiColor.ACCENT -> acc
                AsciiColor.MONO -> 0
            }
            for (c in 0 until cols) {
                val i = idx(c, r)
                val l = cells.l[i].coerceIn(0f, 1f)
                cells.l[i] = l
                if (l < .09f && cells.glyph[i] < 0) continue
                var col = if (mode == AsciiColor.MONO) grey(l) else scale(rowColor, .45f + .75f * l)
                if (amount[i] > 0f) col = mix(col, brighten(highlight[i], .55f + .6f * l), amount[i])
                cells.color[i] = col
            }
        }
    }

    private fun skyRowColor(r: Int): Int {
        val lk = look
        return when {
            r < horizonRow -> mix(lk.top.toInt(), lk.bottom.toInt(), r.toFloat() / horizonRow)
            r < groundRow -> scale(mix(lk.top.toInt(), lk.bottom.toInt(), ((horizonRow - 1 - (r - horizonRow) * 1.25f) / horizonRow).coerceIn(0f, 1f)), .8f)
            else -> scale(lk.bottom.toInt(), .5f)
        }
    }

    companion object {
        const val MIN_ROWS = 6
        const val MIN_COLS = 4
        const val SUN_TINT = 0xFFFFE0A0.toInt()
        const val MOON_TINT = 0xFFD6E2FF.toInt()

        fun grey(l: Float): Int { val g = (60 + 190 * l).toInt().coerceIn(0, 255); return argb(g, g, g) }
        fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        fun scale(c: Int, f: Float) = argb(ch(c, 16, f), ch(c, 8, f), ch(c, 0, f))
        private fun ch(c: Int, s: Int, f: Float) = (((c shr s) and 0xFF) * f).toInt().coerceIn(0, 255)
        fun mix(a: Int, b: Int, t: Float): Int {
            fun m(s: Int) = (((a shr s) and 0xFF) + ((((b shr s) and 0xFF) - ((a shr s) and 0xFF)) * t)).toInt().coerceIn(0, 255)
            return argb(m(16), m(8), m(0))
        }
        /** Raises a colour so its brightest channel is at least [min] (keeps very dark accents visible). */
        fun lift(c: Int, min: Int): Int {
            val mx = max(max((c shr 16) and 0xFF, (c shr 8) and 0xFF), c and 0xFF)
            if (mx >= min) return c or (0xFF shl 24)
            if (mx == 0) return argb(min, min, min)
            return scale(c, min.toFloat() / mx)
        }
        private fun brighten(c: Int, f: Float) = scale(c, f.coerceAtMost(1.15f))

        fun defaultLook(scene: AsciiScene): SkyLook = when (scene) {
            AsciiScene.DAWN -> skyAt(6 * 60 + 5)
            else -> skyAt(0)
        }

        internal fun vnoise(h: Int, w: Int, cy: Float, cx: Float, rnd: java.util.Random = java.util.Random(7)): FloatArray {
            val cyy = max(cy, 1f); val cxx = max(cx, 1f)
            val gh = (h / cyy).toInt() + 3; val gw = (w / cxx).toInt() + 3
            val g = FloatArray(gh * gw) { rnd.nextFloat() }
            val out = FloatArray(h * w)
            for (y in 0 until h) {
                val fy0 = y / cyy; val y0 = fy0.toInt(); var fy = fy0 - y0; fy = fy * fy * (3 - 2 * fy)
                for (x in 0 until w) {
                    val fx0 = x / cxx; val x0 = fx0.toInt(); var fx = fx0 - x0; fx = fx * fx * (3 - 2 * fx)
                    val a = g[y0 * gw + x0]; val b = g[y0 * gw + x0 + 1]
                    val c = g[(y0 + 1) * gw + x0]; val d = g[(y0 + 1) * gw + x0 + 1]
                    val top = a + (b - a) * fx; val bot = c + (d - c) * fx
                    out[y * w + x] = top + (bot - top) * fy
                }
            }
            return out
        }

        private fun FloatArray.normalized(): FloatArray {
            val lo = minOrNull() ?: 0f; val hi = maxOrNull() ?: 1f
            val span = (hi - lo).takeIf { abs(it) > 1e-6f } ?: 1f
            return FloatArray(size) { (this[it] - lo) / span }
        }
    }

    private fun vnoise(h: Int, w: Int, cy: Float, cx: Float) = Companion.vnoise(h, w, cy, cx, rnd)

    private fun fbm(h: Int, w: Int, cy: Float, cx: Float, oct: Int = 4): FloatArray {
        val out = FloatArray(h * w)
        var amp = 1f; var tot = 0f
        for (o in 0 until oct) {
            val n = vnoise(h, w, max(1f, cy / (1 shl o)), max(1f, cx / (1 shl o)))
            for (i in out.indices) out[i] += amp * n[i]
            tot += amp; amp *= .5f
        }
        for (i in out.indices) out[i] /= tot
        return out
    }
}
