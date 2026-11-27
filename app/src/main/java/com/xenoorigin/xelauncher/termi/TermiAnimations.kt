package com.xenoorigin.xelauncher.termi

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class TermiAnim(val seconds: Float) {
    IDLE(8f), HELLO(4.2f), CODING(5f), PROMPT_AI(5f), THINK(4.6f), COFFEE(5f), MUSIC(4.6f), BUILD_FAIL(6f),
    BUG_SQUASH(5.4f), BUILD_PASS(5f), LOVE(4.6f), HUNGRY(5.6f), HUNGRY_WAIT(3f), SLEEP(6f), NAP(4f),
}

/** What the scene behind Termi should do in this frame. */
data class SceneHint(val rain: Float = 0f, val dawn: Float = 0f, val dim: Float = 0f)

data class Frame(val pose: TermiPose, val props: List<Px>, val scene: SceneHint = SceneHint())

/**
 * The approved animations, ported from the concept reel. Positions are written in the concept's pixels
 * (U = 6 px, Termi's feet at (320, 304)) and converted to sprite sub-units.
 */
object TermiAnimations {
    private const val U = 6f
    private const val CX = 320f
    private const val GROUND = 304f
    private const val BX = CX - 25 * U
    private const val BY = GROUND - 36 * U

    private const val WH = 0xFFE8E8EE.toInt(); private const val BR = 0xFFFFFFFF.toInt()
    private const val PINK = 0xFFFF8CB4.toInt(); private const val RED = 0xFFFF6078.toInt()
    private const val GOLD = 0xFFFFD166.toInt(); private const val CYAN = 0xFF7FE7FF.toInt()
    private const val VIOL = 0xFFC27BFF.toInt(); private const val GREY = 0xFF8A8A96.toInt()
    private const val GREEN = 0xFF6EE696.toInt(); private const val PANEL = 0xFF2A2A32.toInt()

    private val HEART = listOf(".X.X.", "XXXXX", "XXXXX", ".XXX.", "..X..")
    private val DROP = listOf(".X.", "XXX", "XXX", ".X.")
    private val BANG = listOf("X", "X", "X", ".", "X")
    private val QMARK = listOf("XXX", "..X", ".XX", "...", ".X.")
    private val SPARK1 = listOf(".X.", "XXX", ".X.")
    private val SPARK2 = listOf("X.X", ".X.", "X.X")
    private val ANGER = listOf("XX.XX", "X...X", ".....", "X...X", "XX.XX")
    private val NOTE = listOf("..XX", "..XX", "..X.", "..X.", "XXX.", "XXX.")
    private val ZED = listOf("XXXX", "..X.", ".X..", "XXXX")
    private val BULB = listOf(".XXX.", "XXXXX", "XXXXX", ".XXX.", ".YYY.", "..Y..")
    private val CLOUD = listOf("...XXX...", ".XXXXXXX.", "XXXXXXXXX", ".XXXXXXX.")
    private val HAND = listOf("YXXXXXY", "XXXXXXX", "XXXXXXX", ".X.X.X.", ".X.X.X.")
    private val CHECK = listOf("....X", "...X.", "X.X..", ".X...")
    private val BOWL = listOf("FFFFFFF", "WWWWWWW", ".WWWWW.", "..WWW..")
    private val MUG = listOf("XXXX.", "XXXXY", "XXXXY", "XXXX.", ".XX..")

    private fun ease(t: Float): Float { val c = t.coerceIn(0f, 1f); return .5f - .5f * cos(PI.toFloat() * c) }
    private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
    private fun sinf(x: Float) = sin(x)
    private const val TAU = (2 * PI).toFloat()

    /** Geometry of the Termi drawn in this frame, in concept pixels. */
    private class G(val top: Float, val cx: Float, val left: Float, val right: Float, val t: Int)

    private class B {
        var pose = TermiPose()
        val props = ArrayList<Px>()
        var scene = SceneHint()

        fun box(x: Float, y: Float, w: Float, h: Float, argb: Int, a: Int = 255) {
            if (w < 1f || h < 1f || a <= 0) return
            val col = (argb and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
            val x0 = ((x - BX) / 2f).roundToInt(); val y0 = ((y - BY) / 2f).roundToInt()
            val x1 = ((x + w - BX) / 2f).roundToInt(); val y1 = ((y + h - BY) / 2f).roundToInt()
            val cx0 = x0.coerceIn(0, TermiSprite.BOX_W); val cy0 = y0.coerceIn(0, TermiSprite.BOX_H)
            val cx1 = max(x1, x0 + 1).coerceIn(0, TermiSprite.BOX_W); val cy1 = max(y1, y0 + 1).coerceIn(0, TermiSprite.BOX_H)
            if (cx1 > cx0 && cy1 > cy0) props += Px(cx0, cy0, cx1 - cx0, cy1 - cy0, col)
        }

        fun pat(rows: List<String>, x: Float, y: Float, px: Float, cmap: Map<Char, Int>, a: Int = 255) {
            rows.forEachIndexed { r, line -> line.forEachIndexed { c, ch -> cmap[ch]?.let { box(x + c * px, y + r * px, px, px, it, a) } } }
        }

        fun termi(
            dx: Float = 0f, dy: Float = 0f, squash: Int = 0, face: String = "neutral", mouth: String? = null,
            arms: Pair<String, String> = "down" to "down", armDy: Pair<Float, Float> = 0f to 0f,
            legs: Pair<String, String> = "stand" to "stand", screen: Int = TermiSprite.SCREEN, lean: Float = 0f,
            sit: Boolean = false, handFree: Boolean = true,
        ): G {
            pose = TermiPose(face, mouth, arms, armDy.first / U to armDy.second / U, legs, squash, lean / U, dx / U, dy / U, screen, sit, handFree)
            val t = 3 + (if (squash == -1) 1 else 0) - (if (squash == 1) 1 else 0)
            val bh = t + 9
            val lh = if (sit) 0 else 2
            val ox = CX - 10 * U + (dx / 2f).roundToInt() * 2f
            val oy = GROUND + (dy / 2f).roundToInt() * 2f - (lh + bh) * U
            val ln = (lean / 2f).roundToInt() * 2f
            return G(oy, ox + 10 * U + ln, ox + 3 * U, ox + 17 * U, t)
        }

        fun build() = Frame(pose, props.toList(), scene)
    }

    fun frame(a: TermiAnim, t: Float, i: Int): Frame = B().apply {
        when (a) {
            TermiAnim.IDLE -> idle(t, i)
            TermiAnim.HELLO -> hello(t, i)
            TermiAnim.CODING -> coding(t, i)
            TermiAnim.PROMPT_AI -> prompt(t, i)
            TermiAnim.THINK -> think(t, i)
            TermiAnim.COFFEE -> coffee(t, i)
            TermiAnim.MUSIC -> music(t, i)
            TermiAnim.BUILD_FAIL -> fail(t, i)
            TermiAnim.BUG_SQUASH -> bug(t, i)
            TermiAnim.BUILD_PASS -> pass(t, i)
            TermiAnim.LOVE -> love(t, i)
            TermiAnim.HUNGRY -> hungry(t, i)
            TermiAnim.HUNGRY_WAIT -> hungryWait(t, i)
            TermiAnim.SLEEP -> sleep(t, i)
            TermiAnim.NAP -> nap(t, i)
        }
    }.build()

    /**
     * The 8-second idle loop, never still (owner, 27 Nov): a stroll to the right with bouncing steps and swinging
     * arms, a look around with hands on hips, a stroll left, a happy hop, then back to the middle, swaying and
     * blinking all the while.
     */
    private fun B.idle(t: Float, i: Int) {
        val sway = sinf(t * TAU * 4) * 2
        val blink = (i % 37) in 0..1
        // Walking between two x positions (concept px) during [a, b]: bounce per step, legs and arms alternate.
        fun walk(a: Float, b: Float, from: Float, to: Float, face: String) {
            val k = seg(t, a, b)
            val x = from + (to - from) * (k * k * (3 - 2 * k))
            val steps = 4f
            val ph = k * steps
            val step = ph.toInt() % 2
            val bob = -kotlin.math.abs(sinf(ph * PI.toFloat())) * 6
            termi(
                dx = x, dy = bob, face = if (blink) "blink" else face, lean = if (to > from) 3f else -3f,
                legs = if (step == 0) "lift" to "stand" else "stand" to "lift",
                armDy = (if (step == 0) -4f else 2f) to (if (step == 0) 2f else -4f),
            )
        }
        when {
            t < .10f -> termi(face = if (blink) "blink" else "neutral", lean = sway, squash = if (sinf(t * TAU * 6) > .7f) 1 else 0, armDy = -sway to sway)
            t < .30f -> walk(.10f, .30f, 0f, 24f, "right")
            t < .42f -> {
                val k = seg(t, .30f, .42f)
                termi(dx = 24f, face = if (blink) "blink" else if (k < .5f) "left" else "up", arms = "hip" to "hip", lean = sway)
            }
            t < .64f -> walk(.42f, .64f, 24f, -22f, "left")
            t < .76f -> {
                val k = seg(t, .64f, .76f)
                termi(dx = -22f, dy = -sinf(k * PI.toFloat()) * 16, face = "happy", squash = if (k < .15f || k > .9f) 1 else if (k < .5f) -1 else 0,
                    arms = "up" to "up", legs = if (k in .15f..0.85f) "tuck" to "tuck" else "stand" to "stand")
            }
            t < .92f -> walk(.76f, .92f, -22f, 0f, "content")
            else -> termi(face = if (blink) "blink" else "content", lean = sway, armDy = sway to -sway, squash = if (sinf(t * TAU * 8) > .8f) 1 else 0)
        }
    }

    private fun B.hello(t: Float, i: Int) {
        when {
            t < .12f -> termi()
            t < .25f -> {
                val k = seg(t, .12f, .25f)
                val g = termi(face = "surprised", dy = -sinf(k * PI.toFloat()) * 14, squash = if (k < .6f) -1 else 0,
                    arms = "up" to "up", legs = if (k in .1f..0.9f) "tuck" to "tuck" else "stand" to "stand")
                pat(BANG, g.right + 10, g.top - 20, 4f, mapOf('X' to GOLD))
            }
            t < .72f -> {
                val seq = listOf("raise", "up", "raise", "up", "raise", "up", "raise")
                val k = seg(t, .25f, .72f)
                val arm = seq[min(seq.size - 1, (k * seq.size).toInt())]
                val g = termi(face = "joy", arms = "down" to arm, lean = -sinf(k * TAU * 3) * 4, squash = if ((k * 14).toInt() % 4 == 0) 1 else 0)
                listOf(-26f to -10f, (g.right - g.left + 18) to -24f, (g.right - g.left + 30) to 8f).forEachIndexed { j, (ox, oy) ->
                    pat(if ((i + j) % 4 < 2) SPARK1 else SPARK2, g.left + ox, g.top + oy, 3f, mapOf('X' to GOLD))
                }
            }
            else -> {
                val k = seg(t, .72f, 1f)
                val g = termi(face = "shy", arms = if (k > .3f) "cover" to "cover" else "down" to "down",
                    lean = sinf(t * TAU * 4) * 3, squash = if (k < .15f) 1 else 0)
                if (k > .3f) for (j in 0..1) {
                    val ph = (k * 2 + j * .5f) % 1f
                    pat(HEART, g.right + 6 + j * 14, g.top - ph * 30, 2f, mapOf('X' to PINK), (255 * (1 - ph)).toInt())
                }
            }
        }
    }

    private fun B.coding(t: Float, i: Int) {
        val eureka = t > .72f
        if (!eureka) {
            val g = termi(face = "focused", arms = "front" to "front", armDy = (if (i % 2 == 1) -4f else 0f) to (if (i % 2 == 1) 0f else -4f),
                squash = if ((i / 3) % 5 == 0) 1 else 0, handFree = false)
            if (t > .4f && (i / 4) % 2 == 1) pat(DROP, g.right + 2, g.top + 6 + (i % 4) * 2, 2f, mapOf('X' to CYAN))
        } else {
            val k = seg(t, .72f, .85f)
            val g = termi(face = "star", arms = "up" to "up", dy = -sinf(k * PI.toFloat()) * 12,
                squash = if (k in 0.001f..0.6f) -1 else if (k >= 1f && (i / 3) % 2 == 1) 1 else 0, handFree = false)
            for (j in 0 until 6) {
                val ang = j / 6f * TAU + t * 4
                val r = 46 + 6 * sinf(i * .7f + j)
                pat(if ((i + j) % 2 == 1) SPARK1 else SPARK2, g.cx + cos(ang) * r, g.top + 30 + sin(ang) * r * .6f, 3f, mapOf('X' to GOLD))
            }
        }
        val kx = CX - 54; val ky = GROUND - 10
        box(kx, ky, 108f, 10f, 0xFF2A2A32.toInt())
        for (k in 0 until 12) box(kx + 3 + k * 9, ky + 3, 6f, 4f, if ((k * 5 + i) % 11 == 0 && !eureka) CYAN else 0xFF50505C.toInt())
        val wx = CX + 72; val wy = GROUND - 150
        box(wx, wy, 70f, 60f, 0xFF22222A.toInt(), 235)
        box(wx, wy, 70f, 8f, 0xFF34343C.toInt())
        listOf(PINK, GOLD, GREEN).forEachIndexed { c, col -> box(wx + 4 + c * 6, wy + 2, 4f, 4f, col) }
        val n = min(7, (seg(t, 0f, .7f) * 8).toInt())
        val widths = listOf(40f, 26f, 48f, 20f, 36f, 44f, 30f)
        val cols = listOf(CYAN, VIOL, GOLD, CYAN, PINK, VIOL, GOLD)
        for (ln in 0 until n) box(wx + 6 + if (ln in listOf(1, 2, 4)) 8 else 0, wy + 12 + ln * 6, widths[ln], 3f, cols[ln])
        if (eureka) pat(CHECK, wx + 48, wy + 38, 3f, mapOf('X' to GREEN))
    }

    private fun B.prompt(t: Float, i: Int) {
        val face: String
        var armDy = 0f to 0f
        when {
            t < .3f -> { face = "focused"; armDy = 0f to if (i % 4 < 2) -3f else 0f }
            t < .62f -> face = if ((i / 6) % 2 == 1) "upR" else "up"
            else -> face = if (t < .82f) "star" else "joy"
        }
        val leg = "stand" to if (t in .3f..0.62f && (i / 3) % 2 == 1) "lift" else "stand"
        val bounce = -sinf(seg(t, .62f, .75f) * PI.toFloat()) * 10
        val g = termi(face = face, arms = "down" to "hold", armDy = armDy, legs = leg, dy = bounce,
            squash = if (t in .62f..0.7f) -1 else 0, handFree = false)
        val px = g.right + 14; val py = g.top + 5 * U
        box(px, py, 12f, 20f, 0xFF28282F.toInt())
        box(px + 2, py + 2, 8f, 14f, if (i % 6 < 3) 0xFF4678FF.toInt() else 0xFF3C64E6.toInt())
        val bx = CX + 70; val by = GROUND - 170
        if (t > .25f) {
            val k = seg(t, .25f, .33f)
            val s = ease(k) + .18f * sinf(k * PI.toFloat())
            box(bx, by, 70 * s, 22 * s, PANEL)
            if (s > .9f) { box(bx + 6, by + 8, 6f, 6f, CYAN); box(bx + 16, by + 10, if ((i / 3) % 2 == 1) 40f else 32f, 3f, BR) }
        }
        if (t in .35f..0.62f) for (k in 0 until ((t - .35f) * 40).toInt() % 4) box(bx + 4 + k * 10, by + 32, 5f, 5f, GREY)
        if (t > .6f) {
            val r = ease(seg(t, .6f, .68f))
            box(bx - 10, by + 30, 90 * r, 34 * r, 0xFF3C3C46.toInt())
            if (r > .95f) {
                listOf(70f, 50f, 62f).forEachIndexed { ln, w -> box(bx - 4, by + 36 + ln * 9, w, 4f, listOf(GOLD, CYAN, VIOL)[ln]) }
                listOf(-18f to 24f, 84f to 22f, 88f to 60f).forEachIndexed { j, (ox, oy) ->
                    if ((i + j * 2) % 6 < 3) pat(SPARK1, bx + ox, by + oy, 3f, mapOf('X' to GOLD))
                }
            }
        }
    }

    private fun B.think(t: Float, i: Int) {
        if (t < .6f) {
            val face = if (t > .25f) "confused" else if ((i / 8) % 2 == 1) "upL" else "upR"
            val g = termi(face = face, arms = if (t < .35f) "down" to "hold" else "down" to "head",
                armDy = 0f to if (i % 4 < 2 && t > .35f) -2f else 0f, lean = sinf(t * TAU * 2) * 4)
            if (t > .25f) pat(QMARK, g.right + 8, g.top - 26 + sinf(t * 20) * 2, 4f, mapOf('X' to WH))
            val bx = g.right + 30; val by = g.top - 50
            box(bx, by, 52f, 22f, PANEL)
            box(bx - 8, by + 24, 6f, 6f, PANEL); box(bx - 14, by + 32, 4f, 4f, PANEL)
            for (k in 0 until min(3, (t * 14).toInt() % 4)) box(bx + 10 + k * 12, by + 9, 5f, 5f, BR)
        } else {
            val k = seg(t, .6f, .72f)
            val g = termi(face = if (t < .75f) "surprised" else "happy", arms = "down" to "up",
                dy = -sinf(k * PI.toFloat()) * 10, squash = if (k in 0.001f..0.7f) -1 else 0)
            box(g.cx - 18, g.top - 46, 36f, 36f, GOLD, 40)
            pat(BULB, g.cx - 10, g.top - 42, 4f, mapOf('X' to GOLD, 'Y' to GREY))
            for (j in 0 until 4) {
                val ang = j * PI.toFloat() / 3 - PI.toFloat() * .9f
                box(g.cx + cos(ang) * 30, g.top - 30 + sin(ang) * 26, 4f, 4f, GOLD)
            }
        }
    }

    private fun B.coffee(t: Float, i: Int) {
        val lift = ease(seg(t, .25f, .4f)) - ease(seg(t, .68f, .78f))
        val face = when { t < .12f -> "sleepy"; t < .25f -> "yawn"; t < .68f -> "content"; t < .86f -> "star"; else -> "joy" }
        val zap = t > .72f
        val g = termi(face = face, arms = "down" to "hold", armDy = 0f to -lift * 14,
            dy = if (zap) -sinf(seg(t, .72f, .84f) * PI.toFloat()) * 12 else 0f,
            squash = if (t < .25f && (i / 6) % 2 == 1) 1 else if (t in .72f..0.8f) -1 else 0, handFree = false)
        val mx = g.right + 10; val my = g.top + (g.t + 1) * U - lift * 14 + 4
        pat(MUG, mx, my, 4f, mapOf('X' to VIOL, 'Y' to VIOL))
        box(mx + 2, my, 12f, 3f, 0xFFA0673A.toInt())
        for (k in 0..2) {
            val ph = (t * 4 + k / 3f) % 1f
            box(mx + 6 + sinf(ph * TAU + k) * 4, my - 6 - ph * 22, 4f, 4f, GREY, (220 * (1 - ph)).toInt())
        }
        if (zap) for (j in 0..3) {
            val ang = j * TAU / 4 + .6f
            val r = 52f + (i % 3) * 4
            val x = g.cx + cos(ang) * r; val y = g.top + 30 + sin(ang) * r * .55f
            box(x, y, 8f, 3f, GOLD); box(x + 4, y + 3, 3f, 6f, GOLD)
        }
    }

    private fun B.music(t: Float, i: Int) {
        val beat = (t * 4.6f * 2) % 1f
        val sway = sinf(t * TAU * 2.2f) * 8
        val g = termi(dx = sway, lean = sway * .5f, face = "content", squash = if (beat < .2f) 1 else 0,
            armDy = (if (beat < .2f) -3f else 0f) to (if (beat < .2f) -3f else 0f),
            legs = if ((t * 18).toInt() % 2 == 1) "lift" to "stand" else "stand" to "stand")
        val x0 = g.cx - 10 * U
        box(x0 + 3 * U, g.top - 2 * U, 14 * U, U, 0xFF5A5A66.toInt())
        box(x0 + 2 * U, g.top - U, U, 3 * U, 0xFF5A5A66.toInt()); box(x0 + 17 * U, g.top - U, U, 3 * U, 0xFF5A5A66.toInt())
        box(x0 + U, g.top + U, 2 * U, 3 * U, VIOL); box(x0 + 17 * U, g.top + U, 2 * U, 3 * U, VIOL)
        listOf((CX - 110) to VIOL, (CX + 92) to CYAN, (CX + 120) to PINK).forEachIndexed { k, (x0n, col) ->
            val ph = (t * 2.2f + k * .33f) % 1f
            pat(NOTE, x0n + sinf(ph * TAU) * 8, GROUND - 60 - ph * 90, 3f, mapOf('X' to col), (255 * (1 - ph * .7f)).toInt())
        }
    }

    private fun B.fail(t: Float, i: Int) {
        scene = SceneHint(rain = ease(seg(t, .45f, .6f)))
        val red = 0xFF5A181E.toInt()
        when {
            t < .1f -> termi(screen = if (i % 2 == 1) red else TermiSprite.SCREEN)
            t < .45f -> {
                val k = seg(t, .1f, .45f)
                val hop = -sinf(seg(k, 0f, .3f) * PI.toFloat()) * 16
                val shake = sinf(i * 2.3f) * 5 * (1 - k)
                val g = termi(face = if (k > .25f) "x" else "surprised", dx = shake, dy = hop, arms = "up" to "up",
                    squash = if (hop < -4) -1 else 0, legs = if (hop < -4) "tuck" to "tuck" else "stand" to "stand", screen = red)
                if ((i / 2) % 2 == 1) pat(BANG, g.right + 14, g.top - 24, 4f, mapOf('X' to RED))
                for (j in 0..1) {
                    val ph = (k * 3 + j * .5f) % 1f
                    pat(DROP, g.left - 10 - j * 6, g.top + 4 + ph * 30, 2f, mapOf('X' to CYAN), (255 * (1 - ph)).toInt())
                }
            }
            else -> {
                val k = seg(t, .45f, 1f)
                val sob = if (k > .4f) sinf(t * TAU * 5) * 1.5f else 0f
                val g = termi(face = if (k > .4f) "cry" else "sad", arms = "droop" to "droop", squash = 1, dy = sob, lean = sob, screen = 0xFF28161C.toInt())
                val cx = g.cx - 27; val cy = g.top - 44 + sinf(t * 6) * 2
                pat(CLOUD, cx, cy, 6f, mapOf('X' to 0xFF6E6E7A.toInt()))
                for (j in 0..3) {
                    val ph = (t * 6 + j * .25f) % 1f
                    box(cx + 8 + j * 12, cy + 26 + ph * 30, 2f, 6f, CYAN, (230 * (1 - ph)).toInt())
                }
            }
        }
    }

    private fun B.bug(t: Float, i: Int) {
        val bx = CX + 120 - ease(seg(t, 0f, .42f)) * 60
        val angry = 0xFF3C1418.toInt()
        when {
            t < .25f -> termi(face = "right")
            t < .45f -> {
                val g = termi(face = "angry", arms = "hip" to "hip", screen = angry, squash = if ((i / 2) % 2 == 1) 1 else 0)
                pat(ANGER, g.right - 8, g.top - 16, 3f, mapOf('X' to RED))
                for (j in 0..1) {
                    val ph = (t * 5 + j * .5f) % 1f
                    box(g.left + 10 + j * 40 + sinf(ph * 6) * 3, g.top - 6 - ph * 22, 6f, 6f, GREY, (200 * (1 - ph)).toInt())
                }
            }
            t < .6f -> {
                val k = seg(t, .45f, .6f)
                termi(face = "angry", dx = ease(k) * 40, dy = -sinf(k * PI.toFloat()) * 30, squash = -1,
                    arms = "up" to "up", legs = "tuck" to "tuck", screen = angry, handFree = false)
            }
            t < .68f -> {
                termi(face = "angry", dx = 40f, squash = 1, handFree = false)
                for (k in listOf(-1, 1)) { box(CX + 40 + k * 66, GROUND - 8, 8f, 8f, GREY); box(CX + 40 + k * 78, GROUND - 16, 4f, 4f, GREY) }
            }
            else -> {
                val k = seg(t, .68f, 1f)
                termi(face = if (k > .2f) "cool" else "happy", dx = 40f, arms = if (k > .2f) "hip" to "hip" else "down" to "down",
                    squash = if (k in .2f..0.3f) -1 else 0)
            }
        }
        if (t < .6f) {
            val wig = if (i % 2 == 1) 2f else -2f
            box(bx, GROUND - 12, 14f, 10f, RED)
            box(bx - 4, GROUND - 4 + wig, 4f, 4f, RED); box(bx + 14, GROUND - 4 - wig, 4f, 4f, RED)
            box(bx + 2, GROUND - 18, 2f, 6f, RED); box(bx + 10, GROUND - 18, 2f, 6f, RED)
            box(bx + 3, GROUND - 10, 3f, 3f, BR); box(bx + 9, GROUND - 10, 3f, 3f, BR)
        } else box(CX + 18, GROUND - 3, 44f, 3f, RED)
    }

    private val CONF = (0 until 26).map { k -> Triple(((k * 53) % 280 + 180).toFloat(), -((k * 29) % 140 + 10).toFloat(), listOf(GOLD, VIOL, CYAN, PINK, GREEN)[k % 5]) to ((k % 7) - 3) }

    private fun B.pass(t: Float, i: Int) {
        scene = SceneHint(dawn = ease(seg(t, .1f, .6f)) * .6f)
        val green = 0xFF123C24.toInt()
        if (t < .12f) termi(face = "surprised", screen = green)
        else if (t < .75f) {
            val cyc = (seg(t, .12f, .75f) * 3) % 1f
            if (cyc < .15f) termi(face = if ((i / 4) % 3 > 0) "joy" else "star", arms = "up" to "up", screen = green, squash = 1)
            else if (cyc < .8f) {
                val k = (cyc - .15f) / .65f
                termi(face = if ((i / 4) % 3 > 0) "joy" else "star", arms = "up" to "up", screen = green,
                    squash = if (k < .5f) -1 else 0, dy = -sinf(k * PI.toFloat()) * 30, legs = "tuck" to "tuck")
            } else termi(face = "joy", arms = "up" to "up", screen = green, squash = 1)
        } else {
            val g = termi(face = "cool", arms = "hip" to "hip", screen = green, squash = if (t < .8f) -1 else 0)
            if ((i / 3) % 2 == 1) pat(SPARK1, g.left + 2 * U, g.top + (g.t + 2) * U - 8, 2f, mapOf('X' to BR))
        }
        if (t > .12f) {
            val tt = (t - .12f) * 5
            for ((p, vx) in CONF) {
                val (x, y0, col) = p
                val y = y0 + tt * 60
                if (y > 0 && y < GROUND) box(x + vx * tt * 6 + sinf(tt * 3 + x) * 4, y, if ((i + x.toInt()) % 2 == 1) 4f else 6f, 4f, col)
            }
        }
    }

    private fun B.love(t: Float, i: Int) {
        val pet = t in .1f..0.55f
        val wig = sinf(t * TAU * 5) * 3
        val face = if (t < .1f) "neutral" else if (pet) "content" else "love"
        val hop = sinf(t * TAU * 3)
        val g = termi(face = face, lean = if (t > .1f) wig else 0f, squash = if (pet && (i / 3) % 2 == 1) 1 else 0,
            arms = if (t < .55f) "down" to "down" else "up" to "up",
            dy = if (t > .6f) -abs(hop) * 8 else 0f,
            legs = if (t > .6f && hop * hop > .3f) "tuck" to "tuck" else "stand" to "stand")
        if (pet) {
            val pt = abs(sinf(t * TAU * 4))
            pat(HAND, g.cx - 21, g.top - 34 - pt * 10, 6f, mapOf('X' to 0xFFFFD6B6.toInt(), 'Y' to 0xFFE6B496.toInt()))
        }
        if (t > .25f) for (j in 0 until 5) {
            val ph = (t * 1.6f + j * .2f) % 1f
            val x = g.cx - 60 + j * 30 + sinf(ph * TAU + j) * 8
            pat(HEART, x, g.top + 20 - ph * 80, if (j % 2 == 1) 4f else 3f, mapOf('X' to if (j % 2 == 1) RED else PINK), (255 * (1 - ph)).toInt())
        }
    }

    private fun B.bowl(food: Boolean, left: Float = 1f) {
        val bx = CX + 64; val by = GROUND - 16
        pat(BOWL, bx, by, 4f, mapOf('W' to WH, 'F' to if (food) GOLD else WH))
        if (food && left < 1f) box(bx + 4, by - 6, 20 * left, 6f, GOLD)
    }

    private fun B.hungryWait(t: Float, i: Int) {
        val g = termi(face = "puppy", arms = "front" to "front", lean = sinf(t * 40) * 1.5f, armDy = (if ((i / 4) % 2 == 1) -2f else 0f) to 0f)
        if ((i / 5) % 3 == 0) for (j in 0..2) { box(g.left - 14, g.top + 40 + j * 6, 8f, 2f, GREY); box(g.right + 6, g.top + 40 + j * 6, 8f, 2f, GREY) }
        bowl(false)
    }

    private fun B.hungry(t: Float, i: Int) {
        val bx = CX + 64; val by = GROUND - 16
        when {
            t < .2f -> { hungryWait(t * 5, i); return }
            t < .3f -> {
                val k = seg(t, .2f, .3f)
                termi(face = "surprised", arms = "up" to "up", dy = -sinf(k * PI.toFloat()) * 10, squash = -1)
                val fy = -60 + ease(seg(t, .2f, .28f)) * 52
                box(bx + 6, by + fy - 8, 16f, 8f, GOLD); box(bx + 10, by + fy - 12, 6f, 4f, GOLD)
                bowl(false)
            }
            t < .75f -> {
                val chew = (i / 2) % 2 == 1
                val g = termi(face = "nom", mouth = if (chew) "open" else "chew", squash = if (chew) 1 else 0, arms = "front" to "front", dx = 24f, handFree = false)
                if (chew) { box(g.cx + 22, g.top + 30, 3f, 3f, GOLD); box(g.cx - 26, g.top + 34, 3f, 3f, GOLD) }
                bowl(true, 1 - seg(t, .3f, .75f))
            }
            else -> {
                val g = termi(face = "content", dx = 24f, squash = 1, sit = true, arms = "front" to "front", lean = sinf(t * TAU * 1.5f) * 2)
                pat(HEART, g.right + 4, g.top - 14 - seg(t, .75f, 1f) * 16, 3f, mapOf('X' to PINK))
                bowl(false)
            }
        }
    }

    private fun B.zeds(g: G, t: Float) {
        for (k in 0..2) {
            val ph = (t * 1.8f + k / 3f) % 1f
            val x = g.right + 4 + ph * 50 + sinf(ph * TAU) * 6
            val y = g.top + 10 - ph * 80
            pat(ZED, x, y, if (ph < .5f) 2f else 3f, mapOf('X' to 0xFFAAAAB9.toInt()), (255 * (1 - ph * .8f)).toInt())
        }
    }

    private fun B.sleep(t: Float, i: Int) {
        scene = SceneHint(dim = ease(seg(t, .4f, .7f)) * .35f)
        when {
            t < .15f -> termi(face = "yawn", arms = "up" to "up", squash = if ((i / 6) % 2 == 1) -1 else 0)
            t < .4f -> {
                val nod = (seg(t, .15f, .4f) * 3) % 1f
                termi(face = if (nod < .6f) "sleepy" else "asleep", dy = if (nod > .6f) 6f else 0f, squash = if (nod > .6f) 1 else 0, lean = if (nod > .6f) -3f else 0f)
            }
            else -> nap(t, i)
        }
    }

    private fun B.nap(t: Float, i: Int) {
        if (scene.dim == 0f) scene = SceneHint(dim = .35f)
        val b = sinf(t * TAU * 2)
        val g = termi(face = "asleep", sit = true, squash = if (b > .3f) 1 else 0, screen = 0xFF0A0A0C.toInt(), arms = "front" to "front", lean = b * 1.5f)
        zeds(g, t)
    }
}

/** Picks what Termi does next. Events win; then mood; when idle, a random vibe-coder bit every 20–40 s. */
class AnimDirector(private val random: java.util.Random = java.util.Random()) {
    private val queue = ArrayDeque<TermiAnim>()
    private var lastVibe = 0L
    private var gap = 6_000L + random.nextInt(8_000)

    fun next(mood: Mood, event: TermiEvent?, nowMs: Long): TermiAnim {
        when (event) {
            TermiEvent.FED -> return TermiAnim.HUNGRY
            TermiEvent.PLAYED -> return listOf(TermiAnim.MUSIC, TermiAnim.BUG_SQUASH, TermiAnim.HELLO)[random.nextInt(3)]
            TermiEvent.SLEPT -> return TermiAnim.SLEEP
            TermiEvent.WOKE -> return TermiAnim.HELLO
            TermiEvent.CODED -> { queue.addLast(TermiAnim.BUILD_PASS); return TermiAnim.CODING }
            TermiEvent.RESTED -> return TermiAnim.HELLO
            TermiEvent.LEVEL_UP, TermiEvent.STAGE_UP -> return TermiAnim.BUILD_PASS
            null -> Unit
        }
        queue.removeFirstOrNull()?.let { return it }
        return when (mood) {
            Mood.ASLEEP -> TermiAnim.NAP
            Mood.HUNGRY -> TermiAnim.HUNGRY_WAIT
            else -> if (nowMs - lastVibe >= gap) {
                lastVibe = nowMs; gap = 6_000L + random.nextInt(8_000)
                if (random.nextInt(10) == 0) TermiAnim.BUILD_FAIL else VIBES.filter { it != TermiAnim.BUILD_FAIL }[random.nextInt(VIBES.size - 1)]
            } else TermiAnim.IDLE
        }
    }

    companion object {
        val VIBES = listOf(TermiAnim.CODING, TermiAnim.PROMPT_AI, TermiAnim.THINK, TermiAnim.COFFEE, TermiAnim.MUSIC, TermiAnim.BUG_SQUASH, TermiAnim.BUILD_FAIL)
    }
}
