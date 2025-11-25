# Mastercard Mobile PoC - Android WebView Communication Patterns

Proof-of-concept demonstrating industry-standard communication patterns between Android native apps and WebView-hosted web applications. Built for banking stakeholder presentations.

## Overview

Android shell app hosting TanStack Start web application with three communication patterns:

- **JavaScript Interface Bridge** - Direct native method invocation
- **Deep Linking** - Custom URI scheme navigation
- **HTTP + WebSocket** - Asynchronous backend-mediated communication

## Prerequisites

### Required Software

1. **Bun** - JavaScript runtime and package manager
   - Install: `curl -fsSL https://bun.sh/install | bash`
   - Verify: `bun --version`

2. **Android Studio** - IDE and Android SDK
   - Download: [developer.android.com/studio](https://developer.android.com/studio)
   - Install Android SDK Platform 36 (API level 36)
   - Configure Android Virtual Device (AVD) - Pixel 8 equivalent

### Network Requirements

**IMPORTANT:** Disable ZScaler or any corporate VPN before running Android Studio.

- ZScaler interferes with Gradle dependency downloads
- Required for initial setup and Gradle sync
- Can re-enable after dependencies are cached

## Project Structure

```
.
├── apps/
│   ├── android/          # Native Android shell (Kotlin)
│   ├── backend/          # Hono + Bun WebSocket server
│   └── website/          # TanStack Start web app
├── turbo.json
└── package.json
```

## Running the Project

### 1. Install Dependencies

From project root:

```bash
bun install
```

### 2. Start Development Servers

From project root (starts both frontend and backend):

```bash
bun dev
```

This runs:
- **Frontend** (TanStack Start): `http://localhost:3000`
- **Backend** (Hono): `http://localhost:3001`

**Note:** Frontend uses `--host` flag to expose server on all network interfaces (required for Android emulator access via `10.0.2.2`).

### 3. Open Android Studio

1. Launch Android Studio
2. Open project at: `apps/android/`
3. Wait for Gradle sync to complete (may take 2-5 minutes first time)
4. If Gradle sync fails:
   - Verify ZScaler/VPN is disabled
   - Check Android SDK Platform 36 is installed
   - Retry sync: **File → Sync Project with Gradle Files**

### 4. Run Android App

1. Select device: **Pixel 8 API 36** (or create new AVD)
2. Click **Run** (▶️) or press `Shift + F10`
3. Emulator launches (may take 1-2 minutes cold start)
4. App loads with WebView displaying TanStack Start app

### 5. Test Communication Patterns

In the running app, test each pattern:

1. **JavaScript Interface** - Tap button, native Toast appears
2. **Deep Linking** - Tap button, native handler logs navigation
3. **HTTP + WebSocket** - Tap button, backend notifies native app

### Hot Module Reloading

Edit files in `apps/website/src/` - changes reflect immediately in Android emulator WebView. No app rebuild required.

## Communication Patterns

### Pattern Comparison

| Pattern | Latency | Complexity | Use Case | Data Flow |
|---------|---------|------------|----------|-----------|
| **JavaScript Interface** | <1ms | Low | Synchronous native calls | WebView → Native |
| **Deep Linking** | <10ms | Medium | Navigation, small data | WebView → Native |
| **HTTP + WebSocket** | 50-200ms | High | Asynchronous events | WebView → Backend → Native |

### Detailed Analysis

#### 1. JavaScript Interface Bridge

**How it works:**
JavaScript calls `window.AndroidBridge.methodName()` directly. Kotlin method executes synchronously.

**Pros:**
- Fastest method (direct function call)
- Type-safe with `@JavaScriptInterface` annotation
- Bidirectional (native can call JS via `evaluateJavascript()`)
- No network dependency

**Cons:**
- Only works when WebView is loaded
- Security risk if methods not properly validated
- Limited to serializable data types

**When to use:**
- Quick native actions (show Toast, vibrate, log)
- Accessing device APIs (camera, location, contacts)
- Synchronous data retrieval (device info, app version)

**Example:**
```javascript
// In WebView JavaScript
window.AndroidBridge.showMessage('Hello', 'From WebView');
```

```kotlin
// In AndroidBridge.kt
@JavascriptInterface
fun showMessage(title: String, message: String) {
    Toast.makeText(context, "$title: $message", Toast.LENGTH_SHORT).show()
}
```

---

#### 2. Deep Linking

**How it works:**
WebView navigates to custom URI scheme (`myapp://action?param=value`). Android intercepts in `shouldOverrideUrlLoading()`.

**Pros:**
- Standard mobile pattern (works across iOS/Android)
- Can trigger app-to-app navigation
- Works even if WebView is destroyed (system-level routing)
- Good for passing structured data via query params

**Cons:**
- URL length limits (typically 2,048 characters)
- String-only parameters (requires serialization)
- Requires URI parsing and validation
- Can be intercepted by other apps if not secured

**When to use:**
- Navigation between screens
- Opening native settings/features
- Passing IDs or small data payloads
- Deferred deep links (notification → app → specific screen)

**Example:**
```javascript
// In WebView JavaScript
window.location.href = 'myapp://openCard?cardId=12345';
```

```kotlin
// In MainActivity.kt WebViewClient
override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
    val uri = request.url
    if (uri.scheme == "myapp") {
        handleDeepLink(uri) // Parse and route
        return true
    }
    return false
}
```

---

#### 3. HTTP + WebSocket Communication

**How it works:**
WebView sends HTTP request to backend. Backend processes and pushes notification to native app via WebSocket.

**Pros:**
- Decouples WebView from native code
- Supports server-side business logic
- Can notify multiple clients simultaneously
- Works with external triggers (push notifications, scheduled jobs)
- Enables offline queue processing

**Cons:**
- Highest latency (network round-trip)
- Requires backend infrastructure
- Needs connection management (reconnection logic)
- More complex error handling

**When to use:**
- Asynchronous workflows (payment processing, document uploads)
- Multi-client synchronization (real-time updates)
- Server-side validation before native action
- Long-running operations (background jobs)
- Analytics/telemetry (non-blocking)

**Example:**
```javascript
// In WebView JavaScript
fetch('http://10.0.2.2:3001/api/process', {
    method: 'POST',
    body: JSON.stringify({ action: 'design_card', data: {...} })
});
```

```kotlin
// In BackendClient.kt WebSocketListener
override fun onMessage(webSocket: WebSocket, text: String) {
    val notification = parseJson(text)
    // Update native UI based on server event
    mainActivity.runOnUiThread {
        updateCard(notification.data)
    }
}
```

---

## Architecture Decisions

### Why Three Patterns?

Banking stakeholders need to understand trade-offs:

- **Startups/MVPs**: JavaScript Interface (fastest implementation)
- **Enterprise/Regulated**: HTTP + WebSocket (audit trail, server validation)
- **Hybrid approach**: Deep Linking for navigation + JS Interface for actions

### Network Configuration

**From host machine:**
- Frontend: `http://localhost:3000`
- Backend: `http://localhost:3001`

**From Android emulator:**
- Frontend: `http://10.0.2.2:3000`
- Backend: `http://10.0.2.2:3001`

`10.0.2.2` is special emulator gateway to host's `localhost`.

## Debugging

### WebView Debugging

1. Ensure `WebView.setWebContentsDebuggingEnabled(true)` in code
2. Open Chrome on host machine
3. Navigate to: `chrome://inspect`
4. Click **inspect** under your app's WebView
5. Use Chrome DevTools to debug JavaScript

### Native Debugging

Android Studio Logcat filters:
- `MainActivity` - Activity lifecycle and WebView events
- `AndroidBridge` - JavaScript Interface method calls
- `BackendClient` - WebSocket connection events

### Common Issues

**App won't build:**
- Disable ZScaler/VPN
- File → Invalidate Caches → Invalidate and Restart
- Check `libs.versions.toml` has `kotlin = "2.2.0"`

**WebView shows blank screen:**
- Verify `bun dev` is running
- Check logcat for connection errors
- Try manual navigation: `http://10.0.2.2:3000` in emulator browser

**WebSocket won't connect:**
- Check backend logs for connection attempts
- Verify `INTERNET` permission in AndroidManifest.xml
- Ensure firewall allows local connections

## Production Considerations

### Security Hardening

- [ ] Replace HTTP with HTTPS (certificate pinning)
- [ ] Validate all JavaScript Interface inputs
- [ ] Implement deep link signature verification
- [ ] Add Content Security Policy headers
- [ ] Disable WebView file access (`setAllowFileAccess(false)`)

### Performance Optimization

- [ ] Enable hardware acceleration for WebView
- [ ] Implement WebSocket reconnection with exponential backoff
- [ ] Cache static web assets locally
- [ ] Add Redis pub/sub for multi-instance backend scaling

### Monitoring

- [ ] Integrate crash reporting (Sentry)
- [ ] Track JavaScript errors from WebView
- [ ] Log all communication pattern invocations
- [ ] Monitor WebSocket connection health

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Mobile Shell | Kotlin | 2.2.0 |
| Android SDK | API 28-36 | minSdk: 28, targetSdk: 36 |
| Web Framework | TanStack Start | Latest |
| Backend Framework | Hono | Latest |
| Runtime | Bun | Latest |
| Package Manager | Bun | Latest |
| Monorepo | Turborepo | Latest |

## References

- [Android WebView Documentation](https://developer.android.com/develop/ui/views/layout/webapps/webview)
- [TanStack Start Docs](https://tanstack.com/start)
- [Hono Documentation](https://hono.dev)
- [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md) - Full technical specification

## License

Proprietary - CGI Mastercard Mobile PoC
