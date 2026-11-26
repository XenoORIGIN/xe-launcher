package com.xenoorigin.xelauncher.drawer

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.LensGlass
import com.xenoorigin.xelauncher.design.LocalGlassBackdrop
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.backdropSource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BarH = 66.dp
private val Bubble = 62.dp
private val Gap = 8.dp
private val LiftRoom = 46.dp

/**
 * The drawer's bottom bar as one piece of Liquid Glass, after the owner's reference clip: the category bar
 * and a round search bubble sit side by side and melt together. Press the bubble and it lifts out of the
 * bar, stretching a neck of glass that snaps; let go and search opens while the bubble drops back and merges
 * with a wobble. [lift] (0..1) is exposed for previews.
 */
@Composable
fun GooeyBar(onSearch: () -> Unit, modifier: Modifier = Modifier, lift: Float? = null, bar: @Composable (Modifier) -> Unit) {
    val d = LocalDensity.current
    val anim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val backdrop = LocalGlassBackdrop.current
    var at by remember { mutableStateOf(Rect.Zero) }
    val scheme = LocalXeScheme.current
    BoxWithConstraints(
        modifier.fillMaxWidth().height(BarH + LiftRoom)
            .onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) { b -> val r = b.boundsInRoot; at = Rect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat()) },
    ) {
        val wPx = with(d) { maxWidth.toPx() }; val hPx = with(d) { (BarH + LiftRoom).toPx() }
        val dp = d.density
        val barW = wPx - (Bubble + Gap).value * dp
        val barCy = hPx - BarH.value * dp / 2f
        val r = Bubble.value * dp / 2f
        val liftPx = (LiftRoom.value - 4f) * dp
        val l = lift ?: anim.value
        val q = (l * 40).roundToInt() // the glass is rebuilt in 40 steps of lift, not every frame
        val ql = q / 40f
        val bubbleCy = barCy - ql * liftPx
        val bubbleCx = wPx - r
        val melt = 34f * dp * (1f - .3f * ql)
        val sdf = { x: Float, y: Float ->
            LensGlass.smin(
                LensGlass.roundRect(x, y, barW / 2f, barCy, barW / 2f, BarH.value * dp / 2f, BarH.value * dp / 2f),
                LensGlass.circle(x, y, bubbleCx, bubbleCy, r), melt,
            )
        }
        // Magnify toward the bubble's centre inside it, and only up/down along the long bar.
        val center = { x: Float, y: Float ->
            if (LensGlass.circle(x, y, bubbleCx, bubbleCy, r) < LensGlass.roundRect(x, y, barW / 2f, barCy, barW / 2f, BarH.value * dp / 2f, BarH.value * dp / 2f)) bubbleCx to bubbleCy else x to barCy
        }
        val looks = remember(wPx.roundToInt(), hPx.roundToInt(), q) { LensGlass.buildOverlay(wPx.toInt(), hPx.toInt(), dp / 2f, dp, sdf, .05f) }
        val lens = remember(dp) { LensGlass.Lens(strength = 26f * dp, edge = 26f * dp, magnify = .22f, chroma = .8f) }
        // The lens is worked out off the main thread whenever the bar moves or the bubble lifts a step;
        // until it's ready the previous picture is drawn.
        val img = backdrop?.image()
        val pixels = remember(img) { img?.asAndroidBitmap()?.let { b -> IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) } } }
        var picture by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        val atKey = Rect((at.left / 6).roundToInt().toFloat(), (at.top / 6).roundToInt().toFloat(), 0f, 0f)
        androidx.compose.runtime.LaunchedEffect(img, atKey, q, wPx, lift) {
            val b = img?.asAndroidBitmap() ?: return@LaunchedEffect
            val px = pixels ?: return@LaunchedEffect
            val area = if (at.isEmpty) Rect(0f, 0f, wPx, hPx) else at
            val src = backdropSource(area, IntSize(b.width, b.height), backdrop.screen(), backdrop.parallaxPx())
            picture = kotlinx.coroutines.withContext(com.xenoorigin.xelauncher.design.lensDispatcher) {
                LensGlass.render(wPx.toInt(), hPx.toInt(), dp / 2f, px, b.width, b.height, RectF(src.left, src.top, src.right, src.bottom), sdf, center, lens, dp)
            }
        }
        Canvas(Modifier.size(maxWidth, BarH + LiftRoom).testTag("gooeyGlass")) {
            drawIntoCanvas { c ->
                val nc = c.nativeCanvas
                val (mask, over) = looks
                val p = picture
                val dst = RectF(0f, 0f, size.width, size.height)
                if (p != null) nc.drawBitmap(p, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                else nc.drawBitmap(mask, null, dst, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = PorterDuffColorFilter(0x2EFFFFFF, PorterDuff.Mode.SRC_IN) })
                nc.drawBitmap(over, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
        bar(Modifier.align(Alignment.BottomStart).width(maxWidth - Bubble - Gap).height(BarH))
        val search = stringResource(R.string.search)
        Box(
            Modifier.align(Alignment.BottomEnd).offset { IntOffset(0, -(l * liftPx).roundToInt()) }.size(Bubble)
                .testTag("gooeySearch")
                .semantics { role = Role.Button; contentDescription = search; onClick { onSearch(); true } }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        com.xenoorigin.xelauncher.sound.XeSound.play(com.xenoorigin.xelauncher.sound.XeSound.Kind.OPEN)
                        scope.launch { anim.animateTo(1f, spring(dampingRatio = .7f, stiffness = 420f)) }
                        val up = waitForUpOrCancellation()
                        if (up != null) onSearch()
                        scope.launch { anim.animateTo(0f, spring(dampingRatio = .38f, stiffness = 260f)) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(R.drawable.ti_search), null, Modifier.size(24.dp), tint = scheme.text) }
    }
}
