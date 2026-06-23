package com.clicker.auto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.random.Random

/**
 * Accessibility Service responsible for dispatching tap gestures.
 * This is the standard, OS-sanctioned mechanism Android provides for apps
 * to programmatically interact with the screen on the user's behalf.
 * The user must explicitly enable this service in system Settings.
 */
class TapAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isTapping = false
    private var tapsRemaining = -1
    private var interval = 100L
    private var jitterPercent = 0
    private var targetX = 0
    private var targetY = 0

    private val tapRunnable = object : Runnable {
        override fun run() {
            if (!isTapping) return
            if (tapsRemaining == 0) {
                stopTapping()
                return
            }

            performTap(targetX, targetY)

            if (tapsRemaining > 0) tapsRemaining--

            val jitterMs = if (jitterPercent > 0) {
                val range = (interval * jitterPercent / 100.0).toLong()
                Random.nextLong(-range, range + 1)
            } else 0L

            val nextDelay = (interval + jitterMs).coerceAtLeast(10L)
            handler.postDelayed(this, nextDelay)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — this service only dispatches gestures, doesn't read screen content.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopTapping()
    }

    fun startTapping(x: Int, y: Int, intervalMs: Long, jitter: Int, count: Int) {
        targetX = x
        targetY = y
        interval = intervalMs
        jitterPercent = jitter
        tapsRemaining = count
        isTapping = true
        handler.removeCallbacks(tapRunnable)
        handler.post(tapRunnable)
    }

    fun stopTapping() {
        isTapping = false
        handler.removeCallbacks(tapRunnable)
    }

    private fun performTap(x: Int, y: Int) {
        // Apply small positional jitter so repeated taps aren't pixel-identical
        val jx = if (jitterPercent > 0) x + Random.nextInt(-jitterPercent / 3, jitterPercent / 3 + 1) else x
        val jy = if (jitterPercent > 0) y + Random.nextInt(-jitterPercent / 3, jitterPercent / 3 + 1) else y

        val path = Path()
        path.moveTo(jx.toFloat(), jy.toFloat())

        val stroke = GestureDescription.StrokeDescription(path, 0, 40)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, null, null)
    }

    companion object {
        var instance: TapAccessibilityService? = null
    }
}
