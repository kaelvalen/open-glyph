package com.kaelvalen.glyphmatrixdraw.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kaelvalen.glyphmatrixdraw.R
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityGalleryBinding
import com.kaelvalen.glyphmatrixdraw.glyph.Pattern
import com.kaelvalen.glyphmatrixdraw.glyph.PatternStore
import com.kaelvalen.glyphmatrixdraw.glyph.PixelStore

class GalleryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PIXELS = "pixels"
        const val EXTRA_BRIGHTNESS = "brightness"
    }

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: PatternAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PatternAdapter(
            onLoad = { pattern -> loadPattern(pattern) },
            onDelete = { pattern -> confirmDelete(pattern) }
        )

        binding.rvPatterns.layoutManager = LinearLayoutManager(this)
        binding.rvPatterns.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.rvPatterns.adapter = adapter

        binding.btnGalleryBack.setOnClickListener { finish() }
        binding.fabNewPattern.setOnClickListener { promptNewPattern() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(PatternStore.getAll(this))
    }

    private fun loadPattern(pattern: Pattern) {
        val intent = Intent().apply {
            putExtra(EXTRA_PIXELS, pattern.pixels)
            putExtra(EXTRA_BRIGHTNESS, pattern.brightness)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun confirmDelete(pattern: Pattern) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_confirm, pattern.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                PatternStore.delete(this, pattern.id)
                refresh()
                Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun promptNewPattern() {
        val current = PixelStore.loadPixels(this)
        val brightness = PixelStore.loadBrightness(this)
        val input = EditText(this).apply {
            hint = getString(R.string.pattern_name_hint)
            setText(getString(R.string.pattern_name_format, PatternStore.getAll(this@GalleryActivity).size + 1))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.save_current_drawing)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().ifBlank { "Pattern" }
                PatternStore.save(this, Pattern(PatternStore.newId(), name, current, brightness))
                refresh()
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
