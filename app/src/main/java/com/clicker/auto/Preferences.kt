package com.clicker.auto

import android.content.Context

/** Stores user customization choices for the floating bubble. */
object Preferences {
    private const val PREFS = "autotapper_prefs"

    private const val KEY_BUBBLE_COLOR = "bubble_color"
    private const val KEY_BUBBLE_SIZE = "bubble_size"
    private const val KEY_VIBRATE = "vibrate_on_tap"
    private const val KEY_SOUND = "tap_sound"

    // Stored as ARGB color ints; default matches the app's signature purple.
    const val COLOR_PURPLE = 0xFF7C5CFF.toInt()
    const val COLOR_GREEN = 0xFF3DDC97.toInt()
    const val COLOR_RED = 0xFFFF5C7A.toInt()
    const val COLOR_ORANGE = 0xFFFFA94D.toInt()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBubbleColor(context: Context): Int =
        prefs(context).getInt(KEY_BUBBLE_COLOR, COLOR_PURPLE)

    fun setBubbleColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_BUBBLE_COLOR, color).apply()
    }

    fun getBubbleSizeDp(context: Context): Int =
        prefs(context).getInt(KEY_BUBBLE_SIZE, 60)

    fun setBubbleSizeDp(context: Context, sizeDp: Int) {
        prefs(context).edit().putInt(KEY_BUBBLE_SIZE, sizeDp).apply()
    }

    fun isVibrateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VIBRATE, false)

    fun setVibrateEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_VIBRATE, enabled).apply()
    }

    fun isSoundEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOUND, false)

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOUND, enabled).apply()
    }
}
