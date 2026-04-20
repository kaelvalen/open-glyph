package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityTextBinding
import com.kaelvalen.glyphmatrixdraw.glyph.ActiveState
import com.kaelvalen.glyphmatrixdraw.glyph.Anim
import com.kaelvalen.glyphmatrixdraw.glyph.AnimationStore
import com.kaelvalen.glyphmatrixdraw.glyph.BitmapFont
import com.kaelvalen.glyphmatrixdraw.glyph.PixelStore

/**
 * Compose and send bitmap-font text to the Glyph Matrix — either as a static
 * frame or as a scrolling marquee animation.
 */
class TextActivity : AppCompatActivity() {

    companion object { const val EXTRA_PIXELS = "pixels" }

    private lateinit var binding: ActivityTextBinding

    private enum class Mode { STATIC, SCROLL }
    private var mode: Mode = Mode.STATIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTextBack.setOnClickListener { finish() }
        binding.btnTextStatic.setOnClickListener { setMode(Mode.STATIC) }
        binding.btnTextScroll.setOnClickListener { setMode(Mode.SCROLL) }

        binding.etText.setText("HELLO")
        binding.etText.setOnEditorActionListener { _, id, _ -> id == EditorInfo.IME_ACTION_DONE }
        binding.etText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderPreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val ms = stepSpeed(progress)
                binding.tvSpeed.text = getString(R.string.speed_value_format, ms)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnTextApplyEditor.setOnClickListener {
            val pixels = currentStaticFrame()
            val intent = Intent().putExtra(EXTRA_PIXELS, pixels)
            setResult(RESULT_OK, intent)
            finish()
        }
        binding.btnTextSend.setOnClickListener {
            when (mode) {
                Mode.STATIC -> sendStatic()
                Mode.SCROLL -> sendScroll()
            }
        }

        setMode(Mode.STATIC)
    }

    private fun stepSpeed(progress: Int): Int = (50 + progress).coerceAtLeast(50)

    private fun setMode(newMode: Mode) {
        mode = newMode
        val staticSel = newMode == Mode.STATIC
        binding.btnTextStatic.background = ContextCompat.getDrawable(this, if (staticSel) R.drawable.chip_bg_selected else R.drawable.chip_bg)
        binding.btnTextStatic.setTextColor(if (staticSel) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
        binding.btnTextScroll.background = ContextCompat.getDrawable(this, if (!staticSel) R.drawable.chip_bg_selected else R.drawable.chip_bg)
        binding.btnTextScroll.setTextColor(if (!staticSel) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
        binding.btnTextSend.text = getString(if (staticSel) R.string.text_send_static else R.string.text_send_scroll)
        binding.speedRow.visibility = if (staticSel) android.view.View.GONE else android.view.View.VISIBLE
        renderPreview()
    }

    private fun renderPreview() {
        val frame = currentStaticFrame()
        binding.textPreview.setPixels(frame, pushToUndo = false)
    }

    private fun currentStaticFrame(): IntArray {
        val text = binding.etText.text.toString()
        return BitmapFont.renderStatic(text)
    }

    private fun sendStatic() {
        val pixels = currentStaticFrame()
        val brightness = 1.0f
        PixelStore.save(this, pixels, brightness)
        ActiveState.setStatic(this, null)
        Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show()
    }

    private fun sendScroll() {
        val text = binding.etText.text.toString().ifBlank { " " }
        val frames = BitmapFont.renderMarquee(text)
        val anim = Anim(
            id = AnimationStore.newId(),
            name = "Text: ${text.take(10)}",
            delayMs = stepSpeed(binding.seekSpeed.progress),
            frames = frames,
        )
        AnimationStore.save(this, anim)
        ActiveState.setAnimation(this, anim.id)
        Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show()
    }
}
