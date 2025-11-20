package com.example.mastercarddesignstudiopoc

import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var deepLinkHandler: DeepLinkHandler
    private lateinit var backendClient: BackendClient

    companion object {
        private const val TAG = "MainActivity"
        private const val WEBVIEW_URL = "http://10.0.2.2:3000"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity onCreate")

        // Initialize DeepLinkHandler
        deepLinkHandler = DeepLinkHandler(this)

        // Initialize BackendClient for Pattern C (HTTP + WebSocket)
        backendClient = BackendClient(this)
        backendClient.connectWebSocket()

        // Create and configure WebView
        webView = WebView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        configureWebView()
        setContentView(webView)

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        // Load the web application
        Log.d(TAG, "Loading URL: $WEBVIEW_URL")
        webView.loadUrl(WEBVIEW_URL)
    }

    private fun configureWebView() {
        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true

            // Allow mixed content (HTTP on localhost during development)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Enable debugging in development
            WebView.setWebContentsDebuggingEnabled(true)

            Log.d(TAG, "WebView settings configured")
        }

        // Add JavaScript Interface for Pattern A
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")
        Log.d(TAG, "AndroidBridge JavaScript interface added")

        // Set custom WebViewClient to handle deep links (Pattern B)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d(TAG, "shouldOverrideUrlLoading: $url")

                // Check if this is a deep link
                if (url.startsWith("myapp://")) {
                    deepLinkHandler.handleDeepLink(url)
                    return true // Prevent WebView from loading the URL
                }

                // Allow normal HTTP/HTTPS URLs to load
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backendClient.cleanup()
        webView.destroy()
        Log.d(TAG, "MainActivity destroyed")
    }

    /**
     * Expose backendClient for AndroidBridge to check connection status
     */
    fun getBackendClient(): BackendClient = backendClient
}
