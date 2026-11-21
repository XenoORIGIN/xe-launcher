package com.xenoorigin.xestudio.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Pictures the user brings in are shrunk and re-encoded so they stay small on the phone and in the cloud. */
object ImageImport {
    const val MAX_SIDE = 512
    const val MAX_BYTES = 1_000_000

    /** WebP, longest side at most [MAX_SIDE] px, at most [MAX_BYTES]. */
    fun normalize(src: Bitmap): ByteArray {
        val longest = maxOf(src.width, src.height)
        val scaled = if (longest > MAX_SIDE) {
            val k = MAX_SIDE.toFloat() / longest
            Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true)
        } else src
        var quality = 90
        while (true) {
            val out = ByteArrayOutputStream()
            @Suppress("DEPRECATION")
            val format = if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            scaled.compress(format, quality, out)
            if (out.size() <= MAX_BYTES || quality <= 20) return out.toByteArray()
            quality -= 15
        }
    }

    /** Decodes a picked picture without loading a huge photo at full size. */
    fun decode(context: Context, uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_SIDE * 2) {
                val k = MAX_SIDE * 2f / longest
                d.setTargetSize((info.size.width * k).toInt(), (info.size.height * k).toInt())
            }
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()
}
