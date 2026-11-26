package com.xenoorigin.xelauncher.termi

import kotlin.math.roundToInt

/**
 * One coloured block of Termi, in sub-units: 3 sub-units = 1 pixel-art unit. The sprite box is
 * [TermiSprite.BOX_W] × [TermiSprite.BOX_H] sub-units (50 × 40 units); Termi's feet centre sits at
 * ([TermiSprite.FEET_X], [TermiSprite.FEET_Y]).
 */
data class Px(val x: Int, val y: Int, val w: Int, val h: Int, val argb: Int)

/** How Termi stands in one frame (same knobs as the approved concept). */
data class TermiPose(
    val face: String = "neutral",
    val mouth: String? = null,
    val arms: Pair<String, String> = "down" to "down",
    val armDy: Pair<Float, Float> = 0f to 0f,
    val legs: Pair<String, String> = "stand" to "stand",
    val squash: Int = 0,
    val lean: Float = 0f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val screen: Int = TermiSprite.SCREEN,
    val sit: Boolean = false,
    /** False while an animation puts its own prop in Termi's hands: the held accessory is hidden. */
    val handFree: Boolean = true,
)

/**
 * Builds Termi as coloured blocks: the window body, its screen face, short mitt arms, two legs, the stage's
 * own items and the worn accessories. Pure Kotlin so it can be tested and drawn anywhere.
 */
object TermiSprite {
    const val S = 3
    const val BOX_W = 50 * S
    const val BOX_H = 40 * S
    const val FEET_X = 25 * S
    const val FEET_Y = 36 * S

    const val SCREEN = 0xFF101012.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val DARK = 0xFF121214.toInt()
    const val PINK = 0xFFFF8CB4.toInt()
    const val RED = 0xFFFF6078.toInt()
    const val GOLD = 0xFFFFD166.toInt()
    const val CYAN = 0xFF7FE7FF.toInt()
    const val VIOLET = 0xFFC27BFF.toInt()
    const val GREY = 0xFF5A5A66.toInt()
    const val GLASS_FRAME = 0xFFC8C8D2.toInt()
    const val HEADPHONE = 0xFF5A5A66.toInt() + 1
    const val HOOD = 0xFF6E6E7C.toInt()
    const val MONITOR = 0xFF3C3C48.toInt()
    const val CAP_RED = 0xFFFF5A6E.toInt()
    const val DUCK_YELLOW = 0xFFFFD34D.toInt()

    private const val E = 'E'; private const val P = 'P'; private const val R = 'R'
    private const val G = 'G'; private const val C = 'C'
    private data class F(val c: Int, val r: Int, val w: Int = 1, val h: Int = 1, val k: Char = E)
    private fun px(k: Char = E, vararg p: Pair<Int, Int>) = p.map { F(it.first, it.second, k = k) }

    private val HAPPY_EYES = px(E, 2 to 1, 3 to 0, 4 to 1, 7 to 1, 8 to 0, 9 to 1)
    private val CLOSED_SOFT = px(E, 2 to 1, 3 to 2, 4 to 1, 7 to 1, 8 to 2, 9 to 1)
    private val SMILE = px(E, 4 to 4, 5 to 5, 6 to 5, 7 to 4)
    private val FROWN = px(E, 4 to 5, 5 to 4, 6 to 4, 7 to 5)
    private val OPEN_D = listOf(F(4, 4, 4, 1), F(5, 5, 2, 1))
    private val BLUSH = listOf(F(0, 3, 2, 1, P), F(10, 3, 2, 1, P))
    private val SAD_BROWS = px(E, 2 to 1, 3 to 0, 9 to 1, 8 to 0)

    private val faceTable: Map<String, List<F>> = mapOf(
        "neutral" to listOf(F(3, 1, 1, 2), F(8, 1, 1, 2), F(5, 4, 2, 1)),
        "blink" to listOf(F(2, 2, 2, 1), F(8, 2, 2, 1), F(5, 4, 2, 1)),
        "up" to listOf(F(3, 0, 1, 2), F(8, 0, 1, 2), F(5, 4, 2, 1)),
        "upL" to listOf(F(2, 0, 1, 2), F(7, 0, 1, 2), F(5, 4)),
        "upR" to listOf(F(4, 0, 1, 2), F(9, 0, 1, 2), F(6, 4)),
        "right" to listOf(F(4, 1, 1, 2), F(9, 1, 1, 2), F(6, 4, 2, 1)),
        "left" to listOf(F(2, 1, 1, 2), F(7, 1, 1, 2), F(4, 4, 2, 1)),
        "down" to listOf(F(3, 2, 1, 2), F(8, 2, 1, 2), F(5, 5, 2, 1)),
        "happy" to HAPPY_EYES + SMILE,
        "joy" to HAPPY_EYES + OPEN_D + BLUSH,
        "content" to CLOSED_SOFT + SMILE + BLUSH,
        "love" to px(R, 2 to 0, 4 to 0, 2 to 1, 3 to 1, 4 to 1, 3 to 2, 7 to 0, 9 to 0, 7 to 1, 8 to 1, 9 to 1, 8 to 2) + SMILE + BLUSH,
        "sad" to SAD_BROWS + listOf(F(3, 2), F(8, 2)) + FROWN,
        "cry" to SAD_BROWS + listOf(F(2, 2, 2, 1), F(8, 2, 2, 1)) + FROWN + listOf(F(3, 3, 1, 2, C), F(8, 3, 1, 2, C)),
        "angry" to px(E, 2 to 0, 3 to 1, 2 to 2, 3 to 2, 9 to 0, 8 to 1, 8 to 2, 9 to 2) + F(3, 4, 6, 1),
        "surprised" to listOf(F(2, 1, 2, 2), F(8, 1, 2, 2), F(5, 4, 2, 2)),
        "x" to px(E, 2 to 0, 4 to 0, 3 to 1, 2 to 2, 4 to 2, 7 to 0, 9 to 0, 8 to 1, 7 to 2, 9 to 2) + px(E, 3 to 5, 4 to 4, 5 to 5, 6 to 4, 7 to 5, 8 to 4),
        "focused" to listOf(F(2, 0, 2, 1), F(8, 0, 2, 1), F(3, 2), F(8, 2), F(5, 4, 2, 1)),
        "confused" to listOf(F(3, 1, 1, 2), F(8, 2), F(8, 0, 2, 1)) + px(E, 4 to 5, 5 to 4, 6 to 5, 7 to 4),
        "sleepy" to listOf(F(2, 1, 2, 1), F(3, 2), F(8, 1, 2, 1), F(8, 2), F(6, 4)),
        "yawn" to listOf(F(2, 1, 2, 1), F(8, 1, 2, 1), F(5, 3, 2, 3)),
        "asleep" to listOf(F(2, 2, 2, 1), F(8, 2, 2, 1)),
        "star" to px(G, 3 to 0, 2 to 1, 3 to 1, 4 to 1, 3 to 2, 8 to 0, 7 to 1, 8 to 1, 9 to 1, 8 to 2) + OPEN_D,
        "cool" to listOf(F(2, 1, 2, 1), F(8, 1, 2, 1), F(3, 2), F(9, 2), F(5, 4, 3, 1), F(8, 3)),
        "puppy" to listOf(F(2, 1, 2, 2), F(8, 1, 2, 2), F(2, 2, k = C), F(8, 2, k = C), F(5, 4, 2, 1)),
        "nom" to HAPPY_EYES + BLUSH,
        "shy" to listOf(F(4, 1, 1, 2), F(9, 1, 1, 2)) + BLUSH + F(5, 4, 2, 1),
    )
    private val mouths: Map<String, List<F>> = mapOf(
        "open" to listOf(F(5, 4, 2, 2)), "small" to listOf(F(6, 4)), "cursor" to listOf(F(5, 4, 2, 1)),
        "chew" to listOf(F(5, 5, 2, 1)), "big" to listOf(F(4, 3, 4, 3)), "none" to emptyList(),
    )

    val FACES: Set<String> get() = faceTable.keys

    /** Termi's body colour from the theme accent, raised so its brightest channel is at least 160. */
    fun bodyColor(accent: Long): Int {
        val c = accent.toInt()
        val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
        val mx = maxOf(r, g, b)
        if (mx >= 160) return c or (0xFF shl 24)
        if (mx == 0) return argb(160, 160, 160)
        val f = 160f / mx
        return argb((r * f).toInt().coerceAtMost(255), (g * f).toInt().coerceAtMost(255), (b * f).toInt().coerceAtMost(255))
    }

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    fun build(p: TermiPose, stage: Stage, equipped: Map<Slot, Item>, body: Int): List<Px> {
        val out = ArrayList<Px>(96)
        val t = 3 + (if (p.squash == -1) 1 else 0) - (if (p.squash == 1) 1 else 0)
        val wid = if (p.squash == 1) 1 else 0
        val bh = t + 9
        val lh = if (p.sit) 0 else 2
        // Sub-unit origin of unit column 0 / body top row 0.
        val ox = FEET_X - 10 * S + (p.dx * S).roundToInt()
        val oy = FEET_Y + (p.dy * S).roundToInt() - (lh + bh) * S
        val lean = (p.lean * S).roundToInt()
        fun b(c: Float, r: Float, w: Float, h: Float, col: Int, x: Int = 0, y: Int = 0) {
            val w3 = (w * S).roundToInt(); val h3 = (h * S).roundToInt()
            if (w3 > 0 && h3 > 0) out += Px(ox + (c * S).roundToInt() + x, oy + (r * S).roundToInt() + y, w3, h3, col)
        }
        fun b(c: Int, r: Int, w: Int, h: Int, col: Int, x: Int = 0, y: Int = 0) = b(c.toFloat(), r.toFloat(), w.toFloat(), h.toFloat(), col, x, y)

        val hat = equipped[Slot.HEAD]
        val eyes = equipped[Slot.EYES]
        val held = equipped[Slot.HAND]?.takeIf { p.handFree }

        // Lead's monitors float behind.
        if (stage == Stage.LEAD) for ((mx, my) in listOf(-7f to -5f, 21f to -6f, 22.5f to 5f)) {
            b(3 + mx, my, 4.3f, 3f, MONITOR, x = lean); b(3 + mx + .33f, my + .33f, 3.67f, 2.33f, 0xFF1E3C5A.toInt(), x = lean)
            for (k in 0..2) b(3 + mx + .67f, my + .67f + k * .67f, listOf(2.33f, 1.67f, 2.67f)[k], .34f, listOf(CYAN, 0xFFFFC85A.toInt(), VIOLET)[k], x = lean)
        }
        if (!p.sit) for ((c0, lg) in listOf(5 to p.legs.first, 12 to p.legs.second)) when (lg) {
            "stand" -> b(c0, bh, 3, 2, body)
            "lift", "tuck" -> b(c0, bh, 3, 1, body)
        }
        // Senior's hoodie hugs the body.
        if (stage == Stage.SENIOR) {
            b(2 - wid, -1, 16 + 2 * wid, 1, HOOD, x = lean); b(2 - wid, 0, 1, bh, HOOD, x = lean); b(17 + wid, 0, 1, bh, HOOD, x = lean)
        }
        b(3 - wid, 0, 14 + 2 * wid, bh, body, x = lean)
        b(4, t, 12, 8, p.screen, x = lean)
        for (c in intArrayOf(4, 6, 8)) b(c, t - 2, 1, 1, DARK, x = lean)
        var feats = faceTable[p.face] ?: faceTable.getValue("neutral")
        if (p.mouth != null) feats = feats.filterNot { it.r >= 3 && it.k == E && it.c in 3..8 } + mouths.getValue(p.mouth)
        for (f in feats) b(4 + f.c, t + 1 + f.r, f.w, f.h, color(f.k), x = lean)

        // Arms (a free right hand holds the accessory).
        val rightArm = if (held != null && p.arms.second == "down") "hold" else p.arms.second
        for ((side, kind) in listOf(0 to p.arms.first, 1 to rightArm)) {
            val ay = ((if (side == 0) p.armDy.first else p.armDy.second) * S).roundToInt()
            fun a(c: Int, r: Int, w: Int, h: Int, x: Int = 0) {
                val cc = (if (side == 1) 20 - c - w else c) + if (side == 1) wid else -wid
                b(cc, r, w, h, body, x = x, y = ay)
            }
            when (kind) {
                "down" -> a(1, bh - 3, 2, 3, lean / 2)
                "droop" -> a(1, bh - 2, 2, 3, lean / 2)
                "up" -> { a(1, -3, 2, 3, lean); a(2, 0, 1, 1, lean) }
                "raise" -> { a(0, 1, 2, 3, lean); a(2, 3, 1, 1, lean) }
                "front" -> a(2, bh - 2, 3, 2)
                "chin" -> a(4, t + 5, 3, 2, lean)
                "hold" -> a(1, t + 1, 2, 3, lean)
                "head" -> a(4, -2, 3, 2, lean)
                "cover" -> a(5, t + 2, 3, 2, lean)
                "hip" -> { a(1, 4, 2, 1); a(1, 5, 1, 2); a(1, 7, 2, 1) }
            }
        }

        // Glasses: worn ones, else Junior's round frames.
        when (eyes ?: if (stage == Stage.JUNIOR) Item.ROUND else null) {
            Item.ROUND -> {
                val r = t + 2f
                for (cx in intArrayOf(6, 11)) {
                    b(cx.toFloat(), r - 1, 3f, 1f, GLASS_FRAME, x = lean); b(cx.toFloat(), r + 2, 3f, .34f, GLASS_FRAME, x = lean)
                    b(cx.toFloat(), r - 1, .34f, 3f, GLASS_FRAME, x = lean); b(cx + 2.67f, r - 1, .34f, 3f, GLASS_FRAME, x = lean)
                }
                b(9f, r, 2f, .34f, GLASS_FRAME, x = lean)
            }
            Item.SUNGLASSES -> {
                val r = t + 1.67f
                b(5f, r, 10f, 1f, 0xFF1E1E24.toInt(), x = lean); b(5f, r + 1, 4f, 1f, 0xFF1E1E24.toInt(), x = lean); b(11f, r + 1, 4f, 1f, 0xFF1E1E24.toInt(), x = lean)
                b(5f, r, 10f, .34f, GLASS_FRAME, x = lean); b(6f, r + .67f, .67f, .67f, WHITE, x = lean); b(12f, r + .67f, .67f, .67f, WHITE, x = lean)
            }
            Item.VR -> {
                val r = t + 1f
                b(4f, r, 12f, 3f, 0xFF3C3C48.toInt(), x = lean); b(5f, r + .34f, 10f, 2f, VIOLET, x = lean)
                b(3f, r + 1, 1f, 1f, GREY, x = lean); b(16f, r + 1, 1f, 1f, GREY, x = lean)
            }
            else -> Unit
        }

        // Head: worn hat, else the stage's headphones or crown.
        when (hat) {
            Item.CAP -> { b(4, -3, 10, 3, CAP_RED, x = lean); b(3, -1, 15, 1, CAP_RED, x = lean); b(8, -4, 2, 1, CAP_RED, x = lean); b(5, -2, 2, 1, 0xFFFF8C96.toInt(), x = lean) }
            Item.BEANIE -> {
                b(4, -3, 12, 3, 0xFF468CFF.toInt(), x = lean); b(3, -1, 14, 1, 0xFF326EDC.toInt(), x = lean); b(9, -5, 2, 2, WHITE, x = lean)
                for (k in 0..4) b(5 + 2 * k, -3, 1, 2, 0xFF5AA0FF.toInt(), x = lean)
            }
            Item.PARTY -> {
                listOf(5 to 10, 6 to 8, 7 to 6, 8 to 4, 9 to 2).forEachIndexed { i, (c, w) -> b(c, -(i + 1), w, 1, if (i % 2 == 1) PINK else GOLD, x = lean) }
                b(9, -7, 2, 1, CYAN, x = lean)
            }
            Item.WIZARD -> {
                b(2, -1, 16, 1, VIOLET, x = lean)
                listOf(5 to 10, 6 to 8, 7 to 6, 8 to 5, 9 to 4, 10 to 3, 11 to 2).forEachIndexed { i, (c, w) -> b(c, -(i + 2), w, 1, VIOLET, x = lean) }
                b(12, -9, 2, 1, VIOLET, x = lean); b(8, -4, 1, 1, GOLD, x = lean); b(11, -6, 1, 1, GOLD, x = lean)
            }
            else -> when (stage) {
                Stage.SENIOR -> {
                    b(3, -2, 14, 1, HEADPHONE, x = lean); b(2, -1, 1, 3, HEADPHONE, x = lean); b(17, -1, 1, 3, HEADPHONE, x = lean)
                    b(1, 1, 2, 3, VIOLET, x = lean); b(17, 1, 2, 3, VIOLET, x = lean)
                }
                Stage.LEAD -> {
                    b(5, -2, 10, 2, GOLD, x = lean)
                    for (c in intArrayOf(5, 9, 13)) b(c, -4, 2, 2, GOLD, x = lean)
                    b(9f, -1.67f, 2f, 1f, RED, x = lean); b(6f, -1.67f, 1f, 1f, CYAN, x = lean); b(13f, -1.67f, 1f, 1f, CYAN, x = lean)
                }
                Stage.JUNIOR -> Unit
            }
        }

        // Held accessory beside the raised right mitt.
        if (held != null && rightArm == "hold") {
            val hx = 18.67f + wid; val hy = t + 1.67f
            when (held) {
                Item.COFFEE -> { b(hx, hy, 2.67f, 2.67f, VIOLET, x = lean); b(hx + 2.67f, hy + .67f, .67f, 1.33f, VIOLET, x = lean); b(hx + .33f, hy, 2f, .5f, 0xFFA0673A.toInt(), x = lean) }
                Item.KEYBOARD -> { b(hx - .33f, hy + .67f, 5f, 2f, 0xFF2A2A32.toInt(), x = lean); for (k in 0..5) b(hx + .17f + k * .75f, hy + 1.17f, .5f, .5f, 0xFF5A5A68.toInt(), x = lean) }
                Item.DUCK -> { b(hx + .67f, hy, 1.33f, .67f, DUCK_YELLOW, x = lean); b(hx, hy + .67f, 2.67f, 1.33f, DUCK_YELLOW, x = lean); b(hx + .33f, hy + 2f, 1.67f, .5f, DUCK_YELLOW, x = lean); b(hx + 2f, hy + .33f, .67f, .67f, 0xFFFF963C.toInt(), x = lean) }
                Item.PHONE -> { b(hx + .33f, hy, 2f, 3.33f, 0xFF28282F.toInt(), x = lean); b(hx + .67f, hy + .33f, 1.33f, 2.33f, 0xFF4678FF.toInt(), x = lean) }
                else -> Unit
            }
        }
        return out.map { it.clip() }.filter { it.w > 0 && it.h > 0 }
    }

    private fun Px.clip(): Px {
        val x0 = x.coerceIn(0, BOX_W); val y0 = y.coerceIn(0, BOX_H)
        val x1 = (x + w).coerceIn(0, BOX_W); val y1 = (y + h).coerceIn(0, BOX_H)
        return Px(x0, y0, x1 - x0, y1 - y0, argb)
    }

    private fun color(k: Char) = when (k) { P -> PINK; R -> RED; G -> GOLD; C -> CYAN; else -> WHITE }
}
