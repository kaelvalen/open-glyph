package com.kaelvalen.glyphmatrixdraw.glyph

import android.content.Context

object PixelStore {
    private const val PREFS = "glyph_pixels"
    private const val KEY_PIXELS = "pixels"
    private const val KEY_BRIGHTNESS = "brightness"

    fun save(context: Context, pixels: IntArray, brightness: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PIXELS, pack(pixels))
            .putFloat(KEY_BRIGHTNESS, brightness)
            .apply()
    }

    fun loadPixels(context: Context): IntArray {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PIXELS, null) ?: return IntArray(625)
        return unpack(s)
    }

    fun loadBrightness(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_BRIGHTNESS, 1.0f)

    // Encoding: 2 hex chars per pixel (00–ff). Legacy 625-char binary strings also accepted.
    internal fun pack(pixels: IntArray): String =
        buildString(1250) { pixels.forEach { append("%02x".format(it.coerceIn(0, 255))) } }

    internal fun unpack(s: String): IntArray = when {
        s.length == 1250 -> IntArray(625) { i -> s.substring(i * 2, i * 2 + 2).toInt(16) }
        s.length == 625  -> IntArray(625) { i -> if (s[i] == '1') 255 else 0 } // legacy
        else             -> IntArray(625)
    }
}
