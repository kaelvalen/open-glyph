package com.kaelvalen.glyphmatrixdraw.glyph

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GlyphDrawToyService : Service() {

    companion object {
        private const val TAG = "GlyphDrawToyService"
        private const val KEY_DATA = "data"
    }

    private var glyphMatrixManager: GlyphMatrixManager? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var animJob: Job? = null

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(p0: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                Log.d(TAG, "onServiceConnected")
                gmm.register(Glyph.DEVICE_23112)
                handleDisplay()
            }
        }
        override fun onServiceDisconnected(p0: ComponentName?) { animJob?.cancel() }
    }

    private val buttonHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    msg.data?.getString(KEY_DATA)?.let { value ->
                        Log.d(TAG, "Event: $value")
                        when (value) {
                            GlyphToy.EVENT_ACTION_DOWN -> handleDisplay()
                            GlyphToy.EVENT_CHANGE      -> cycleNextPattern()
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val messenger = Messenger(buttonHandler)

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        glyphMatrixManager = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManager?.init(gmmCallback)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        animJob?.cancel()
        serviceJob.cancel()
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        return false
    }

    private fun handleDisplay() {
        animJob?.cancel()
        when (ActiveState.getMode(applicationContext)) {
            ActiveState.MODE_ANIMATION -> {
                val id = ActiveState.getId(applicationContext) ?: return
                val anim = AnimationStore.get(applicationContext, id) ?: return
                val frames = anim.playbackFrames()
                if (frames.isEmpty()) return
                animJob = serviceScope.launch {
                    var idx = 0
                    while (isActive) {
                        pushPixels(frames[idx], 1.0f)
                        delay(anim.delayMs.toLong())
                        idx = (idx + 1) % frames.size
                    }
                }
            }
            else -> {
                pushPixels(
                    PixelStore.loadPixels(applicationContext),
                    PixelStore.loadBrightness(applicationContext)
                )
            }
        }
    }

    private fun cycleNextPattern() {
        val patterns = PatternStore.getAll(applicationContext)
        if (patterns.isEmpty()) { handleDisplay(); return }
        val next = (ActiveState.getPatternIndex(applicationContext) + 1) % patterns.size
        ActiveState.setPatternIndex(applicationContext, next)
        val p = patterns[next]
        ActiveState.setStatic(applicationContext, p.id)
        PixelStore.save(applicationContext, p.pixels, p.brightness)
        pushPixels(p.pixels, p.brightness)
        Log.d(TAG, "Cycled to pattern ${next + 1}/${patterns.size}: ${p.name}")
    }

    /**
     * Push a 25x25 pixel array straight to the Glyph Matrix.
     *
     * We bypass `GlyphMatrixFrame`/`GlyphMatrixObject` on purpose — those go
     * through `GlyphMatrixUtils.convertToGlyphMatrix`, which re-draws the
     * bitmap through a `Matrix` with bilinear filtering even at scale=1.0
     * and leaks brightness into neighbouring LEDs. Sending the raw IntArray
     * gives a 1:1 mapping between the editor grid and the physical LEDs.
     */
    private fun pushPixels(pixels: IntArray, brightness: Float) {
        val gmm = glyphMatrixManager ?: return
        val out = GlyphMask.toMatrixColors(pixels, brightness)
        try {
            gmm.setMatrixFrame(out)
        } catch (e: Exception) {
            Log.e(TAG, "setMatrixFrame failed: ${e.message}")
        }
    }
}
