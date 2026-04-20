package com.kaelvalen.glyphmatrixdraw.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ItemAnimationBinding
import com.kaelvalen.glyphmatrixdraw.glyph.Anim

class AnimationAdapter(
    private val onLoad: (Anim) -> Unit,
    private val onMore: (Anim, View) -> Unit,
    private val onToggleFavorite: (Anim) -> Unit,
) : RecyclerView.Adapter<AnimationAdapter.VH>() {

    private val items = mutableListOf<Anim>()

    fun submit(list: List<Anim>) {
        val sorted = list.sortedWith(compareByDescending<Anim> { it.favorite }.thenByDescending { it.updatedAt })
        items.clear()
        items.addAll(sorted)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAnimationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val b: ItemAnimationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: Anim) {
            val ctx = b.root.context
            b.tvAnimationName.text = a.name
            b.tvAnimationFrames.text = ctx.getString(R.string.frames_count, a.frames.size)
            b.tvAnimationDelay.text = ctx.getString(R.string.delay_ms_format, a.delayMs)
            b.imgAnimationThumb.setImageBitmap(a.frames.firstOrNull()?.toThumbnail())
            b.btnAnimFavorite.text = if (a.favorite) ctx.getString(R.string.favorite_on) else ctx.getString(R.string.favorite_off)
            b.btnAnimFavorite.setTextColor(if (a.favorite) 0xFFFFD44F.toInt() else 0xFF666666.toInt())
            b.btnAnimFavorite.setOnClickListener { onToggleFavorite(a) }
            b.btnAnimMore.setOnClickListener { onMore(a, it) }
            b.root.setOnClickListener { onLoad(a) }
        }
    }
}
