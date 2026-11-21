package com.xenoorigin.xelauncher.play

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.xenoorigin.xelauncher.design.GlassTilt
import kotlin.random.Random

private class Speck(val x: Float, val y: Float, val r: Float, val a: Float, val warm: Boolean)

private val speckCache = HashMap<Int, List<Speck>>()

private fun specks(seed: Int): List<Speck> = speckCache.getOrPut(seed and 0x3F) {
    val rnd = Random(seed and 0x3F)
    List(70) { Speck(rnd.nextFloat(), rnd.nextFloat(), .006f + rnd.nextFloat() * .02f, .35f + rnd.nextFloat() * .55f, rnd.nextFloat() < .55f) }
}

/**
 * Dust over an icon tile: a dull grey veil and specks, thicker with [level] (0..1); [wiped] (0..1) is how
 * much has been rubbed off so far. Drawn inside the tile's [shape].
 */
fun Modifier.dust(level: () -> Float, wiped: () -> Float, seed: Int, shape: Shape): Modifier = drawWithContent {
    drawContent()
    val l = level() * (1f - wiped())
    if (l <= .01f) return@drawWithContent
    val clip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }
    clipPath(clip) {
        drawRect(Color(0xFF8C8578).copy(alpha = .30f * l))
        drawRect(Brush.verticalGradient(listOf(Color(0xFFB8AE9C).copy(alpha = .22f * l), Color.Transparent)))
        val all = specks(seed)
        val n = (all.size * l).toInt().coerceAtLeast(6)
        for (i in 0 until n) {
            val s = all[i]
            drawCircle(
                (if (s.warm) Color(0xFFD8CDB8) else Color(0xFFA9A49A)).copy(alpha = s.a * l),
                s.r * size.minDimension, Offset(s.x * size.width, s.y * size.height),
            )
        }
    }
}

/**
 * Rubbing a dusty icon: press, pause for a moment, then rub back and forth. A quick swipe is left for
 * the pages; while rubbing, the touch is kept from opening the app or scrolling.
 */
fun Modifier.rubToClean(enabled: Boolean, iconSizePx: Float, onProgress: (Float) -> Unit, onTick: () -> Unit, onClean: () -> Unit): Modifier =
    if (!enabled) this else pointerInput(iconSizePx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var rub: RubProgress? = null
            var last = down.position
            var sinceTick = 0f
            val slop = viewConfiguration.touchSlop
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                val c = e.changes.firstOrNull { it.id == down.id } ?: break
                if (!c.pressed) { if (rub != null) c.consume(); break }
                if (rub == null) {
                    val moved = (c.position - down.position).getDistance()
                    if (moved > slop * .6f) {
                        if (c.uptimeMillis - down.uptimeMillis < HOLD_MS) break // a swipe: not ours
                        rub = RubProgress(iconSizePx)
                    }
                }
                val r = rub ?: continue
                c.consume()
                val d = (c.position - last).getDistance()
                sinceTick += d
                if (sinceTick > iconSizePx * .35f) { sinceTick = 0f; onTick() }
                val done = r.add(d)
                onProgress(r.progress)
                if (done) onClean()
                last = c.position
            }
        }
    }

private const val HOLD_MS = 150L

/** A soft band of light across an icon tile that slides as the phone tilts (reads [GlassTilt] while drawing). */
fun Modifier.tiltSheen(enabled: Boolean, shape: Shape): Modifier = if (!enabled) this else drawWithContent {
    drawContent()
    val t = GlassTilt.value.value
    if (t == Offset.Zero) return@drawWithContent
    val clip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }
    val w = size.width; val h = size.height
    val cx = w * (.5f + t.x * .9f); val cy = h * (.5f - t.y * .9f)
    clipPath(clip) {
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent, .42f to Color.White.copy(alpha = .0f), .5f to Color.White.copy(alpha = .20f), .58f to Color.White.copy(alpha = .0f), 1f to Color.Transparent,
                start = Offset(cx - w * .7f, cy - h * .7f), end = Offset(cx + w * .7f, cy + h * .7f),
            ),
        )
    }
}

/** A little cloud of dust blowing off an icon that was just wiped clean; plays each time [trigger] changes. */
@androidx.compose.runtime.Composable
fun DustPuff(trigger: Int, modifier: Modifier) {
    val t = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(trigger) { t.snapTo(0f); t.animateTo(1f, androidx.compose.animation.core.tween(650, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) }
    androidx.compose.foundation.Canvas(modifier) {
        val p = t.value
        if (p >= 1f) return@Canvas
        val rnd = Random(trigger)
        val c = center
        repeat(16) {
            val a = rnd.nextFloat() * 6.2832f
            val sp = .35f + rnd.nextFloat() * .4f
            val d = size.minDimension * sp * p
            val r = size.minDimension * (.02f + rnd.nextFloat() * .035f) * (1f + p)
            drawCircle(Color(0xFFD8CDB8).copy(alpha = (1f - p) * .7f), r, Offset(c.x + kotlin.math.cos(a) * d, c.y + kotlin.math.sin(a) * d - p * 18f))
        }
        drawCircle(Color.White.copy(alpha = (1f - p) * .35f), size.minDimension * .5f * (.6f + p * .5f), c, style = androidx.compose.ui.graphics.drawscope.Stroke(3f * (1f - p) + .5f))
    }
}
