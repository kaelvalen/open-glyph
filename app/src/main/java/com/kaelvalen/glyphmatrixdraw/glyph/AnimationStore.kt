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
    val frames: List<IntArray>  // each frame: 625 values 0–255
)

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
            frames = frames
        )
    }

    private fun toJson(a: Anim) = JSONObject().apply {
        put("id", a.id)
        put("name", a.name)
        put("delayMs", a.delayMs)
        put("frames", JSONArray().also { arr -> a.frames.forEach { f -> arr.put(PixelStore.pack(f)) } })
    }
}
