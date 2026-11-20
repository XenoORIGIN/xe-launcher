package com.xenoorigin.xelauncher.home

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeMotion
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.pressSquish
import kotlin.math.roundToInt

data class MenuAction(val icon: Int, val label: String, val run: () -> Unit)

/** Glass long-press menu that springs out of the icon: app shortcuts first, then actions. */
@Composable
fun IconMenu(
    bounds: Rect,
    shortcuts: List<ShortcutInfo>,
    shortcutIcon: (ShortcutInfo) -> Drawable?,
    onShortcut: (ShortcutInfo) -> Unit,
    actions: List<MenuAction>,
    onDismiss: () -> Unit,
) {
    val scheme = LocalXeScheme.current
    val speed = LocalAnimationSpeed.current
    val density = LocalDensity.current
    val conf = LocalConfiguration.current
    val screenW = with(density) { conf.screenWidthDp.dp.toPx() }
    val screenH = with(density) { conf.screenHeightDp.dp.toPx() }
    val panelW = with(density) { 272.dp.toPx() }
    var panelH by remember { mutableIntStateOf(0) }
    val above = bounds.center.y > screenH * .55f
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, XeMotion.spring(speed, dampingRatio = .62f, stiffness = 520f)) }
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f * anim.value.coerceIn(0f, 1f))).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss)) {
        val x = (bounds.center.x - panelW / 2).coerceIn(with(density) { 12.dp.toPx() }, screenW - panelW - with(density) { 12.dp.toPx() })
        val gap = with(density) { 10.dp.toPx() }
        val y = if (above) bounds.top - panelH - gap else bounds.bottom + gap
        val originX = ((bounds.center.x - x) / panelW).coerceIn(0f, 1f)
        GlassSurface(
            RoundedCornerShape(26.dp),
            Modifier.offset { IntOffset(x.roundToInt(), y.coerceAtLeast(0f).roundToInt()) }.width(272.dp)
                .onSizeChanged { panelH = it.height }
                .graphicsLayer {
                    val v = anim.value
                    scaleX = .6f + .4f * v; scaleY = .6f + .4f * v; alpha = v.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(originX, if (above) 1f else 0f)
                }
                .clickable(enabled = false) {},
            LocalGlassStyle.current.let { it.copy(tint = if (it.dark) Color(0xFF16161A).copy(alpha = .62f) else Color.White.copy(alpha = .6f)) },
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                shortcuts.forEach { s ->
                    val img by produceState<ImageBitmap?>(null, s) { value = runCatching { shortcutIcon(s)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull() }
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clickable { onShortcut(s) }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        img?.let { Image(it, null, Modifier.size(26.dp)) } ?: Box(Modifier.size(26.dp))
                        Text((s.shortLabel ?: s.longLabel ?: "").toString(), Modifier.padding(start = 14.dp), style = XeType.Body, color = scheme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 12.dp), color = scheme.textSecondary.copy(alpha = .25f))
                actions.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { a ->
                            val src = remember { MutableInteractionSource() }
                            Column(
                                Modifier.weight(1f).pressSquish(src).clickable(src, null) { a.run() }.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(painterResource(a.icon), a.label, Modifier.size(24.dp), tint = scheme.text)
                                Text(a.label, Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp), style = XeType.Label, color = scheme.text, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
