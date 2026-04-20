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
}
