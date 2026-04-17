package com.kaelvalen.glyphmatrixdraw.glyph

import android.content.Context

object ActiveState {
    private const val PREFS = "active_state"
    private const val KEY_MODE = "mode"
    private const val KEY_ID = "active_id"
    private const val KEY_PATTERN_INDEX = "pattern_index"

    const val MODE_STATIC = "static"
    const val MODE_ANIMATION = "animation"

    fun setStatic(context: Context, patternId: String?) {
        prefs(context).edit()
            .putString(KEY_MODE, MODE_STATIC)
            .putString(KEY_ID, patternId)
            .apply()
    }

    fun setAnimation(context: Context, animId: String) {
        prefs(context).edit()
            .putString(KEY_MODE, MODE_ANIMATION)
            .putString(KEY_ID, animId)
            .apply()
    }

    fun getMode(context: Context): String =
        prefs(context).getString(KEY_MODE, MODE_STATIC) ?: MODE_STATIC

    fun getId(context: Context): String? =
        prefs(context).getString(KEY_ID, null)

    fun getPatternIndex(context: Context): Int =
        prefs(context).getInt(KEY_PATTERN_INDEX, 0)

    fun setPatternIndex(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_PATTERN_INDEX, index).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
