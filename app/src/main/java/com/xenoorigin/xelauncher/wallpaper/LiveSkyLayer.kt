package com.xenoorigin.xelauncher.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val r: Float, val base: Float, val speed: Float, val phase: Float, val tint: Color)

private val Stars: List<Star> = Random(2026).let { rnd ->
    List(220) {
        val big = rnd.nextFloat() < .06f
        Star(
            x = rnd.nextFloat(), y = rnd.nextFloat() * rnd.nextFloat() * .85f,
            r = if (big) 1.5f + rnd.nextFloat() else .4f + rnd.nextFloat() * .8f,
            base = .35f + rnd.nextFloat() * .65f, speed = .6f + rnd.nextFloat() * 1.6f, phase = rnd.nextFloat() * 6.28f,
            tint = when { rnd.nextFloat() < .2f -> Color(0xFFFFDDE6); rnd.nextFloat() < .2f -> Color(0xFFD8E4FF); else -> Color.White },
        )
    }
}

/** Slow cloud banks: vertical position, size, drift speed (screen widths per minute), opacity. */
private val Clouds = listOf(
    floatArrayOf(.16f, .9f, .010f, 1f), floatArrayOf(.30f, 1.3f, .006f, .7f), floatArrayOf(.46f, 1.1f, .004f, .55f),
)

/**
 * The live sky drawn right on Home: the same colours as the phone wallpaper, plus twinkling stars and
 * clouds drifting slowly. With [animate] off it is a still picture that only changes with [minute].
 * While animating it redraws about 8 times a second, not every frame, to keep the glass blur cheap.
 */
/** The sky's timers only run while Xe is on screen; in the background they would wake the phone for nothing. */
fun skyLoopRuns(animate: Boolean, state: Lifecycle.State): Boolean = animate && state.isAtLeast(Lifecycle.State.STARTED)

/** Whether Xe is currently visible (started), as state. */
@Composable
fun rememberXeVisible(): Boolean {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return state.isAtLeast(Lifecycle.State.STARTED)
}

@Composable
fun LiveSkyLayer(sun: SunTimes, minute: () -> Int, animate: Boolean, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running = skyLoopRuns(animate, state)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) { t = (System.nanoTime() - start) / 1e9f; delay(125) }
    }
    Canvas(modifier) {
        val look = skyAt(minute(), sun)
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(0f to Color(look.top), .58f to Color(look.mid), 1f to Color(look.bottom)))

        if (look.stars > 0f) {
            val unit = w / 400f
            for (s in Stars) {
                val twinkle = if (animate) .72f + .28f * sin(t * s.speed + s.phase) else 1f
                val a = (look.stars * s.base * twinkle).coerceIn(0f, 1f)
                val c = Offset(s.x * w, s.y * h)
                if (s.r > 1.4f) drawCircle(Brush.radialGradient(listOf(s.tint.copy(alpha = a / 3), Color.Transparent), c, s.r * unit * 5), s.r * unit * 5, c)
                drawCircle(s.tint.copy(alpha = a), s.r * unit, c)
            }
        }

        // Clouds catch the light: bright by day, faint silver at night.
        val dayness = 1f - look.stars
        val cloudColor = Color(mixToWhite(look.mid, .55f + .35f * dayness))
        for ((bi, c) in Clouds.withIndex()) {
            val span = w * 1.8f
            val drift = (t * c[2] * w) % span
            val cy = c[0] * h
            for (k in 0 until 6) {
                // Uneven puffs: each bank is a few soft blobs of different sizes, squashed flat.
                val seed = Random(bi * 31 + k)
                val r = w * (.12f + seed.nextFloat() * .16f) * c[1]
                val cx = ((k * w * .34f + seed.nextFloat() * w * .2f + drift) % span) - w * .3f
                val dy = (seed.nextFloat() - .5f) * h * .03f
                val alpha = (.06f + .13f * dayness) * c[3] * (.6f + .4f * seed.nextFloat())
                val center = Offset(cx, cy + dy)
                scale(1f, .42f, pivot = center) {
                    drawCircle(Brush.radialGradient(listOf(cloudColor.copy(alpha = alpha), cloudColor.copy(alpha = alpha * .45f), Color.Transparent), center, r), r, center)
                }
            }
        }

        // Glow, then the sun or moon.
        val o = Offset(look.orbX * w, look.orbY * h)
        val warm = if (look.sunUp) Color(0xFFFFE0A0) else Color(0xFFD6E2FF)
        val glowR = w * (if (look.sunUp) .75f else .5f)
        val ga = (if (look.sunUp) .5f else .22f) * look.orbGlow
        drawCircle(Brush.radialGradient(0f to warm.copy(alpha = ga), .35f to warm.copy(alpha = ga / 4), 1f to Color.Transparent, center = o, radius = glowR), glowR, o)
        val discR = w * (if (look.sunUp) .055f else .04f)
        drawCircle(if (look.sunUp) Color(0xFFFFF6E0) else Color(0xFFF2F5FF), discR, o)
        if (!look.sunUp) drawCircle(Color(look.top).copy(alpha = .82f), discR * .9f, o + Offset(discR * .45f, -discR * .25f))
    }
}

private fun mixToWhite(c: Long, t: Float): Long {
    fun ch(s: Int) = ((c shr s) and 0xFF).toFloat().let { it + (255 - it) * t }.toLong().coerceIn(0, 255)
    return (0xFFL shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

