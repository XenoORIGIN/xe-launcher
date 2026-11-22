package com.xenoorigin.xelauncher.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * What Liquid Glass shows through: a frosted copy of the wallpaper ([image]), where the wallpaper
 * currently sits ([parallaxPx]) and the size of the screen it covers ([screen]).
 * Lambdas so the draw phase, not composition, reads them.
 */
class GlassBackdrop(val image: () -> ImageBitmap?, val parallaxPx: () -> Offset, val screen: () -> Size)

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** True when the icon style is Liquid Glass: the dock, folders and widgets turn clear too. */
val LocalLiquidGlass = compositionLocalOf { false }

/** The part of the backdrop bitmap that lies behind [tile] (window pixels), using the wallpaper's own mapping. */
fun backdropSource(tile: Rect, bmp: IntSize, screen: Size, parallaxPx: Offset): Rect {
    val m = com.xenoorigin.xelauncher.home.wallMapping(bmp, screen, parallaxPx)
    return Rect((tile.left - m.offset.x) / m.scale, (tile.top - m.offset.y) / m.scale, (tile.right - m.offset.x) / m.scale, (tile.bottom - m.offset.y) / m.scale)
}

/**
 * Clear Liquid Glass: soft shadow, the frosted wallpaper behind this spot, a thin white film,
 * the content, then a thick bright rim catching light top-left and bottom-right.
 * Without a backdrop (nothing drawn yet) it falls back to a denser frosted film.
 */
fun Modifier.liquidGlass(shape: Shape, film: Float = .08f): Modifier = composed {
    val backdrop = LocalGlassBackdrop.current
    val style = LocalGlassStyle.current
    var at by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { at = it.positionInRoot() }
        .drawWithCache {
            val outline: Outline = shape.createOutline(size, layoutDirection, this)
            val clip = Path().apply { addOutline(outline) }
            val rimW = 2.4.dp.toPx()
            val tint = style.rimTint
            fun rimColor(a: Float) = (if (tint.alpha > 0f) lerp(Color.White, tint.copy(alpha = 1f), (tint.alpha * 2f).coerceAtMost(.7f)) else Color.White).copy(alpha = a)
            val rim = Brush.linearGradient(
                0f to rimColor(.95f), .28f to rimColor(.35f), .5f to rimColor(.10f), .78f to rimColor(.30f), 1f to rimColor(.75f),
                start = Offset.Zero, end = Offset(size.width, size.height),
            )
            val sheen = Brush.verticalGradient(0f to Color.White.copy(alpha = .22f), .45f to Color.Transparent, 1f to Color.White.copy(alpha = .06f))
            val shadowStep = 1.4.dp.toPx()
            onDrawWithContent {
                // Soft shadow under the block.
                for (i in 1..3) translate(0f, shadowStep * i) { drawOutline(outline, Color.Black.copy(alpha = .09f - .02f * i)) }
                val img = backdrop?.image()
                if (img != null) {
                    val src = backdropSource(Rect(at, size), IntSize(img.width, img.height), backdrop.screen(), backdrop.parallaxPx())
                    clipPath(clip) {
                        drawImage(
                            img,
                            srcOffset = IntOffset(src.left.roundToInt().coerceIn(0, img.width - 1), src.top.roundToInt().coerceIn(0, img.height - 1)),
                            srcSize = IntSize(src.width.roundToInt().coerceIn(1, img.width), src.height.roundToInt().coerceIn(1, img.height)),
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.Low,
                        )
                    }
                    drawOutline(outline, Color.White.copy(alpha = film))
                } else {
                    drawOutline(outline, Color.White.copy(alpha = .18f))
                }
                drawOutline(outline, sheen)
                drawContent()
                // Thick bevel: dark inner line for depth, then the bright rim.
                clipPath(clip) { drawOutline(outline, Color.Black.copy(alpha = .10f), style = Stroke(rimW * 2.2f)) }
                clipPath(clip) { drawOutline(outline, rim, style = Stroke(rimW * 2f)) }
            }
        }
}

/** A panel (dock, folder, widget) that turns clear when Liquid Glass is on, and is the usual glass otherwise. */
@Composable
fun LiquidSurface(shape: Shape, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    if (LocalLiquidGlass.current) Box(modifier.liquidGlass(shape, film = .10f), content = content)
    else GlassSurface(shape, modifier, content = content)
}
