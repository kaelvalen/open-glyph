package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import com.kaelvalen.glyphmatrixdraw.glyph.GlyphMask
import java.io.File
import java.io.FileOutputStream

/**
 * Turns a 625-pixel frame into a shareable PNG. The output is drawn with a
 * dark background and the circular Glyph Matrix mask, scaled up for clarity.
 */
object FrameExport {

    private const val OUT_DIR = "exports"
    private const val AUTHORITY = "com.kaelvalen.glyphmatrixdraw.fileprovider"

    fun toBitmap(pixels: IntArray, brightness: Float = 1f, scale: Int = 24): Bitmap {
        val size = GlyphMask.SIZE * scale
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#080808"))
        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG)
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#141414") }
        val paintDead = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#080808") }
        for (r in 0 until GlyphMask.SIZE) for (c in 0 until GlyphMask.SIZE) {
            val rect = Rect(c * scale + 1, r * scale + 1, (c + 1) * scale - 1, (r + 1) * scale - 1)
            if (!GlyphMask.isActive(c, r)) {
                canvas.drawRect(rect, paintDead)
            } else {
                val v = (pixels[r * GlyphMask.SIZE + c] * brightness).toInt().coerceIn(0, 255)
                if (v > 0) {
                    paintOn.color = Color.rgb(v, v, v)
                    canvas.drawRect(rect, paintOn)
                } else canvas.drawRect(rect, paintOff)
            }
        }
        return bmp
    }

    fun sharePng(context: Context, pixels: IntArray, brightness: Float, title: String) {
        val dir = File(context.cacheDir, OUT_DIR).apply { mkdirs() }
        val file = File(dir, "${sanitize(title)}.png")
        FileOutputStream(file).use { toBitmap(pixels, brightness).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri: Uri = FileProvider.getUriForFile(context, AUTHORITY, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title))
    }

    private fun sanitize(s: String): String = s.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "glyph" }
}
