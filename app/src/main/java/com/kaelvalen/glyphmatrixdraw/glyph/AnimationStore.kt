package com.kaelvalen.glyphmatrixdraw.glyph

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Anim(
    val id: String,
    val name: String,
    val delayMs: Int,
    val frames: List<IntArray>,
    val pingPong: Boolean = false,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    override fun equals(other: Any?): Boolean = other is Anim && other.id == id
    override fun hashCode(): Int = id.hashCode()

    /**
     * Expand [frames] into the display order — when [pingPong] is true,
     * appends the frames in reverse (excluding endpoints) so the sequence
     * bounces instead of snapping back.
     */
    fun playbackFrames(): List<IntArray> = if (!pingPong || frames.size < 3) frames else {
        frames + frames.subList(1, frames.size - 1).reversed()
    }
}

object AnimationStore {
    private const val DIR = "animations"

    fun getAll(context: Context): List<Anim> {
        val dir = dir(context)
        if (!dir.exists()) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .sortedBy { it.lastModified() }
            .mapNotNull { runCatching { read(it) }.getOrNull() }
    }

    fun get(context: Context, id: String): Anim? =
        runCatching { read(File(dir(context), "$id.json")) }.getOrNull()

    fun save(context: Context, anim: Anim) {
        File(dir(context).also { it.mkdirs() }, "${anim.id}.json")
            .writeText(toJson(anim).toString())
    }

    fun delete(context: Context, id: String) {
        File(dir(context), "$id.json").delete()
    }

    fun rename(context: Context, id: String, newName: String) {
        val a = get(context, id) ?: return
        save(context, a.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    fun toggleFavorite(context: Context, id: String): Boolean {
        val a = get(context, id) ?: return false
        val updated = a.copy(favorite = !a.favorite, updatedAt = System.currentTimeMillis())
        save(context, updated)
        return updated.favorite
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun dir(context: Context) = File(context.filesDir, DIR)

    private fun read(file: File): Anim {
        val json = JSONObject(file.readText())
        val arr = json.getJSONArray("frames")
        val frames = (0 until arr.length()).map { i -> PixelStore.unpack(arr.getString(i)) }
        return Anim(
            id = json.getString("id"),
            name = json.getString("name"),
            delayMs = json.getInt("delayMs"),
            frames = frames,
            pingPong = json.optBoolean("pingPong", false),
            favorite = json.optBoolean("favorite", false),
            createdAt = json.optLong("createdAt", file.lastModified()),
            updatedAt = json.optLong("updatedAt", file.lastModified()),
        )
    }

    fun toJson(a: Anim): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("name", a.name)
        put("delayMs", a.delayMs)
        put("pingPong", a.pingPong)
        put("favorite", a.favorite)
        put("createdAt", a.createdAt)
        put("updatedAt", a.updatedAt)
        put("frames", JSONArray().also { arr -> a.frames.forEach { f -> arr.put(PixelStore.pack(f)) } })
    }

    fun fromJson(json: JSONObject): Anim {
        val arr = json.getJSONArray("frames")
        val frames = (0 until arr.length()).map { i -> PixelStore.unpack(arr.getString(i)) }
        return Anim(
            id = json.optString("id").ifBlank { newId() },
            name = json.optString("name", "Animation"),
            delayMs = json.optInt("delayMs", 150),
            frames = frames,
            pingPong = json.optBoolean("pingPong", false),
            favorite = json.optBoolean("favorite", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        )
    }
}
