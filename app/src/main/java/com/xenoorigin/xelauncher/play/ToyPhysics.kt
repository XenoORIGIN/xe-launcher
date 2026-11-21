package com.xenoorigin.xelauncher.play

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Springy offsets for the icons on a drawer page (in dp). Each icon hangs on a spring at its grid spot
 * with its own weight, so a tilt makes them slosh unevenly, a shake throws them about, and they always
 * come home. Forces are in m/s² (the change in gravity across the screen).
 */
class ToyPhysics(initial: Int = 0) {
    private var n = 0
    private var px = FloatArray(0); private var py = FloatArray(0)
    private var vx = FloatArray(0); private var vy = FloatArray(0)
    private var rot = FloatArray(0); private var vr = FloatArray(0)
    private var cx = FloatArray(0); private var cy = FloatArray(0)
    private var mass = FloatArray(0)

    /** True when nothing is moving: no need to redraw. */
    var resting = true
        private set

    init { ensure(initial) }

    fun ensure(count: Int) {
        if (count <= n) return
        fun FloatArray.grow() = copyOf(count)
        px = px.grow(); py = py.grow(); vx = vx.grow(); vy = vy.grow(); rot = rot.grow(); vr = vr.grow()
        cx = cx.grow(); cy = cy.grow(); mass = mass.grow()
        for (i in n until count) mass[i] = .78f + ((i * 7919) % 11) / 20f
        n = count
    }

    fun x(i: Int) = if (i < n) px[i] else 0f
    fun y(i: Int) = if (i < n) py[i] else 0f
    fun rotation(i: Int) = if (i < n) rot[i] else 0f

    /** Where icon [i]'s centre is on screen (dp), for [push]. */
    fun setCenter(i: Int, x: Float, y: Float) { ensure(i + 1); cx[i] = x; cy[i] = y }

    fun step(dt: Float, fx: Float, fy: Float) {
        val d = dt.coerceIn(0f, 1f / 30f)
        val forced = fx != 0f || fy != 0f
        var moving = false
        for (i in 0 until n) {
            val m = mass[i]
            val ax = fx * GAIN / m - K * px[i] - C * vx[i]
            val ay = fy * GAIN / m - K * py[i] - C * vy[i]
            vx[i] += ax * d; vy[i] += ay * d
            px[i] += vx[i] * d; py[i] += vy[i] * d
            // Soft wall: an icon never wanders further than MAX_TRAVEL_DP.
            val dist = hypot(px[i], py[i])
            if (dist > MAX_TRAVEL_DP) {
                val f = MAX_TRAVEL_DP / dist
                px[i] *= f; py[i] *= f; vx[i] *= .5f; vy[i] *= .5f
            }
            // Rolling: sideways speed turns the icon a little.
            val ar = vx[i] * .9f - KR * rot[i] - CR * vr[i]
            vr[i] += ar * d; rot[i] += vr[i] * d
            if (!forced && hypot(px[i], py[i]) < REST && hypot(vx[i], vy[i]) < REST * 4 && kotlin.math.abs(rot[i]) < REST && kotlin.math.abs(vr[i]) < REST * 4) {
                px[i] = 0f; py[i] = 0f; vx[i] = 0f; vy[i] = 0f; rot[i] = 0f; vr[i] = 0f
            } else moving = true
        }
        resting = !moving
    }

    /** A shake: every icon is flung in its own direction and spun. */
    fun shake(seed: Int) {
        for (i in 0 until n) {
            val h = ((i + 1) * 2654435761L + seed * 40503L).toInt() and 0xFFFF
            val a = h / 65535f * 6.2832f
            val speed = 260f + (h % 97) * 3f
            vx[i] += cos(a) * speed / mass[i]; vy[i] += sin(a) * speed / mass[i]
            vr[i] += if (h % 2 == 0) 260f else -260f
        }
        resting = false
    }

    /** A splash at ([x], [y]) dp: icons within [radiusDp] are pushed away from it, nearer ones harder. */
    fun push(x: Float, y: Float, radiusDp: Float) {
        for (i in 0 until n) {
            if (cx[i] == 0f && cy[i] == 0f) continue
            val dx = cx[i] - x; val dy = cy[i] - y
            val d = hypot(dx, dy)
            if (d >= radiusDp) continue
            val k = (1f - d / radiusDp) * 420f / mass[i]
            val ux = if (d < .01f) 0f else dx / d; val uy = if (d < .01f) -1f else dy / d
            vx[i] += ux * k; vy[i] += uy * k
            resting = false
        }
    }

    companion object {
        const val MAX_TRAVEL_DP = 46f
        private const val GAIN = 190f
        private const val K = 38f
        private const val C = 4.6f
        private const val KR = 44f
        private const val CR = 5.5f
        private const val REST = .05f
    }
}

/**
 * Turns gravity readings into the force that sloshes icons: only changes in tilt count, so icons drift
 * back once the phone is held still at any angle. A sharp jolt is reported as a shake.
 */
class SloshFilter {
    private var avgX = Float.NaN; private var avgY = Float.NaN
    var fx = 0f; private set
    var fy = 0f; private set

    /** [gx], [gy] are Android gravity readings (m/s²); [dt] seconds since the last one. */
    fun update(gx: Float, gy: Float, dt: Float) {
        if (avgX.isNaN()) { avgX = gx; avgY = gy }
        val a = (dt / TAU).coerceIn(0f, 1f)
        avgX += (gx - avgX) * a; avgY += (gy - avgY) * a
        // Right edge dips -> gx goes negative -> icons slide right. Top tips away -> gy drops -> icons rise.
        fx = -(gx - avgX); fy = gy - avgY
        if (kotlin.math.abs(fx) < DEAD) fx = 0f
        if (kotlin.math.abs(fy) < DEAD) fy = 0f
    }

    private companion object { const val TAU = .9f; const val DEAD = .25f }
}

/** Detects a shake from linear acceleration (m/s², gravity removed), at most once per [cooldownMs]. */
class ShakeDetector(private val threshold: Float = 13f, private val cooldownMs: Long = 900) {
    private var last = Long.MIN_VALUE / 2
    fun sample(ax: Float, ay: Float, az: Float, nowMs: Long): Boolean {
        if (nowMs - last < cooldownMs) return false
        if (kotlin.math.sqrt(ax * ax + ay * ay + az * az) < threshold) return false
        last = nowMs
        return true
    }
}
