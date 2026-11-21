package com.xenoorigin.xelauncher.play

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.XeType
import kotlin.random.Random

/** Bubble Pop: 30 seconds to pop as many rising bubbles as you can. Gold ones are worth 5. Pixels throughout. */
class BubbleGameSim(private val w: Float, private val h: Float, private val unit: Float, seed: Int) {
    private val rnd = Random(seed)
    val bubbles = ArrayList<Bubble>()
    val pops = ArrayList<Ring>()
    var score = 0; private set
    var spawned = 0; private set
    var timeLeft = ROUND; private set
    val over: Boolean get() = timeLeft <= 0f
    private var nextSpawn = 0f

    fun step(dt: Float) {
        pops.forEach { it.age += dt }; pops.removeAll { it.age >= it.life }
        bubbles.forEach { it.age += dt; it.y -= it.rise * dt; it.x += kotlin.math.sin(it.age * 2.2f + it.phase) * it.r * .8f * dt }
        bubbles.removeAll { it.y < -it.r * 2 }
        if (over) return
        timeLeft = (timeLeft - dt).coerceAtLeast(0f)
        nextSpawn -= dt
        if (nextSpawn <= 0f && !over) {
            val progress = 1f - timeLeft / ROUND
            nextSpawn = .85f - .68f * progress
            val r = (20f + rnd.nextFloat() * 22f - progress * 8f) * unit
            val golden = rnd.nextFloat() < .08f
            bubbles += Bubble(r + rnd.nextFloat() * (w - 2 * r), h + r, r, (95f + progress * 190f + rnd.nextFloat() * 60f) * unit * (if (golden) 1.35f else 1f), rnd.nextFloat() * 6.28f, 99f, golden)
            spawned++
        }
    }

    fun tap(x: Float, y: Float): Boolean {
        if (over) return false
        val hit = bubbles.lastOrNull { val dx = it.x - x; val dy = it.y - y; dx * dx + dy * dy <= (it.r * 1.25f) * (it.r * 1.25f) } ?: return false
        bubbles.remove(hit)
        pops += Ring(hit.x, hit.y, hit.r * 2.4f, .35f)
        score += if (hit.golden) 5 else 1
        return true
    }

    companion object { const val ROUND = 30f }
}

/** The secret game, full screen. [best] is the record so far; [onRecord] is called with a new one. */
@Composable
fun BubbleGame(best: Int, onRecord: (Int) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val haptic = LocalHapticFeedback.current
    val unit = LocalDensity.current.density
    var round by remember { mutableIntStateOf(0) }
    var frame by remember { mutableIntStateOf(0) }
    var size by remember { mutableIntStateOf(0) }
    val sim = remember(round, size) { if (size == 0) null else BubbleGameSim((size shr 16).toFloat(), (size and 0xFFFF).toFloat(), unit, seed = round * 31 + 7) }
    val bump = remember(round) { Animatable(1f) }
    var recorded by remember(round) { mutableIntStateOf(-1) }
    val bestBefore = remember(round) { best }
    LaunchedEffect(sim) {
        if (sim == null) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { t -> sim.step(if (last == 0L) 0f else ((t - last) / 1e9f).coerceAtMost(.05f)); last = t; frame++ }
            if (sim.over && recorded < 0) { recorded = sim.score; if (sim.score > best) onRecord(sim.score) }
        }
    }
    Box(
        Modifier.fillMaxSize().onSizeChanged { size = (it.width shl 16) or it.height }.background(Brush.verticalGradient(listOf(Color(0xFF040A24), Color(0xFF0B2350), Color(0xFF0E4A7A)))).testTag("bubbleGame")
            .pointerInput(sim) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    if (sim?.tap(d.position.x, d.position.y) == true) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        frame++
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            frame
            val s = sim ?: return@Canvas
            s.bubbles.forEach { drawBubble(it.x, it.y, it.r, 1f, it.golden) }
            s.pops.forEach { drawRing(it, if (it.maxR > 0) Color(0xFFBFE6FF) else Color.White) }
        }
        val s = sim
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bubble Pop", style = XeType.Title, color = Color.White)
                    frame
                    Text(stringResource(R.string.game_best, maxOf(best, s?.score ?: 0)), style = XeType.Caption, color = Color.White.copy(alpha = .65f))
                }
                GlassIconButton(R.drawable.ti_x, stringResource(R.string.close), onClose)
            }
            frame
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${s?.score ?: 0}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("gameScore"))
                Text("${kotlin.math.ceil(s?.timeLeft ?: BubbleGameSim.ROUND).toInt()}s", color = Color.White.copy(alpha = .8f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (s != null && s.over) {
            LaunchedEffect(Unit) { bump.snapTo(.6f); bump.animateTo(1f, spring(dampingRatio = .45f, stiffness = 380f)) }
            Column(Modifier.align(Alignment.Center).graphicsLayer { scaleX = bump.value; scaleY = bump.value }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(if (s.score > bestBefore) R.string.game_record else R.string.game_over), style = XeType.Title, color = Color.White)
                Text("${s.score}", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
                GlassPillButton(stringResource(R.string.game_again), { round++ }, Modifier.padding(top = 12.dp))
            }
        }
    }
}
