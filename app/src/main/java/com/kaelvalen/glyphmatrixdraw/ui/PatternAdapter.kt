package com.kaelvalen.glyphmatrixdraw.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kaelvalen.glyphmatrixdraw.databinding.ItemPatternBinding
import com.kaelvalen.glyphmatrixdraw.glyph.Pattern

class PatternAdapter(
    private val onLoad: (Pattern) -> Unit,
    private val onDelete: (Pattern) -> Unit
) : RecyclerView.Adapter<PatternAdapter.VH>() {

    private val items = mutableListOf<Pattern>()

    fun submit(list: List<Pattern>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPatternBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val b: ItemPatternBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Pattern) {
            b.tvPatternName.text = p.name
            b.imgPatternThumb.setImageBitmap(p.pixels.toThumbnail(p.brightness))
            b.btnLoadPattern.setOnClickListener { onLoad(p) }
            b.btnDeletePattern.setOnClickListener { onDelete(p) }
        }
    }
}

fun IntArray.toThumbnail(brightness: Float = 1.0f): Bitmap {
    val bmp = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
    val scale = brightness.coerceIn(0f, 1f)
    for (i in 0 until 625) {
        val v = (this[i].coerceIn(0, 255) * scale).toInt()
        bmp.setPixel(i % 25, i / 25, if (v > 0) Color.rgb(v, v, v) else Color.parseColor("#1A1A1A"))
    }
    return bmp
}
