package com.clicker.auto

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clicker.auto.databinding.ActivityFaqBinding

class FaqActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaqBinding

    private val faqs = listOf(
        "Why does Google Play Protect warn me about this app?" to
            "AutoTapper isn't installed from the Play Store and isn't signed with a recognized " +
            "developer certificate, so Play Protect flags it by default for any sideloaded app. " +
            "It doesn't mean the app is harmful — you can review the source yourself and choose " +
            "to install anyway.",

        "Why do I need to enable Accessibility Service?" to
            "Android only allows apps to simulate taps on your behalf through the Accessibility " +
            "Service API. This is the same mechanism screen readers and assistive tools use. " +
            "AutoTapper only uses it to dispatch taps — it never reads your screen content.",

        "Why do I need to allow overlay permission?" to
            "The floating control bubble that lets you start, pause, and stop tapping needs to " +
            "draw on top of other apps. That requires the \"display over other apps\" permission.",

        "The app closed when I tapped Start — is that normal?" to
            "Yes. Tapping Start intentionally minimizes the app so the floating bubble can take " +
            "over. Look for the bubble on your screen, or check your notification shade for the " +
            "\"Overlay controls active\" notification confirming it's running.",

        "How do I stop tapping completely?" to
            "Tap the close button next to the floating bubble. This immediately stops all tapping " +
            "and removes the bubble. You can also reopen the app and tap Stop.",

        "What is jitter, and should I use it?" to
            "Jitter adds small random variation to timing and tap position so repeated taps aren't " +
            "perfectly identical. It's optional — set it to 0% for perfectly consistent taps.",

        "Is there a limit on how long it can run?" to
            "Yes, as a safety measure even \"infinite\" mode automatically stops after a very high " +
            "tap count, so a session can never run away indefinitely without you noticing.",

        "Will this work in every app or game?" to
            "It works for any on-screen tap your phone can physically register. Some apps detect " +
            "and block automated input as part of their own anti-cheat systems — that's outside " +
            "this app's control and isn't something it tries to bypass.",

        "Does this app collect my data?" to
            "No. AutoTapper doesn't use the internet, doesn't collect analytics, and doesn't read " +
            "screen content. All settings and stats are stored only on your device."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        faqs.forEach { (question, answer) -> addFaqItem(question, answer) }
    }

    private fun addFaqItem(question: String, answer: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val questionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
        }

        val questionText = TextView(this).apply {
            text = question
            setTextColor(getColor(R.color.text_primary))
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val chevron = TextView(this).apply {
            text = "+"
            setTextColor(getColor(R.color.accent_light))
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        questionRow.addView(questionText)
        questionRow.addView(chevron)

        val answerText = TextView(this).apply {
            text = answer
            setTextColor(getColor(R.color.text_secondary))
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
            visibility = View.GONE
        }

        questionRow.setOnClickListener {
            val expanded = answerText.visibility == View.VISIBLE
            answerText.visibility = if (expanded) View.GONE else View.VISIBLE
            chevron.text = if (expanded) "+" else "−"
        }

        card.addView(questionRow)
        card.addView(answerText)
        binding.faqContainer.addView(card)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
