package com.kaelvalen.glyphmatrixdraw.glyph

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Export / import patterns and animations as a single JSON bundle.
 * Schema version is bumped whenever a breaking change is introduced.
 */
object BackupStore {

    private const val SCHEMA_VERSION = 1

    fun exportAll(context: Context): String {
        val root = JSONObject()
        root.put("schema", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("patterns", JSONArray().also { arr -> PatternStore.getAll(context).forEach { arr.put(PatternStore.toJson(it)) } })
        root.put("animations", JSONArray().also { arr -> AnimationStore.getAll(context).forEach { arr.put(AnimationStore.toJson(it)) } })
        return root.toString(2)
    }

    data class ImportSummary(val patterns: Int, val animations: Int)

    /** Parses [text] and saves all entries. Returns counts of imported items. */
    fun importFrom(context: Context, text: String): ImportSummary {
        val root = JSONObject(text)
        var patterns = 0
        var animations = 0
        root.optJSONArray("patterns")?.let { arr ->
            for (i in 0 until arr.length()) {
                runCatching { PatternStore.save(context, PatternStore.fromJson(arr.getJSONObject(i))) }
                    .onSuccess { patterns++ }
            }
        }
        root.optJSONArray("animations")?.let { arr ->
            for (i in 0 until arr.length()) {
                runCatching { AnimationStore.save(context, AnimationStore.fromJson(arr.getJSONObject(i))) }
                    .onSuccess { animations++ }
            }
        }
        return ImportSummary(patterns, animations)
    }

    fun importFromUri(context: Context, uri: Uri): ImportSummary {
        val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: return ImportSummary(0, 0)
        return importFrom(context, text)
    }
}
