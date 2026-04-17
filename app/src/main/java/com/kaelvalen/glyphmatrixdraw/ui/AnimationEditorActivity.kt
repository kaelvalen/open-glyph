package com.kaelvalen.glyphmatrixdraw.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityAnimationEditorBinding
import com.kaelvalen.glyphmatrixdraw.glyph.ActiveState
import com.kaelvalen.glyphmatrixdraw.glyph.Anim
import com.kaelvalen.glyphmatrixdraw.glyph.AnimationStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnimationEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimationEditorBinding
    private lateinit var frameAdapter: FrameAdapter

    private var currentAnimId: String = AnimationStore.newId()
    private var previewJob: Job? = null
    private var isPlaying = false

    private fun seekToDelay(progress: Int): Int = 50 + progress * 10
    private fun delayToSeek(ms: Int): Int = ((ms - 50) / 10).coerceIn(0, 195)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimationEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        frameAdapter = FrameAdapter { index -> selectFrame(index) }

        binding.rvFrames.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFrames.adapter = frameAdapter

        frameAdapter.submit(listOf(IntArray(625)))

        binding.animPixelGrid.onPixelsChanged = { pixels ->
            frameAdapter.updateFrame(frameAdapter.selectedIndex, pixels)
        }

        binding.seekDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                binding.tvDelayMs.text = "${seekToDelay(p)}ms"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnAddFrame.setOnClickListener { addFrame() }
        binding.btnDupFrame.setOnClickListener { dupFrame() }
        binding.btnDeleteFrame.setOnClickListener { deleteFrame() }
        binding.btnPlayAnim.setOnClickListener { togglePlay() }
        binding.btnSendAnim.setOnClickListener { sendToGlyph() }
        binding.btnAnimBack.setOnClickListener { finish() }

        binding.etAnimName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { binding.etAnimName.clearFocus(); true } else false
        }
    }

    override fun onPause() { super.onPause(); stopPreview() }

    private fun selectFrame(index: Int) {
        stopPreview()
        frameAdapter.select(index)
        binding.animPixelGrid.setPixels(frameAdapter.getFrames()[index])
    }

    private fun addFrame() {
        val frames = frameAdapter.getFrames().toMutableList()
        frames.add(IntArray(625))
        frameAdapter.submit(frames, frames.size - 1)
        binding.animPixelGrid.setPixels(IntArray(625))
        binding.rvFrames.smoothScrollToPosition(frames.size - 1)
    }

    private fun dupFrame() {
        val frames = frameAdapter.getFrames().toMutableList()
        val copy = frames[frameAdapter.selectedIndex].clone()
        val insertAt = frameAdapter.selectedIndex + 1
        frames.add(insertAt, copy)
        frameAdapter.submit(frames, insertAt)
        binding.rvFrames.smoothScrollToPosition(insertAt)
    }

    private fun deleteFrame() {
        val frames = frameAdapter.getFrames().toMutableList()
        if (frames.size <= 1) { Toast.makeText(this, "En az 1 kare olmalı", Toast.LENGTH_SHORT).show(); return }
        frames.removeAt(frameAdapter.selectedIndex)
        val newIdx = (frameAdapter.selectedIndex - 1).coerceAtLeast(0)
        frameAdapter.submit(frames, newIdx)
        binding.animPixelGrid.setPixels(frames[newIdx])
    }

    private fun togglePlay() {
        if (isPlaying) stopPreview() else startPreview()
    }

    private fun startPreview() {
        val frames = frameAdapter.getFrames()
        if (frames.isEmpty()) return
        isPlaying = true
        binding.btnPlayAnim.text = "⏹ DUR"
        val delayMs = seekToDelay(binding.seekDelay.progress).toLong()
        previewJob = lifecycleScope.launch {
            var idx = 0
            while (isActive) {
                selectFrame(idx)
                delay(delayMs)
                idx = (idx + 1) % frames.size
            }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel(); previewJob = null
        isPlaying = false
        binding.btnPlayAnim.text = "▶ OYNAT"
    }

    private fun sendToGlyph() {
        val name = binding.etAnimName.text.toString().ifBlank { "Animasyon" }
        val anim = Anim(currentAnimId, name, seekToDelay(binding.seekDelay.progress), frameAdapter.getFrames())
        AnimationStore.save(this, anim)
        ActiveState.setAnimation(this, currentAnimId)
        Toast.makeText(this, "Kaydedildi ✓ Glyph butonundan seç", Toast.LENGTH_LONG).show()
    }
}
