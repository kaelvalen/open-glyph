package com.kaelvalen.glyphmatrixdraw.ui

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.kaelvalen.glyphmatrixdraw.databinding.ItemFrameBinding

class FrameAdapter(
    private val onClick: (Int) -> Unit,
    private val onReorder: ((from: Int, to: Int) -> Unit)? = null,
) : RecyclerView.Adapter<FrameAdapter.VH>() {

    private val frames = mutableListOf<IntArray>()
    var selectedIndex = 0
        private set

    private var itemTouchHelper: ItemTouchHelper? = null

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

    /** Attach drag-to-reorder support to the given recycler view. */
    fun attachToRecyclerView(rv: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                val moved = frames.removeAt(from)
                frames.add(to, moved)
                if (selectedIndex == from) selectedIndex = to
                else if (from < selectedIndex && to >= selectedIndex) selectedIndex--
                else if (from > selectedIndex && to <= selectedIndex) selectedIndex++
                notifyItemMoved(from, to)
                onReorder?.invoke(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = true
        }
        itemTouchHelper = ItemTouchHelper(callback).also { it.attachToRecyclerView(rv) }
    }

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
            b.root.setOnLongClickListener {
                itemTouchHelper?.startDrag(this)
                true
            }
        }
    }
}
