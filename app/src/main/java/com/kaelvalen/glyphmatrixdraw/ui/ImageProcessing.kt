package com.kaelvalen.glyphmatrixdraw.ui

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Image-to-matrix pipeline: grayscale conversion, contrast/brightness adjustment,
 * rotation, and a choice of dithering algorithms.
 */
object ImageProcessing {

    enum class Dither(val label: String) {
        GRAYSCALE("Grayscale"),
        THRESHOLD("Threshold"),
        FLOYD_STEINBERG("Floyd–Steinberg"),
        ATKINSON("Atkinson"),
        BAYER("Bayer 4×4"),
    }

    /**
     * Produce a 25x25 IntArray (0..255) from [bitmap].
     * @param threshold used by THRESHOLD / *_DITHER as the cut point (0..1).
     * @param brightness linear gain applied before quantisation.
     * @param contrast 1.0 = neutral, >1.0 steeper, <1.0 flatter.
     * @param invert flip bright/dark.
     */
    fun process(
        bitmap: Bitmap,
        dither: Dither,
        threshold: Float,
        brightness: Float,
        contrast: Float,
        invert: Boolean,
    ): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        val gray = FloatArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val p = bitmap.getPixel(x, y)
            val g = (Color.red(p) + Color.green(p) + Color.blue(p)) / (3f * 255f)
            val adjusted = (((g - 0.5f) * contrast) + 0.5f) * brightness
            val clipped = adjusted.coerceIn(0f, 1f)
            gray[y * w + x] = if (invert) 1f - clipped else clipped
        }
        return when (dither) {
            Dither.GRAYSCALE -> gray.map { (it * 255f).toInt().coerceIn(0, 255) }.toIntArray()
            Dither.THRESHOLD -> IntArray(w * h) { i -> if (gray[i] > threshold) 255 else 0 }
            Dither.FLOYD_STEINBERG -> floyd(gray, w, h, threshold)
            Dither.ATKINSON -> atkinson(gray, w, h, threshold)
            Dither.BAYER -> bayer(gray, w, h, threshold)
        }
    }

    private fun floyd(gray: FloatArray, w: Int, h: Int, threshold: Float): IntArray {
        val g = gray.copyOf()
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val idx = y * w + x
            val old = g[idx]
            val new = if (old > threshold) 1f else 0f
            out[idx] = (new * 255f).toInt()
            val err = old - new
            if (x + 1 < w) g[idx + 1] += err * 7f / 16f
            if (y + 1 < h) {
                if (x > 0)     g[idx + w - 1] += err * 3f / 16f
                g[idx + w]     += err * 5f / 16f
                if (x + 1 < w) g[idx + w + 1] += err * 1f / 16f
            }
        }
        return out
    }

    private fun atkinson(gray: FloatArray, w: Int, h: Int, threshold: Float): IntArray {
        val g = gray.copyOf()
        val out = IntArray(w * h)
        val share = 1f / 8f
        for (y in 0 until h) for (x in 0 until w) {
            val idx = y * w + x
            val old = g[idx]
            val new = if (old > threshold) 1f else 0f
            out[idx] = (new * 255f).toInt()
            val err = old - new
            fun add(dx: Int, dy: Int) {
                val xx = x + dx; val yy = y + dy
                if (xx in 0 until w && yy in 0 until h) g[yy * w + xx] += err * share
            }
            add(1, 0); add(2, 0)
            add(-1, 1); add(0, 1); add(1, 1)
            add(0, 2)
        }
        return out
    }

    private val BAYER_4 = arrayOf(
        intArrayOf(0,  8,  2, 10),
        intArrayOf(12, 4, 14,  6),
        intArrayOf(3, 11,  1,  9),
        intArrayOf(15, 7, 13,  5),
    )

    private fun bayer(gray: FloatArray, w: Int, h: Int, threshold: Float): IntArray {
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val t = BAYER_4[y and 3][x and 3] / 16f
            val v = gray[y * w + x]
            out[y * w + x] = if (v > (threshold * 0.5f + t * 0.5f)) 255 else 0
        }
        return out
    }

    fun rotate90(bitmap: Bitmap): Bitmap {
        val m = android.graphics.Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }
}
