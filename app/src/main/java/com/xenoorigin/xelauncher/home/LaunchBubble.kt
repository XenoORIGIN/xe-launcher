package com.xenoorigin.xelauncher.home

import android.app.ActivityOptions
import android.os.Bundle
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.xenoorigin.xelauncher.design.AnimationSpeed
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.hypot

/** Glass bubble shows only when the setting is on, animations run and we know where the icon is. */
fun showBubble(glassLaunch: Boolean, speed: AnimationSpeed, icon: Rect): Boolean =
    glassLaunch && speed != AnimationSpeed.OFF && icon.width > 0f && icon.height > 0f

/** System scale-up from the icon; null keeps the platform's plain open animation. */
fun launchOptions(view: View, icon: Rect, glassLaunch: Boolean, speed: AnimationSpeed): Bundle? =
    if (!showBubble(glassLaunch, speed, icon)) null
    else ActivityOptions.makeScaleUpAnimation(
        view, icon.left.toInt(), icon.top.toInt(), icon.width.toInt(), icon.height.toInt(),
    ).toBundle()

/**
 * A glass bubble that swells out of the tapped icon while the app opens, then melts away.
 * Grows with a 180 ms-feel spring, fades over the tail so the incoming app takes over cleanly.
 */
@Composable
fun LaunchBubble(icon: Rect, speed: AnimationSpeed, tint: Color, onDone: () -> Unit) {
    val grow = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    // The app has taken over: drop the bubble so it is not still melting when the person comes home.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onDone() }
    LaunchedEffect(icon) {
        val s = speed.scale.coerceAtLeast(.4f)
        coroutineScope {
            val g = async { grow.animateTo(1f, spring(dampingRatio = .82f, stiffness = Spring.StiffnessMedium / s)) }
            val f = async { kotlinx.coroutines.delay((140 * s).toLong()); fade.animateTo(0f, tween((240 * s).toInt())) }
            g.await(); f.await()
        }
        onDone()
    }
    Canvas(Modifier.fillMaxSize().testTag("launchBubble")) {
        val c = icon.center
        val start = icon.minDimension / 2f
        val far = maxOf(
            hypot(c.x, c.y), hypot(size.width - c.x, c.y),
            hypot(c.x, size.height - c.y), hypot(size.width - c.x, size.height - c.y),
        )
        val r = start + (far - start) * grow.value
        val a = fade.value
        if (r <= 0f || a <= 0f) return@Canvas
        // Film: bright core near the icon, thinning to clear glass at the edge.
        drawCircle(
            Brush.radialGradient(
                0f to tint.copy(alpha = .30f * a), .7f to Color.White.copy(alpha = .10f * a), 1f to Color.White.copy(alpha = .22f * a),
                center = c, radius = r,
            ), r, c,
        )
        // Rim: a light catch on the upper-left, darker lower-right.
        drawCircle(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = .75f * a), Color.White.copy(alpha = .08f * a), Color.White.copy(alpha = .35f * a)),
                start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r),
            ), r, c, style = Stroke(width = 2.5f + 3f * (1f - grow.value)),
        )
        // Specular highlight that rides the bubble.
        drawCircle(Color.White.copy(alpha = .35f * a * (1f - grow.value * .6f)), r * .16f, Offset(c.x - r * .45f, c.y - r * .5f))
    }
}
