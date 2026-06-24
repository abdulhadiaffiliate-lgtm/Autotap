package com.clicker.auto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clicker.auto.databinding.ActivityCustomizeBinding

class CustomizeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomizeBinding
    private var selectedColor: Int = Preferences.COLOR_PURPLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        selectedColor = Preferences.getBubbleColor(this)
        highlightSelectedColor()

        binding.colorPurple.setOnClickListener { selectColor(Preferences.COLOR_PURPLE) }
        binding.colorGreen.setOnClickListener { selectColor(Preferences.COLOR_GREEN) }
        binding.colorRed.setOnClickListener { selectColor(Preferences.COLOR_RED) }
        binding.colorOrange.setOnClickListener { selectColor(Preferences.COLOR_ORANGE) }

        binding.sliderBubbleSize.value = Preferences.getBubbleSizeDp(this).toFloat()
        binding.tvBubbleSizeValue.text = "${Preferences.getBubbleSizeDp(this)}dp"
        binding.sliderBubbleSize.addOnChangeListener { _, value, _ ->
            binding.tvBubbleSizeValue.text = "${value.toInt()}dp"
        }

        binding.switchVibrate.isChecked = Preferences.isVibrateEnabled(this)
        binding.switchSound.isChecked = Preferences.isSoundEnabled(this)

        binding.btnSaveCustomize.setOnClickListener { saveAndExit() }
    }

    private fun selectColor(color: Int) {
        selectedColor = color
        highlightSelectedColor()
    }

    private fun highlightSelectedColor() {
        val swatches = mapOf(
            Preferences.COLOR_PURPLE to binding.colorPurple,
            Preferences.COLOR_GREEN to binding.colorGreen,
            Preferences.COLOR_RED to binding.colorRed,
            Preferences.COLOR_ORANGE to binding.colorOrange
        )
        swatches.forEach { (color, view) ->
            view.alpha = if (color == selectedColor) 1f else 0.45f
            view.scaleX = if (color == selectedColor) 1.08f else 1f
            view.scaleY = if (color == selectedColor) 1.08f else 1f
        }
    }

    private fun saveAndExit() {
        Preferences.setBubbleColor(this, selectedColor)
        Preferences.setBubbleSizeDp(this, binding.sliderBubbleSize.value.toInt())
        Preferences.setVibrateEnabled(this, binding.switchVibrate.isChecked)
        Preferences.setSoundEnabled(this, binding.switchSound.isChecked)
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
