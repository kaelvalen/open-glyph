package com.kaelvalen.glyphmatrixdraw.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityAnimationGalleryBinding
import com.kaelvalen.glyphmatrixdraw.glyph.Anim
import com.kaelvalen.glyphmatrixdraw.glyph.AnimationStore

class AnimationGalleryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ANIMATION_ID = "animation_id"
    }

    private lateinit var binding: ActivityAnimationGalleryBinding
    private lateinit var adapter: AnimationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimationGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AnimationAdapter(
            onLoad = { anim -> loadAnimation(anim) },
            onDelete = { anim -> confirmDelete(anim) }
        )

        binding.rvAnimations.layoutManager = LinearLayoutManager(this)
        binding.rvAnimations.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.rvAnimations.adapter = adapter

        binding.btnAnimGalleryBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(AnimationStore.getAll(this))
    }

    private fun loadAnimation(anim: Anim) {
        val intent = Intent(this, AnimationEditorActivity::class.java).apply {
            putExtra(AnimationEditorActivity.EXTRA_LOAD_ANIMATION_ID, anim.id)
        }
        startActivity(intent)
    }

    private fun confirmDelete(anim: Anim) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete \"${anim.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                AnimationStore.delete(this, anim.id)
                refresh()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
