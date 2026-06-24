package com.clicker.auto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.clicker.auto.databinding.ActivityStatsBinding

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnResetStats.setOnClickListener {
            StatsStore.resetAll(this)
            refresh()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val totalTaps = StatsStore.getTotalTaps(this)
        val sessions = StatsStore.getTotalSessions(this)
        val bestCps = StatsStore.getBestCpsTest(this)
        val totalMs = StatsStore.getTotalRunMs(this)

        binding.tvTotalTaps.text = formatCount(totalTaps)
        binding.tvTotalSessions.text = sessions.toString()
        binding.tvBestCps.text = if (bestCps > 0) String.format("%.1f", bestCps) else "--"
        binding.tvTotalTime.text = formatDuration(totalMs)
    }

    private fun formatCount(n: Long): String = when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
        n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
        else -> n.toString()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalSeconds}s"
        }
    }
}
