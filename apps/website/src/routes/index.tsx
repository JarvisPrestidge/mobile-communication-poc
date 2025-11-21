import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Alert, AlertDescription, AlertTitle } from "~/components/ui/alert";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "~/components/ui/card";

export const Route = createFileRoute("/")({
    component: HomePage,
});

function HomePage() {
    const [isAndroid, setIsAndroid] = useState(false);
    const [deviceInfo, setDeviceInfo] = useState<Record<string, unknown> | null>(null);
    const [backendResponse, setBackendResponse] = useState<string | null>(null);

    useEffect(() => {
        // Detect Android WebView
        const hasAndroidBridge = typeof window.AndroidBridge !== "undefined";
        setIsAndroid(hasAndroidBridge);

        if (hasAndroidBridge) {
            // Log to native on page load
            window.AndroidBridge?.logToNative("WebView page loaded successfully");

            // Get device info
            try {
                const infoJson = window.AndroidBridge?.getDeviceInfo();
                if (infoJson) {
                    const info = JSON.parse(infoJson);
                    setDeviceInfo(info);
                }
            } catch (error) {
                console.error("Failed to get device info:", error);
            }
        }
    }, []);

    // JavaScript Interface Bridge handlers
    const handleShowToast = () => {
        if (window.AndroidBridge) {
            window.AndroidBridge.showToast("Hello from WebView!");
            window.AndroidBridge.logToNative("showToast button clicked");
        } else {
            toast.error("AndroidBridge not available (not running in WebView)");
        }
    };

    const handleShowDialog = () => {
        if (window.AndroidBridge) {
            window.AndroidBridge.showMessage(
                "JavaScript Bridge Demo",
                "This is a native Android dialog triggered from JavaScript!"
            );
            window.AndroidBridge.logToNative("showMessage button clicked");
        } else {
            toast.error("AndroidBridge not available (not running in WebView)");
        }
    };

    const handlePerformAction = () => {
        if (window.AndroidBridge) {
            const payload = JSON.stringify({
                cardId: "12345",
                cardName: "Premium Card",
                timestamp: new Date().toISOString(),
            });
            window.AndroidBridge.performAction("shareCard", payload);
            window.AndroidBridge.logToNative(`performAction called with payload: ${payload}`);
        } else {
            toast.error("AndroidBridge not available (not running in WebView)");
        }
    };

    // Deep Link handlers
    const handleNavigateDeepLink = () => {
        window.location.href = "myapp://navigate?screen=settings";
    };

    const handleOpenCardDeepLink = () => {
        window.location.href = "myapp://openCard?cardId=67890";
    };

    const handleCallNativeDeepLink = () => {
        window.location.href = "myapp://callNative?method=shareCard&data=testValue";
    };

    const handleCustomDialogDeepLink = () => {
        window.location.href = "myapp://showDialog?title=Deep Link Test&message=Hello from deep link!";
    };

    // HTTP + WebSocket handler
    const handleSendToBackend = async () => {
        try {
            setBackendResponse("Sending to backend...");

            const response = await fetch("http://10.0.2.2:3001/api/card/design", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    cardData: {
                        cardNumber: "**** **** **** 1234",
                        cardholderName: "John Doe",
                        expiryDate: "12/25",
                        cvv: "***",
                        designColor: "blue",
                        timestamp: new Date().toISOString(),
                    },
                }),
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            setBackendResponse(JSON.stringify(data, null, 2));
            toast.success(`Backend processed design! Notified ${data.notifiedClients} native clients.`);

            window.AndroidBridge?.logToNative(`Backend response: ${JSON.stringify(data)}`);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : "Unknown error";
            setBackendResponse(`Error: ${errorMessage}`);
            toast.error(`Backend error: ${errorMessage}`);
            console.error("Backend request failed:", error);
        }
    };

    return (
        <div className="container mx-auto min-h-svh p-6">
            <div className="mb-8">
                <h1 className="mb-4 text-center font-bold text-4xl">WebView Communication Demo</h1>
                <h2 className="font-semibold text-lg">Demonstrating three patterns:</h2>
                <p className="text-muted-foreground">
                    • JavaScript Interface Bridge
                    {<br />}• Deep Linking
                    {<br />}• HTTP + WebSocket
                </p>
            </div>

            {/* Platform Detection */}
            <Alert className="mb-6">
                <AlertTitle>Platform Status</AlertTitle>
                <AlertDescription className="flex items-center gap-2">
                    {isAndroid ? (
                        <>
                            <Badge className="bg-green-600" variant="default">
                                Android WebView
                            </Badge>
                            <span>Running in native Android WebView</span>
                        </>
                    ) : (
                        <>
                            <Badge variant="secondary">Browser</Badge>
                            <span>Running in web browser (some features unavailable)</span>
                        </>
                    )}
                </AlertDescription>
            </Alert>

            {/* Device Info */}
            {deviceInfo && (
                <Card className="mb-6">
                    <CardHeader>
                        <CardTitle>Device Information</CardTitle>
                        <CardDescription>Retrieved via AndroidBridge.getDeviceInfo()</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <dl className="grid grid-cols-2 gap-2 text-sm">
                            {Object.entries(deviceInfo).map(([key, value]) => (
                                <div className="flex flex-col" key={key}>
                                    <dt className="font-medium text-muted-foreground">{key}</dt>
                                    <dd className="truncate font-mono">{String(value)}</dd>
                                </div>
                            ))}
                        </dl>
                    </CardContent>
                </Card>
            )}

            {/* JavaScript Interface Bridge */}
            <Card className="mb-6">
                <CardHeader>
                    <CardTitle>JavaScript Interface Bridge</CardTitle>
                    <CardDescription>
                        Direct method invocation from JavaScript to native Android code via window.AndroidBridge
                    </CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-3">
                    <Button className="w-full" onClick={handleShowToast} variant="default">
                        Show Toast
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Calls: <code className="rounded bg-muted px-1 py-0.5">window.AndroidBridge.showToast()</code>
                    </p>

                    <Button className="w-full" onClick={handleShowDialog} variant="default">
                        Show Dialog
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Calls: <code className="rounded bg-muted px-1 py-0.5">window.AndroidBridge.showMessage()</code>
                    </p>

                    <Button className="w-full" onClick={handlePerformAction} variant="default">
                        Perform Action (Share Card)
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Calls:{" "}
                        <code className="rounded bg-muted px-1 py-0.5">window.AndroidBridge.performAction()</code>
                    </p>
                </CardContent>
            </Card>

            {/* Deep Linking */}
            <Card className="mb-6">
                <CardHeader>
                    <CardTitle>Deep Linking</CardTitle>
                    <CardDescription>
                        Navigation via custom URI schemes (myapp://) intercepted by native Android
                    </CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-3">
                    <Button className="w-full" onClick={handleNavigateDeepLink} variant="secondary">
                        Navigate to Settings
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Deep Link:{" "}
                        <code className="rounded bg-muted px-1 py-0.5">myapp://navigate?screen=settings</code>
                    </p>

                    <Button className="w-full" onClick={handleOpenCardDeepLink} variant="secondary">
                        Open Card #67890
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Deep Link: <code className="rounded bg-muted px-1 py-0.5">myapp://openCard?cardId=67890</code>
                    </p>

                    <Button className="w-full" onClick={handleCallNativeDeepLink} variant="secondary">
                        Call Native Method
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Deep Link:{" "}
                        <code className="rounded bg-muted px-1 py-0.5">
                            myapp://callNative?method=shareCard&data=testValue
                        </code>
                    </p>

                    <Button className="w-full" onClick={handleCustomDialogDeepLink} variant="secondary">
                        Show Custom Dialog
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Deep Link:{" "}
                        <code className="rounded bg-muted px-1 py-0.5">
                            myapp://showDialog?title=Deep Link Test&message=...
                        </code>
                    </p>
                </CardContent>
            </Card>

            {/* HTTP + WebSocket Communication */}
            <Card className="mb-6">
                <CardHeader>
                    <CardTitle>HTTP + WebSocket Communication</CardTitle>
                    <CardDescription>
                        WebView sends HTTP request to backend, backend processes and notifies native app via WebSocket
                    </CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-3">
                    <Button className="w-full" onClick={handleSendToBackend} variant="outline">
                        Send Card Design to Backend
                    </Button>
                    <p className="text-muted-foreground text-xs">
                        Flow: WebView → POST /api/card/design → Backend processes → WebSocket notification to native app
                    </p>

                    {backendResponse && (
                        <div className="mt-3">
                            <p className="mb-1 font-medium text-sm">Backend Response:</p>
                            <pre className="overflow-x-auto rounded-md border bg-muted p-2 text-xs">
                                {backendResponse}
                            </pre>
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Instructions */}
            <Card>
                <CardHeader>
                    <CardTitle>Testing Instructions</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                    <p>
                        <strong>JavaScript Interface Bridge:</strong> Buttons call native methods directly through
                        window.AndroidBridge. Watch for Toast messages and Dialogs.
                    </p>
                    <p>
                        <strong>Deep Linking:</strong> Buttons navigate to custom myapp:// URLs, intercepted by native
                        shouldOverrideUrlLoading handler.
                    </p>
                    <p>
                        <strong>HTTP + WebSocket:</strong> WebView sends HTTP request to backend, which then notifies
                        the native app via WebSocket. Check native app for dialog notification.
                    </p>
                    <p className="text-muted-foreground">
                        Open Android Studio logcat with filter "MainActivity", "AndroidBridge", or "BackendClient" to
                        see detailed logs.
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}
