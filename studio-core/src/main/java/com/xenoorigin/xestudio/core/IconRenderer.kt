package com.xenoorigin.xestudio.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import kotlin.math.cos
import kotlin.math.sin

/**
 * Turns an [IconRecipe] and an app's own icon into the finished tile. Pure android.graphics, so it can
 * run off the main thread in both Xe Launcher and Xe Studio and give the same pixels in each.
 */
object IconRenderer {
    /**
     * @param appKey the app's AppKey.encode() string, used to find its override
     * @param label the app's name; its first letter is drawn when there is no icon
     * @param overrideImage loads an override picture by file name (null when missing)
     */
    fun render(
        recipe: IconRecipe,
        appKey: String,
        label: String,
        source: Drawable?,
        sizePx: Int,
        overrideImage: (String) -> Bitmap? = { null },
    ): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val s = sizePx.toFloat()
        // Leave room for the shadow so it isn't cut off.
        val inset = (recipe.shadow.coerceIn(0f, 1f) * s * .06f)
        val tile = s - inset * 2
        val path = tilePath(recipe.shape, tile).apply { offset(inset, inset) }

        if (recipe.shadow > 0f) {
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((90 * recipe.shadow.coerceIn(0f, 1f)).toInt(), 0, 0, 0) }
            canvas.save(); canvas.translate(0f, inset * .8f); canvas.drawPath(path, shadow); canvas.restore()
        }

        canvas.save()
        canvas.clipPath(path)
        when (val o = recipe.overrides[appKey]) {
            is IconOverride.Image -> {
                val img = overrideImage(o.file)
                if (img != null) drawCover(canvas, img, inset, tile)
                else drawStyled(canvas, recipe, label, source, inset, tile)
            }
            is IconOverride.Letter -> {
                fill(canvas, o.bg, inset, tile)
                letter(canvas, o.text, o.color, inset, tile)
            }
            null -> drawStyled(canvas, recipe, label, source, inset, tile)
        }
        canvas.restore()

        if (recipe.outlineWidth > 0f) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = recipe.outlineColor
                strokeWidth = recipe.outlineWidth * s / 48f
            }
            canvas.save(); canvas.clipPath(path); canvas.drawPath(path, stroke.apply { strokeWidth *= 2 }); canvas.restore()
        }
        return out
    }

    private fun drawStyled(c: Canvas, r: IconRecipe, label: String, source: Drawable?, inset: Float, tile: Float) {
        fill(c, r.background, inset, tile)
        if (source == null) {
            letter(c, label.trim().take(1).uppercase().ifEmpty { "?" }, r.tint, inset, tile)
            return
        }
        val glyph = tile * r.glyphScale.coerceIn(.2f, 1.2f)
        val left = inset + (tile - glyph) / 2f
        val g = Bitmap.createBitmap(glyph.toInt().coerceAtLeast(1), glyph.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val gc = Canvas(g)
        // Draw a private copy: the caller's drawable may be drawn by other renders at the same time.
        val own = source.constantState?.newDrawable()?.mutate() ?: source
        val d = if (r.treatment != Treatment.ORIGINAL && own is AdaptiveIconDrawable) own.foreground ?: own else own
        if (d is AdaptiveIconDrawable || (d !== own)) {
            // Adaptive layers are 108dp with the symbol in the middle 72dp: draw larger so the symbol fills the glyph box.
            val extra = g.width * .25f
            d.setBounds((-extra).toInt(), (-extra).toInt(), (g.width + extra).toInt(), (g.height + extra).toInt())
        } else d.setBounds(0, 0, g.width, g.height)
        d.draw(gc)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = filterFor(r) }
        c.drawBitmap(g, left, left, paint)
    }

    private fun filterFor(r: IconRecipe) = when (r.treatment) {
        Treatment.ORIGINAL -> null
        Treatment.MONO -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        Treatment.TINT -> PorterDuffColorFilter(r.tint, PorterDuff.Mode.SRC_IN)
        Treatment.DUOTONE -> {
            // Luminance l in 0..1 → dark colour + l × (light − dark), per channel.
            val lo = r.tint2; val hi = r.tint
            fun row(dark: Int, light: Int): FloatArray {
                val k = (light - dark) / 255f
                return floatArrayOf(.299f * k, .587f * k, .114f * k, 0f, dark.toFloat())
            }
            ColorMatrixColorFilter(
                ColorMatrix(
                    row(Color.red(lo), Color.red(hi)) + row(Color.green(lo), Color.green(hi)) +
                        row(Color.blue(lo), Color.blue(hi)) + floatArrayOf(0f, 0f, 0f, 1f, 0f),
                ),
            )
        }
    }

    private fun fill(c: Canvas, f: Fill, inset: Float, tile: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val colors = f.colors.ifEmpty { listOf(Color.BLACK) }
        if (colors.size == 1) p.color = colors[0]
        else {
            val a = Math.toRadians(f.angle.toDouble())
            val cx = inset + tile / 2; val dx = (cos(a) * tile / 2).toFloat(); val dy = (sin(a) * tile / 2).toFloat()
            p.shader = LinearGradient(cx - dx, cx - dy, cx + dx, cx + dy, colors.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        c.drawRect(inset, inset, inset + tile, inset + tile, p)
    }

    private fun letter(c: Canvas, text: String, color: Int, inset: Float, tile: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textAlign = Paint.Align.CENTER; textSize = tile * .5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val bounds = Rect().also { p.getTextBounds(text, 0, text.length, it) }
        c.drawText(text, inset + tile / 2f, inset + tile / 2f - bounds.exactCenterY(), p)
    }

    /** Scales [img] to cover the tile, cropping the longer side. */
    private fun drawCover(c: Canvas, img: Bitmap, inset: Float, tile: Float) {
        val scale = maxOf(tile / img.width, tile / img.height)
        val w = img.width * scale; val h = img.height * scale
        val l = inset + (tile - w) / 2f; val t = inset + (tile - h) / 2f
        c.drawBitmap(img, null, RectF(l, t, l + w, t + h), Paint(Paint.FILTER_BITMAP_FLAG))
    }
}
