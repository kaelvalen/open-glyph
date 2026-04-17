package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class FreehandCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val GRID_SIZE = 25
        const val STROKE_WIDTH = 24f
    }

    private var drawBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null
    private val drawPath = Path()

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint().apply {
        color = Color.argb(30, 255, 255, 255)
        strokeWidth = 1f
    }

    var onDrawingChanged: ((IntArray) -> Unit)? = null

    private var lastX = 0f
    private var lastY = 0f
    private var hasDrawing = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            drawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(drawBitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A1A"))
        drawBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.drawPath(drawPath, strokePaint)
        drawGridOverlay(canvas)
    }

    private fun drawGridOverlay(canvas: Canvas) {
        val cellW = width.toFloat() / GRID_SIZE
        val cellH = height.toFloat() / GRID_SIZE
        for (i in 1 until GRID_SIZE) {
            canvas.drawLine(i * cellW, 0f, i * cellW, height.toFloat(), gridPaint)
            canvas.drawLine(0f, i * cellH, width.toFloat(), i * cellH, gridPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(x, y)
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val midX = (lastX + x) / 2
                val midY = (lastY + y) / 2
                drawPath.quadTo(lastX, lastY, midX, midY)
                lastX = x; lastY = y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                drawPath.lineTo(x, y)
                drawCanvas?.drawPath(drawPath, strokePaint)
                drawPath.reset()
                hasDrawing = true
                invalidate()
                onDrawingChanged?.invoke(toPixelArray())
            }
        }
        return true
    }

    fun toPixelArray(): IntArray {
        if (!hasDrawing || width == 0 || height == 0) return IntArray(GRID_SIZE * GRID_SIZE)
        val temp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(temp)
        c.drawColor(Color.BLACK)
        drawBitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
        c.drawPath(drawPath, strokePaint)
        val scaled = Bitmap.createScaledBitmap(temp, GRID_SIZE, GRID_SIZE, true)
        temp.recycle()
        val result = IntArray(GRID_SIZE * GRID_SIZE)
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val pixel = scaled.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                result[y * GRID_SIZE + x] = brightness.coerceIn(0, 255)
            }
        }
        scaled.recycle()
        return result
    }

    fun clear() {
        drawPath.reset()
        hasDrawing = false
        drawBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
        onDrawingChanged?.invoke(IntArray(GRID_SIZE * GRID_SIZE))
    }
}
