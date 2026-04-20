package com.kaelvalen.glyphmatrixdraw.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ItemPatternBinding
import com.kaelvalen.glyphmatrixdraw.glyph.GlyphMask
import com.kaelvalen.glyphmatrixdraw.glyph.Pattern

class PatternAdapter(
    private val onLoad: (Pattern) -> Unit,
    private val onMore: (Pattern, View) -> Unit,
    private val onToggleFavorite: (Pattern) -> Unit,
) : RecyclerView.Adapter<PatternAdapter.VH>() {

    private val items = mutableListOf<Pattern>()

    fun submit(list: List<Pattern>) {
        val sorted = list.sortedWith(compareByDescending<Pattern> { it.favorite }.thenByDescending { it.updatedAt })
        items.clear()
        items.addAll(sorted)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPatternBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val b: ItemPatternBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Pattern) {
            b.tvPatternName.text = p.name
            val ctx = b.root.context
            b.tvPatternMeta.text = ctx.getString(R.string.brightness_percent_format, (p.brightness * 100).toInt())
            b.imgPatternThumb.setImageBitmap(p.pixels.toThumbnail(p.brightness))
            b.btnFavorite.text = if (p.favorite) ctx.getString(R.string.favorite_on) else ctx.getString(R.string.favorite_off)
            b.btnFavorite.setTextColor(if (p.favorite) 0xFFFFD44F.toInt() else 0xFF666666.toInt())
            b.btnFavorite.setOnClickListener { onToggleFavorite(p) }
            b.btnMore.setOnClickListener { onMore(p, it) }
            b.root.setOnClickListener { onLoad(p) }
        }
    }
}

fun IntArray.toThumbnail(brightness: Float = 1.0f): Bitmap {
    val bmp = Bitmap.createBitmap(GlyphMask.SIZE, GlyphMask.SIZE, Bitmap.Config.ARGB_8888)
    val scale = brightness.coerceIn(0f, 1f)
    for (r in 0 until GlyphMask.SIZE) for (c in 0 until GlyphMask.SIZE) {
        val idx = r * GlyphMask.SIZE + c
        if (!GlyphMask.isActive(c, r)) { bmp.setPixel(c, r, Color.parseColor("#080808")); continue }
        val v = (this[idx].coerceIn(0, 255) * scale).toInt()
        bmp.setPixel(c, r, if (v > 0) Color.rgb(v, v, v) else Color.parseColor("#181818"))
    }
    return bmp
}
