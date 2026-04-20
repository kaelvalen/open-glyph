package com.kaelvalen.glyphmatrixdraw.glyph

/**
 * Compact 3x5 bitmap font (uppercase, digits, common punctuation) for
 * rendering short strings to the 25x25 Glyph Matrix.
 *
 * Each glyph is 5 rows tall. Rows are encoded as 3-bit values, MSB = leftmost column.
 */
object BitmapFont {

    const val W = 3
    const val H = 5

    private val GLYPHS = mapOf(
        ' ' to intArrayOf(0, 0, 0, 0, 0),
        'A' to intArrayOf(0b010, 0b101, 0b111, 0b101, 0b101),
        'B' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b110),
        'C' to intArrayOf(0b011, 0b100, 0b100, 0b100, 0b011),
        'D' to intArrayOf(0b110, 0b101, 0b101, 0b101, 0b110),
        'E' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b111),
        'F' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b100),
        'G' to intArrayOf(0b011, 0b100, 0b101, 0b101, 0b011),
        'H' to intArrayOf(0b101, 0b101, 0b111, 0b101, 0b101),
        'I' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b111),
        'J' to intArrayOf(0b001, 0b001, 0b001, 0b101, 0b010),
        'K' to intArrayOf(0b101, 0b101, 0b110, 0b101, 0b101),
        'L' to intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111),
        'M' to intArrayOf(0b101, 0b111, 0b111, 0b101, 0b101),
        'N' to intArrayOf(0b101, 0b111, 0b111, 0b111, 0b101),
        'O' to intArrayOf(0b010, 0b101, 0b101, 0b101, 0b010),
        'P' to intArrayOf(0b110, 0b101, 0b110, 0b100, 0b100),
        'Q' to intArrayOf(0b010, 0b101, 0b101, 0b111, 0b011),
        'R' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b101),
        'S' to intArrayOf(0b011, 0b100, 0b010, 0b001, 0b110),
        'T' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b010),
        'U' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b011),
        'V' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b010),
        'W' to intArrayOf(0b101, 0b101, 0b111, 0b111, 0b101),
        'X' to intArrayOf(0b101, 0b101, 0b010, 0b101, 0b101),
        'Y' to intArrayOf(0b101, 0b101, 0b010, 0b010, 0b010),
        'Z' to intArrayOf(0b111, 0b001, 0b010, 0b100, 0b111),
        '0' to intArrayOf(0b010, 0b101, 0b101, 0b101, 0b010),
        '1' to intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        '2' to intArrayOf(0b110, 0b001, 0b010, 0b100, 0b111),
        '3' to intArrayOf(0b110, 0b001, 0b010, 0b001, 0b110),
        '4' to intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        '5' to intArrayOf(0b111, 0b100, 0b110, 0b001, 0b110),
        '6' to intArrayOf(0b011, 0b100, 0b111, 0b101, 0b011),
        '7' to intArrayOf(0b111, 0b001, 0b010, 0b010, 0b010),
        '8' to intArrayOf(0b010, 0b101, 0b010, 0b101, 0b010),
        '9' to intArrayOf(0b010, 0b101, 0b011, 0b001, 0b110),
        '.' to intArrayOf(0, 0, 0, 0, 0b010),
        ',' to intArrayOf(0, 0, 0, 0b010, 0b100),
        '!' to intArrayOf(0b010, 0b010, 0b010, 0, 0b010),
        '?' to intArrayOf(0b110, 0b001, 0b010, 0, 0b010),
        ':' to intArrayOf(0, 0b010, 0, 0b010, 0),
        '-' to intArrayOf(0, 0, 0b111, 0, 0),
        '+' to intArrayOf(0, 0b010, 0b111, 0b010, 0),
        '*' to intArrayOf(0b101, 0b010, 0b111, 0b010, 0b101),
        '/' to intArrayOf(0b001, 0b001, 0b010, 0b100, 0b100),
        '\\' to intArrayOf(0b100, 0b100, 0b010, 0b001, 0b001),
        '<' to intArrayOf(0b001, 0b010, 0b100, 0b010, 0b001),
        '>' to intArrayOf(0b100, 0b010, 0b001, 0b010, 0b100),
        '=' to intArrayOf(0, 0b111, 0, 0b111, 0),
        '(' to intArrayOf(0b001, 0b010, 0b010, 0b010, 0b001),
        ')' to intArrayOf(0b100, 0b010, 0b010, 0b010, 0b100),
        '[' to intArrayOf(0b011, 0b010, 0b010, 0b010, 0b011),
        ']' to intArrayOf(0b110, 0b010, 0b010, 0b010, 0b110),
        '#' to intArrayOf(0b101, 0b111, 0b101, 0b111, 0b101),
        '\'' to intArrayOf(0b010, 0b010, 0, 0, 0),
        '"' to intArrayOf(0b101, 0b101, 0, 0, 0),
        '&' to intArrayOf(0b010, 0b101, 0b010, 0b101, 0b011),
        '@' to intArrayOf(0b010, 0b101, 0b111, 0b100, 0b011),
    )

    /** Width in columns (including 1px spacing between characters) of a string. */
    fun measure(text: String): Int {
        if (text.isEmpty()) return 0
        return text.length * (W + 1) - 1
    }

    /**
     * Draws [text] onto a 625-pixel frame starting at (offsetX, offsetY).
     * Pixels outside the circular mask are clipped. On-pixels use [value].
     * Text is uppercased first; unknown characters render as space.
     */
    fun draw(pixels: IntArray, text: String, offsetX: Int, offsetY: Int, value: Int = 255) {
        val upper = text.uppercase()
        var x = offsetX
        for (ch in upper) {
            val glyph = GLYPHS[ch] ?: GLYPHS[' ']!!
            for (row in 0 until H) {
                val bits = glyph[row]
                for (col in 0 until W) {
                    val on = (bits shr (W - 1 - col)) and 1 == 1
                    if (on) {
                        val gx = x + col
                        val gy = offsetY + row
                        if (gy in 0 until GlyphMask.SIZE && gx in 0 until GlyphMask.SIZE && GlyphMask.isActive(gx, gy)) {
                            pixels[gy * GlyphMask.SIZE + gx] = value
                        }
                    }
                }
            }
            x += W + 1
        }
    }

    /** Static centered text into a single frame (value = LED brightness). */
    fun renderStatic(text: String, value: Int = 255): IntArray {
        val out = IntArray(GlyphMask.COUNT)
        val w = measure(text).coerceAtMost(GlyphMask.SIZE)
        val ox = (GlyphMask.SIZE - w) / 2
        val oy = (GlyphMask.SIZE - H) / 2
        draw(out, text, ox, oy, value)
        return out
    }

    /**
     * Produce horizontally-scrolling frames of [text] entering from right edge and
     * exiting left. Each frame advances by [stepPx] columns.
     */
    fun renderMarquee(text: String, stepPx: Int = 1, value: Int = 255): List<IntArray> {
        val cleaned = if (text.isBlank()) " " else text
        val textWidth = measure(cleaned)
        val oy = (GlyphMask.SIZE - H) / 2
        val startX = GlyphMask.SIZE
        val endX = -textWidth
        val frames = mutableListOf<IntArray>()
        var x = startX
        while (x >= endX) {
            val buf = IntArray(GlyphMask.COUNT)
            draw(buf, cleaned, x, oy, value)
            frames += buf
            x -= stepPx.coerceAtLeast(1)
        }
        return frames
    }
}
