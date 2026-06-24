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
 *
 * Safety features:
 *  - Hard safety cap: even "infinite" mode auto-stops after SAFETY_MAX_TAPS,
 *    so a runaway session cannot lock the device indefinitely.
 *  - Bubble exclusion zone: taps are nudged away from the floating control
 *    bubble's current position, so the bubble can never be tapped-on-by-itself
 *    and become unreachable.
 */
class TapAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isTapping = false
    private var tapsRemaining = -1
    private var tapsPerformed = 0
    private var interval = 100L
    private var jitterPercent = 0
    private var targetX = 0
    private var targetY = 0

    private var bubbleX: Int? = null
    private var bubbleY: Int? = null

    private val tapRunnable = object : Runnable {
        override fun run() {
            if (!isTapping) return

            if (tapsRemaining == 0 || tapsPerformed >= SAFETY_MAX_TAPS) {
                stopTapping()
                OverlayService.instanceRef?.onSafetyStop()
                return
            }

            performTap(targetX, targetY)
            tapsPerformed++
            StatsStore.addTaps(applicationContext, 1)

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
        tapsPerformed = 0
        isTapping = true
        handler.removeCallbacks(tapRunnable)
        handler.post(tapRunnable)
    }

    fun stopTapping() {
        isTapping = false
        handler.removeCallbacks(tapRunnable)
    }

    fun isCurrentlyTapping(): Boolean = isTapping

    fun totalTapsPerformed(): Int = tapsPerformed

    /** Called by OverlayService whenever the bubble is created or dragged, so
     * we always know its current position and can avoid tapping on it. */
    fun updateBubblePosition(x: Int?, y: Int?) {
        bubbleX = x
        bubbleY = y
    }

    private fun performTap(x: Int, y: Int) {
        var jx = if (jitterPercent > 0) x + Random.nextInt(-jitterPercent / 3, jitterPercent / 3 + 1) else x
        var jy = if (jitterPercent > 0) y + Random.nextInt(-jitterPercent / 3, jitterPercent / 3 + 1) else y

        val bx = bubbleX
        val by = bubbleY
        if (bx != null && by != null) {
            val dx = jx - bx
            val dy = jy - by
            val distSq = dx * dx + dy * dy
            if (distSq < EXCLUSION_RADIUS_PX * EXCLUSION_RADIUS_PX) {
                jx = if (jx >= bx) jx + EXCLUSION_RADIUS_PX else jx - EXCLUSION_RADIUS_PX
                jy = if (jy >= by) jy + EXCLUSION_RADIUS_PX else jy - EXCLUSION_RADIUS_PX
            }
        }

        val path = Path()
        path.moveTo(jx.toFloat(), jy.toFloat())

        val stroke = GestureDescription.StrokeDescription(path, 0, 40)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, null, null)
    }

    companion object {
        var instance: TapAccessibilityService? = null

        // Even "infinite" mode stops automatically after this many taps as a
        // hard safety net against runaway/unreachable sessions.
        const val SAFETY_MAX_TAPS = 50_000

        // Minimum distance (px) any dispatched tap must keep from the bubble.
        const val EXCLUSION_RADIUS_PX = 90
    }
}
