package com.xenoorigin.xelauncher.icons

import com.xenoorigin.xelauncher.design.liquidGlass
import androidx.compose.ui.draw.alpha

import com.xenoorigin.xelauncher.design.iconLens
import com.xenoorigin.xelauncher.design.iconSheen
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloat
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
/** Version of the Xe Studio icon theme stored in Xe, or null when none was ever applied. */
val LocalStudioThemeVersion = staticCompositionLocalOf<Long?> { null }
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
    /** Drawn with the tile only (not the label): dust, sheen. */
    tileModifier: Modifier = Modifier,
) {
    Column(modifier.width(size + 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            IconTile(app, size, style, shape, tileModifier)
            com.xenoorigin.xelauncher.badges.UnreadBadge(
                com.xenoorigin.xelauncher.badges.badgeCountOf(app), size,
                Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-7).dp),
            )
        }
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
    val studioVersion = LocalStudioThemeVersion.current
    // Without a Studio theme, the Studio style looks exactly like the default.
    @Suppress("NAME_SHADOWING") val style = if (style == IconStyle.STUDIO && studioVersion == null) IconStyle.GLASS_DARK else style
    val studioKey = if (style == IconStyle.STUDIO) studioVersion else null
    val ready = remember(app.key, style, px, contrast, custom, studioKey) { repo?.peek(app.key, style, px, contrast) }
    val icon by produceState(ready, app.key, style, px, contrast, custom, studioKey) {
        if (value == null) value = repo?.icon(app.key, style, px, contrast)
    }
    // Icons that were already rendered appear at once; ones that had to load pop in.
    val appear = remember(app.key, style, px, custom, studioKey) { androidx.compose.animation.core.Animatable(if (ready != null) 1f else 0f) }
    LaunchedEffect(icon != null) {
        if (icon != null && appear.value < 1f) appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = .62f, stiffness = 420f))
    }
    val pop = Modifier.graphicsLayer {
        val v = appear.value
        alpha = v.coerceIn(0f, 1f); scaleX = .78f + .22f * v; scaleY = .78f + .22f * v
    }
    val s = shape.toShape()
    val base = LocalGlassStyle.current
    val glass = com.xenoorigin.xelauncher.design.LocalIconGlass.current
    val lens = if (glass == com.xenoorigin.xelauncher.settings.IconGlass.LENS)
        Modifier.iconLens(s) { com.xenoorigin.xelauncher.design.lensStrength(com.xenoorigin.xelauncher.design.ScrollFlow.velocity.floatValue) } else Modifier
    // Light slides across the glass as Home scrolls (and bends it, in Lens mode).
    Box(modifier.size(size).then(lens).iconSheen(s, glass)) {
        val i = icon
        when {
            i == null && (style == IconStyle.MONO || style == IconStyle.ORIGINAL || style == IconStyle.STUDIO) -> LoadingTile(s, base)
            // A Studio theme brings its own shape, colours and outline: draw it as made.
            i != null && style == IconStyle.STUDIO -> Image(i.bitmap, app.label, Modifier.fillMaxSize().then(pop))
            // Liquid Glass: a clear block of glass with the milky-white symbol floating in it.
            style == IconStyle.GLASS_CLEAR -> Box(Modifier.fillMaxSize().liquidGlass(s), contentAlignment = Alignment.Center) {
                if (i == null) Shimmer(s)
                else {
                    // A faint dark copy just below keeps white symbols readable on pale wallpapers.
                    Image(
                        i.bitmap, null,
                        Modifier.fillMaxSize().scale(glyphScale).offset(y = 1.dp).then(pop).alpha(.18f),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black),
                    )
                    Image(i.bitmap, app.label, Modifier.fillMaxSize().scale(glyphScale).then(pop).alpha(.85f))
                }
            }
            i != null && i.fillsTile -> Image(
                i.bitmap, app.label, Modifier.fillMaxSize().then(pop).clip(s).glassFilmAndRim(s, base.copy(rimAlpha = base.rimAlpha * .5f)),
            )
            style == IconStyle.GLASS_COLOR -> {
                val c = Color(i?.dominantColor ?: 0xFF8E8E93.toInt())
                Box(
                    Modifier.fillMaxSize().clip(s)
                        .background(Brush.linearGradient(listOf(lerp(c, Color.White, .35f), c, lerp(c, Color.Black, .35f))))
                        .glassFilmAndRim(s, base),
                    contentAlignment = Alignment.Center,
                ) { if (i == null) Shimmer(s) else Image(i.bitmap, app.label, Modifier.fillMaxSize().scale(glyphScale).then(pop)) }
            }
            else -> {
                val fill = when (style) {
                    else -> Color(0xFF1C1C1E).copy(alpha = .78f)
                }
                Box(Modifier.fillMaxSize().glassTile(s, base.copy(dark = true), fill), contentAlignment = Alignment.Center) {
                    if (i == null) Shimmer(s) else Image(i.bitmap, app.label, Modifier.fillMaxSize().scale(glyphScale).then(pop))
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

/** Empty glass tile with a moving sheen, shown while an icon is being prepared. */
@Composable
private fun LoadingTile(shape: androidx.compose.ui.graphics.Shape, base: com.xenoorigin.xelauncher.design.GlassStyle) {
    Box(Modifier.fillMaxSize().glassTile(shape, base.copy(dark = true), Color(0xFF1C1C1E).copy(alpha = .6f))) { Shimmer(shape) }
}

@Composable
private fun Shimmer(shape: androidx.compose.ui.graphics.Shape) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        -1f, 2f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.LinearEasing)),
        label = "x",
    )
    Box(
        Modifier.fillMaxSize().clip(shape).drawBehind {
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = .14f), Color.Transparent),
                    start = androidx.compose.ui.geometry.Offset(w * (x - .5f), 0f),
                    end = androidx.compose.ui.geometry.Offset(w * x, size.height),
                ),
            )
        },
    )
}
