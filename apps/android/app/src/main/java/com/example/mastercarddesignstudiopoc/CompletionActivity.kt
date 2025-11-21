package com.example.mastercarddesignstudiopoc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding

/**
 * CompletionActivity displays the card design completion confirmation.
 * Provides option to return to the Demo Bank landing screen.
 */
class CompletionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CompletionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "CompletionActivity onCreate")

        // Create UI programmatically
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64)
            setBackgroundColor(0xFFFFFFFF.toInt()) // White background
        }

        // Success message
        val completionTextView = TextView(this).apply {
            text = "Card Design Completed"
            textSize = 28f
            setTextColor(0xFF000000.toInt()) // Black text
            setPadding(0, 0, 0, 32)
        }

        // Subtext
        val subTextView = TextView(this).apply {
            text = "Your card design has been submitted successfully."
            textSize = 16f
            setTextColor(0xFF666666.toInt()) // Gray text
            setPadding(0, 0, 0, 64)
        }

        // Button: "Return to Bank App"
        val returnButton = Button(this).apply {
            text = "Return to Bank App"
            textSize = 16f
            setPadding(32, 24, 32, 24)

            setOnClickListener {
                Log.d(TAG, "Return to Bank App button clicked")
                returnToLanding()
            }
        }

        // Add views to layout
        layout.addView(completionTextView)
        layout.addView(subTextView)
        layout.addView(returnButton)

        setContentView(layout)
    }

    /**
     * Return to LandingActivity and clear back stack
     */
    private fun returnToLanding() {
        val intent = Intent(this, LandingActivity::class.java).apply {
            // Clear back stack so user can't go back to WebView
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
        Log.d(TAG, "Returned to LandingActivity")
    }
}
