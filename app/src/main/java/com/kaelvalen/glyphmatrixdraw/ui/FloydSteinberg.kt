package com.kaelvalen.glyphmatrixdraw.ui

import android.graphics.Bitmap
import android.graphics.Color

object FloydSteinberg {
    /**
     * Binary dither — returns IntArray(w*h) with values 0 or 255.
     * Used by freehand canvas.
     */
    fun dither(bitmap: Bitmap, threshold: Float = 0.5f, brightness: Float = 1.0f): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        val gray = FloatArray(w * h) { i ->
            val p = bitmap.getPixel(i % w, i / w)
            val raw = (Color.red(p) + Color.green(p) + Color.blue(p)) / (3f * 255f)
            (raw * brightness).coerceIn(0f, 1f)
        }
        val result = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val old = gray[idx].coerceIn(0f, 1f)
                val new = if (old > threshold) 1f else 0f
                result[idx] = if (new == 1f) 255 else 0
                val err = old - new
                if (x + 1 < w) gray[idx + 1] = (gray[idx + 1] + err * 7f / 16f)
                if (y + 1 < h) {
                    if (x > 0) gray[idx + w - 1] = (gray[idx + w - 1] + err * 3f / 16f)
                    gray[idx + w] = (gray[idx + w] + err * 5f / 16f)
                    if (x + 1 < w) gray[idx + w + 1] = (gray[idx + w + 1] + err * 1f / 16f)
                }
            }
        }
        return result
    }

    /**
     * Grayscale — returns IntArray(w*h) with values 0–255 per pixel.
     * Used by image import for per-pixel LED brightness.
     */
    fun grayscale(bitmap: Bitmap, brightness: Float = 1.0f): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        return IntArray(w * h) { i ->
            val p = bitmap.getPixel(i % w, i / w)
            val raw = (Color.red(p) + Color.green(p) + Color.blue(p)) / (3f * 255f)
            ((raw * brightness).coerceIn(0f, 1f) * 255f).toInt()
        }
    }
}
