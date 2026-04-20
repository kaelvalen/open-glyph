package com.kaelvalen.glyphmatrixdraw.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
 * Browse procedural effects by category, live-preview the selected effect
 * in the 25×25 grid, tune frames/delay, and save as a reusable animation.
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

        buildCategories()

        binding.seekFxFrames.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvFxFrames.text = getString(R.string.frames_value_format, frames())
                if (fromUser) regenerateAndAutoPlay()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        binding.seekFxDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvFxDelay.text = getString(R.string.delay_ms_format, delayMs())
                if (fromUser && previewJob?.isActive == true) {
                    // restart to pick up new delay
                    startPreview()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnFxUseCurrent.setOnClickListener {
            useCurrentDrawing = !useCurrentDrawing
            highlightChip(binding.btnFxUseCurrent, useCurrentDrawing)
            regenerateAndAutoPlay()
        }
        binding.btnFxPingPong.setOnClickListener {
            pingPong = !pingPong
            highlightChip(binding.btnFxPingPong, pingPong)
            binding.btnFxPingPong.text = getString(if (pingPong) R.string.ping_pong_on else R.string.ping_pong)
            if (previewJob?.isActive == true) startPreview()
        }

        binding.btnFxPreview.setOnClickListener { togglePreview() }
        binding.btnFxSave.setOnClickListener { saveAsAnimation() }

        binding.tvFxFrames.text = getString(R.string.frames_value_format, frames())
        binding.tvFxDelay.text = getString(R.string.delay_ms_format, delayMs())

        selectEffect(EffectGenerator.Kind.BREATHING, autoPlay = true)
    }

    override fun onResume() {
        super.onResume()
        if (cachedFrames.isNotEmpty() && previewJob == null) startPreview()
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
    }

    // ─── Layout building ───────────────────────────────────────────────

    private fun buildCategories() {
        val density = resources.displayMetrics.density
        val container = binding.fxCategoryContainer
        container.removeAllViews()

        val groups = EffectGenerator.Kind.values().groupBy { it.category }
        val orderedCats = listOf(
            EffectGenerator.Category.AMBIENT to R.string.fx_cat_ambient,
            EffectGenerator.Category.RADIAL to R.string.fx_cat_radial,
            EffectGenerator.Category.GEOMETRIC to R.string.fx_cat_geometric,
            EffectGenerator.Category.PARTICLES to R.string.fx_cat_particles,
        )

        for ((cat, labelRes) in orderedCats) {
            val kinds = groups[cat].orEmpty()
            if (kinds.isEmpty()) continue

            // Category heading
            val heading = TextView(this).apply {
                text = getString(labelRes)
                setTextColor(0xFF666666.toInt())
                textSize = 9f
                typeface = Typeface.MONOSPACE
                letterSpacing = 0.25f
            }
            val hLp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = (10 * density).toInt()
                bottomMargin = (6 * density).toInt()
            }
            container.addView(heading, hLp)

            // Chip rows — max 4 per row, wrap after
            var row = newChipRow(density)
            container.addView(row)
            var inRow = 0
            for (kind in kinds) {
                if (inRow >= 4) {
                    row = newChipRow(density)
                    container.addView(row)
                    inRow = 0
                }
                val chip = makeChip(kind, density)
                val lp = LinearLayout.LayoutParams(0, (36 * density).toInt(), 1f).apply {
                    marginEnd = (6 * density).toInt()
                }
                row.addView(chip, lp)
                effectButtons[kind] = chip
                inRow++
            }
            // Pad the last row so weights don't stretch fewer chips awkwardly
            while (inRow in 1..3) {
                val spacer = View(this)
                val lp = LinearLayout.LayoutParams(0, (36 * density).toInt(), 1f).apply {
                    marginEnd = (6 * density).toInt()
                }
                row.addView(spacer, lp)
                inRow++
            }
        }
    }

    private fun newChipRow(density: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val bottomPad = (6 * density).toInt()
            setPadding(0, 0, 0, bottomPad)
        }
    }

    private fun makeChip(kind: EffectGenerator.Kind, density: Float): TextView {
        val label = getString(labelRes(kind))
        return TextView(this).apply {
            text = label
            textSize = 10f
            setTextColor(0xFFCCCCCC.toInt())
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.1f
            setPadding((8 * density).toInt(), 0, (8 * density).toInt(), 0)
            background = ContextCompat.getDrawable(this@EffectsActivity, R.drawable.tool_bg)
            setOnClickListener { selectEffect(kind, autoPlay = true) }
        }
    }

    private fun labelRes(kind: EffectGenerator.Kind): Int = when (kind) {
        EffectGenerator.Kind.BREATHING    -> R.string.fx_breathing
        EffectGenerator.Kind.PULSE        -> R.string.fx_pulse
        EffectGenerator.Kind.FADE         -> R.string.fx_fade
        EffectGenerator.Kind.SHIMMER      -> R.string.fx_shimmer
        EffectGenerator.Kind.PLASMA       -> R.string.fx_plasma
        EffectGenerator.Kind.WAVE         -> R.string.fx_wave
        EffectGenerator.Kind.RIPPLE       -> R.string.fx_ripple
        EffectGenerator.Kind.CLOCK_PULSE  -> R.string.fx_heartbeat
        EffectGenerator.Kind.ORBIT        -> R.string.fx_orbit
        EffectGenerator.Kind.LOADING      -> R.string.fx_loading
        EffectGenerator.Kind.SPIRAL       -> R.string.fx_spiral
        EffectGenerator.Kind.CHECKERBOARD -> R.string.fx_checkerboard
        EffectGenerator.Kind.SCANLINE     -> R.string.fx_scanline
        EffectGenerator.Kind.BOUNCE       -> R.string.fx_bounce
        EffectGenerator.Kind.STARFIELD    -> R.string.fx_starfield
        EffectGenerator.Kind.RAIN         -> R.string.fx_rain
        EffectGenerator.Kind.MATRIX_RAIN  -> R.string.fx_matrix_rain
        EffectGenerator.Kind.FIRE         -> R.string.fx_fire
        EffectGenerator.Kind.FIREWORK     -> R.string.fx_firework
        EffectGenerator.Kind.LIGHTNING    -> R.string.fx_lightning
    }

    private fun descRes(kind: EffectGenerator.Kind): Int = when (kind) {
        EffectGenerator.Kind.BREATHING    -> R.string.fx_desc_breathing
        EffectGenerator.Kind.PULSE        -> R.string.fx_desc_pulse
        EffectGenerator.Kind.FADE         -> R.string.fx_desc_fade
        EffectGenerator.Kind.SHIMMER      -> R.string.fx_desc_shimmer
        EffectGenerator.Kind.PLASMA       -> R.string.fx_desc_plasma
        EffectGenerator.Kind.WAVE         -> R.string.fx_desc_wave
        EffectGenerator.Kind.RIPPLE       -> R.string.fx_desc_ripple
        EffectGenerator.Kind.CLOCK_PULSE  -> R.string.fx_desc_heartbeat
        EffectGenerator.Kind.ORBIT        -> R.string.fx_desc_orbit
        EffectGenerator.Kind.LOADING      -> R.string.fx_desc_loading
        EffectGenerator.Kind.SPIRAL       -> R.string.fx_desc_spiral
        EffectGenerator.Kind.CHECKERBOARD -> R.string.fx_desc_checkerboard
        EffectGenerator.Kind.SCANLINE     -> R.string.fx_desc_scanline
        EffectGenerator.Kind.BOUNCE       -> R.string.fx_desc_bounce
        EffectGenerator.Kind.STARFIELD    -> R.string.fx_desc_starfield
        EffectGenerator.Kind.RAIN         -> R.string.fx_desc_rain
        EffectGenerator.Kind.MATRIX_RAIN  -> R.string.fx_desc_matrix_rain
        EffectGenerator.Kind.FIRE         -> R.string.fx_desc_fire
        EffectGenerator.Kind.FIREWORK     -> R.string.fx_desc_firework
        EffectGenerator.Kind.LIGHTNING    -> R.string.fx_desc_lightning
    }

    // ─── Behaviour ─────────────────────────────────────────────────────

    private fun selectEffect(kind: EffectGenerator.Kind, autoPlay: Boolean) {
        selectedKind = kind
        for ((k, view) in effectButtons) {
            val sel = k == kind
            view.background = ContextCompat.getDrawable(
                this, if (sel) R.drawable.tool_bg_selected else R.drawable.tool_bg,
            )
            view.setTextColor(if (sel) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
        }
        binding.tvFxDescription.setText(descRes(kind))
        regenerate()
        if (autoPlay) startPreview() else {
            cachedFrames.firstOrNull()?.let { binding.fxPreview.setPixels(it, pushToUndo = false) }
        }
    }

    private fun highlightChip(view: TextView, selected: Boolean) {
        view.background = ContextCompat.getDrawable(
            this, if (selected) R.drawable.chip_bg_selected else R.drawable.chip_bg,
        )
        view.setTextColor(if (selected) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
    }

    private fun frames(): Int = (binding.seekFxFrames.progress + 8).coerceIn(8, 64)
    private fun delayMs(): Int = (binding.seekFxDelay.progress + 40).coerceAtLeast(40)

    private fun regenerate() {
        val base = if (useCurrentDrawing) PixelStore.loadPixels(this) else null
        cachedFrames = EffectGenerator.generate(selectedKind, frames(), base)
    }

    private fun regenerateAndAutoPlay() {
        regenerate()
        if (previewJob?.isActive == true) {
            startPreview()
        } else {
            cachedFrames.firstOrNull()?.let { binding.fxPreview.setPixels(it, pushToUndo = false) }
        }
    }

    private fun togglePreview() {
        if (previewJob?.isActive == true) stopPreview() else startPreview()
    }

    private fun startPreview() {
        stopPreview()
        val frames = cachedFrames
        if (frames.isEmpty()) return
        binding.btnFxPreview.text = getString(R.string.fx_pause)
        val d = delayMs().toLong()
        val sequence = if (pingPong && frames.size > 2) {
            frames + frames.subList(1, frames.size - 1).reversed()
        } else frames
        previewJob = lifecycleScope.launch {
            var idx = 0
            while (isActive) {
                binding.fxPreview.setPixels(sequence[idx], pushToUndo = false)
                delay(d)
                idx = (idx + 1) % sequence.size
            }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        binding.btnFxPreview.text = getString(R.string.fx_preview)
    }

    private fun saveAsAnimation() {
        val frames = cachedFrames
        if (frames.isEmpty()) return
        val label = getString(labelRes(selectedKind))
        val anim = Anim(
            id = AnimationStore.newId(),
            name = "FX $label",
            delayMs = delayMs(),
            frames = frames,
            pingPong = pingPong,
        )
        AnimationStore.save(this, anim)
        ActiveState.setAnimation(this, anim.id)
        Toast.makeText(this, R.string.fx_saved, Toast.LENGTH_SHORT).show()
    }
}
