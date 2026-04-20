package com.kaelvalen.glyphmatrixdraw.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kaelvalen.glyphmatrixdraw.R
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

    companion object {
        const val EXTRA_LOAD_ANIMATION_ID = "load_animation_id"
    }

    private lateinit var binding: ActivityAnimationEditorBinding
    private lateinit var frameAdapter: FrameAdapter

    private var currentAnimId: String = AnimationStore.newId()
    private var previewJob: Job? = null
    private var isPlaying = false
    private var onionEnabled = false
    private var pingPongEnabled = false

    private fun seekToDelay(progress: Int): Int = 50 + progress * 10
    private fun delayToSeek(ms: Int): Int = ((ms - 50) / 10).coerceIn(0, 195)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimationEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        frameAdapter = FrameAdapter(
            onClick = { index -> selectFrame(index) },
            onReorder = { _, _ -> refreshOnion() }
        )

        binding.rvFrames.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFrames.adapter = frameAdapter
        frameAdapter.attachToRecyclerView(binding.rvFrames)

        frameAdapter.submit(listOf(IntArray(625)))

        binding.animPixelGrid.onPixelsChanged = { pixels ->
            frameAdapter.updateFrame(frameAdapter.selectedIndex, pixels)
        }

        binding.seekDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                binding.tvDelayMs.text = getString(R.string.delay_ms_format, seekToDelay(p))
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

        binding.btnOnion.setOnClickListener { toggleOnion() }
        binding.btnPingPong.setOnClickListener { togglePingPong() }

        binding.etAnimName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { binding.etAnimName.clearFocus(); true } else false
        }

        val animId = intent.getStringExtra(EXTRA_LOAD_ANIMATION_ID)
        if (animId != null) loadAnimation(animId)
    }

    override fun onPause() { super.onPause(); stopPreview() }

    private fun selectFrame(index: Int) {
        stopPreview()
        frameAdapter.select(index)
        binding.animPixelGrid.setPixels(frameAdapter.getFrames()[index])
        refreshOnion()
    }

    private fun addFrame() {
        val frames = frameAdapter.getFrames().toMutableList()
        frames.add(IntArray(625))
        frameAdapter.submit(frames, frames.size - 1)
        binding.animPixelGrid.setPixels(IntArray(625))
        binding.rvFrames.smoothScrollToPosition(frames.size - 1)
        refreshOnion()
    }

    private fun dupFrame() {
        val frames = frameAdapter.getFrames().toMutableList()
        val copy = frames[frameAdapter.selectedIndex].clone()
        val insertAt = frameAdapter.selectedIndex + 1
        frames.add(insertAt, copy)
        frameAdapter.submit(frames, insertAt)
        binding.animPixelGrid.setPixels(copy)
        binding.rvFrames.smoothScrollToPosition(insertAt)
        refreshOnion()
    }

    private fun deleteFrame() {
        val frames = frameAdapter.getFrames().toMutableList()
        if (frames.size <= 1) {
            Toast.makeText(this, R.string.at_least_one_frame, Toast.LENGTH_SHORT).show()
            return
        }
        frames.removeAt(frameAdapter.selectedIndex)
        val newIdx = (frameAdapter.selectedIndex - 1).coerceAtLeast(0)
        frameAdapter.submit(frames, newIdx)
        binding.animPixelGrid.setPixels(frames[newIdx])
        refreshOnion()
    }

    private fun togglePlay() {
        if (isPlaying) stopPreview() else startPreview()
    }

    private fun toggleOnion() {
        onionEnabled = !onionEnabled
        binding.btnOnion.text = getString(if (onionEnabled) R.string.onion_skin_on else R.string.onion_skin)
        binding.btnOnion.setTextColor(if (onionEnabled) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt())
        refreshOnion()
    }

    private fun togglePingPong() {
        pingPongEnabled = !pingPongEnabled
        binding.btnPingPong.text = getString(if (pingPongEnabled) R.string.ping_pong_on else R.string.ping_pong)
        binding.btnPingPong.setTextColor(if (pingPongEnabled) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt())
    }

    private fun refreshOnion() {
        val idx = frameAdapter.selectedIndex
        val frames = frameAdapter.getFrames()
        binding.animPixelGrid.onionPixels = if (!onionEnabled || idx <= 0) null else frames.getOrNull(idx - 1)
    }

    private fun startPreview() {
        val frames = if (pingPongEnabled && frameAdapter.getFrames().size >= 3) {
            val f = frameAdapter.getFrames()
            f + f.subList(1, f.size - 1).reversed()
        } else frameAdapter.getFrames()
        if (frames.isEmpty()) return
        isPlaying = true
        binding.btnPlayAnim.text = getString(R.string.stop)
        binding.animPixelGrid.onionPixels = null
        val delayMs = seekToDelay(binding.seekDelay.progress).toLong()
        previewJob = lifecycleScope.launch {
            var idx = 0
            while (isActive) {
                val pixels = frames[idx]
                binding.animPixelGrid.setPixels(pixels, pushToUndo = false)
                delay(delayMs)
                idx = (idx + 1) % frames.size
            }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel(); previewJob = null
        isPlaying = false
        binding.btnPlayAnim.text = getString(R.string.play)
        if (frameAdapter.getFrames().isNotEmpty()) {
            binding.animPixelGrid.setPixels(frameAdapter.getFrames()[frameAdapter.selectedIndex], pushToUndo = false)
            refreshOnion()
        }
    }

    private fun sendToGlyph() {
        val name = binding.etAnimName.text.toString().ifBlank { getString(R.string.animation_title) }
        val existing = AnimationStore.get(this, currentAnimId)
        val anim = Anim(
            id = currentAnimId,
            name = name,
            delayMs = seekToDelay(binding.seekDelay.progress),
            frames = frameAdapter.getFrames(),
            pingPong = pingPongEnabled,
            favorite = existing?.favorite ?: false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        AnimationStore.save(this, anim)
        ActiveState.setAnimation(this, currentAnimId)
        Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show()
    }

    private fun loadAnimation(animId: String) {
        val anim = AnimationStore.get(this, animId) ?: return
        currentAnimId = anim.id
        binding.etAnimName.setText(anim.name)
        binding.seekDelay.progress = delayToSeek(anim.delayMs)
        binding.tvDelayMs.text = getString(R.string.delay_ms_format, anim.delayMs)
        frameAdapter.submit(anim.frames, 0)
        binding.animPixelGrid.setPixels(anim.frames.firstOrNull() ?: IntArray(625))
        if (anim.pingPong && !pingPongEnabled) togglePingPong()
        refreshOnion()
    }
}
