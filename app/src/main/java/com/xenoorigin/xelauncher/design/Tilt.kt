package com.xenoorigin.xelauncher.design

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/**
 * How the phone is held, as -1..1 on each axis: x > 0 when the right edge dips, y > 0 when the phone
 * lies flatter than a normal reading angle. [gx], [gy], [gz] are Android gravity readings (m/s²).
 */
fun tiltFromGravity(gx: Float, gy: Float, gz: Float): Offset =
    Offset((-gx / 4.9f).coerceIn(-1f, 1f), ((gz - gy) / 9f).coerceIn(-1f, 1f))

/** Counts quick taps; [tap] is true on the [needed]th tap if each came within [windowMs] of the last. */
class TapCounter(private val needed: Int, private val windowMs: Long) {
    private var count = 0
    private var last = Long.MIN_VALUE / 2
    /** Taps so far in the current run (0 after a hit). */
    val progress: Int get() = count
    fun tap(now: Long): Boolean {
        count = if (now - last <= windowMs) count + 1 else 1
        last = now
        if (count >= needed) { count = 0; last = Long.MIN_VALUE / 2; return true }
        return false
    }
}

/** The current tilt, read while drawing glass rims (a draw-only read: no recomposition). */
object GlassTilt {
    val value = mutableStateOf(Offset.Zero)
}

/** Feeds [GlassTilt] from the gravity sensor, smoothed, only while started. */
class TiltSensor(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var sx = 0f
    private var sy = 0f
    private var running = false

    fun start() {
        if (running || sensor == null) return
        running = sm?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) == true
    }

    fun stop() {
        if (!running) return
        sm?.unregisterListener(this); running = false
        GlassTilt.value.value = Offset.Zero
    }

    override fun onSensorChanged(e: SensorEvent) {
        val t = tiltFromGravity(e.values[0], e.values[1], e.values[2])
        sx += (t.x - sx) * .12f; sy += (t.y - sy) * .12f
        // Publish in small steps only: every change redraws all glass, so sensor noise must not.
        val q = Offset((sx / STEP).roundToInt() * STEP, (sy / STEP).roundToInt() * STEP)
        if (q != GlassTilt.value.value) GlassTilt.value.value = q
    }

    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit

    private companion object { const val STEP = .025f }
}
