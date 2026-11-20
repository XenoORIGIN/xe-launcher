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
import com.xenoorigin.xelauncher.design.glassTile
import androidx.compose.ui.draw.scale
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassFilmAndRim
import com.xenoorigin.xelauncher.design.toShape

val LocalIconRepository = staticCompositionLocalOf<IconRepository?> { null }
val LocalGrayscaleContrast = staticCompositionLocalOf { 1.15f }
/** The user's custom icon pictures (AppKey.encode() -> file); a change re-renders that tile. */
val LocalCustomIcons = staticCompositionLocalOf<Map<String, java.io.File>> { emptyMap() }
/** How large the symbol is drawn on its tile (1 = default). */
val LocalGlyphScale = staticCompositionLocalOf { 1f }

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
    val glyphScale = LocalGlyphScale.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val custom = LocalCustomIcons.current[app.key.encode()]
    val icon by produceState(repo?.peek(app.key, style, px, contrast), app.key, style, px, contrast, custom) {
        if (value == null) value = repo?.icon(app.key, style, px, contrast)
    }
    val s = shape.toShape()
    val base = LocalGlassStyle.current
    Box(modifier.size(size)) {
        val i = icon
        when {
            i != null && i.fillsTile -> Image(
                i.bitmap, app.label, Modifier.fillMaxSize().clip(s).glassFilmAndRim(s, base.copy(rimAlpha = base.rimAlpha * .5f)),
            )
            style == IconStyle.GLASS_COLOR -> {
                val c = Color(i?.dominantColor ?: 0xFF8E8E93.toInt())
                Box(
                    Modifier.fillMaxSize().clip(s)
                        .background(Brush.linearGradient(listOf(lerp(c, Color.White, .35f), c, lerp(c, Color.Black, .35f))))
                        .glassFilmAndRim(s, base),
                    contentAlignment = Alignment.Center,
                ) { i?.let { Image(it.bitmap, app.label, Modifier.fillMaxSize().scale(glyphScale)) } }
            }
            else -> {
                val fill = when (style) {
                    IconStyle.GLASS_CLEAR -> Color.White.copy(alpha = .10f)
                    else -> Color(0xFF1C1C1E).copy(alpha = .78f)
                }
                Box(Modifier.fillMaxSize().glassTile(s, base.copy(dark = true), fill), contentAlignment = Alignment.Center) {
                    i?.let { Image(it.bitmap, app.label, Modifier.fillMaxSize().scale(glyphScale)) }
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
