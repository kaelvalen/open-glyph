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
    }

    // 0 = off, 1–255 = brightness level
    val pixels = IntArray(PIXEL_COUNT) { 0 }

    var brightness: Float = 1.0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var eraseMode: Boolean = false

    var onPixelsChanged: ((IntArray) -> Unit)? = null

    private val pixelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pixelOffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A1A1A") }

    private var cellSize = 0f
    private var lastPaintedIndex = -1

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = minOf(w, h).toFloat() / GRID_SIZE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val index = row * GRID_SIZE + col
                val left = col * cellSize
                val top = row * cellSize
                val v = pixels[index]
                if (v > 0) {
                    val scaled = (v * brightness).toInt().coerceIn(1, 255)
                    pixelPaint.color = Color.rgb(scaled, scaled, scaled)
                    canvas.drawRect(left + 1, top + 1, left + cellSize - 1, top + cellSize - 1, pixelPaint)
                } else {
                    canvas.drawRect(left + 1, top + 1, left + cellSize - 1, top + cellSize - 1, pixelOffPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { lastPaintedIndex = -1; paintAtPosition(event.x, event.y, toggle = true) }
            MotionEvent.ACTION_MOVE -> paintAtPosition(event.x, event.y, toggle = false)
            MotionEvent.ACTION_UP   -> onPixelsChanged?.invoke(pixels.clone())
        }
        return true
    }

    private fun paintAtPosition(x: Float, y: Float, toggle: Boolean) {
        val col = floor(x / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)
        val row = floor(y / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)
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
        pixels.fill(255)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun setPixels(newPixels: IntArray) {
        if (newPixels.size != PIXEL_COUNT) return
        newPixels.copyInto(pixels)
        invalidate()
    }

    fun invertAll() {
        for (i in pixels.indices) pixels[i] = if (pixels[i] > 0) 0 else 255
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }
}
