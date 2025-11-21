package com.example.mastercarddesignstudiopoc

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * BackendClient manages HTTP and WebSocket communication with the Hono backend.
 * This implements Pattern C: HTTP Request → Backend Notification via WebSocket
 */
class BackendClient(private val context: Context) {

    companion object {
        private const val TAG = "BackendClient"
        private const val BACKEND_URL = "http://10.0.2.2:3001"
        private const val WS_URL = "ws://10.0.2.2:3001/ws"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false

    /**
     * Connect to the backend WebSocket server
     */
    fun connectWebSocket() {
        if (isConnected) {
            Log.d(TAG, "WebSocket already connected")
            return
        }

        Log.d(TAG, "Connecting to WebSocket: $WS_URL")

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "WebSocket connection opened")

                // Send registration message
                val registrationMessage = JSONObject().apply {
                    put("type", "register")
                    put("data", JSONObject().apply {
                        put("deviceId", android.os.Build.MODEL)
                        put("platform", "Android")
                    })
                }
                webSocket.send(registrationMessage.toString())

                showToastOnUiThread("WebSocket connected to backend")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                handleWebSocketMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "WebSocket binary message received: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
                isConnected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                isConnected = false
                showToastOnUiThread("WebSocket disconnected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                isConnected = false
                showToastOnUiThread("WebSocket connection failed: ${t.message}")
            }
        })
    }

    /**
     * Handle incoming WebSocket messages from the backend
     */
    private fun handleWebSocketMessage(messageJson: String) {
        try {
            val json = JSONObject(messageJson)
            val type = json.getString("type")
            val data = json.optJSONObject("data")

            Log.d(TAG, "Message type: $type")

            when (type) {
                "connection_established" -> {
                    val message = data?.optString("message") ?: "Connected"
                    Log.d(TAG, "Connection established: $message")
                }

                "registration_success" -> {
                    val clientId = data?.optString("clientId") ?: "unknown"
                    Log.d(TAG, "Registration successful: $clientId")
                    showToastOnUiThread("Registered with backend")
                }

                "design_processed" -> {
                    // This is the notification from the backend after the WebView sent an HTTP request
                    Log.d(TAG, "Design processed notification received")
                    val designId = data?.optString("id") ?: "unknown"
                    val status = data?.optString("status") ?: "unknown"

                    showDialogOnUiThread(
                        title = "Backend Notification",
                        message = "Card design processed!\n\nID: $designId\nStatus: $status"
                    )
                }

                "pong" -> {
                    Log.d(TAG, "Pong received from backend")
                }

                "exit_studio" -> {
                    // Backend notified us to exit the studio (Pattern C exit)
                    Log.d(TAG, "Exit studio notification received")
                    navigateToCompletion()
                }

                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WebSocket message", e)
        }
    }

    /**
     * Send a ping message to the backend
     */
    fun sendPing() {
        if (!isConnected) {
            Log.w(TAG, "Cannot send ping: WebSocket not connected")
            return
        }

        val pingMessage = JSONObject().apply {
            put("type", "ping")
            put("data", JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
            })
        }

        webSocket?.send(pingMessage.toString())
        Log.d(TAG, "Ping sent to backend")
    }

    /**
     * Disconnect from WebSocket
     */
    fun disconnect() {
        if (!isConnected) {
            Log.d(TAG, "WebSocket already disconnected")
            return
        }

        Log.d(TAG, "Disconnecting WebSocket")
        webSocket?.close(1000, "Client initiated close")
        isConnected = false
    }

    /**
     * Check if WebSocket is connected
     */
    fun isWebSocketConnected(): Boolean = isConnected

    /**
     * Show a toast message on the UI thread
     */
    private fun showToastOnUiThread(message: String) {
        (context as? WebViewActivity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show a dialog on the UI thread
     */
    private fun showDialogOnUiThread(title: String, message: String) {
        (context as? WebViewActivity)?.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }
    }

    /**
     * Navigate to CompletionActivity (called when exit_studio message received)
     */
    private fun navigateToCompletion() {
        (context as? WebViewActivity)?.runOnUiThread {
            val intent = Intent(context, CompletionActivity::class.java)
            context.startActivity(intent)
            (context as? WebViewActivity)?.finish()
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()
        client.dispatcher.executorService.shutdown()
    }
}
