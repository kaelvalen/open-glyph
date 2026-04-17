package com.kaelvalen.glyphmatrixdraw.glyph

import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager

class GlyphController(private val context: Context) {

    companion object {
        private const val TAG = "GlyphController"
        const val PIXEL_COUNT = 625
    }

    private var manager: GlyphMatrixManager? = null
    private var ready = false

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            try {
                manager?.register(Glyph.DEVICE_23112)
                ready = true
                Log.d(TAG, "Connected and registered")
            } catch (e: GlyphException) {
                Log.e(TAG, "register failed: ${e.message ?: "unknown"}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ready = false
            Log.d(TAG, "Disconnected")
        }
    }

    fun init() {
        try {
            manager = GlyphMatrixManager.getInstance(context).also { it.init(callback) }
        } catch (e: Exception) {
            Log.e(TAG, "init failed: ${e.message ?: "unknown"}")
        }
    }

    /** pixels: IntArray(625) with values 0–255 per LED. brightness scales all values. */
    fun sendFrame(pixels: IntArray, brightness: Float = 1.0f) {
        if (!ready) { Log.w(TAG, "not ready"); return }
        val arr = pixelsToColorArray(pixels, brightness)
        try { manager?.setMatrixFrame(arr) } catch (e: GlyphException) { Log.e(TAG, e.message ?: "unknown") }
    }

    fun clearMatrix() {
        if (!ready) return
        try { manager?.setMatrixFrame(IntArray(PIXEL_COUNT) { 0 }) } catch (e: GlyphException) { Log.e(TAG, e.message ?: "unknown") }
    }

    fun destroy() {
        ready = false
        try { manager?.unInit() } catch (e: Exception) { Log.e(TAG, e.message ?: "unknown") }
        manager = null
    }

    fun isReady() = ready
}

fun pixelsToColorArray(pixels: IntArray, brightness: Float): IntArray {
    val scale = brightness.coerceIn(0f, 1f)
    return IntArray(625) { i ->
        val v = (pixels[i].coerceIn(0, 255) * scale).toInt()
        if (v > 0) Color.rgb(v, v, v) else 0
    }
}
