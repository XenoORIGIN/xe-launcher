package com.xenoorigin.xelauncher.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassFilmAndRim
import com.xenoorigin.xelauncher.design.toShape

val LocalIconRepository = staticCompositionLocalOf<IconRepository?> { null }
val LocalGrayscaleContrast = staticCompositionLocalOf { 1.15f }

/** One app tile: icon in the chosen style plus optional label. */
@Composable
fun AppIcon(
    app: AppEntry,
    size: Dp,
    style: IconStyle,
    shape: IconShape,
    label: Boolean,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.White,
) {
    Column(modifier.width(size + 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        IconTile(app, size, style, shape)
        if (label) {
            Text(
                app.label, style = XeType.Label.copy(shadow = Shadow(Color.Black.copy(alpha = .45f), blurRadius = 6f)),
                color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun IconTile(app: AppEntry, size: Dp, style: IconStyle, shape: IconShape, modifier: Modifier = Modifier) {
    val repo = LocalIconRepository.current
    val contrast = LocalGrayscaleContrast.current
    val px = with(LocalDensity.current) { (size * if (style == IconStyle.ORIGINAL) 1f else 0.62f).roundToPx() }
    val icon by produceState<RenderedIcon?>(null, app.key, style, px, contrast) {
        value = repo?.icon(app.key, style, px, contrast)
    }
    val s = shape.toShape()
    Box(modifier.size(size)) {
        when (style) {
            IconStyle.ORIGINAL -> icon?.let { Image(it.bitmap, app.label, Modifier.fillMaxSize().clip(s)) }
            IconStyle.GLASS_COLOR -> {
                val c = Color(icon?.dominantColor ?: 0xFF8E8E93.toInt())
                Box(
                    Modifier.fillMaxSize().clip(s)
                        .background(Brush.linearGradient(listOf(lerp(c, Color.White, .35f), c, lerp(c, Color.Black, .35f))))
                        .glassFilmAndRim(s, LocalGlassStyle.current),
                    contentAlignment = Alignment.Center,
                ) { icon?.let { Image(it.bitmap, app.label, Modifier.size(size * .62f)) } }
            }
            IconStyle.GLASS_CLEAR, IconStyle.GLASS_DARK -> {
                val base = LocalGlassStyle.current
                val glass = if (style == IconStyle.GLASS_DARK) base.copy(tint = Color.Black.copy(alpha = .42f), dark = true) else base.copy(tint = Color.White.copy(alpha = .06f))
                GlassSurface(s, Modifier.fillMaxSize(), glass) {
                    icon?.let { Image(it.bitmap, app.label, Modifier.size(size * .62f).align(Alignment.Center)) }
                }
            }
        }
        if (app.isClone) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(20.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ti_copy), null, Modifier.padding(3.dp), tint = Color.Black) }
        }
    }
}
