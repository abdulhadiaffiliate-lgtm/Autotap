package com.clicker.auto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var crosshairView: View? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var isRunning = false

    // Bubble's last known on-screen bounds, used to keep the tap point away from it.
    private var bubbleLeft = 0
    private var bubbleTop = 0
    private var bubbleRight = 0
    private var bubbleBottom = 0
    private val bubbleSafeMargin = 140 // px buffer around the bubble that taps must avoid

    override fun onCreate() {
        super.onCreate()
        Log.d("AutoTapperDebug", "OverlayService onCreate() called")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        StatsStore.recordSessionStart(this)
        instanceRef = this
    }

    /** Called by TapAccessibilityService if the hard safety cap is hit. */
    fun onSafetyStop() {
        isRunning = false
        updateBubbleIcon()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AutoTapperDebug", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                val interval = intent.getLongExtra(EXTRA_INTERVAL, 100L)
                val jitter = intent.getIntExtra(EXTRA_JITTER, 0)
                val count = intent.getIntExtra(EXTRA_COUNT, -1)
                var x = intent.getIntExtra(EXTRA_X, -1)
                var y = intent.getIntExtra(EXTRA_Y, -1)

                if (x < 0 || y < 0) {
                    val metrics = resources.displayMetrics
                    x = metrics.widthPixels / 2
                    y = metrics.heightPixels / 2
                }

                // Safety: never allow the tap point to land on the bubble itself.
                // If it would, nudge it away so the app can't tap-lock its own controls.
                val safeXY = pushPointAwayFromBubble(x, y)

                TapAccessibilityService.instance?.startTapping(
                    safeXY.first, safeXY.second, interval, jitter, count
                )
                isRunning = true
                showBubble()
            }
            ACTION_STOP -> {
                stopAllTapping()
            }
            ACTION_PICK_POINT -> {
                showCrosshairPicker()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopAllTapping() {
        TapAccessibilityService.instance?.stopTapping()
        isRunning = false
        updateBubbleIcon()
    }

    /**
     * If (x, y) falls inside the bubble's bounds (plus a safety margin), move it
     * to the opposite side of the screen so the auto-tap can never re-trigger
     * the bubble's own controls and cause a runaway loop.
     */
    private fun pushPointAwayFromBubble(x: Int, y: Int): Pair<Int, Int> {
        if (bubbleRight == 0 && bubbleBottom == 0) return Pair(x, y) // bubble not placed yet

        val insideDangerZone =
            x in (bubbleLeft - bubbleSafeMargin)..(bubbleRight + bubbleSafeMargin) &&
            y in (bubbleTop - bubbleSafeMargin)..(bubbleBottom + bubbleSafeMargin)

        if (!insideDangerZone) return Pair(x, y)

        val metrics = resources.displayMetrics
        // Push to the vertical center of the opposite half of the screen from the bubble.
        val newY = if (bubbleTop < metrics.heightPixels / 2) {
            (metrics.heightPixels * 3) / 4
        } else {
            metrics.heightPixels / 4
        }
        Log.d("AutoTapperDebug", "Tap point was inside bubble safe-zone, relocated to ($x, $newY)")
        return Pair(x, newY)
    }

    private fun showBubble() {
        Log.d("AutoTapperDebug", "showBubble() called, bubbleView currently null=${bubbleView == null}")
        if (bubbleView != null) {
            updateBubbleIcon()
            return
        }

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bubble, null)
        bubbleView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200
        bubbleParams = params
        updateBubbleBounds(view, params)

        var lastTouchX = 0f
        var lastTouchY = 0f
        var lastParamX = 0
        var lastParamY = 0
        var moved = false

        // Dragging is handled on the root container so it works no matter which
        // child (close/main/settings) the finger started on, as long as it moves.
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    lastParamX = params.x
                    lastParamY = params.y
                    moved = false
                    false // let child views still receive click events
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastTouchX).toInt()
                    val dy = (event.rawY - lastTouchY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) {
                        moved = true
                        params.x = lastParamX + dx
                        params.y = lastParamY + dy
                        windowManager.updateViewLayout(view, params)
                        updateBubbleBounds(view, params)
                    }
                    moved
                }
                else -> false
            }
        }

        val btnMain = view.findViewById<View>(R.id.btnBubbleMain)
        val btnClose = view.findViewById<View>(R.id.btnBubbleClose)
        val btnSettings = view.findViewById<View>(R.id.btnBubbleSettings)

        applyCustomization(btnMain)

        btnMain.setOnClickListener { toggleFromBubble() }

        btnClose.setOnClickListener {
            // Close ALWAYS stops tapping and fully removes the overlay + service.
            stopAllTapping()
            removeBubble()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        try {
            windowManager.addView(view, params)
            Log.d("AutoTapperDebug", "bubble addView SUCCESS")
        } catch (e: Exception) {
            Log.e("AutoTapperDebug", "bubble addView FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        }
        updateBubbleIcon()
    }

    private fun updateBubbleBounds(view: View, params: WindowManager.LayoutParams) {
        // Approximate bounds in screen coordinates; good enough for the safe-zone check.
        bubbleLeft = params.x
        bubbleTop = params.y
        bubbleRight = params.x + 160 // combined width of close+main+settings row
        bubbleBottom = params.y + 60
        // Also tell the accessibility service so its own exclusion-radius check
        // (a second, independent layer of the same safety net) stays in sync.
        TapAccessibilityService.instance?.updateBubblePosition(
            bubbleLeft + (bubbleRight - bubbleLeft) / 2,
            bubbleTop + (bubbleBottom - bubbleTop) / 2
        )
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
    }

    private fun applyCustomization(mainButtonView: View) {
        val color = Preferences.getBubbleColor(this)
        val sizeDp = Preferences.getBubbleSizeDp(this)
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()

        // mainButtonView is the FrameLayout; its first child is the colored circle View.
        if (mainButtonView is android.view.ViewGroup && mainButtonView.childCount > 0) {
            val circle = mainButtonView.getChildAt(0)
            val bg = circle.background?.mutate()
            if (bg is android.graphics.drawable.GradientDrawable) {
                bg.setColor(color)
            }
            circle.layoutParams = circle.layoutParams.apply {
                width = sizePx
                height = sizePx
            }
            mainButtonView.layoutParams = mainButtonView.layoutParams.apply {
                width = sizePx
                height = sizePx
            }
        }
    }

    private fun maybeVibrate() {
        if (!Preferences.isVibrateEnabled(this)) return
        val vibrator = getSystemService(android.os.Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(20, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(20)
        }
    }

    private fun toggleFromBubble() {
        maybeVibrate()
        if (isRunning) {
            stopAllTapping()
        } else {
            TapAccessibilityService.instance?.let {
                isRunning = true
            }
            updateBubbleIcon()
        }
    }

    private fun updateBubbleIcon() {
        val icon = bubbleView?.findViewById<ImageView>(R.id.ivBubbleIcon) ?: return
        icon.setImageResource(
            if (isRunning) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun showCrosshairPicker() {
        if (crosshairView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_crosshair, null)
        crosshairView = view

        val metrics = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = metrics.widthPixels / 2 - 25
        params.y = metrics.heightPixels / 2 - 25

        var lastTouchX = 0f
        var lastTouchY = 0f
        var lastParamX = 0
        var lastParamY = 0

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    lastParamX = params.x
                    lastParamY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastTouchX).toInt()
                    val dy = (event.rawY - lastTouchY).toInt()
                    params.x = lastParamX + dx
                    params.y = lastParamY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val centerX = params.x + 25
                    val centerY = params.y + 25
                    getSharedPreferences("autotapper", MODE_PRIVATE).edit()
                        .putInt("tap_x", centerX)
                        .putInt("tap_y", centerY)
                        .apply()

                    windowManager.removeView(view)
                    crosshairView = null

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun startForegroundNotification() {
        val channelId = "autotapper_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "AutoTapper Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AutoTapper")
            .setContentText("Tap the bubble's X to stop and close")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("AutoTapperDebug", "startForeground FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        StatsStore.recordSessionEnd(this)
        TapAccessibilityService.instance?.stopTapping()
        TapAccessibilityService.instance?.updateBubblePosition(null, null)
        instanceRef = null
        removeBubble()
        crosshairView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var instanceRef: OverlayService? = null

        const val ACTION_START = "com.clicker.auto.START"
        const val ACTION_STOP = "com.clicker.auto.STOP"
        const val ACTION_PICK_POINT = "com.clicker.auto.PICK_POINT"
        const val EXTRA_INTERVAL = "interval"
        const val EXTRA_JITTER = "jitter"
        const val EXTRA_COUNT = "count"
        const val EXTRA_X = "x"
        const val EXTRA_Y = "y"
    }
}
