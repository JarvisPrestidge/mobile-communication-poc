package com.example.mastercarddesignstudiopoc

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject

/**
 * AndroidBridge provides JavaScript interface methods for Pattern A communication.
 * Methods in this class can be called directly from JavaScript via window.AndroidBridge.*
 */
class AndroidBridge(private val context: Context) {

    companion object {
        private const val TAG = "AndroidBridge"
    }

    /**
     * Display a native Android Toast message.
     * Called from JavaScript: window.AndroidBridge.showToast("Hello from WebView!")
     */
    @JavascriptInterface
    fun showToast(message: String) {
        Log.d(TAG, "showToast called with message: $message")

        // Toast must be shown on UI thread
        (context as? MainActivity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Display a native Android Dialog with title and message.
     * Called from JavaScript: window.AndroidBridge.showMessage("Title", "Message content")
     */
    @JavascriptInterface
    fun showMessage(title: String, message: String) {
        Log.d(TAG, "showMessage called - Title: $title, Message: $message")

        (context as? MainActivity)?.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Dialog dismissed")
                }
                .setCancelable(true)
                .show()
        }
    }

    /**
     * Execute a native action with a payload.
     * This demonstrates business logic execution triggered from JavaScript.
     * Called from JavaScript: window.AndroidBridge.performAction("shareCard", '{"cardId": "12345"}')
     */
    @JavascriptInterface
    fun performAction(actionName: String, payload: String) {
        Log.d(TAG, "performAction called - Action: $actionName, Payload: $payload")

        (context as? MainActivity)?.runOnUiThread {
            when (actionName) {
                "shareCard" -> {
                    // Example: Parse payload and perform sharing action
                    Toast.makeText(
                        context,
                        "Share card action triggered with payload: $payload",
                        Toast.LENGTH_LONG
                    ).show()
                }
                "navigateToSettings" -> {
                    Toast.makeText(
                        context,
                        "Navigate to settings action triggered",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                "refreshData" -> {
                    Toast.makeText(
                        context,
                        "Refresh data action triggered",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    Log.w(TAG, "Unknown action: $actionName")
                    Toast.makeText(
                        context,
                        "Unknown action: $actionName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Return device and app metadata as JSON string.
     * Called from JavaScript: const deviceInfo = window.AndroidBridge.getDeviceInfo()
     */
    @JavascriptInterface
    fun getDeviceInfo(): String {
        Log.d(TAG, "getDeviceInfo called")

        val deviceInfo = JSONObject().apply {
            put("platform", "Android")
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("osVersion", Build.VERSION.RELEASE)
            put("sdkVersion", Build.VERSION.SDK_INT)
            put("packageName", context.packageName)
            put("appVersion", getAppVersion())
        }

        val jsonString = deviceInfo.toString()
        Log.d(TAG, "Device info: $jsonString")
        return jsonString
    }

    /**
     * Log a message from WebView to native Android logcat.
     * Useful for debugging web application from native side.
     * Called from JavaScript: window.AndroidBridge.logToNative("Debug message from WebView")
     */
    @JavascriptInterface
    fun logToNative(message: String) {
        Log.d(TAG, "[WebView Log] $message")
    }

    /**
     * Get the application version name.
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app version", e)
            "Unknown"
        }
    }
}
