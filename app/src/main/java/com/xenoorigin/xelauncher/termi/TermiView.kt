package com.xenoorigin.xelauncher.termi

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.wallpaper.ascii.AsciiField
import com.xenoorigin.xelauncher.wallpaper.ascii.AsciiGlyphs
import com.xenoorigin.xelauncher.wallpaper.ascii.AsciiRaster
import com.xenoorigin.xelauncher.wallpaper.ascii.AsciiScene
import com.xenoorigin.xelauncher.wallpaper.minuteNow
import com.xenoorigin.xelauncher.wallpaper.skyAt
import com.xenoorigin.xelauncher.wallpaper.skyLoopRuns
import com.xenoorigin.xelauncher.wallpaper.sunToday
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/** Counts frames drawn, for tests. */
internal object TermiViewProbe { @Volatile var frames = 0 }

/** Plays animations one after another, asking the director what's next. */
internal class TermiPlayer(private val director: AnimDirector = AnimDirector()) {
    var anim = TermiAnim.IDLE; private set
    private var startMs = 0L

    fun onEvent(e: TermiEvent, mood: Mood, now: Long) { anim = director.next(mood, e, now); startMs = now }

    /** The frame at [now]; moves on to the next animation when this one ends. */
    fun frame(now: Long, mood: Mood): Frame {
        if (startMs == 0L) startMs = now
        var t = (now - startMs) / (anim.seconds * 1000f)
        if (t >= 1f) { anim = director.next(mood, null, now); startMs = now; t = 0f }
        return TermiAnimations.frame(anim, t, ((now - startMs) / 66L).toInt())
    }
}

/** The ASCII scene behind Termi, redrawn into one bitmap. */
private class SceneFrame(val cols: Int, val rows: Int) {
    val field = AsciiField(cols, rows, seed = 5)
    val rw = cols * AsciiGlyphs.CW
    val rh = rows * AsciiGlyphs.CH
    private val px = IntArray(rw * rh)
    private val bmp = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
    val image = bmp.asImageBitmap()
    fun draw(t: Float, hint: SceneHint, ascii: AsciiColor, accent: Long) {
        val scene = when { hint.rain > .5f -> AsciiScene.RAIN; hint.dawn > .3f -> AsciiScene.DAWN; else -> AsciiScene.LAKE }
        field.fill(scene, t, if (scene == AsciiScene.LAKE) skyAt(minuteNow(), sunToday()) else null, ascii, accent)
        AsciiRaster.rasterize(field.cells, px)
        bmp.setPixels(px, 0, rw, 0, 0, rw, rh)
    }
}

/**
 * Termi on its ASCII lake: the scene fills the view, Termi stands on the shore. Both are drawn with whole
 * pixels and no smoothing. Moves about 12 times a second while Xe is visible and animations are on.
 * [event] (with a changing [eventKey]) starts the matching animation.
 */
@Composable
fun TermiView(
    stage: Stage, equipped: Map<Slot, Item>, mood: Mood, event: TermiEvent?, body: Int,
    ascii: AsciiColor, accent: Long, animate: Boolean, modifier: Modifier = Modifier, eventKey: Int = 0,
) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth; val h = constraints.maxHeight
        if (w <= 0 || h <= 0 || w == Int.MAX_VALUE || h == Int.MAX_VALUE) return@BoxWithConstraints
        // Scene glyph pixels: whole pixels, about 60 glyphs across at most.
        val k = max(1, (w / 300f).roundToInt())
        val cols = (w + 5 * k - 1) / (5 * k)
        val rows = (h + 8 * k - 1) / (8 * k)
        val scene = remember(cols, rows) { SceneFrame(cols, rows) }
        val player = remember { TermiPlayer() }
        var frame by remember { mutableStateOf(player.frame(System.currentTimeMillis(), mood)) }
        var tick by remember { mutableIntStateOf(0) }
        remember(scene, ascii, accent) { scene.draw(0f, frame.scene, ascii, accent); TermiViewProbe.frames++; 0 }
        LaunchedEffect(eventKey) { if (event != null) { player.onEvent(event, mood, System.currentTimeMillis()); frame = player.frame(System.currentTimeMillis(), mood); tick++ } }
        val life by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
        val running = skyLoopRuns(animate, life)
        LaunchedEffect(running, scene, ascii, accent) {
            if (!running) return@LaunchedEffect
            var t = 0f
            while (true) {
                delay(83); t += .083f
                frame = player.frame(System.currentTimeMillis(), mood)
                scene.draw(t, frame.scene, ascii, accent)
                TermiViewProbe.frames++
                tick++
            }
        }
        val sprite = TermiSprite.build(frame.pose, stage, equipped, body)
        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_EXPRESSION") tick
            drawRect(Color(AsciiRaster.GROUND))
            val ox = ((size.width - scene.rw * k) / 2f).roundToInt()
            val oy = ((size.height - scene.rh * k) / 2f).roundToInt()
            drawImage(scene.image, IntOffset.Zero, IntSize(scene.rw, scene.rh), IntOffset(ox, oy), IntSize(scene.rw * k, scene.rh * k), filterQuality = FilterQuality.None)
            if (frame.scene.dim > 0f) drawRect(Color.Black.copy(alpha = frame.scene.dim))
            // Termi: its 14-unit body about 35% of the width, feet on the scene's shore.
            val s = max(1, (size.width * .35f / 42f).roundToInt()).coerceAtMost(((size.height * .8f) / (TermiSprite.FEET_Y - 20)).toInt().coerceAtLeast(1))
            val groundY = oy + scene.field.groundRow * AsciiGlyphs.CH * k
            val feetY = minOf(groundY.toFloat(), size.height - 2f * s)
            val bx = (size.width / 2f - TermiSprite.FEET_X * s).roundToInt()
            val by = (feetY - TermiSprite.FEET_Y * s).roundToInt()
            for (p in sprite + frame.props) {
                drawRect(Color(p.argb), Offset((bx + p.x * s).toFloat(), (by + p.y * s).toFloat()), Size((p.w * s).toFloat(), (p.h * s).toFloat()))
            }
        }
    }
}
