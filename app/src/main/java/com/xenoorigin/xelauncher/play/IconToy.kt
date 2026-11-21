package com.xenoorigin.xelauncher.play

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.isActive

/**
 * The drawer's icon toy: one spring set per category page, fed by tilt and shakes. Icons read [frame]
 * in their graphics layer, so motion only redraws, never recomposes.
 */
class IconToy {
    private val pages = HashMap<Int, ToyPhysics>()
    val frame = mutableIntStateOf(0)
    @Volatile var fx = 0f
    @Volatile var fy = 0f
    @Volatile var shakes = 0

    fun reset() { pages.clear(); frame.intValue++ }

    fun page(p: Int): ToyPhysics = pages.getOrPut(p) { ToyPhysics() }

    internal fun step(dt: Float, shakeSeed: Int?): Boolean {
        var moving = false
        for (ph in pages.values) {
            if (shakeSeed != null) ph.shake(shakeSeed)
            ph.step(dt, fx, fy)
            if (!ph.resting) moving = true
        }
        return moving
    }

    /** Ripples in the pond push icons on [page] away from ([x], [y]) dp. */
    fun splash(page: Int, x: Float, y: Float) { pages[page]?.push(x, y, radiusDp = 150f); frame.intValue++ }
}

val LocalIconToy = staticCompositionLocalOf<IconToy?> { null }

/** Gravity and shake readings, only while started. */
private class ToySensor(context: Context, private val toy: IconToy) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gravity = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val linear = sm?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val slosh = SloshFilter()
    private val shake = ShakeDetector()
    private var lastNs = 0L

    fun start() {
        gravity?.let { sm?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        linear?.let { sm?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() { sm?.unregisterListener(this); toy.fx = 0f; toy.fy = 0f; lastNs = 0L }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                val dt = if (lastNs == 0L) 0f else (e.timestamp - lastNs) / 1e9f
                lastNs = e.timestamp
                slosh.update(e.values[0], e.values[1], dt)
                toy.fx = slosh.fx; toy.fy = slosh.fy
            }
            Sensor.TYPE_LINEAR_ACCELERATION ->
                if (shake.sample(e.values[0], e.values[1], e.values[2], e.timestamp / 1_000_000)) toy.shakes++
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** Runs the toy while [active] (drawer open, toy switched on): sensors on, springs stepped each frame. */
@Composable
fun rememberIconToy(active: Boolean): IconToy {
    val ctx = LocalContext.current
    val toy = remember { IconToy() }
    val sensor = remember { ToySensor(ctx.applicationContext, toy) }
    DisposableEffect(active) {
        if (active) sensor.start()
        onDispose { sensor.stop() }
    }
    LaunchedEffect(active) {
        if (!active) { toy.reset(); return@LaunchedEffect }
        var last = 0L
        var seen = toy.shakes
        var wasMoving = true
        while (isActive) {
            withFrameNanos { t ->
                val dt = if (last == 0L) 0f else (t - last) / 1e9f
                last = t
                val s = toy.shakes
                val moving = toy.step(dt, if (s != seen) s.also { seen = s } else null)
                // Redraw while anything moves, plus one last frame to put icons exactly home.
                if (moving || wasMoving) toy.frame.intValue++
                wasMoving = moving
            }
        }
    }
    return toy
}
