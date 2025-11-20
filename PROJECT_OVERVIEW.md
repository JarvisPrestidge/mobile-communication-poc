# Android WebView Mobile Digital Card Design PoC - Technical Specification

## Overview

Build a proof-of-concept Android shell application that hosts a digital card design web application within a WebView. Demonstrate three industry-standard approaches for native ↔ WebView communication suitable for banking stakeholders.

## Goals

- Build minimal Android shell app hosting a local WebView
- Implement three distinct communication patterns between WebView and native Android
- Demonstrate deep linking navigation from WebView to native functionality
- Test HTTP request flow with Hono/Bun backend notification to native app
- Provide clear, production-quality code suitable for banking industry

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Mobile Shell | Android Studio, Kotlin, API 28+ |
| Web Application | TanStack Start (React + TypeScript) |
| Backend | Hono + Bun |
| Emulation | Android Studio Emulator (Pixel 8 equivalent, API 34+) |

## System Architecture

### Native Android Shell

Minimal Activity-based application with core responsibilities:

- Host WebView component loading local TanStack Start app
- Implement JavaScript Interface bridge for direct native method invocation from JS
- Handle custom URI schemes (deep links) for navigation and data passing
- Manage HTTP client for server-initiated notifications
- Configure WebView security policies and permissions

### WebView Communication Patterns

#### Pattern A: JavaScript Interface Bridge

Direct method invocation from JavaScript to native Android code. Cleanest and most performant for synchronous operations.

**Flow:** JavaScript calls `window.AndroidBridge.methodName()` → Kotlin method executes → Optional callback returned via JavaScript

#### Pattern B: Deep Linking

Navigation via custom URI schemes. Useful for navigation flows and passing small amounts of data.

**Flow:** JavaScript navigates to `myapp://action?data=value` → Android intercepts in `shouldOverrideUrlLoading` → Native handler processes → Optional return to WebView state

#### Pattern C: HTTP Request → Backend Notification

WebView makes HTTP request to Hono/Bun backend; backend notifies native app via WebSocket. Best for asynchronous, stateful operations.

**Flow:** JavaScript sends `fetch()` request to backend → Backend processes → Backend notifies native app (WebSocket) → Native app updates UI or state

## Project Structure

```
android/
├── app/
│   ├── src/main/java/com/example/mastercarddesignstudiopoc/
│   │   ├── MainActivity.kt
│   │   ├── AndroidBridge.kt
│   │   ├── DeepLinkHandler.kt
│   │   └── BackendClient.kt
│   ├── src/main/AndroidManifest.xml
│   └── build.gradle.kts
│
website/ (TanStack Start)
│   ├── src/
│   │   ├── routes/
│   │   └── components/
│   │       └── ui/ (shadn components)
│   ├── package.json
│   └── tsconfig.json
│
backend/ (Hono + Bun)
│   ├── src/
│       ├── index.ts
│   ├── package.json
│   └── bunfig.toml
```

## Implementation Phases

### Phase 1: Environment Setup

1. Install Android Studio on macOS
2. Create Android Virtual Device (AVD) for Pixel 8 equivalent (API 34+)
3. Verify emulator network connectivity for localhost access (emulator default gateway: 10.0.2.2 from emulator to host)

### Phase 2: Local TanStack Start Development

1. Set up TanStack Start project (TypeScript, Vite)
2. Add demo buttons for each communication pattern
3. Configure to load at http://localhost:3000
4. Ensure WebView can access via http://10.0.2.2:3000 from emulator

### Phase 3: Hono/Bun Backend

1. Create Hono + Bun server
2. Set up endpoints for Pattern C HTTP communication
3. Implement WebSocket support for native notifications
4. Run on http://localhost:3000

### Phase 4: Android Shell Implementation

1. Create new Android project in Android Studio (Kotlin)
2. Implement MainActivity with WebView hosting logic
3. Add AndroidBridge.kt with @JavaScriptInterface methods
4. Configure WebView security and JavaScript enabled
5. Implement shouldOverrideUrlLoading for deep link interception

### Phase 5: Communication Patterns Implementation

1. Implement Pattern A: JavaScript Interface
   - Methods in AndroidBridge.kt that JS can call directly
   - Examples: showMessage(), performAction(), getDeviceInfo()

2. Implement Pattern B: Deep Link Handler
   - Custom URI scheme: `myapp://[action]?[params]`
   - Examples:
     - `myapp://navigate?screen=settings`
     - `myapp://openCard?cardId=12345`
     - `myapp://callNative?method=shareCard&data=value`

3. Implement Pattern C: HTTP + WebSocket
   - JS makes fetch() to backend
   - Backend processes and sends WebSocket event to native app
   - Native app receives and updates UI

### Phase 6: Testing & Documentation

1. Test all communication patterns in emulator
2. Create test scenarios for each pattern
3. Verify network communication (localhost to emulator)
4. Generate README with setup and running instructions

## Detailed Specifications

### MainActivity.kt

Responsibilities:

- Initialize and configure WebView
- Add AndroidBridge as JavaScript interface
- Handle WebView client callbacks (shouldOverrideUrlLoading for deep links)
- Load http://10.0.2.2:3000 (localhost from emulator perspective)
- Log lifecycle events and navigation events

Key configuration:

```kotlin
val settings = webView.settings.apply {
    javaScriptEnabled = true
    mixedContentMode = WebSettings.MIXED_CONTENT_ALLOW_ALL // Allow http on localhost
    domStorageEnabled = true
    databaseEnabled = true
}

webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")
webView.loadUrl("http://10.0.2.2:3000")
```

### AndroidBridge.kt

JavaScript-callable methods for Pattern A:

- `showMessage(title: String, message: String)` — Display native Android Toast or Dialog
- `performAction(actionName: String, payload: String)` — Execute native business logic
- `getDeviceInfo(): String` — Return device/app metadata as JSON
- `logToNative(message: String)` — Log from WebView to native logcat

All methods must be annotated with `@JavaScriptInterface`

### DeepLinkHandler.kt

Intercepts custom URI schemes in `shouldOverrideUrlLoading`:

- Parse `myapp://[action]?[params]` format
- Extract action and query parameters
- Route to appropriate handler method
- Log all deep link invocations for debugging

### BackendClient.kt

Manages HTTP/WebSocket communication:

- OkHttp client for HTTP requests
- WebSocket listener for server push notifications
- Callback mechanism to notify MainActivity of server events
- Error handling and retry logic
- Connect to http://10.0.2.2:3000 (backend from emulator)

### Hono Backend (src/index.ts)

Endpoints for Pattern C:

```typescript
// POST /api/card/design - accept design data from WebView
// Validate and process
// Send WebSocket notification to connected native clients
// Example: { type: "design_processed", data: { ...result } }

// GET /api/health - health check endpoint

// WebSocket /ws - bidirectional communication
// Listen for "register" from native app
// Send notifications on design events
```

## Networking

**From Host (macOS dev machine):**
- TanStack dev server: http://localhost:3000
- Hono backend: http://localhost:3001

**From Android Emulator:**
- TanStack dev server: http://10.0.2.2:3000
- Hono backend: http://10.0.2.2:3001

## Testing Strategy

### Communication Pattern Tests

| Pattern | Test Case | Validation |
|---------|-----------|-----------|
| JS Bridge | Call `AndroidBridge.showMessage()` | Toast/Dialog displayed with correct text |
| JS Bridge | Call `AndroidBridge.performAction()` | Native method executes; logs confirm execution |
| Deep Link | Navigate to custom URI | Custom handler invoked; logs confirm param parsing |
| HTTP + Backend | Fetch to backend; backend notifies native | Native receives notification; state/UI updated |

### Network Tests

1. Verify emulator can reach host machine on 10.0.2.2
2. Test WebView loads TanStack app successfully
3. Test HTTP requests from WebView to backend
4. Test WebSocket connection from native app to backend

## Code Quality Standards

### Kotlin Standards

- Follow Kotlin style guide (Google's Kotlin style guide)
- Use sealed classes and data classes for type safety
- Implement null safety with nullable vs non-nullable types
- Use coroutines for asynchronous operations
- Add comprehensive inline documentation for complex logic

### TypeScript/React Standards

- Strict tsconfig settings (no implicit any, strict mode)
- Use functional components with hooks
- Type all props and state explicitly
- Add JSDoc comments for public functions and components

### Android Manifest Requirements

- Set minSdk: 28, targetSdk: 34+
- Add internet permission: `<uses-permission android:name="android.permission.INTERNET" />`
- Register deep link intent filter in MainActivity
- Configure WebView provider if needed

## Dependencies

### Android (build.gradle.kts)

```
minSdk: 28
targetSdk: 34

implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("com.google.code.gson:gson:2.10.1")
```

### TanStack Start

```
@tanstack/start@latest
react@latest
typescript@latest
vite@latest
```

### Hono + Bun

```
hono@latest
ws@latest (for WebSocket support)
```

## Running Development

1. **Terminal 1: TanStack Start dev server**
   ```bash
   cd apps/website
   npm run dev  # runs on http://localhost:3000
   ```

2. **Terminal 2: Hono backend**
   ```bash
   cd apps/backend
   bun run dev # runs on http://localhost:3001
   ```

3. **Android Studio: Build and run APK on emulator**
   - Configure emulator to Pixel 8 equivalent
   - Build and deploy to emulator
   - Use logcat for debugging

## Success Criteria

- All three communication patterns working in emulator
- WebView loads TanStack Start app correctly
- JavaScript ↔ native communication bidirectional and reliable
- Network communication verified (WebView to backend via 10.0.2.2)
- Code demonstrates industry-standard patterns suitable for banking presentation
- Documentation clear enough for external developers to understand architecture

## Deliverables

- Functional Android shell app (.apk)
- TanStack Start web application
- Hono/Bun backend server
- README.md with setup and running instructions
- Architecture documentation
- Code with comprehensive comments and documentation