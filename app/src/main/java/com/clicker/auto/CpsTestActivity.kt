package com.clicker.auto

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.clicker.auto.databinding.ActivityCpsTestBinding

class CpsTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCpsTestBinding
    private var durationSeconds = 5
    private var tapCount = 0
    private var testRunning = false
    private var startTimeMs = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!testRunning) return
            val elapsed = System.currentTimeMillis() - startTimeMs
            val remaining = (durationSeconds * 1000L) - elapsed
            if (remaining <= 0) {
                finishTest()
                return
            }
            val secondsLeft = (remaining / 1000) + 1
            binding.tvInstruction.text = "${secondsLeft}s left — keep tapping!"
            updateLiveCps(elapsed)
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCpsTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnDuration5.setOnClickListener { selectDuration(5, binding.btnDuration5) }
        binding.btnDuration10.setOnClickListener { selectDuration(10, binding.btnDuration10) }
        binding.btnDuration30.setOnClickListener { selectDuration(30, binding.btnDuration30) }

        binding.tapZone.setOnClickListener { onTapZoneTapped() }

        refreshBest()
    }

    private fun selectDuration(seconds: Int, selectedButton: android.widget.Button) {
        if (testRunning) return
        durationSeconds = seconds
        listOf(binding.btnDuration5, binding.btnDuration10, binding.btnDuration30).forEach {
            it.setBackgroundResource(android.R.color.transparent)
            it.setTextColor(getColor(R.color.text_secondary))
        }
        selectedButton.setBackgroundResource(R.drawable.bg_btn_primary)
        selectedButton.setTextColor(getColor(R.color.white))
    }

    private fun onTapZoneTapped() {
        if (!testRunning) {
            startTest()
        }
        tapCount++
        binding.tvTapCount.text = "$tapCount taps"
        val elapsed = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1)
        updateLiveCps(elapsed)
    }

    private fun startTest() {
        testRunning = true
        tapCount = 0
        startTimeMs = System.currentTimeMillis()
        binding.tvRank.text = ""
        handler.post(tickRunnable)
    }

    private fun updateLiveCps(elapsedMs: Long) {
        val cps = tapCount / (elapsedMs / 1000.0)
        binding.tvCpsBig.text = String.format("%.1f", cps)
    }

    private fun finishTest() {
        testRunning = false
        handler.removeCallbacks(tickRunnable)
        val finalCps = tapCount / durationSeconds.toDouble()
        binding.tvCpsBig.text = String.format("%.1f", finalCps)
        binding.tvInstruction.text = "Tap anywhere here to try again"
        binding.tvRank.text = rankFor(finalCps)
        StatsStore.recordCpsTestResult(this, finalCps)
        refreshBest()
    }

    private fun rankFor(cps: Double): String = when {
        cps < 3 -> "🐢 Getting started"
        cps < 5 -> "🚶 Steady clicker"
        cps < 7 -> "🐇 Quick fingers"
        cps < 9 -> "🐆 Cheetah speed"
        cps < 11 -> "⚡ Lightning hands"
        else -> "🔥 Inhuman reflexes"
    }

    private fun refreshBest() {
        val best = StatsStore.getBestCpsTest(this)
        binding.tvBestResult.text = if (best > 0) {
            "Best: ${String.format("%.1f", best)} CPS"
        } else {
            "Best: -- CPS"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
    }
}
