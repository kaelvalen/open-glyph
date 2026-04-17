package com.kaelvalen.glyphmatrixdraw.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            .setTitle("Sil")
            .setMessage("\"${pattern.name}\" silinsin mi?")
            .setPositiveButton("Sil") { _, _ ->
                PatternStore.delete(this, pattern.id)
                refresh()
                Toast.makeText(this, "Silindi", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun promptNewPattern() {
        val current = PixelStore.loadPixels(this)
        val brightness = PixelStore.loadBrightness(this)
        val input = EditText(this).apply {
            hint = "Pattern adı"
            setText("Pattern ${PatternStore.getAll(this@GalleryActivity).size + 1}")
        }
        AlertDialog.Builder(this)
            .setTitle("Mevcut çizimi kaydet")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val name = input.text.toString().ifBlank { "Pattern" }
                PatternStore.save(this, Pattern(PatternStore.newId(), name, current, brightness))
                refresh()
                Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
