package com.kaelvalen.glyphmatrixdraw.glyph

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Pattern(
    val id: String,
    val name: String,
    val pixels: IntArray,       // 625 values, 0–255 per pixel
    val brightness: Float = 1.0f
)

object PatternStore {
    private const val DIR = "patterns"

    fun getAll(context: Context): List<Pattern> {
        val dir = dir(context)
        if (!dir.exists()) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .sortedBy { it.lastModified() }
            .mapNotNull { runCatching { read(it) }.getOrNull() }
    }

    fun save(context: Context, pattern: Pattern) {
        File(dir(context).also { it.mkdirs() }, "${pattern.id}.json")
            .writeText(toJson(pattern).toString())
    }

    fun delete(context: Context, id: String) {
        File(dir(context), "$id.json").delete()
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun dir(context: Context) = File(context.filesDir, DIR)

    private fun read(file: File): Pattern {
        val json = JSONObject(file.readText())
        return Pattern(
            id = json.getString("id"),
            name = json.getString("name"),
            pixels = PixelStore.unpack(json.getString("pixels")),
            brightness = json.optDouble("brightness", 1.0).toFloat()
        )
    }

    private fun toJson(p: Pattern) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("pixels", PixelStore.pack(p.pixels))
        put("brightness", p.brightness.toDouble())
    }
}
