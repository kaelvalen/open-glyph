package com.kaelvalen.glyphmatrixdraw.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    enum class DrawMode { GRID, FREEHAND }

    val currentMode = MutableLiveData(DrawMode.GRID)
    val currentPixels = MutableLiveData(IntArray(625))
    val brightness = MutableLiveData(1.0f)
    val glyphReady = MutableLiveData(false)

    fun switchMode(mode: DrawMode) {
        if (currentMode.value != mode) currentMode.value = mode
    }

    fun updatePixels(pixels: IntArray) {
        currentPixels.value = pixels
    }

    fun updateBrightness(value: Float) {
        brightness.value = value.coerceIn(0f, 1f)
    }
}
