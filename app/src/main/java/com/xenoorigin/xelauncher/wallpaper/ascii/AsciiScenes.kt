package com.xenoorigin.xelauncher.wallpaper.ascii

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Highlight colours of the still scenes. */
object AsciiScenes {
    const val DAWN_SUN = 0xFFFFC27A.toInt()
    const val DAWN_GLOW = 0xFFFF9E7A.toInt()
    const val FIREFLY = 0xFFC8FF8C.toInt()
    const val WINDOW = 0xFFFFD166.toInt()
    const val RING = 0xFFC27BFF.toInt()
    const val NEBULA = 0xFF7A5CFF.toInt()
    const val BEAM = 0xFFFFD166.toInt()
}

private fun AsciiField.set(c: Int, r: Int, l: Float) {
    if (c in 0 until cols && r in 0 until rows) cells.l[idx(c, r)] = l
}

/** Rain on the lake: heavy cloud, no moon, streaks over everything, rings on the water. */
internal fun AsciiField.paintRain(t: Float) {
    sky(0f)
    clouds(t, 1f, horizonRow)
    ridges(.7f)
    water(t, -1000f, 0f, false)
    shore()
    val step = (t * 6).toInt()
    for (r in horizonRow until groundRow) for (c in 0 until cols) {
        val i = idx(c, r)
        if ((rand[i] * 997f + step * .618f) % 1f < .025f) cells.l[i] = min(1f, cells.l[i] + .45f)
    }
    val drops = (size * .045f).toInt()
    for (j in 0 until drops) {
        val x0 = rand[j % size] * (cols + 30)
        val y0 = rand[(j * 7 + 3) % size] * rows
        val sp = rand[(j * 13 + 5) % size]
        val y = (y0 + t * (16 + 10 * sp) * sy) % rows
        for (len in 0..1) {
            val yy = y.toInt() - len
            if (yy !in 0 until rows) continue
            val xx = Math.floorMod((x0 - yy * .45f).toInt(), cols)
            val i = idx(xx, yy)
            cells.glyph[i] = AsciiGlyphs.RAIN
            cells.l[i] = max(cells.l[i], .5f)
        }
    }
}

/** Dawn: a low warm sun in the lowest gap of the ridge, a glowing band, mist, the sun's path on the lake. */
internal fun AsciiField.paintDawn(t: Float) {
    sky(0f, glowBand = 1f)
    val from = (cols * .4f).toInt(); val to = (cols * .8f).toInt()
    val gap = (from until to).maxBy { far[it] }
    val x = gap + .5f
    val rx = .1f * cols
    val y = max(far[gap] - rx * 5f / 8f * .4f, horizonRow * .6f)
    orb(x, y, rx, true, tintColor = AsciiScenes.DAWN_SUN)
    for (r in max(0, horizonRow - (6 * sy).toInt()) until horizonRow) for (c in 0 until cols) {
        val k = 1f - (horizonRow - r) / (6f * sy)
        tint(idx(c, r), AsciiScenes.DAWN_GLOW, .45f * k)
    }
    clouds(t, 0f)
    ridges(.9f)
    for (r in max(0, horizonRow - (3 * sy).toInt()) until horizonRow) for (c in 0 until cols) {
        val i = idx(c, r)
        cells.l[i] = min(1f, cells.l[i] + .16f * n1[i])
    }
    water(t, x, 1.2f, true)
    shore()
}

/** Pine forest at night: moonlit haze, tree silhouettes standing on the forest floor, fireflies. */
internal fun AsciiField.paintPines(t: Float) {
    sky(0f, glowBand = .5f)
    orb(.72f * cols, .18f * horizonRow, .1f * cols, false)
    clouds(t, 0f)
    stars(t, 1f)
    ridges(.6f)
    for (r in horizonRow until rows) for (c in 0 until cols) {
        val i = idx(c, r)
        cells.l[i] = .05f + .1f * gnd[i] + if (r == horizonRow) .12f else 0f
    }
    // Moonlit haze behind the trees so the dark silhouettes read.
    for (r in (horizonRow * .55f).toInt() until groundRow) for (c in 0 until cols) {
        val i = idx(c, r)
        if (r >= near[c] || r >= horizonRow) cells.l[i] = max(cells.l[i], .26f + .16f * n1[i] - .1f * (r - horizonRow).coerceAtLeast(0) / (rows - horizonRow).toFloat())
        else cells.l[i] = max(cells.l[i], .3f + .14f * n1[i])
    }
    val n = 8
    for (k in 0 until n) {
        val cx = ((k + .5f) / n * cols + (rand[k * 31 % size] - .5f) * cols / n * .6f)
        val base = groundRow + (rand[k * 17 % size] * 2).toInt()
        val h = (rows * (.15f + .2f * rand[k * 53 % size])).roundToInt().coerceAtLeast(4)
        val top = base - h
        val tier = max(2, h / 4)
        for (r in top..base) {
            val d = r - top
            val half = (d % tier + 1) * .55f * sx * 2f + d * .12f * sx * 2f
            for (c in (cx - half).toInt()..(cx + half).toInt()) {
                if (c !in 0 until cols || r !in 0 until rows) continue
                val edge = c >= (cx + half).toInt() - 0
                set(c, r, if (edge) .55f else .02f)
                amount[idx(c, r)] = 0f
            }
        }
        for (r in base..min(rows - 1, base + 1)) set(cx.toInt(), r, .02f)
        trees++
    }
    for (j in 0 until 11) {
        val fx = (rand[(j * 41 + 7) % size] * cols + sin(t * .7f + j) * 3f).roundToInt().coerceIn(0, cols - 1)
        val fy = (groundRow - 2 - rand[(j * 23 + 11) % size] * rows * .25f + sin(t * 1.1f + j * 2) * 1.5f).roundToInt().coerceIn(0, rows - 1)
        val i = idx(fx, fy)
        cells.glyph[i] = AsciiGlyphs.SPARK
        cells.l[i] = .7f
        tint(i, AsciiScenes.FIREFLY, .9f)
    }
}

/** City at night: a skyline with lit windows, glow over the town, the river reflecting it all. */
internal fun AsciiField.paintCity(t: Float) {
    sky(0f, glowBand = .6f)
    orb(.2f * cols, .15f * horizonRow, .09f * cols, false)
    clouds(t, 0f)
    stars(t, .6f, overRidges = true)
    var x = 0f
    var k = 0
    while (x < cols && k < 16) {
        val w = max(3f, (4 + rand[k * 29 % size] * 5) * sx * 1.4f)
        val top = (rows * (.45f + .25f * rand[k * 61 % size])).roundToInt()
        val c0 = x.toInt(); val c1 = min(cols - 1, (x + w).toInt() - 1)
        for (r in top until groundRow) for (c in c0..c1) {
            val i = idx(c, r)
            val edge = c == c0 || r == top
            var l = if (edge) .48f else .24f + .1f * n1[i]
            amount[i] = 0f
            val wx = (c - c0) % 2 == 1 && c < c1
            val wy = (r - top) % 3 == 2 && r < groundRow - 1
            if (wx && wy) {
                if (rand[i] < .3f) { l = .9f; tint(i, AsciiScenes.WINDOW, .9f) } else l = .1f
            }
            cells.l[i] = l
        }
        x += w + if (rand[k * 7 % size] < .3f) 1f else 0f
        k++
    }
    buildings = k
    // River: mirror of the city above the ground row.
    for (r in groundRow until rows) {
        val d = r - groundRow
        val ym = (groundRow - 1 - d * 1.3f).toInt().coerceIn(0, groundRow - 1)
        val xo = 1.4f * sin(r * .9f + t * 2.2f)
        for (c in 0 until cols) {
            val xm = (c + xo).roundToInt().coerceIn(0, cols - 1)
            val src = idx(xm, ym)
            val i = idx(c, r)
            val sh = sin(c / sx * .23f + t * 1.1f + r * 1.7f) * sin(c / sx * .07f - t * .6f + r)
            cells.l[i] = cells.l[src] * .55f + ((sh - .55f) * 3f).coerceIn(0f, 1f) * .35f
            if (highlight[src] == AsciiScenes.WINDOW) tint(i, AsciiScenes.WINDOW, .5f)
        }
    }
}

/** Space: dense stars, a soft nebula, a shaded planet with a tilted ring, a meteor crossing. */
internal fun AsciiField.paintSpace(t: Float) {
    for (i in 0 until size) cells.l[i] = .03f
    for (i in 0 until size) if (n1[i] > .58f) { cells.l[i] = (n1[i] - .58f) * 1.6f; tint(i, AsciiScenes.NEBULA, .35f) }
    stars(t, 1f, density = 5f, maxRow = rows, overRidges = true)
    val px = .62f * cols; val py = .42f * rows
    val rx = .3f * cols; val ry = rx * 5f / 8f
    val tilt = 20.0 * PI / 180.0
    val ct = cos(tilt).toFloat(); val st = sin(tilt).toFloat()
    val rPx = rx * 5f
    for (r in 0 until rows) for (c in 0 until cols) {
        val i = idx(c, r)
        val dxn = (c + .5f - px) / rx; val dyn = (r + .5f - py) / ry
        val d = hypot(dxn, dyn)
        val ux = (c + .5f - px) * 5f; val uy = (r + .5f - py) * 8f
        val u = ux * ct + uy * st; val v = -ux * st + uy * ct
        val q = hypot(u, v / .25f) / rPx
        val onRing = q in 1.25f..1.65f
        if (d < 1f) {
            if (onRing && v > 0) { cells.l[i] = .8f; tint(i, AsciiScenes.RING, .8f); continue }
            val z = sqrt(max(0f, 1f - d * d))
            val light = ((dxn * .7f - dyn * .45f + .45f) * z).coerceIn(0f, 1f)
            cells.l[i] = .14f + .78f * light + .08f * sin(dyn * 9f)
            amount[i] = 0f
            lastStars -= 0
        } else if (onRing) {
            cells.l[i] = .75f; tint(i, AsciiScenes.RING, .8f)
        }
    }
    val head = (.12f + .18f * ((t * .15f) % 1f)) * cols
    val hy = .12f * rows
    for (k in 0 until 9) {
        val c = (head - k * 1.2f * sx * 1.6f).roundToInt()
        val r = (hy + k * .6f * sy).roundToInt()
        if (c in 0 until cols && r in 0 until rows) {
            cells.l[idx(c, r)] = .95f - k * .08f
            meteorCells++
        }
    }
}

/** Sea at night with a lighthouse on a cliff, its beam sweeping across the water. */
internal fun AsciiField.paintLighthouse(t: Float) {
    sky(0f)
    clouds(t, .2f)
    stars(t, 1f, overRidges = true)
    water(t, -1000f, 0f, false, bottom = rows)
    for (r in horizonRow until rows) if ((r - horizonRow) % 3 == 1) for (c in 0 until cols) {
        val w = sin(c / sx * .3f + t * 1.6f + r) * .5f + .5f
        val i = idx(c, r)
        if (w > .7f) cells.l[i] = max(cells.l[i], .35f * w)
    }
    val c0 = .65f * cols
    fun cliffTop(c: Int): Int = (horizonRow + 2 - sqrt(((c - c0) / (.35f * cols)).coerceIn(0f, 1f)) * rows * .22f).roundToInt()
    for (c in c0.toInt() until cols) for (r in max(0, cliffTop(c)) until rows) {
        val i = idx(c, r)
        cells.l[i] = .2f + .2f * mt[i] + if (r == cliffTop(c)) .3f else 0f
        amount[i] = 0f
    }
    val tx = (.74f * cols).roundToInt()
    val tw = max(3, (.05f * cols).roundToInt())
    val base = cliffTop(tx)
    val th = max(5, (.2f * rows).roundToInt())
    for (r in base - th until base) for (c in tx - tw / 2..tx + tw / 2) {
        if (c !in 0 until cols || r !in 0 until rows) continue
        cells.l[idx(c, r)] = if (((r - (base - th)) / 2) % 2 == 0) .85f else .45f
        amount[idx(c, r)] = 0f
    }
    val ly = base - th - 1
    for (c in tx - tw / 2..tx + tw / 2) if (c in 0 until cols && ly in 0 until rows) {
        cells.l[idx(c, ly)] = .95f; tint(idx(c, ly), AsciiScenes.BEAM, .9f)
    }
    val lampX = (tx + .5f) * 5f; val lampY = (ly + .5f) * 8f
    val ang = PI.toFloat() + .35f * sin(t * .6f)
    val len = .7f * cols * 5f
    for (r in 0 until rows) for (c in 0 until cols) {
        val dx = (c + .5f) * 5f - lampX; val dy = (r + .5f) * 8f - lampY
        val d = hypot(dx, dy)
        if (d < 8f || d > len) continue
        var da = atan2(dy, dx) - ang
        while (da > PI) da -= (2 * PI).toFloat()
        while (da < -PI) da += (2 * PI).toFloat()
        if (abs(da) < .11f) {
            val i = idx(c, r)
            cells.l[i] = max(cells.l[i], .6f * (1f - d / len) + .25f)
            tint(i, AsciiScenes.BEAM, .6f * exp(-d / len))
        }
    }
}
