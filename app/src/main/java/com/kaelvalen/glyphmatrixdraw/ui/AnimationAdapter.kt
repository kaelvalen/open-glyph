package com.kaelvalen.glyphmatrixdraw.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kaelvalen.glyphmatrixdraw.databinding.ItemAnimationBinding
import com.kaelvalen.glyphmatrixdraw.glyph.Anim

class AnimationAdapter(
    private val onLoad: (Anim) -> Unit,
    private val onDelete: (Anim) -> Unit
) : RecyclerView.Adapter<AnimationAdapter.VH>() {

    private val items = mutableListOf<Anim>()

    fun submit(list: List<Anim>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAnimationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val b: ItemAnimationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: Anim) {
            b.tvAnimationName.text = a.name
            b.tvAnimationFrames.text = "${a.frames.size} frames"
            b.tvAnimationDelay.text = "${a.delayMs}ms"
            b.imgAnimationThumb.setImageBitmap(a.frames.firstOrNull()?.toThumbnail())
            b.btnLoadAnimation.setOnClickListener { onLoad(a) }
            b.btnDeleteAnimation.setOnClickListener { onDelete(a) }
        }
    }
}
