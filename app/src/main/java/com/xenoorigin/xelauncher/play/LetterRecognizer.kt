package com.xenoorigin.xelauncher.play

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class Pt(val x: Float, val y: Float)

/**
 * Reads a hand-drawn capital letter with the \$P point-cloud recogniser (Vatavu, Anthony & Wobbrock, 2012):
 * the drawing and each template are resampled to the same number of points, normalised, and compared as
 * clouds, so stroke order and direction don't matter.
 */
class LetterRecognizer {
    private val templates: List<Pair<Char, List<Pt>>> =
        LetterShapes.all.flatMap { (ch, variants) -> variants.map { ch to normalize(it) } }

    /** Best guesses first, one entry per letter; empty when the drawing is too small to be a letter. */
    fun recognize(strokes: List<List<Pt>>): List<Char> {
        val pts = strokes.flatten()
        if (pts.size < 3) return emptyList()
        val w = pts.maxOf { it.x } - pts.minOf { it.x }; val h = pts.maxOf { it.y } - pts.minOf { it.y }
        if (maxOf(w, h) < 24f) return emptyList()
        val cloud = normalize(strokes.filter { it.isNotEmpty() })
        val best = HashMap<Char, Float>()
        for ((ch, t) in templates) {
            val d = greedyMatch(cloud, t)
            if (d < (best[ch] ?: Float.MAX_VALUE)) best[ch] = d
        }
        return best.entries.sortedBy { it.value }.map { it.key }
    }

    private fun greedyMatch(a: List<Pt>, b: List<Pt>): Float {
        val step = sqrt(N.toFloat()).toInt()
        var min = Float.MAX_VALUE
        var i = 0
        while (i < N) {
            min = minOf(min, cloudDistance(a, b, i), cloudDistance(b, a, i))
            i += step
        }
        return min
    }

    private fun cloudDistance(a: List<Pt>, b: List<Pt>, start: Int): Float {
        val matched = BooleanArray(N)
        var sum = 0f
        var i = start
        do {
            var best = Float.MAX_VALUE; var index = -1
            for (j in 0 until N) if (!matched[j]) {
                val d = hypot(a[i].x - b[j].x, a[i].y - b[j].y)
                if (d < best) { best = d; index = j }
            }
            matched[index] = true
            val weight = 1f - ((i - start + N) % N).toFloat() / N
            sum += weight * best
            i = (i + 1) % N
        } while (i != start)
        return sum
    }

    private fun normalize(strokes: List<List<Pt>>): List<Pt> {
        val pts = resample(strokes)
        val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }
        val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
        val w = (maxX - minX).coerceAtLeast(1e-4f); val h = (maxY - minY).coerceAtLeast(1e-4f)
        // Letters are stretched to a square, so a narrow N still looks like an N; very thin shapes (an I)
        // keep their proportions instead of being blown up into a box.
        val thin = minOf(w, h) / maxOf(w, h) < .3f
        val sx = if (thin) maxOf(w, h) else w; val sy = if (thin) maxOf(w, h) else h
        val scaled = pts.map { Pt((it.x - minX) / sx, (it.y - minY) / sy) }
        val cx = scaled.sumOf { it.x.toDouble() }.toFloat() / scaled.size
        val cy = scaled.sumOf { it.y.toDouble() }.toFloat() / scaled.size
        return scaled.map { Pt(it.x - cx, it.y - cy) }
    }

    /** N points spaced evenly along all strokes together (no points are placed in the gaps between strokes). */
    private fun resample(strokes: List<List<Pt>>): List<Pt> {
        val length = strokes.sumOf { s -> s.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y).toDouble() } }.toFloat()
        val single = strokes.flatten()
        if (length <= 0f) return List(N) { single.first() }
        val interval = length / (N - 1)
        val out = ArrayList<Pt>(N)
        var acc = 0f
        for (s in strokes) {
            if (s.isEmpty()) continue
            if (out.isEmpty()) out.add(s.first())
            var prev = s.first()
            var k = 1
            while (k < s.size) {
                val cur = s[k]
                val d = hypot(cur.x - prev.x, cur.y - prev.y)
                if (d > 0f && acc + d >= interval) {
                    val t = (interval - acc) / d
                    val q = Pt(prev.x + t * (cur.x - prev.x), prev.y + t * (cur.y - prev.y))
                    out.add(q); prev = q; acc = 0f
                } else { acc += d; prev = cur; k++ }
            }
        }
        while (out.size < N) out.add(single.last())
        return out.take(N)
    }

    private companion object { const val N = 32 }
}

/** The first Latin letter of an app's name, upper-case (null if it has none, e.g. a Thai name). */
fun firstLetterOf(label: String): Char? = label.firstOrNull { it in 'a'..'z' || it in 'A'..'Z' }?.uppercaseChar()

/** Capital letters as strokes in a unit box (y down). Some letters have more than one common way of writing. */
object LetterShapes {
    private fun l(vararg p: Float) = p.toList().chunked(2).map { Pt(it[0], it[1]) }
    private fun arc(cx: Float, cy: Float, rx: Float, ry: Float, from: Float, to: Float, steps: Int = 14) =
        (0..steps).map { k ->
            val a = Math.toRadians((from + (to - from) * k / steps).toDouble())
            Pt(cx + rx * cos(a).toFloat(), cy + ry * sin(a).toFloat())
        }

    private val O = arc(.5f, .5f, .45f, .5f, -90f, 270f, 24)

    val all: List<Pair<Char, List<List<List<Pt>>>>> = listOf(
        'A' to listOf(listOf(l(0f, 1f, .5f, 0f, 1f, 1f), l(.22f, .6f, .78f, .6f))),
        'B' to listOf(listOf(l(0f, 0f, 0f, 1f), l(0f, 0f, .5f, 0f) + arc(.5f, .25f, .3f, .25f, -90f, 90f) + l(.5f, .5f, 0f, .5f),
            l(0f, .5f, .55f, .5f) + arc(.55f, .75f, .35f, .25f, -90f, 90f) + l(.55f, 1f, 0f, 1f))),
        'C' to listOf(listOf(arc(.55f, .5f, .5f, .5f, -45f, -315f, 20))),
        'D' to listOf(listOf(l(0f, 0f, 0f, 1f), l(0f, 0f, .3f, 0f) + arc(.3f, .5f, .6f, .5f, -90f, 90f, 18) + l(.3f, 1f, 0f, 1f))),
        'E' to listOf(listOf(l(.9f, 0f, 0f, 0f, 0f, 1f, .9f, 1f), l(0f, .5f, .7f, .5f))),
        'F' to listOf(listOf(l(.9f, 0f, 0f, 0f, 0f, 1f), l(0f, .5f, .7f, .5f))),
        'G' to listOf(listOf(arc(.55f, .5f, .5f, .5f, -45f, -360f, 22) + l(1.05f, .5f, .6f, .5f))),
        'H' to listOf(listOf(l(0f, 0f, 0f, 1f), l(1f, 0f, 1f, 1f), l(0f, .5f, 1f, .5f))),
        'I' to listOf(listOf(l(.5f, 0f, .5f, 1f), l(.2f, 0f, .8f, 0f), l(.2f, 1f, .8f, 1f))),
        'J' to listOf(listOf(l(.75f, 0f, .75f, .7f) + arc(.45f, .7f, .3f, .3f, 0f, 180f))),
        'K' to listOf(listOf(l(0f, 0f, 0f, 1f), l(.9f, 0f, 0f, .55f, .9f, 1f)), listOf(l(0f, 0f, 0f, 1f), l(.9f, 0f, 0f, .55f), l(.3f, .4f, .9f, 1f))),
        'L' to listOf(listOf(l(0f, 0f, 0f, 1f, .75f, 1f))),
        'M' to listOf(listOf(l(0f, 1f, 0f, 0f, .5f, .65f, 1f, 0f, 1f, 1f))),
        'N' to listOf(listOf(l(0f, 1f, 0f, 0f, 1f, 1f, 1f, 0f))),
        'O' to listOf(listOf(O)),
        'P' to listOf(listOf(l(0f, 1f, 0f, 0f), l(0f, 0f, .45f, 0f) + arc(.45f, .27f, .4f, .27f, -90f, 90f) + l(.45f, .54f, 0f, .54f))),
        'Q' to listOf(listOf(O, l(.6f, .7f, 1f, 1.1f))),
        'R' to listOf(listOf(l(0f, 1f, 0f, 0f), l(0f, 0f, .45f, 0f) + arc(.45f, .27f, .4f, .27f, -90f, 90f) + l(.45f, .54f, 0f, .54f), l(.3f, .54f, .9f, 1f))),
        'S' to listOf(listOf(arc(.5f, .25f, .4f, .25f, -20f, -270f) + arc(.5f, .75f, .4f, .25f, -90f, 160f))),
        'T' to listOf(listOf(l(0f, 0f, 1f, 0f), l(.5f, 0f, .5f, 1f))),
        'U' to listOf(listOf(l(0f, 0f, 0f, .6f) + arc(.5f, .6f, .5f, .4f, 180f, 0f) + l(1f, .6f, 1f, 0f))),
        'V' to listOf(listOf(l(0f, 0f, .5f, 1f, 1f, 0f))),
        'W' to listOf(listOf(l(0f, 0f, .25f, 1f, .5f, .35f, .75f, 1f, 1f, 0f))),
        'X' to listOf(listOf(l(0f, 0f, 1f, 1f), l(1f, 0f, 0f, 1f))),
        'Y' to listOf(listOf(l(0f, 0f, .5f, .5f, 1f, 0f), l(.5f, .5f, .5f, 1f))),
        'Z' to listOf(listOf(l(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))),
    )
}
