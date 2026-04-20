package com.kaelvalen.glyphmatrixdraw.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityEffectsBinding
import com.kaelvalen.glyphmatrixdraw.glyph.ActiveState
import com.kaelvalen.glyphmatrixdraw.glyph.Anim
import com.kaelvalen.glyphmatrixdraw.glyph.AnimationStore
import com.kaelvalen.glyphmatrixdraw.glyph.EffectGenerator
import com.kaelvalen.glyphmatrixdraw.glyph.PixelStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pick a procedural effect, tweak its frame count/delay, preview it on the
 * local grid, then save it as a reusable animation.
 */
class EffectsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEffectsBinding

    private var selectedKind = EffectGenerator.Kind.BREATHING
    private val effectButtons = mutableMapOf<EffectGenerator.Kind, TextView>()
    private var useCurrentDrawing = false
    private var pingPong = false

    private var previewJob: Job? = null
    private var cachedFrames: List<IntArray> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEffectsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFxBack.setOnClickListener { finish() }
        buildEffectRow()
        selectEffect(EffectGenerator.Kind.BREATHING)

        binding.seekFxFrames.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvFxFrames.text = getString(R.string.frames_value_format, frames())
                regenerate()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        binding.seekFxDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvFxDelay.text = getString(R.string.delay_ms_format, delayMs())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnFxUseCurrent.setOnClickListener {
            useCurrentDrawing = !useCurrentDrawing
            highlightChip(binding.btnFxUseCurrent, useCurrentDrawing)
            regenerate()
        }
        binding.btnFxPingPong.setOnClickListener {
            pingPong = !pingPong
            highlightChip(binding.btnFxPingPong, pingPong)
            binding.btnFxPingPong.text = getString(if (pingPong) R.string.ping_pong_on else R.string.ping_pong)
        }

        binding.btnFxPreview.setOnClickListener { togglePreview() }
        binding.btnFxSave.setOnClickListener { saveAsAnimation() }

        binding.tvFxFrames.text = getString(R.string.frames_value_format, frames())
        binding.tvFxDelay.text = getString(R.string.delay_ms_format, delayMs())
    }

    override fun onPause() { super.onPause(); stopPreview() }

    private fun buildEffectRow() {
        val kinds = listOf(
            EffectGenerator.Kind.BREATHING to R.string.fx_breathing,
            EffectGenerator.Kind.PULSE to R.string.fx_pulse,
            EffectGenerator.Kind.WAVE to R.string.fx_wave,
            EffectGenerator.Kind.SHIMMER to R.string.fx_shimmer,
            EffectGenerator.Kind.SPIRAL to R.string.fx_spiral,
            EffectGenerator.Kind.STARFIELD to R.string.fx_starfield,
            EffectGenerator.Kind.RAIN to R.string.fx_rain,
            EffectGenerator.Kind.CLOCK_PULSE to R.string.fx_heartbeat,
        )
        val density = resources.displayMetrics.density
        for ((kind, labelRes) in kinds) {
            val tv = TextView(this).apply {
                text = getString(labelRes)
                textSize = 10f
                setTextColor(0xFFCCCCCC.toInt())
                gravity = Gravity.CENTER
                typeface = Typeface.MONOSPACE
                letterSpacing = 0.12f
                setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
                background = ContextCompat.getDrawable(this@EffectsActivity, R.drawable.tool_bg)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (36 * density).toInt()).apply {
                marginEnd = (6 * density).toInt()
            }
            binding.fxRow.addView(tv, lp)
            tv.setOnClickListener { selectEffect(kind) }
            effectButtons[kind] = tv
        }
    }

    private fun selectEffect(kind: EffectGenerator.Kind) {
        selectedKind = kind
        for ((k, view) in effectButtons) {
            val sel = k == kind
            view.background = ContextCompat.getDrawable(this, if (sel) R.drawable.tool_bg_selected else R.drawable.tool_bg)
            view.setTextColor(if (sel) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
        }
        regenerate()
    }

    private fun highlightChip(view: TextView, selected: Boolean) {
        view.background = ContextCompat.getDrawable(this, if (selected) R.drawable.chip_bg_selected else R.drawable.chip_bg)
        view.setTextColor(if (selected) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
    }

    private fun frames(): Int = (binding.seekFxFrames.progress + 8).coerceIn(8, 64)
    private fun delayMs(): Int = (binding.seekFxDelay.progress + 40).coerceAtLeast(40)

    private fun regenerate() {
        val base = if (useCurrentDrawing) PixelStore.loadPixels(this) else null
        cachedFrames = EffectGenerator.generate(selectedKind, frames(), base)
        if (cachedFrames.isNotEmpty()) binding.fxPreview.setPixels(cachedFrames[0], pushToUndo = false)
    }

    private fun togglePreview() {
        if (previewJob?.isActive == true) stopPreview() else startPreview()
    }

    private fun startPreview() {
        val frames = cachedFrames
        if (frames.isEmpty()) return
        val d = delayMs().toLong()
        previewJob = lifecycleScope.launch {
            var idx = 0
            val sequence = if (pingPong && frames.size > 2) frames + frames.subList(1, frames.size - 1).reversed() else frames
            while (isActive) {
                binding.fxPreview.setPixels(sequence[idx], pushToUndo = false)
                delay(d)
                idx = (idx + 1) % sequence.size
            }
        }
    }

    private fun stopPreview() { previewJob?.cancel(); previewJob = null }

    private fun saveAsAnimation() {
        val frames = cachedFrames
        if (frames.isEmpty()) return
        val kindName = selectedKind.name.lowercase().replaceFirstChar { it.uppercase() }
        val anim = Anim(
            id = AnimationStore.newId(),
            name = "FX $kindName",
            delayMs = delayMs(),
            frames = frames,
            pingPong = pingPong,
        )
        AnimationStore.save(this, anim)
        ActiveState.setAnimation(this, anim.id)
        Toast.makeText(this, R.string.fx_saved, Toast.LENGTH_SHORT).show()
    }
}
