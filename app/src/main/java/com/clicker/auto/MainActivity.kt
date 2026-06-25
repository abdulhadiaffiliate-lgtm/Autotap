package com.clicker.auto

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clicker.auto.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var tapX: Int = -1
    private var tapY: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupBottomNav()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun setupListeners() {
        binding.btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnEnableOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        binding.sliderInterval.addOnChangeListener { _, value, _ ->
            binding.tvIntervalValue.text = "${value.toInt()}ms"
        }

        binding.sliderJitter.addOnChangeListener { _, value, _ ->
            binding.tvJitterValue.text = "${value.toInt()}%"
        }

        binding.switchInfinite.setOnCheckedChangeListener { _, checked ->
            binding.etTapCount.isEnabled = !checked
        }

        binding.btnSetPoint.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Enable overlay permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Minimize app, show crosshair picker overlay
            val intent = Intent(this, OverlayService::class.java)
            intent.action = OverlayService.ACTION_PICK_POINT
            startForegroundService(intent)
            moveTaskToBack(true)
        }

        binding.btnStartStop.setOnClickListener {
            if (binding.btnStartStop.text == getString(R.string.btn_start)) {
                startTapping()
            } else {
                stopTapping()
            }
        }
    }

    private fun startTapping() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Enable the Accessibility Service first", Toast.LENGTH_LONG).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Enable overlay permission first", Toast.LENGTH_LONG).show()
            return
        }

        val interval = binding.sliderInterval.value.toLong()
        val jitter = binding.sliderJitter.value.toInt()
        val infinite = binding.switchInfinite.isChecked
        val count = if (infinite) -1 else (binding.etTapCount.text.toString().toIntOrNull() ?: 100)

        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra(OverlayService.EXTRA_INTERVAL, interval)
            putExtra(OverlayService.EXTRA_JITTER, jitter)
            putExtra(OverlayService.EXTRA_COUNT, count)
            putExtra(OverlayService.EXTRA_X, tapX)
            putExtra(OverlayService.EXTRA_Y, tapY)
        }
        startForegroundService(intent)
        binding.btnStartStop.setBackgroundResource(R.drawable.bg_btn_danger)
        binding.tvStatus.text = getString(R.string.status_running)

        moveTaskToBack(true)
    }

    private fun stopTapping() {
        val intent = Intent(this, OverlayService::class.java)
        intent.action = OverlayService.ACTION_STOP
        startForegroundService(intent)

        binding.btnStartStop.text = getString(R.string.btn_start)
        binding.btnStartStop.setBackgroundResource(R.drawable.bg_btn_primary)
        binding.tvStatus.text = getString(R.string.status_ready)
    }

    private fun setupBottomNav() {
        binding.navHome.setOnClickListener { /* already here */ }
        binding.navCps.setOnClickListener {
            startActivity(Intent(this, CpsTestActivity::class.java))
        }
        binding.navStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        binding.navCustomize.setOnClickListener {
            startActivity(Intent(this, CustomizeActivity::class.java))
        }
        binding.navFaq.setOnClickListener {
            startActivity(Intent(this, FaqActivity::class.java))
        }
    }

    private fun refreshStatus() {
        val serviceOn = isAccessibilityServiceEnabled()
        val overlayOn = Settings.canDrawOverlays(this)

        binding.btnEnableService.alpha = if (serviceOn) 0.5f else 1f
        binding.btnEnableOverlay.alpha = if (overlayOn) 0.5f else 1f

        if (!serviceOn) {
            binding.tvStatus.text = getString(R.string.status_service_off)
        } else if (!overlayOn) {
            binding.tvStatus.text = getString(R.string.status_overlay_off)
        } else {
            binding.tvStatus.text = getString(R.string.status_ready)
        }

        // Pick up tap point set while app was in background
        val prefs = getSharedPreferences("autotapper", MODE_PRIVATE)
        val x = prefs.getInt("tap_x", -1)
        val y = prefs.getInt("tap_y", -1)
        if (x >= 0 && y >= 0) {
            tapX = x
            tapY = y
            binding.tvTapPoint.text = "Set at ($x, $y)"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${TapAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { it.equals(expectedComponentName, ignoreCase = true) }
    }
}
