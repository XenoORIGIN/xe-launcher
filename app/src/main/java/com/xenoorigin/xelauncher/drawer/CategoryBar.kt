package com.xenoorigin.xelauncher.drawer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import com.xenoorigin.xelauncher.design.glassTile
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.categories.CategoryWithApps
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeIcons
import com.xenoorigin.xelauncher.design.XeMotion
import com.xenoorigin.xelauncher.design.pressSquish
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val BarHeight = 66.dp
private val BubbleHeight = 50.dp
private val BarPadding = 8.dp
private val MinSlot = 50.dp

/**
 * Capsule glass bar with a liquid bubble that flows between categories. With many categories each slot
 * keeps a comfortable width and the bar scrolls sideways, keeping the selected category in view.
 * Tap picks a category, long-press opens the category editor, and dragging along the bar scrubs
 * through categories with the icons under the finger swelling up (when [magnify] is on).
 */
@Composable
fun CategoryBar(
    categories: List<CategoryWithApps>, state: DrawerState, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, onLongPress: () -> Unit = {}, magnify: Boolean = false,
) {
    val scheme = LocalXeScheme.current
    val speed = LocalAnimationSpeed.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(33.dp)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var scrubbing by remember { mutableStateOf(false) }
    val finger = remember { mutableFloatStateOf(0f) } // content x of the finger while scrubbing
    val swell = remember { Animatable(0f) }
    GlassSurface(shape, modifier.fillMaxWidth().height(BarHeight).testTag("categoryBar")) {
        BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = BarPadding).clip(shape)) {
            val n = categories.size.coerceAtLeast(1)
            val slot = maxOf(maxWidth / n, MinSlot)
            val viewport = maxWidth
            val slotPx = with(density) { slot.toPx() }
            val viewportPx = with(density) { viewport.toPx() }
            // Keep the selected category in view: again once the bar knows how far it can scroll, and
            // not while a finger is scrubbing (it scrolls the bar itself then).
            LaunchedEffect(n, slotPx, viewportPx) {
                snapshotFlow { Triple(state.pager.targetPage, scroll.maxValue, scrubbing) }.collectLatest { (t, max, scrub) ->
                    if (scrub) return@collectLatest
                    val target = (slotPx * t - (viewportPx - slotPx) / 2).toInt().coerceIn(0, max.coerceAtLeast(0))
                    if (scroll.value != target) scroll.animateScrollTo(target)
                }
            }
            Box(
                Modifier.fillMaxHeight().horizontalScroll(scroll, enabled = false)
                    .pointerInput(n, slotPx, viewportPx, magnify) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val tapped = (down.position.x / slotPx).toInt().coerceIn(0, n - 1)
                            val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull UP
                                    if (!c.pressed) return@withTimeoutOrNull UP
                                    if (abs(c.position.x - down.position.x) > viewConfiguration.touchSlop) return@withTimeoutOrNull DRAG
                                }
                                @Suppress("UNREACHABLE_CODE") UP
                            }
                            when (outcome) {
                                null -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress()
                                    do { val e = awaitPointerEvent(); e.changes.forEach { it.consume() } } while (e.changes.any { it.pressed })
                                }
                                UP -> { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(tapped) }
                                else -> {
                                    scrubbing = true
                                    if (magnify) scope.launch { swell.animateTo(1f, XeMotion.spring(speed, dampingRatio = .7f, stiffness = 500f)) }
                                    var last = state.pager.targetPage
                                    val edge = slotPx * .7f
                                    while (true) {
                                        val e = awaitPointerEvent()
                                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!c.pressed) break
                                        c.consume()
                                        // Near either end, the bar slides along under the finger.
                                        val inView = c.position.x - scroll.value
                                        val push = when {
                                            inView > viewportPx - edge -> (inView - (viewportPx - edge)) * .35f
                                            inView < edge -> -(edge - inView) * .35f
                                            else -> 0f
                                        }
                                        if (push != 0f) scope.launch { scroll.scrollTo((scroll.value + push).toInt().coerceIn(0, scroll.maxValue)) }
                                        finger.floatValue = c.position.x
                                        val idx = (c.position.x / slotPx).toInt().coerceIn(0, n - 1)
                                        if (idx != last) {
                                            last = idx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelect(idx)
                                        }
                                    }
                                    scrubbing = false
                                    scope.launch { swell.animateTo(0f, XeMotion.spring(speed, dampingRatio = .8f, stiffness = 380f)) }
                                }
                            }
                        }
                    },
            ) {
                Box(Modifier.width(slot * n).fillMaxHeight()) {
                    // Bubble: follows the pager continuously and stretches while travelling.
                    Box(
                        Modifier.align(Alignment.CenterStart)
                            .graphicsLayer {
                                val f = state.bubbleOffsetFraction.coerceIn(0f, (n - 1).toFloat())
                                val stretch = 1f + bubbleStretch(f)
                                translationX = (f * slot.toPx())
                                scaleX = stretch
                                scaleY = 1f - (stretch - 1f) * .25f
                            }
                            .width(slot).height(BubbleHeight)
                            .glassTile(RoundedCornerShape(25.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (LocalGlassStyle.current.dark) .22f else .55f)),
                    )
                    Row(Modifier.width(slot * n).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        categories.forEachIndexed { i, c ->
                            val selected = state.pager.targetPage == i
                            val scale by animateFloatAsState(if (selected) 1.12f else 1f, XeMotion.spring(speed, dampingRatio = .45f, stiffness = 500f), label = "s")
                            val tint by animateColorAsState(if (selected) scheme.text else scheme.text.copy(alpha = .62f), label = "t")
                            Box(
                                Modifier.width(slot).height(BubbleHeight).testTag("categoryButton")
                                    .semantics {
                                        contentDescription = c.title; this.selected = selected; role = Role.Tab
                                        onClick { onSelect(i); true }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painterResource(XeIcons.res(c.iconName)), null,
                                    Modifier.size(23.dp).graphicsLayer {
                                        val boost = swell.value * magnifyBoost(finger.floatValue, (i + .5f) * slotPx, slotPx)
                                        val s = scale * (1f + boost)
                                        scaleX = s; scaleY = s
                                        translationY = -boost * 9.dp.toPx()
                                    },
                                    tint = tint,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val UP = 1
private const val DRAG = 2

/** How much an icon centred at [center] swells with the finger at [finger]: up to 60%, fading over ~2 slots. */
fun magnifyBoost(finger: Float, center: Float, slot: Float): Float {
    val d = (abs(finger - center) / (slot * 1.9f)).coerceAtMost(1f)
    return .6f * ((cos(d * PI) + 1) / 2).toFloat()
}
