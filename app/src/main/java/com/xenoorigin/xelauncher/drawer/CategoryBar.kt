package com.xenoorigin.xelauncher.drawer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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

/** Capsule glass bar with a liquid bubble that flows between categories. */
@Composable
fun CategoryBar(categories: List<CategoryWithApps>, state: DrawerState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = LocalXeScheme.current
    val speed = LocalAnimationSpeed.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(33.dp)
    GlassSurface(shape, modifier.fillMaxWidth().height(BarHeight).testTag("categoryBar")) {
        BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = BarPadding).clip(shape)) {
            val n = categories.size.coerceAtLeast(1)
            val slot = maxWidth / n
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
                    .width(slot).height(BubbleHeight),
            ) {
                GlassSurface(
                    RoundedCornerShape(25.dp), Modifier.fillMaxWidth().height(BubbleHeight),
                    LocalGlassStyle.current.let { it.copy(tint = Color.White.copy(alpha = if (it.dark) .14f else .5f), refraction = it.refraction * .6f) },
                )
            }
            Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                categories.forEachIndexed { i, c ->
                    val selected = state.pager.targetPage == i
                    val scale by animateFloatAsState(if (selected) 1.12f else 1f, XeMotion.spring(speed, dampingRatio = .45f, stiffness = 500f), label = "s")
                    val tint by animateColorAsState(if (selected) scheme.text else scheme.text.copy(alpha = .62f), label = "t")
                    val src = remember { MutableInteractionSource() }
                    Box(
                        Modifier.weight(1f).height(BubbleHeight).testTag("categoryButton")
                            .semantics { contentDescription = c.title; this.selected = selected }
                            .pressSquish(src, haptics = false)
                            .clickable(src, null, role = Role.Tab) {
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
