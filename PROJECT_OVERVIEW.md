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
| Mobile Shell | Android Studio, Kotlin 2.2.0, API 28-36 |
| Web Application | TanStack Start (React + TypeScript), Vite |
| Backend | Hono + Bun (with createBunWebSocket adapter) |
| Emulation | Android Studio Emulator (Pixel 8 equivalent, API 36) |

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
3. Implement WebSocket support using `createBunWebSocket()` from `hono/bun`
4. Run on http://localhost:3001

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
    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Allow http on localhost
    domStorageEnabled = true
    // Note: databaseEnabled removed (WebSQL deprecated in Chromium)
}

webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")
webView.setWebContentsDebuggingEnabled(true) // Enable Chrome DevTools debugging
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

- Set minSdk: 28, targetSdk: 36, compileSdk: 36
- Add internet permission: `<uses-permission android:name="android.permission.INTERNET" />`
- Enable cleartext traffic for localhost: `android:usesCleartextTraffic="true"`
- Register deep link intent filter in MainActivity:
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.VIEW" />
      <category android:name="android.intent.category.DEFAULT" />
      <category android:name="android.intent.category.BROWSABLE" />
      <data android:scheme="myapp" android:host="*" />
  </intent-filter>
  ```

## Dependencies

### Android (build.gradle.kts)

```kotlin
android {
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 36
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.squareup.okhttp3:okhttp:5.3.2") // Requires Kotlin 2.2.0+
}
```

**Version Compatibility Note:** OkHttp 5.3.2 requires Kotlin 2.2.0+. Ensure `libs.versions.toml` specifies `kotlin = "2.2.0"`.

### TanStack Start

```
@tanstack/start@latest
react@latest
typescript@latest
vite@latest
```

### Hono + Bun

```json
{
  "dependencies": {
    "hono": "latest"
  }
}
```

**WebSocket Implementation:** Use Hono's Bun adapter via `createBunWebSocket()` from `hono/bun` instead of external `ws` library.

**tsconfig.json Requirements:**
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "nodenext",
    "moduleResolution": "nodenext",
    "strict": true
  }
}
```

## Running Development

1. **Terminal 1: TanStack Start dev server**
   ```bash
   cd apps/website
   bun run dev  # runs on http://localhost:3000
   ```
   **Important:** Vite must use `--host` flag in `package.json` script to expose server on all network interfaces (required for emulator access via 10.0.2.2).

2. **Terminal 2: Hono backend**
   ```bash
   cd apps/backend
   bun run dev # runs on http://localhost:3001
   ```

3. **Android Studio: Build and run APK on emulator**
   - Configure emulator to Pixel 8 equivalent (API 36)
   - Gradle sync (ensure Kotlin 2.2.0 and OkHttp 5.3.2 compatibility)
   - Build and deploy to emulator
   - Use logcat filters: `MainActivity`, `AndroidBridge`, `BackendClient` for debugging

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

---

## Key Implementation Decisions & Learnings

### 1. Kotlin & Dependency Version Compatibility

**Issue:** OkHttp 5.x requires Kotlin 2.2.0+. Using older Kotlin versions causes metadata incompatibility errors.

**Solution:** Updated `libs.versions.toml` to specify `kotlin = "2.2.0"` and ensured all Kotlin-related plugins use this version.

**Learning:** Always verify library compatibility matrices before adding dependencies. OkHttp major versions have strict Kotlin compiler requirements.

### 2. Emulator Network Configuration

**Issue:** Vite dev server binds to `127.0.0.1` by default, which is not accessible from the Android emulator.

**Solution:** Added `--host` flag to Vite dev command to bind to `0.0.0.0` (all interfaces).

**Learning:** Android emulator uses `10.0.2.2` as a special gateway to host machine's localhost. Ensure dev servers listen on all interfaces, not just loopback.

### 3. WebView JavaScript Bridge Security

**Implementation:** All JavaScript-callable methods in `AndroidBridge.kt` require `@JavaScriptInterface` annotation.

**Security Note:** Only expose minimal, well-validated methods. Avoid exposing file system access or sensitive device APIs. All bridge methods should validate input and handle errors gracefully.

### 4. WebSQL Deprecation

**Issue:** `webView.settings.databaseEnabled = true` triggers deprecation warnings.

**Reason:** WebSQL is deprecated by W3C and removed from Chromium. Modern web apps use IndexedDB or localStorage.

**Solution:** Removed `databaseEnabled` setting entirely. Rely on `domStorageEnabled = true` for localStorage support.

### 5. Hono WebSocket Implementation

**Approach:** Used Hono's native Bun adapter via `createBunWebSocket()` instead of external `ws` library.

**Benefits:**
- Tight integration with Hono's routing and middleware
- No additional dependencies
- Type-safe WebSocket context (`WSContext<unknown>`)
- Simpler deployment (single export with `fetch` and `websocket`)

**Implementation Pattern:**
```typescript
import { createBunWebSocket } from "hono/bun";

const { upgradeWebSocket, websocket } = createBunWebSocket();

app.get("/ws", upgradeWebSocket((c) => ({
    onOpen(_event, ws) { /* ... */ },
    onMessage(event, ws) { /* ... */ },
    onClose(_event, ws) { /* ... */ }
})));

export default { port: 3001, fetch: app.fetch, websocket };
```

### 6. Android Theme Compatibility

**Issue:** App crashed with "You need to use a Theme.AppCompat theme" error.

**Cause:** Android Studio template defaults to Material theme, but WebView-only apps should use AppCompat.

**Solution:** Changed `themes.xml` parent from `android:Theme.Material.Light.NoActionBar` to `Theme.AppCompat.Light.NoActionBar`.

**Learning:** Match theme to UI framework. Pure WebView apps don't need Material Design themes.

### 7. Modern Kotlin Compiler Configuration

**Deprecation:** `kotlinOptions.jvmTarget` is deprecated in Gradle 8+.

**Migration:**
```kotlin
// Old (deprecated)
kotlinOptions {
    jvmTarget = "11"
}

// New (modern)
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
```

### 8. TypeScript Configuration for Modern JavaScript

**Issue:** `for (const client of connectedClients)` failed with "can only be iterated with --downlevelIteration or ES2015+".

**Solution:** Set `target: "ES2022"` in `tsconfig.json` to enable native Set/Map iteration.

**Learning:** Ensure TypeScript target matches runtime capabilities. Bun supports modern ES features, so ES2022 is appropriate.

### 9. WebView Debugging

**Best Practice:** Always enable WebView debugging in development:
```kotlin
WebView.setWebContentsDebuggingEnabled(true)
```

**Benefit:** Allows Chrome DevTools inspection via `chrome://inspect` on host machine. Essential for debugging JavaScript errors in WebView.

### 10. Pattern Naming for Non-Technical Stakeholders

**Original:** Patterns labeled as "Pattern A", "Pattern B", "Pattern C" in UI.

**Improvement:** Renamed to descriptive names for clarity:
- **JavaScript Interface Bridge** (direct native method calls)
- **Deep Linking** (custom URI navigation)
- **HTTP + WebSocket Communication** (asynchronous backend flow)

**Learning:** Use domain-appropriate terminology for stakeholder demos. Avoid abstract labels.

### 11. WebSocket Client Lifecycle Management

**Critical Implementation:**
- Connect WebSocket in `MainActivity.onCreate()`
- Disconnect in `MainActivity.onDestroy()`
- Register native client with backend after connection opens
- Clean up OkHttp dispatcher on activity destruction

**Error Handling:** Always remove failed clients from connection set to prevent memory leaks.

### 12. Biome Linting Preferences

**Ultracite Preset Rules:**
- No increment/decrement operators (`i++` → `i += 1`)
- All variables in closures must be explicitly passed as parameters
- Prefer explicit error handling over silent failures

**Learning:** Modern linters enforce patterns that prevent subtle bugs (e.g., automatic semicolon insertion issues with `++`).

---

## Production Readiness Considerations

### Security Hardening

1. **JavaScript Interface:**
   - Audit all `@JavaScriptInterface` methods for injection vulnerabilities
   - Validate all input from WebView (treat as untrusted)
   - Use allowlist approach for permitted actions

2. **Deep Link Validation:**
   - Sanitize all URI parameters
   - Implement signature verification for sensitive deep links
   - Rate-limit deep link invocations

3. **Network Security:**
   - Replace cleartext HTTP with HTTPS for production
   - Implement certificate pinning for backend communication
   - Add network security config for production builds

4. **WebView Hardening:**
   - Disable file access: `setAllowFileAccess(false)`
   - Disable universal access: `setAllowUniversalAccessFromFileURLs(false)`
   - Implement Content Security Policy headers

### Performance Optimization

1. **WebSocket Connection Management:**
   - Implement exponential backoff for reconnection
   - Add connection timeout handling
   - Pool connections if multiple WebViews are used

2. **WebView Performance:**
   - Enable hardware acceleration
   - Implement progressive loading strategies
   - Cache static assets locally

3. **Backend Scalability:**
   - Implement WebSocket connection limits
   - Add Redis pub/sub for multi-instance backend scaling
   - Use connection pooling for database access

### Monitoring & Observability

1. **Native Logging:**
   - Implement structured logging (JSON format)
   - Add correlation IDs for request tracing
   - Use analytics SDK for user behavior tracking

2. **Error Reporting:**
   - Integrate Sentry or similar for crash reporting
   - Log all WebSocket disconnections with reason codes
   - Track JavaScript errors from WebView

3. **Metrics:**
   - WebSocket connection duration and reconnection rates
   - JavaScript bridge method invocation frequency
   - Deep link navigation success rates

---

## Architecture Decisions Record (ADR)

### ADR-001: Use WebView Instead of Native UI

**Status:** Accepted

**Context:** Need to host web-based card design tool in mobile app.

**Decision:** Use Android WebView to host TanStack Start application instead of building native UI.

**Consequences:**
- ✅ Single codebase for web and mobile card design logic
- ✅ Rapid iteration without app store deployment
- ⚠️ Requires robust JavaScript ↔ native communication
- ⚠️ Dependent on WebView runtime version

### ADR-002: Three Communication Patterns

**Status:** Accepted

**Context:** Demonstrate industry-standard approaches for banking stakeholders.

**Decision:** Implement three distinct patterns (JS Bridge, Deep Linking, HTTP+WebSocket) in single demo.

**Consequences:**
- ✅ Shows architectural flexibility
- ✅ Addresses different use cases (sync, navigation, async)
- ⚠️ Increased complexity for demo app
- ⚠️ Some patterns may be redundant for actual production use

### ADR-003: Hono + Bun Over Express + Node

**Status:** Accepted

**Context:** Need backend for Pattern C demonstration.

**Decision:** Use Hono framework with Bun runtime instead of Express + Node.js.

**Rationale:**
- Modern TypeScript-first framework
- Native WebSocket support via Bun adapter
- Excellent performance characteristics
- Aligned with monorepo tech stack preferences

**Consequences:**
- ✅ Type-safe WebSocket implementation
- ✅ Single export pattern simplifies deployment
- ⚠️ Less mature ecosystem than Express
- ⚠️ Requires Bun runtime (not standard Node.js)

### ADR-004: Local Development Without Docker

**Status:** Accepted

**Context:** Need simple development setup for PoC.

**Decision:** Run all services directly on host machine without Docker containerization.

**Consequences:**
- ✅ Simpler setup for developers
- ✅ Faster iteration (no container rebuilds)
- ⚠️ Manual port management required
- ⚠️ Production deployment will need containerization

---

## Future Enhancements

### Short Term

1. **Add Unit Tests:**
   - Jest tests for TanStack Start components
   - Kotlin unit tests for AndroidBridge methods
   - Hono endpoint tests

2. **Enhanced Error Handling:**
   - Retry logic for WebSocket reconnection
   - Graceful degradation when backend unavailable
   - User-friendly error messages in WebView

3. **Documentation:**
   - Sequence diagrams for each communication pattern
   - Postman collection for backend API testing
   - Video walkthrough for stakeholder presentation

### Long Term

1. **Multi-Instance Backend:**
   - Redis pub/sub for WebSocket notifications across instances
   - Load balancer configuration
   - Horizontal scaling strategy

2. **Offline Support:**
   - Service Worker for web app offline capability
   - Local SQLite database for native app state
   - Sync mechanism when connectivity restored

3. **Security Enhancements:**
   - JWT-based authentication for WebSocket connections
   - End-to-end encryption for sensitive card data
   - Biometric authentication for native operations

4. **Cross-Platform:**
   - iOS WKWebView implementation
   - Shared Kotlin Multiplatform Mobile (KMM) business logic
   - Flutter wrapper for unified mobile codebase