package com.xenoorigin.xelauncher.play

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.random.Random

class LetterRecognizerTest {
    /** A sloppy human copy of a template: reversed strokes, wobble, stretched, moved and scaled to screen size. */
    private fun scrawl(strokes: List<List<Pt>>, rnd: Random): List<List<Pt>> {
        val sx = 260f * (0.85f + rnd.nextFloat() * .3f); val sy = 300f * (0.85f + rnd.nextFloat() * .3f)
        val ox = 80f + rnd.nextFloat() * 200f; val oy = 300f + rnd.nextFloat() * 200f
        return strokes.shuffled(rnd).map { s ->
            val dense = densify(s)
            val r = if (rnd.nextBoolean()) dense.reversed() else dense
            // A finger wobbles smoothly, it doesn't jump from point to point.
            val ph = rnd.nextFloat() * 6.28f; val dx = (rnd.nextFloat() - .5f) * .06f; val dy = (rnd.nextFloat() - .5f) * .06f
            r.mapIndexed { k, it ->
                val w = kotlin.math.sin(ph + k * .35f) * .025f
                Pt(ox + (it.x + dx + w) * sx, oy + (it.y + dy - w) * sy)
            }
        }
    }

    private fun densify(s: List<Pt>): List<Pt> = if (s.size < 2) s else s.zipWithNext().flatMap { (a, b) ->
        (0 until 6).map { k -> val t = k / 6f; Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) }
    } + s.last()

    @Test fun everyLetter_isReadBack_fromAMessyCopyOfItsShape() {
        val rec = LetterRecognizer()
        val rnd = Random(7)
        var top1 = 0; var total = 0
        val misses = mutableListOf<String>(); val outOfTop3 = mutableListOf<Char>()
        LetterShapes.all.forEach { (ch, variants) ->
            variants.forEach { v ->
                repeat(3) {
                    val guesses = rec.recognize(scrawl(v, rnd))
                    total++
                    if (guesses.first() == ch) top1++ else misses += "$ch->${guesses.take(3).joinToString("")}"
                    if (ch !in guesses.take(3)) outOfTop3 += ch
                }
            }
        }
        assertWithMessage("misread: $misses").that(outOfTop3).isEmpty()
        assertWithMessage("misread: $misses").that(top1.toFloat() / total).isAtLeast(.9f)
    }

    @Test fun narrowOrWideHandwriting_isStillRead() {
        val rec = LetterRecognizer()
        fun scaled(ch: Char, sx: Float, sy: Float) = LetterShapes.all.first { it.first == ch }.second.first().map { s -> s.map { Pt(100f + it.x * sx, 100f + it.y * sy) } }
        listOf('N', 'M', 'H', 'D', 'A', 'K', 'E').forEach { ch ->
            assertWithMessage("narrow $ch").that(rec.recognize(scaled(ch, 330f, 600f)).first()).isEqualTo(ch)
            assertWithMessage("wide $ch").that(rec.recognize(scaled(ch, 600f, 420f)).first()).isEqualTo(ch)
        }
        assertThat(rec.recognize(listOf(listOf(Pt(200f, 100f), Pt(201f, 400f), Pt(203f, 700f)))).first()).isEqualTo('I')
    }

    @Test fun aTapOrTinyScribble_readsAsNothing() {
        val rec = LetterRecognizer()
        assertThat(rec.recognize(listOf(listOf(Pt(10f, 10f))))).isEmpty()
        assertThat(rec.recognize(listOf(listOf(Pt(10f, 10f), Pt(12f, 11f))))).isEmpty()
        assertThat(rec.recognize(emptyList())).isEmpty()
    }

    @Test fun firstLetterOf_skipsSymbolsAndIgnoresCase() {
        assertThat(firstLetterOf("K PLUS")).isEqualTo('K')
        assertThat(firstLetterOf("truemoney")).isEqualTo('T')
        assertThat(firstLetterOf("  @Gmail")).isEqualTo('G')
        assertThat(firstLetterOf("ไทยพาณิชย์")).isNull()
        assertThat(firstLetterOf("7-Eleven")).isEqualTo('E')
    }
}
