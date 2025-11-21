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
 * LandingActivity is the entry point of the application.
 * Displays Demo Bank branding and button to enter Card Design Studio.
 */
class LandingActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LandingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "LandingActivity onCreate")

        // Create UI programmatically for simplicity
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64)
            setBackgroundColor(0xFFFFFFFF.toInt()) // White background
        }

        // Main header: "Demo Bank TM"
        val headerTextView = TextView(this).apply {
            text = "Demo Bank ™"
            textSize = 32f
            setTextColor(0xFF000000.toInt()) // Black text
            setPadding(0, 0, 0, 16)
        }

        // Subheader: "Issuer Banking App"
        val subHeaderTextView = TextView(this).apply {
            text = "Issuer Banking App"
            textSize = 20f
            setTextColor(0xFF666666.toInt()) // Gray text
            setPadding(0, 0, 0, 64)
        }

        // Button: "Enter Card Design Studio"
        val enterButton = Button(this).apply {
            text = "Enter Card Design Studio"
            textSize = 16f
            setPadding(32, 24, 32, 24)

            setOnClickListener {
                Log.d(TAG, "Enter Card Design Studio button clicked")
                navigateToWebView()
            }
        }

        // Add views to layout
        layout.addView(headerTextView)
        layout.addView(subHeaderTextView)
        layout.addView(enterButton)

        setContentView(layout)
    }

    /**
     * Navigate to WebViewActivity (Card Design Studio)
     */
    private fun navigateToWebView() {
        val intent = Intent(this, WebViewActivity::class.java)
        startActivity(intent)
        Log.d(TAG, "Navigated to WebViewActivity")
    }
}
