package com.example.mastercarddesignstudiopoc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * DeepLinkHandler processes custom URI schemes for Pattern B communication.
 * Handles deep links in the format: myapp://[action]?[params]
 */
class DeepLinkHandler(private val context: Context) {

    companion object {
        private const val TAG = "DeepLinkHandler"
        private const val DEEP_LINK_SCHEME = "myapp"
    }

    /**
     * Process a deep link URL and route to appropriate handler.
     * @param url The deep link URL to handle (e.g., "myapp://navigate?screen=settings")
     */
    fun handleDeepLink(url: String) {
        Log.d(TAG, "=== Deep Link Received ===")
        Log.d(TAG, "URL: $url")

        try {
            val uri = Uri.parse(url)

            // Validate scheme
            if (uri.scheme != DEEP_LINK_SCHEME) {
                Log.w(TAG, "Invalid scheme: ${uri.scheme}. Expected: $DEEP_LINK_SCHEME")
                return
            }

            // Extract action (host part of URI)
            val action = uri.host ?: run {
                Log.w(TAG, "No action found in deep link")
                return
            }

            Log.d(TAG, "Action: $action")

            // Extract query parameters
            val params = mutableMapOf<String, String>()
            uri.queryParameterNames.forEach { paramName ->
                val paramValue = uri.getQueryParameter(paramName) ?: ""
                params[paramName] = paramValue
                Log.d(TAG, "Param: $paramName = $paramValue")
            }

            // Route to appropriate handler
            routeAction(action, params)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing deep link: $url", e)
            showError("Failed to process deep link")
        }
    }

    /**
     * Route the action to the appropriate handler method.
     */
    private fun routeAction(action: String, params: Map<String, String>) {
        (context as? WebViewActivity)?.runOnUiThread {
            when (action) {
                "navigate" -> handleNavigate(params)
                "openCard" -> handleOpenCard(params)
                "callNative" -> handleCallNative(params)
                "showDialog" -> handleShowDialog(params)
                "exit" -> handleExit()
                else -> handleUnknownAction(action, params)
            }
        }
    }

    /**
     * Handle navigation deep link.
     * Example: myapp://navigate?screen=settings
     */
    private fun handleNavigate(params: Map<String, String>) {
        val screen = params["screen"] ?: "unknown"
        Log.d(TAG, "Navigate action - Screen: $screen")

        showDialog(
            title = "Navigation",
            message = "Navigate to screen: $screen"
        )
    }

    /**
     * Handle open card deep link.
     * Example: myapp://openCard?cardId=12345
     */
    private fun handleOpenCard(params: Map<String, String>) {
        val cardId = params["cardId"] ?: "unknown"
        Log.d(TAG, "Open card action - Card ID: $cardId")

        showDialog(
            title = "Open Card",
            message = "Opening card with ID: $cardId"
        )
    }

    /**
     * Handle generic native method call deep link.
     * Example: myapp://callNative?method=shareCard&data=value
     */
    private fun handleCallNative(params: Map<String, String>) {
        val method = params["method"] ?: "unknown"
        val data = params["data"] ?: ""
        Log.d(TAG, "Call native action - Method: $method, Data: $data")

        showDialog(
            title = "Native Method Call",
            message = "Method: $method\nData: $data"
        )
    }

    /**
     * Handle show dialog deep link.
     * Example: myapp://showDialog?title=Hello&message=World
     */
    private fun handleShowDialog(params: Map<String, String>) {
        val title = params["title"] ?: "Deep Link"
        val message = params["message"] ?: "No message provided"
        Log.d(TAG, "Show dialog action - Title: $title, Message: $message")

        showDialog(title = title, message = message)
    }

    /**
     * Handle exit deep link - navigates to CompletionActivity.
     * Example: myapp://exit
     */
    private fun handleExit() {
        Log.d(TAG, "Exit action - navigating to CompletionActivity")

        val intent = Intent(context, CompletionActivity::class.java)
        context.startActivity(intent)
        (context as? WebViewActivity)?.finish()
    }

    /**
     * Handle unknown action.
     */
    private fun handleUnknownAction(action: String, params: Map<String, String>) {
        Log.w(TAG, "Unknown action: $action")

        val paramsString = params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        showDialog(
            title = "Unknown Action",
            message = "Action: $action\nParams: $paramsString"
        )
    }

    /**
     * Display a native Android dialog.
     */
    private fun showDialog(title: String, message: String) {
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

    /**
     * Display an error toast.
     */
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
