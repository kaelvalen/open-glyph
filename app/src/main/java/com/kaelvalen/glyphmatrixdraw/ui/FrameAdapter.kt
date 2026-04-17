package com.kaelvalen.glyphmatrixdraw.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ItemFrameBinding

class FrameAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<FrameAdapter.VH>() {

    private val frames = mutableListOf<IntArray>()
    var selectedIndex = 0
        private set

    fun submit(list: List<IntArray>, selected: Int = selectedIndex) {
        frames.clear()
        frames.addAll(list)
        selectedIndex = selected.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
        notifyDataSetChanged()
    }

    fun select(index: Int) {
        val old = selectedIndex
        selectedIndex = index
        notifyItemChanged(old)
        notifyItemChanged(index)
    }

    fun updateFrame(index: Int, pixels: IntArray) {
        if (index in frames.indices) {
            frames[index] = pixels
            notifyItemChanged(index)
        }
    }

    fun getFrames(): List<IntArray> = frames.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(frames[position], position)

    override fun getItemCount() = frames.size

    inner class VH(private val b: ItemFrameBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(pixels: IntArray, index: Int) {
            b.imgFrameThumb.setImageBitmap(pixels.toThumbnail())
            b.tvFrameIndex.text = (index + 1).toString()
            b.frameSelectedBorder.visibility = if (index == selectedIndex) View.VISIBLE else View.GONE
            b.root.setOnClickListener { onClick(index) }
        }
    }
}
