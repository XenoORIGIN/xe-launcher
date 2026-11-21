package com.xenoorigin.xestudio.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

/** Studio is black and white; the only real colour on screen is the user's own work. */
object Ink {
    val Black = Color(0xFF000000)
    val Night = Color(0xFF0E0E0E)
    val Card = Color(0xFF161616)
    val Line = Color(0xFF2A2A2A)
    val Mute = Color(0xFF8A8A8A)
    val Soft = Color(0xFFCFCFCF)
    val White = Color(0xFFFFFFFF)
}

object StudioType {
    val Display = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = Ink.White)
    val Title = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink.White)
    val Body = TextStyle(fontSize = 15.sp, color = Ink.Soft, lineHeight = 21.sp)
    val Label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink.Mute)
}

/** The springs every Studio animation uses, so motion feels like one system. */
object Motion {
    fun <T> settle() = spring<T>(dampingRatio = .78f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = .7f, stiffness = Spring.StiffnessMedium)
}

val LocalStudioHaze = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun StudioTheme(haze: HazeState? = null, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.White, onPrimary = Ink.Black, background = Ink.Black, onBackground = Ink.White,
            surface = Ink.Night, onSurface = Ink.White, outline = Ink.Line,
        ),
    ) { CompositionLocalProvider(LocalStudioHaze provides haze, content = content) }
}

/** Frosted glass in the style of Xe Launcher: blur of what's behind, a dark film and a thin light rim. */
fun Modifier.glassPanel(haze: HazeState?, shape: Shape): Modifier {
    var m = this.clip(shape)
    if (haze != null) {
        m = m.hazeEffect(
            state = haze,
            style = HazeStyle(backgroundColor = Ink.Black, tints = listOf(HazeTint(Color.Black.copy(alpha = .45f))), blurRadius = 22.dp),
        )
    } else m = m.then(Modifier.background(Ink.Night.copy(alpha = .92f)))
    return m.border(1.dp, Color.White.copy(alpha = .14f), shape)
}

/** Squeezes a control a little while it's pressed and springs it back on release. */
fun Modifier.pressSquish(amount: Float = .94f): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                scope.launch { scale.animateTo(amount, Motion.snappy()) }
                waitForUpOrCancellation()
                scope.launch { scale.animateTo(1f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMedium)) }
            }
        }
}
