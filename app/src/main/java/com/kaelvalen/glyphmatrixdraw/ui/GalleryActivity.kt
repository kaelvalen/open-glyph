package com.kaelvalen.glyphmatrixdraw.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityGalleryBinding
import com.kaelvalen.glyphmatrixdraw.glyph.BackupStore
import com.kaelvalen.glyphmatrixdraw.glyph.Pattern
import com.kaelvalen.glyphmatrixdraw.glyph.PatternStore
import com.kaelvalen.glyphmatrixdraw.glyph.PixelStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PIXELS = "pixels"
        const val EXTRA_BRIGHTNESS = "brightness"
    }

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: PatternAdapter

    private val importPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) doImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PatternAdapter(
            onLoad = { pattern -> loadPattern(pattern) },
            onMore = { pattern, anchor -> showMenu(pattern, anchor) },
            onToggleFavorite = { pattern ->
                PatternStore.toggleFavorite(this, pattern.id)
                refresh()
            }
        )

        binding.rvPatterns.layoutManager = LinearLayoutManager(this)
        binding.rvPatterns.adapter = adapter

        binding.btnGalleryBack.setOnClickListener { finish() }
        binding.fabNewPattern.setOnClickListener { promptNewPattern() }
        binding.btnExportBackup.setOnClickListener { exportBackup() }
        binding.btnImportBackup.setOnClickListener { importPicker.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val list = PatternStore.getAll(this)
        adapter.submit(list)
        binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvPatterns.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadPattern(pattern: Pattern) {
        val intent = Intent().apply {
            putExtra(EXTRA_PIXELS, pattern.pixels)
            putExtra(EXTRA_BRIGHTNESS, pattern.brightness)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun showMenu(p: Pattern, anchor: View) {
        val menu = PopupMenu(this, anchor, Gravity.END)
        menu.menu.add(0, 1, 0, R.string.pattern_load)
        menu.menu.add(0, 2, 0, R.string.pattern_rename)
        menu.menu.add(0, 3, 0, R.string.pattern_share)
        menu.menu.add(0, 4, 0, R.string.pattern_delete)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { loadPattern(p); true }
                2 -> { promptRename(p); true }
                3 -> { FrameExport.sharePng(this, p.pixels, p.brightness, p.name); true }
                4 -> { confirmDelete(p); true }
                else -> false
            }
        }
        menu.show()
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

    private fun promptRename(pattern: Pattern) {
        val input = EditText(this).apply {
            setText(pattern.name)
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PatternStore.rename(this, pattern.id, input.text.toString().ifBlank { pattern.name })
                refresh()
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
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.save_current_drawing)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().ifBlank { "Pattern" }
                PatternStore.save(this, Pattern(PatternStore.newId(), name, current.clone(), brightness))
                refresh()
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun exportBackup() {
        val text = BackupStore.exportAll(this)
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val dir = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "glyphdraw-$ts.glyph")
        FileOutputStream(file).use { it.write(text.toByteArray()) }
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "com.kaelvalen.glyphmatrixdraw.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, getString(R.string.backup_exported)))
    }

    private fun doImport(uri: Uri) {
        runCatching { BackupStore.importFromUri(this, uri) }
            .onSuccess { summary ->
                Toast.makeText(this, getString(R.string.backup_imported, summary.patterns, summary.animations), Toast.LENGTH_LONG).show()
                refresh()
            }
            .onFailure { Toast.makeText(this, it.localizedMessage ?: "Import failed", Toast.LENGTH_LONG).show() }
    }
}
