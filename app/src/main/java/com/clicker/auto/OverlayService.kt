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

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

                TapAccessibilityService.instance?.startTapping(x, y, interval, jitter, count)
                isRunning = true
                showBubble()
            }
            ACTION_STOP -> {
                TapAccessibilityService.instance?.stopTapping()
                isRunning = false
                updateBubbleIcon()
            }
            ACTION_PICK_POINT -> {
                showCrosshairPicker()
            }
        }
        return START_NOT_STICKY
    }

    private fun showBubble() {
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

        var lastTouchX = 0f
        var lastTouchY = 0f
        var lastParamX = 0
        var lastParamY = 0
        var moved = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    lastParamX = params.x
                    lastParamY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastTouchX).toInt()
                    val dy = (event.rawY - lastTouchY).toInt()
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) moved = true
                    params.x = lastParamX + dx
                    params.y = lastParamY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        toggleFromBubble()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        updateBubbleIcon()
    }

    private fun toggleFromBubble() {
        if (isRunning) {
            TapAccessibilityService.instance?.stopTapping()
            isRunning = false
        } else {
            TapAccessibilityService.instance?.let {
                isRunning = true
            }
        }
        updateBubbleIcon()
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

        view.setOnTouchListener { v, event ->
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
            .setContentText("Overlay controls active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { windowManager.removeView(it) }
        crosshairView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
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
