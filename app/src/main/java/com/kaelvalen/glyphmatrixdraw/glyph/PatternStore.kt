package com.kaelvalen.glyphmatrixdraw.glyph

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Pattern(
    val id: String,
    val name: String,
    val pixels: IntArray,       // 625 values, 0–255 per pixel
    val brightness: Float = 1.0f,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Pattern) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

object PatternStore {
    private const val DIR = "patterns"

    fun getAll(context: Context): List<Pattern> {
        val dir = dir(context)
        if (!dir.exists()) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .sortedBy { it.lastModified() }
            .mapNotNull { runCatching { read(it) }.getOrNull() }
    }

    fun get(context: Context, id: String): Pattern? =
        runCatching { read(File(dir(context), "$id.json")) }.getOrNull()

    fun save(context: Context, pattern: Pattern) {
        File(dir(context).also { it.mkdirs() }, "${pattern.id}.json")
            .writeText(toJson(pattern).toString())
    }

    fun delete(context: Context, id: String) {
        File(dir(context), "$id.json").delete()
    }

    fun rename(context: Context, id: String, newName: String) {
        val p = get(context, id) ?: return
        save(context, p.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    fun toggleFavorite(context: Context, id: String): Boolean {
        val p = get(context, id) ?: return false
        val updated = p.copy(favorite = !p.favorite, updatedAt = System.currentTimeMillis())
        save(context, updated)
        return updated.favorite
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun dir(context: Context) = File(context.filesDir, DIR)

    private fun read(file: File): Pattern {
        val json = JSONObject(file.readText())
        return Pattern(
            id = json.getString("id"),
            name = json.getString("name"),
            pixels = PixelStore.unpack(json.getString("pixels")),
            brightness = json.optDouble("brightness", 1.0).toFloat(),
            favorite = json.optBoolean("favorite", false),
            createdAt = json.optLong("createdAt", file.lastModified()),
            updatedAt = json.optLong("updatedAt", file.lastModified()),
        )
    }

    fun toJson(p: Pattern): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("pixels", PixelStore.pack(p.pixels))
        put("brightness", p.brightness.toDouble())
        put("favorite", p.favorite)
        put("createdAt", p.createdAt)
        put("updatedAt", p.updatedAt)
    }

    fun fromJson(json: JSONObject): Pattern = Pattern(
        id = json.optString("id").ifBlank { newId() },
        name = json.optString("name", "Pattern"),
        pixels = PixelStore.unpack(json.getString("pixels")),
        brightness = json.optDouble("brightness", 1.0).toFloat(),
        favorite = json.optBoolean("favorite", false),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
    )
}
