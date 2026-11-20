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

private val BarHeight = 66.dp
private val BubbleHeight = 50.dp
private val BarPadding = 8.dp
private val MinSlot = 50.dp

/**
 * Capsule glass bar with a liquid bubble that flows between categories. With many categories each slot
 * keeps a comfortable width and the bar scrolls sideways, following the selected category.
 * Long-press opens the category editor.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryBar(
    categories: List<CategoryWithApps>, state: DrawerState, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, onLongPress: () -> Unit = {},
) {
    val scheme = LocalXeScheme.current
    val speed = LocalAnimationSpeed.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(33.dp)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    GlassSurface(shape, modifier.fillMaxWidth().height(BarHeight).testTag("categoryBar")) {
        BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = BarPadding).clip(shape)) {
            val n = categories.size.coerceAtLeast(1)
            val slot = maxOf(maxWidth / n, MinSlot)
            val viewport = maxWidth
            // Keep the selected category in view.
            LaunchedEffect(state.pager.targetPage, n) {
                val target = with(density) { (slot * state.pager.targetPage - (viewport - slot) / 2).toPx() }.toInt()
                scroll.animateScrollTo(target.coerceIn(0, scroll.maxValue))
            }
            Box(Modifier.fillMaxHeight().horizontalScroll(scroll)) {
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
                            .glassTile(RoundedCornerShape(25.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (LocalGlassStyle.current.dark) .16f else .5f)),
                    )
                    Row(Modifier.width(slot * n).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        categories.forEachIndexed { i, c ->
                            val selected = state.pager.targetPage == i
                            val scale by animateFloatAsState(if (selected) 1.12f else 1f, XeMotion.spring(speed, dampingRatio = .45f, stiffness = 500f), label = "s")
                            val tint by animateColorAsState(if (selected) scheme.text else scheme.text.copy(alpha = .62f), label = "t")
                            val src = remember { MutableInteractionSource() }
                            Box(
                                Modifier.width(slot).height(BubbleHeight).testTag("categoryButton")
                                    .semantics { contentDescription = c.title; this.selected = selected }
                                    .pressSquish(src, haptics = false)
                                    .combinedClickable(src, null, role = Role.Tab, onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress()
                                    }) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(i)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(painterResource(XeIcons.res(c.iconName)), null, Modifier.size(23.dp).graphicsLayer { scaleX = scale; scaleY = scale }, tint = tint)
                            }
                        }
                    }
                }
            }
        }
    }
}
