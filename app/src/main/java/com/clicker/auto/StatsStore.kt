package com.clicker.auto

import android.content.Context

/**
 * Lightweight lifetime-stats tracker, backed by SharedPreferences.
 * Used by the "My Clicking Life" screen and updated by the tapping services.
 */
object StatsStore {
    private const val PREFS = "autotapper_stats"

    private const val KEY_TOTAL_TAPS = "total_taps"
    private const val KEY_TOTAL_SESSIONS = "total_sessions"
    private const val KEY_BEST_CPS = "best_cps"
    private const val KEY_TOTAL_RUN_MS = "total_run_ms"
    private const val KEY_SESSION_START_MS = "session_start_ms"
    private const val KEY_BEST_CPS_TEST = "best_cps_test_result"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recordSessionStart(context: Context) {
        val p = prefs(context)
        p.edit()
            .putInt(KEY_TOTAL_SESSIONS, p.getInt(KEY_TOTAL_SESSIONS, 0) + 1)
            .putLong(KEY_SESSION_START_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordSessionEnd(context: Context) {
        val p = prefs(context)
        val start = p.getLong(KEY_SESSION_START_MS, 0L)
        if (start > 0) {
            val elapsed = System.currentTimeMillis() - start
            p.edit()
                .putLong(KEY_TOTAL_RUN_MS, p.getLong(KEY_TOTAL_RUN_MS, 0L) + elapsed)
                .putLong(KEY_SESSION_START_MS, 0L)
                .apply()
        }
    }

    fun addTaps(context: Context, count: Int) {
        if (count <= 0) return
        val p = prefs(context)
        p.edit()
            .putLong(KEY_TOTAL_TAPS, p.getLong(KEY_TOTAL_TAPS, 0L) + count)
            .apply()
    }

    fun recordCpsTestResult(context: Context, cps: Double) {
        val p = prefs(context)
        val best = java.lang.Double.longBitsToDouble(
            p.getLong(KEY_BEST_CPS_TEST, java.lang.Double.doubleToLongBits(0.0))
        )
        if (cps > best) {
            p.edit()
                .putLong(KEY_BEST_CPS_TEST, java.lang.Double.doubleToLongBits(cps))
                .apply()
        }
    }

    fun getTotalTaps(context: Context): Long = prefs(context).getLong(KEY_TOTAL_TAPS, 0L)
    fun getTotalSessions(context: Context): Int = prefs(context).getInt(KEY_TOTAL_SESSIONS, 0)
    fun getTotalRunMs(context: Context): Long = prefs(context).getLong(KEY_TOTAL_RUN_MS, 0L)
    fun getBestCpsTest(context: Context): Double = java.lang.Double.longBitsToDouble(
        prefs(context).getLong(KEY_BEST_CPS_TEST, java.lang.Double.doubleToLongBits(0.0))
    )

    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
