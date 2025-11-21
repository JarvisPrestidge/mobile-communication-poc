declare global {
    interface Window {
        AndroidBridge?: {
            showToast: (message: string) => void;
            showMessage: (title: string, message: string) => void;
            performAction: (actionName: string, payload: string) => void;
            getDeviceInfo: () => string;
            logToNative: (message: string) => void;
            exitWebView: () => void;
        };
    }
}

export {};
