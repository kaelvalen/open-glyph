package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.floor

class PixelGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val GRID_SIZE = 25
        const val PIXEL_COUNT = GRID_SIZE * GRID_SIZE

        // Circular active pixel mask — extracted from Nothing Phone (3) spec SVG
        // Each entry: (minCol, maxCol) inclusive for that row
        private val CIRCLE_MASK = arrayOf(
            9..15,   // row 0
            7..17,   // row 1
            5..19,   // row 2
            4..20,   // row 3
            3..21,   // row 4
            2..22,   // row 5
            2..22,   // row 6
            1..23,   // row 7
            1..23,   // row 8
            0..24,   // row 9
            0..24,   // row 10
            0..24,   // row 11
            0..24,   // row 12
            0..24,   // row 13
            0..24,   // row 14
            0..24,   // row 15
            1..23,   // row 16
            1..23,   // row 17
            2..22,   // row 18
            2..22,   // row 19
            3..21,   // row 20
            4..20,   // row 21
            5..19,   // row 22
            7..17,   // row 23
            9..15    // row 24
        )

        fun isActive(col: Int, row: Int): Boolean {
            if (row < 0 || row >= GRID_SIZE) return false
            return col in CIRCLE_MASK[row]
        }
    }

    val pixels = IntArray(PIXEL_COUNT) { 0 }

    var brightness: Float = 1.0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var eraseMode: Boolean = false
    var onPixelsChanged: ((IntArray) -> Unit)? = null

    private val pixelOnPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pixelOffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A1A1A") }
    private val pixelDeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0D0D0D") }

    private var cellSize = 0f
    private var lastPaintedIndex = -1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = minOf(w, h).toFloat() / GRID_SIZE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gap = 1f
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val index = row * GRID_SIZE + col
                val left = col * cellSize + gap
                val top  = row * cellSize + gap
                val right  = left + cellSize - gap * 2
                val bottom = top  + cellSize - gap * 2

                if (!isActive(col, row)) {
                    // Outside circle — very dark, no interaction
                    canvas.drawRect(left, top, right, bottom, pixelDeadPaint)
                } else {
                    val v = pixels[index]
                    if (v > 0) {
                        val scaled = (v * brightness).toInt().coerceIn(1, 255)
                        pixelOnPaint.color = Color.rgb(scaled, scaled, scaled)
                        canvas.drawRect(left, top, right, bottom, pixelOnPaint)
                    } else {
                        canvas.drawRect(left, top, right, bottom, pixelOffPaint)
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastPaintedIndex = -1
                paintAtPosition(event.x, event.y, toggle = true)
            }
            MotionEvent.ACTION_MOVE -> paintAtPosition(event.x, event.y, toggle = false)
            MotionEvent.ACTION_UP   -> {
                performClick()
                onPixelsChanged?.invoke(pixels.clone())
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun paintAtPosition(x: Float, y: Float, toggle: Boolean) {
        val col = floor(x / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)
        val row = floor(y / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)

        // Don't paint outside circle
        if (!isActive(col, row)) return

        val index = row * GRID_SIZE + col
        if (index == lastPaintedIndex) return
        lastPaintedIndex = index
        pixels[index] = when {
            toggle    -> if (pixels[index] > 0) 0 else 255
            eraseMode -> 0
            else      -> 255
        }
        invalidate()
    }

    fun clearAll() {
        pixels.fill(0)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun fillAll() {
        // Only fill active pixels
        for (row in 0 until GRID_SIZE)
            for (col in 0 until GRID_SIZE)
                if (isActive(col, row)) pixels[row * GRID_SIZE + col] = 255
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun setPixels(newPixels: IntArray) {
        if (newPixels.size != PIXEL_COUNT) return
        newPixels.copyInto(pixels)
        invalidate()
    }

    fun invertAll() {
        for (row in 0 until GRID_SIZE)
            for (col in 0 until GRID_SIZE)
                if (isActive(col, row)) {
                    val i = row * GRID_SIZE + col
                    pixels[i] = if (pixels[i] > 0) 0 else 255
                }
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }
}