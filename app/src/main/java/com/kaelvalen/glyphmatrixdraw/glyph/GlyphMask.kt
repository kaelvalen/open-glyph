package com.kaelvalen.glyphmatrixdraw.glyph

/**
 * Circular active-pixel mask for the Nothing Phone (3) Glyph Matrix.
 * 25x25 grid, only the LEDs inside the circle are physically active.
 */
object GlyphMask {
    const val SIZE = 25
    const val COUNT = SIZE * SIZE

    @PublishedApi internal val ROW_RANGES = arrayOf(
        9..15, 7..17, 5..19, 4..20, 3..21,
        2..22, 2..22, 1..23, 1..23, 0..24,
        0..24, 0..24, 0..24, 0..24, 0..24,
        0..24, 1..23, 1..23, 2..22, 2..22,
        3..21, 4..20, 5..19, 7..17, 9..15
    )

    fun isActive(col: Int, row: Int): Boolean {
        if (row !in 0 until SIZE) return false
        return col in ROW_RANGES[row]
    }

    inline fun forEachActive(block: (col: Int, row: Int, index: Int) -> Unit) {
        for (row in 0 until SIZE) {
            val range = ROW_RANGES[row]
            for (col in range) block(col, row, row * SIZE + col)
        }
    }

    /** Zeroes inactive (off-circle) LEDs in-place. Returns same array. */
    fun clamp(pixels: IntArray): IntArray {
        for (row in 0 until SIZE) {
            val range = ROW_RANGES[row]
            for (col in 0 until SIZE) {
                if (col !in range) pixels[row * SIZE + col] = 0
            }
        }
        return pixels
    }

    /**
     * Convert an editor-space IntArray (length 625, values 0..255 per LED)
     * into the native matrix format expected by `GlyphMatrixManager.setMatrixFrame(int[])`.
     *
     * The Nothing Glyph service runs the matrix at 12-bit brightness (0..4095) per LED.
     * We scale the 8-bit source, multiply by the user brightness and clamp the
     * circular mask so out-of-bounds LEDs never light up. Sending this array
     * directly avoids the `convertToGlyphMatrix` bitmap path (which applies a
     * bilinear filter on scale=1.0 and causes sub-pixel bleed into neighbour LEDs).
     */
    fun toMatrixColors(pixels: IntArray, brightness: Float = 1f): IntArray {
        val scale = brightness.coerceIn(0f, 1f)
        val out = IntArray(COUNT)
        for (row in 0 until SIZE) {
            val range = ROW_RANGES[row]
            for (col in 0 until SIZE) {
                if (col !in range) continue
                val v = pixels[row * SIZE + col].coerceIn(0, 255)
                if (v == 0) continue
                // 0..255 -> 0..4095 (12-bit), then apply brightness
                val scaled = ((v * 4095) / 255 * scale).toInt().coerceIn(0, 4095)
                out[row * SIZE + col] = scaled
            }
        }
        return out
    }
}
