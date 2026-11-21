package com.xenoorigin.xelauncher.play

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.sin
import kotlin.random.Random

class Bubble(var x: Float, var y: Float, val r: Float, val rise: Float, val phase: Float, val life: Float, val golden: Boolean = false) {
    var age = 0f
    val fade: Float get() = when {
        age < .25f -> age / .25f
        age > life - .6f -> ((life - age) / .6f).coerceAtLeast(0f)
        else -> 1f
    }
}

class Ring(val x: Float, val y: Float, val maxR: Float, val life: Float) { var age = 0f }

/** The drawer's pond: ripples where you tap, bubbles that float up and can be popped. Sizes are pixels. */
class PondSim(seed: Int = 0) {
    private val rnd = Random(seed)
    val bubbles = ArrayList<Bubble>()
    val ripples = ArrayList<Ring>()
    val pops = ArrayList<Ring>()
    val alive: Boolean get() = bubbles.isNotEmpty() || ripples.isNotEmpty() || pops.isNotEmpty()

    /** [unit] is the screen density (pixels per dp). */
    fun splash(x: Float, y: Float, unit: Float) {
        ripples += Ring(x, y, 130f * unit, .95f)
        repeat(3 + rnd.nextInt(3)) {
            if (bubbles.size >= MAX_BUBBLES) bubbles.removeAt(0)
            bubbles += Bubble(
                x + (rnd.nextFloat() - .5f) * 50f * unit, y + (rnd.nextFloat() - .5f) * 24f * unit,
                (9f + rnd.nextFloat() * 15f) * unit, (55f + rnd.nextFloat() * 60f) * unit,
                rnd.nextFloat() * 6.28f, 3.4f + rnd.nextFloat() * 2f,
            )
        }
    }

    /** Pops the bubble under ([x], [y]) if there is one. */
    fun popAt(x: Float, y: Float): Boolean {
        val hit = bubbles.lastOrNull { val dx = it.x - x; val dy = it.y - y; dx * dx + dy * dy <= (it.r * 1.35f) * (it.r * 1.35f) } ?: return false
        bubbles.remove(hit)
        pops += Ring(hit.x, hit.y, hit.r * 2.2f, .32f)
        return true
    }

    fun step(dt: Float) {
        bubbles.forEach { it.age += dt; it.y -= it.rise * dt; it.x += sin(it.age * 2.6f + it.phase) * it.r * .9f * dt }
        bubbles.removeAll { it.age >= it.life }
        ripples.forEach { it.age += dt }; ripples.removeAll { it.age >= it.life }
        pops.forEach { it.age += dt }; pops.removeAll { it.age >= it.life }
    }

    companion object { const val MAX_BUBBLES = 24 }
}

/** A soap bubble: clear middle, bright rim, a hint of rainbow and a highlight. */
fun DrawScope.drawBubble(x: Float, y: Float, r: Float, alpha: Float, golden: Boolean = false) {
    if (alpha <= 0f) return
    val c = Offset(x, y)
    val rim = if (golden) Color(0xFFFFD36B) else Color.White
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent, .72f to rim.copy(alpha = .05f * alpha),
            .9f to (if (golden) Color(0xFFFFB340) else Color(0xFF9AD8FF)).copy(alpha = .28f * alpha), 1f to rim.copy(alpha = .55f * alpha),
            center = c, radius = r,
        ),
        r, c,
    )
    drawCircle(Color(0xFFFF9EE8).copy(alpha = .22f * alpha), r * .96f, c, style = Stroke(r * .06f))
    drawCircle(Color.White.copy(alpha = .75f * alpha), r * .2f, Offset(x - r * .38f, y - r * .4f))
}

fun DrawScope.drawRing(ring: Ring, color: Color = Color.White) {
    val t = (ring.age / ring.life).coerceIn(0f, 1f)
    val a = (1f - t)
    drawCircle(color.copy(alpha = .35f * a), ring.maxR * t, Offset(ring.x, ring.y), style = Stroke(2.5f + 4f * a))
    if (ring.maxR > 60f) drawCircle(color.copy(alpha = .18f * a), ring.maxR * t * .62f, Offset(ring.x, ring.y), style = Stroke(1.5f + 2f * a))
}

/**
 * Draws the pond behind a drawer page and catches taps on empty space. [onSplash] gets the tap in
 * pixels relative to the page, so icons nearby can be nudged.
 */
fun Modifier.pond(sim: PondSim, frame: androidx.compose.runtime.MutableIntState, unit: Float, onSplash: (Offset) -> Unit, onPop: () -> Unit): Modifier =
    this
        .pointerInput(sim) {
            detectTapGestures { p ->
                if (sim.popAt(p.x, p.y)) onPop() else { sim.splash(p.x, p.y, unit); onSplash(p) }
                frame.intValue++
            }
        }
        .drawBehind {
            frame.intValue
            sim.ripples.forEach { drawRing(it) }
            sim.bubbles.forEach { drawBubble(it.x, it.y, it.r, it.fade, it.golden) }
            sim.pops.forEach { drawRing(it, Color(0xFFBFE6FF)) }
        }

/** Pond state for one page, with its own animation loop that runs only while something is moving. */
@Composable
fun rememberPond(): Pair<PondSim, androidx.compose.runtime.MutableIntState> {
    val sim = remember { PondSim(seed = (System.nanoTime() % 10_000).toInt()) }
    val frame = remember { mutableIntStateOf(0) }
    LaunchedEffect(frame.intValue > 0 && sim.alive) {
        var last = 0L
        while (sim.alive) {
            withFrameNanos { t ->
                sim.step(if (last == 0L) 0f else ((t - last) / 1e9f).coerceAtMost(.05f))
                last = t
                frame.intValue++
            }
        }
    }
    return sim to frame
}

/** Haptic for a popped bubble. */
@Composable
fun rememberPopHaptic(): () -> Unit {
    val h = LocalHapticFeedback.current
    return remember(h) { { h.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
}

@Composable
internal fun densityUnit(): Float = LocalDensity.current.density
