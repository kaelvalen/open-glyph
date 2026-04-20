package com.kaelvalen.glyphmatrixdraw.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityAnimationGalleryBinding
import com.kaelvalen.glyphmatrixdraw.glyph.Anim
import com.kaelvalen.glyphmatrixdraw.glyph.AnimationStore

class AnimationGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimationGalleryBinding
    private lateinit var adapter: AnimationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimationGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AnimationAdapter(
            onLoad = { anim -> open(anim) },
            onMore = { anim, anchor -> showMenu(anim, anchor) },
            onToggleFavorite = { anim ->
                AnimationStore.toggleFavorite(this, anim.id)
                refresh()
            }
        )

        binding.rvAnimations.layoutManager = LinearLayoutManager(this)
        binding.rvAnimations.adapter = adapter

        binding.btnAnimGalleryBack.setOnClickListener { finish() }
        binding.fabNewAnimation.setOnClickListener { openEditor(null) }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val list = AnimationStore.getAll(this)
        adapter.submit(list)
        binding.emptyAnimView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAnimations.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun open(anim: Anim) = openEditor(anim.id)

    private fun openEditor(id: String?) {
        val intent = Intent(this, AnimationEditorActivity::class.java)
        if (id != null) intent.putExtra(AnimationEditorActivity.EXTRA_LOAD_ANIMATION_ID, id)
        startActivity(intent)
    }

    private fun showMenu(a: Anim, anchor: View) {
        val menu = PopupMenu(this, anchor, Gravity.END)
        menu.menu.add(0, 1, 0, R.string.pattern_load)
        menu.menu.add(0, 2, 0, R.string.pattern_rename)
        menu.menu.add(0, 3, 0, R.string.pattern_delete)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { open(a); true }
                2 -> { promptRename(a); true }
                3 -> { confirmDelete(a); true }
                else -> false
            }
        }
        menu.show()
    }

    private fun promptRename(a: Anim) {
        val input = EditText(this).apply {
            setText(a.name)
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                AnimationStore.rename(this, a.id, input.text.toString().ifBlank { a.name })
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(a: Anim) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_confirm, a.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                AnimationStore.delete(this, a.id)
                refresh()
                Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
