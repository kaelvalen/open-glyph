package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kaelvalen.glyphmatrixdraw.glyph.GlyphMask
import kotlin.math.floor

/**
 * Interactive 25x25 pixel editor with selectable tools, symmetry and undo/redo.
 * All painting respects the circular Glyph Matrix mask.
 */
class PixelGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val GRID_SIZE = GlyphMask.SIZE
        const val PIXEL_COUNT = GlyphMask.COUNT

        @JvmStatic fun isActive(col: Int, row: Int) = GlyphMask.isActive(col, row)
    }

    val pixels = IntArray(PIXEL_COUNT) { 0 }

    var brightness: Float = 1.0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    /** Value written by the pen tool — allows grayscale brush painting. */
    var penValue: Int = 255
        set(value) { field = value.coerceIn(0, 255) }

    var tool: DrawTool = DrawTool.PEN
        set(value) { field = value; cancelShape(); invalidate() }

    /** Legacy toggle for XML bindings; equivalent to `tool = ERASER`. */
    var eraseMode: Boolean
        get() = tool == DrawTool.ERASER
        set(value) { tool = if (value) DrawTool.ERASER else DrawTool.PEN }

    var symmetry: Symmetry = Symmetry.NONE
        set(value) { field = value; invalidate() }

    /** Optional semi-transparent overlay used by the animation editor. */
    var onionPixels: IntArray? = null
        set(value) { field = value; invalidate() }

    var onPixelsChanged: ((IntArray) -> Unit)? = null
    var onPick: ((Int) -> Unit)? = null

    private val undoStack = UndoStack(maxDepth = 40)
    val canUndo get() = undoStack.canUndo
    val canRedo get() = undoStack.canRedo

    private val pixelOnPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pixelOffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A1A1A") }
    private val pixelDeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0D0D0D") }
    private val onionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 255, 0, 255) }
    private val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 255, 255, 255) }

    private var cellSize = 0f
    private var lastPaintedIndex = -1

    private var shapeStartCol = -1
    private var shapeStartRow = -1
    private var shapeCurCol = -1
    private var shapeCurRow = -1
    private var preShapeSnapshot: IntArray? = null

    init { undoStack.reset(pixels) }

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

                if (!GlyphMask.isActive(col, row)) {
                    canvas.drawRect(left, top, right, bottom, pixelDeadPaint)
                    continue
                }
                val v = pixels[index]
                if (v > 0) {
                    val scaled = (v * brightness).toInt().coerceIn(1, 255)
                    pixelOnPaint.color = Color.rgb(scaled, scaled, scaled)
                    canvas.drawRect(left, top, right, bottom, pixelOnPaint)
                } else {
                    canvas.drawRect(left, top, right, bottom, pixelOffPaint)
                }
                onionPixels?.let { o ->
                    if (v == 0 && o[index] > 0) {
                        onionPaint.alpha = (80 * (o[index] / 255f)).toInt().coerceIn(20, 160)
                        canvas.drawRect(left, top, right, bottom, onionPaint)
                    }
                }
            }
        }
        drawShapePreview(canvas, gap)
    }

    private fun drawShapePreview(canvas: Canvas, gap: Float) {
        val s = shapeStartCol
        val t = shapeStartRow
        val u = shapeCurCol
        val v = shapeCurRow
        if (s < 0 || u < 0) return
        val preview = IntArray(PIXEL_COUNT)
        when (tool) {
            DrawTool.LINE -> ShapePaint.line(preview, s, t, u, v, 255, symmetry)
            DrawTool.RECT -> ShapePaint.rect(preview, s, t, u, v, 255, symmetry)
            DrawTool.RECT_FILL -> ShapePaint.rectFill(preview, s, t, u, v, 255, symmetry)
            DrawTool.CIRCLE -> ShapePaint.circle(preview, s, t, u, v, 255, symmetry)
            DrawTool.CIRCLE_FILL -> ShapePaint.circleFill(preview, s, t, u, v, 255, symmetry)
            else -> return
        }
        for (row in 0 until GRID_SIZE) for (col in 0 until GRID_SIZE) {
            if (preview[row * GRID_SIZE + col] == 0) continue
            if (!GlyphMask.isActive(col, row)) continue
            val left = col * cellSize + gap
            val top  = row * cellSize + gap
            val right  = left + cellSize - gap * 2
            val bottom = top  + cellSize - gap * 2
            canvas.drawRect(left, top, right, bottom, previewPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val col = floor(event.x / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)
        val row = floor(event.y / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onDown(col, row)
            MotionEvent.ACTION_MOVE -> onMove(col, row)
            MotionEvent.ACTION_UP   -> { performClick(); onUp(col, row) }
        }
        return true
    }

    private fun onDown(col: Int, row: Int) {
        lastPaintedIndex = -1
        when (tool) {
            DrawTool.DROPPER -> {
                if (GlyphMask.isActive(col, row)) onPick?.invoke(pixels[row * GRID_SIZE + col])
                return
            }
            DrawTool.FILL -> {
                ShapePaint.flood(pixels, col, row, if (penValue == 0) 0 else penValue)
                undoStack.commit(pixels)
                invalidate()
                onPixelsChanged?.invoke(pixels.clone())
            }
            DrawTool.PEN, DrawTool.ERASER -> {
                paintDot(col, row, toggle = true)
            }
            DrawTool.LINE, DrawTool.RECT, DrawTool.RECT_FILL, DrawTool.CIRCLE, DrawTool.CIRCLE_FILL -> {
                preShapeSnapshot = pixels.clone()
                shapeStartCol = col; shapeStartRow = row; shapeCurCol = col; shapeCurRow = row
                invalidate()
            }
        }
    }

    private fun onMove(col: Int, row: Int) {
        when (tool) {
            DrawTool.PEN, DrawTool.ERASER -> paintDot(col, row, toggle = false)
            DrawTool.LINE, DrawTool.RECT, DrawTool.RECT_FILL, DrawTool.CIRCLE, DrawTool.CIRCLE_FILL -> {
                if (col == shapeCurCol && row == shapeCurRow) return
                shapeCurCol = col; shapeCurRow = row
                invalidate()
            }
            else -> Unit
        }
    }

    private fun onUp(col: Int, row: Int) {
        when (tool) {
            DrawTool.PEN, DrawTool.ERASER -> {
                undoStack.commit(pixels)
                onPixelsChanged?.invoke(pixels.clone())
            }
            DrawTool.LINE, DrawTool.RECT, DrawTool.RECT_FILL, DrawTool.CIRCLE, DrawTool.CIRCLE_FILL -> commitShape()
            else -> Unit
        }
    }

    private fun commitShape() {
        val snap = preShapeSnapshot ?: return
        snap.copyInto(pixels)
        val v = if (tool == DrawTool.ERASER) 0 else penValue
        val s = shapeStartCol; val t = shapeStartRow; val u = shapeCurCol; val x = shapeCurRow
        when (tool) {
            DrawTool.LINE -> ShapePaint.line(pixels, s, t, u, x, v, symmetry)
            DrawTool.RECT -> ShapePaint.rect(pixels, s, t, u, x, v, symmetry)
            DrawTool.RECT_FILL -> ShapePaint.rectFill(pixels, s, t, u, x, v, symmetry)
            DrawTool.CIRCLE -> ShapePaint.circle(pixels, s, t, u, x, v, symmetry)
            DrawTool.CIRCLE_FILL -> ShapePaint.circleFill(pixels, s, t, u, x, v, symmetry)
            else -> Unit
        }
        cancelShape()
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    private fun cancelShape() {
        preShapeSnapshot = null
        shapeStartCol = -1; shapeStartRow = -1; shapeCurCol = -1; shapeCurRow = -1
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun paintDot(col: Int, row: Int, toggle: Boolean) {
        if (!GlyphMask.isActive(col, row)) return
        val index = row * GRID_SIZE + col
        if (index == lastPaintedIndex) return
        lastPaintedIndex = index
        val newVal = when {
            toggle && tool == DrawTool.PEN -> if (pixels[index] > 0) 0 else penValue
            tool == DrawTool.ERASER -> 0
            else -> penValue
        }
        val value = newVal
        ShapePaint.pen(pixels, col, row, value, symmetry)
        invalidate()
    }

    fun clearAll() {
        pixels.fill(0)
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun fillAll() {
        GlyphMask.forEachActive { _, _, i -> pixels[i] = penValue.coerceAtLeast(1) }
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun invertAll() {
        GlyphMask.forEachActive { _, _, i -> pixels[i] = if (pixels[i] > 0) 0 else penValue.coerceAtLeast(1) }
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun rotateCW() {
        val snap = pixels.copyOf()
        pixels.fill(0)
        for (r in 0 until GRID_SIZE) for (c in 0 until GRID_SIZE) {
            val v = snap[r * GRID_SIZE + c]
            if (v == 0) continue
            val nr = c
            val nc = (GRID_SIZE - 1) - r
            if (GlyphMask.isActive(nc, nr)) pixels[nr * GRID_SIZE + nc] = v
        }
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun flipH() {
        val snap = pixels.copyOf()
        for (r in 0 until GRID_SIZE) for (c in 0 until GRID_SIZE) {
            val mc = (GRID_SIZE - 1) - c
            pixels[r * GRID_SIZE + c] = snap[r * GRID_SIZE + mc]
        }
        GlyphMask.clamp(pixels)
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun flipV() {
        val snap = pixels.copyOf()
        for (r in 0 until GRID_SIZE) for (c in 0 until GRID_SIZE) {
            val mr = (GRID_SIZE - 1) - r
            pixels[r * GRID_SIZE + c] = snap[mr * GRID_SIZE + c]
        }
        GlyphMask.clamp(pixels)
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun shift(dx: Int, dy: Int) {
        val snap = pixels.copyOf()
        pixels.fill(0)
        for (r in 0 until GRID_SIZE) for (c in 0 until GRID_SIZE) {
            val v = snap[r * GRID_SIZE + c]
            if (v == 0) continue
            val nr = r + dy
            val nc = c + dx
            if (nr in 0 until GRID_SIZE && nc in 0 until GRID_SIZE && GlyphMask.isActive(nc, nr)) {
                pixels[nr * GRID_SIZE + nc] = v
            }
        }
        undoStack.commit(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
    }

    fun setPixels(newPixels: IntArray, pushToUndo: Boolean = true) {
        if (newPixels.size != PIXEL_COUNT) return
        newPixels.copyInto(pixels)
        if (pushToUndo) undoStack.commit(pixels) else undoStack.reset(pixels)
        invalidate()
    }

    fun undo(): Boolean {
        val s = undoStack.undo() ?: return false
        s.copyInto(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
        return true
    }

    fun redo(): Boolean {
        val s = undoStack.redo() ?: return false
        s.copyInto(pixels)
        invalidate()
        onPixelsChanged?.invoke(pixels.clone())
        return true
    }
}
