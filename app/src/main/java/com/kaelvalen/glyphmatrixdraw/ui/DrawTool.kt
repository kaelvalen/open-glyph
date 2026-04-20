package com.kaelvalen.glyphmatrixdraw.ui

import com.kaelvalen.glyphmatrixdraw.glyph.GlyphMask
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

enum class DrawTool { PEN, ERASER, FILL, LINE, RECT, RECT_FILL, CIRCLE, CIRCLE_FILL, DROPPER }

enum class Symmetry { NONE, HORIZONTAL, VERTICAL, BOTH, QUAD }

object ShapePaint {

    /** Paint a single pixel and its mirrors under [symmetry]. */
    fun pen(pixels: IntArray, col: Int, row: Int, value: Int, symmetry: Symmetry) {
        emit(col, row, symmetry) { c, r -> put(pixels, c, r, value) }
    }

    /** Bresenham's line painting, mask-aware, symmetry-aware. */
    fun line(pixels: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, value: Int, symmetry: Symmetry) {
        var cx = x0; var cy = y0
        val dx = abs(x1 - cx); val dy = -abs(y1 - cy)
        val sx = if (cx < x1) 1 else -1
        val sy = if (cy < y1) 1 else -1
        var err = dx + dy
        while (true) {
            emit(cx, cy, symmetry) { c, r -> put(pixels, c, r, value) }
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; cx += sx }
            if (e2 <= dx) { err += dx; cy += sy }
        }
    }

    /** Axis-aligned rectangle outline. */
    fun rect(pixels: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, value: Int, symmetry: Symmetry) {
        val (a, b) = minMax(x0, x1); val (c, d) = minMax(y0, y1)
        for (x in a..b) { emit(x, c, symmetry) { cc, rr -> put(pixels, cc, rr, value) }; emit(x, d, symmetry) { cc, rr -> put(pixels, cc, rr, value) } }
        for (y in c..d) { emit(a, y, symmetry) { cc, rr -> put(pixels, cc, rr, value) }; emit(b, y, symmetry) { cc, rr -> put(pixels, cc, rr, value) } }
    }

    fun rectFill(pixels: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, value: Int, symmetry: Symmetry) {
        val (a, b) = minMax(x0, x1); val (c, d) = minMax(y0, y1)
        for (y in c..d) for (x in a..b) emit(x, y, symmetry) { cc, rr -> put(pixels, cc, rr, value) }
    }

    /** Midpoint-circle-style ellipse outline between two corners. */
    fun circle(pixels: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, value: Int, symmetry: Symmetry) {
        val (ax, bx) = minMax(x0, x1); val (ay, by) = minMax(y0, y1)
        val cx = (ax + bx) / 2f; val cy = (ay + by) / 2f
        val rx = (bx - ax) / 2f; val ry = (by - ay) / 2f
        if (rx <= 0f && ry <= 0f) { emit(ax, ay, symmetry) { c, r -> put(pixels, c, r, value) }; return }
        val steps = max(8, ((rx + ry) * 8).toInt())
        for (i in 0 until steps) {
            val t = (i.toDouble() / steps) * 2.0 * Math.PI
            val x = (cx + rx * kotlin.math.cos(t)).toInt()
            val y = (cy + ry * kotlin.math.sin(t)).toInt()
            emit(x, y, symmetry) { c, r -> put(pixels, c, r, value) }
        }
    }

    fun circleFill(pixels: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, value: Int, symmetry: Symmetry) {
        val (ax, bx) = minMax(x0, x1); val (ay, by) = minMax(y0, y1)
        val cx = (ax + bx) / 2f; val cy = (ay + by) / 2f
        val rx = (bx - ax) / 2f + 0.5f; val ry = (by - ay) / 2f + 0.5f
        val rxs = max(rx * rx, 0.25f); val rys = max(ry * ry, 0.25f)
        for (y in ay..by) for (x in ax..bx) {
            val nx = (x - cx); val ny = (y - cy)
            if ((nx * nx) / rxs + (ny * ny) / rys <= 1f) emit(x, y, symmetry) { c, r -> put(pixels, c, r, value) }
        }
    }

    /** 4-connected scanline flood fill that respects the circular mask. */
    fun flood(pixels: IntArray, startX: Int, startY: Int, value: Int) {
        if (!GlyphMask.isActive(startX, startY)) return
        val targetOn = pixels[startY * GlyphMask.SIZE + startX] > 0
        val newOn = value > 0
        if (targetOn == newOn) return
        val stack = ArrayDeque<Int>()
        stack.addLast(startY * GlyphMask.SIZE + startX)
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            val cx = idx % GlyphMask.SIZE
            val cy = idx / GlyphMask.SIZE
            if (!GlyphMask.isActive(cx, cy)) continue
            val on = pixels[idx] > 0
            if (on != targetOn) continue
            pixels[idx] = value
            if (cx > 0) stack.addLast(idx - 1)
            if (cx < GlyphMask.SIZE - 1) stack.addLast(idx + 1)
            if (cy > 0) stack.addLast(idx - GlyphMask.SIZE)
            if (cy < GlyphMask.SIZE - 1) stack.addLast(idx + GlyphMask.SIZE)
        }
    }

    private inline fun emit(col: Int, row: Int, symmetry: Symmetry, block: (Int, Int) -> Unit) {
        block(col, row)
        val mx = (GlyphMask.SIZE - 1) - col
        val my = (GlyphMask.SIZE - 1) - row
        when (symmetry) {
            Symmetry.NONE -> {}
            Symmetry.HORIZONTAL -> block(mx, row)
            Symmetry.VERTICAL   -> block(col, my)
            Symmetry.BOTH       -> { block(mx, row); block(col, my); block(mx, my) }
            Symmetry.QUAD       -> { block(mx, row); block(col, my); block(mx, my); block(row, col); block(my, col); block(row, mx); block(my, mx) }
        }
    }

    private inline fun put(pixels: IntArray, col: Int, row: Int, value: Int) {
        if (!GlyphMask.isActive(col, row)) return
        pixels[row * GlyphMask.SIZE + col] = value.coerceIn(0, 255)
    }

    private fun minMax(a: Int, b: Int) = if (a <= b) a to b else b to a
    @Suppress("UNUSED") private fun hypot2(dx: Float, dy: Float) = sqrt(dx * dx + dy * dy)
}
