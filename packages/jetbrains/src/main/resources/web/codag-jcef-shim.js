/**
 * JCEF Bridge Shim — replaces VS Code acquireVsCodeApi() for JetBrains JCEF WebView.
 *
 * Message flow:
 *   JS → Kotlin: window.codagBridge.postMessage(msg) → CefMessageRouter → CodagMessageBridge
 *   Kotlin → JS: CodagMessageBridge → browser.executeJavaScript("window.__codagDispatch(json)")
 */
(function () {
    'use strict';

    var pendingCallbacks = {};
    var messageListeners = [];

    // Bridge object that replaces vscode API
    var bridge = {
        postMessage: function (message) {
            var json = JSON.stringify(message);
            // CefMessageRouter query — Kotlin side handles via CefMessageRouterHandler
            if (window.cefQuery) {
                window.cefQuery({
                    request: json,
                    onSuccess: function () { },
                    onFailure: function (code, msg) {
                        console.error('[codag-bridge] cefQuery failed:', code, msg);
                    }
                });
            } else {
                console.warn('[codag-bridge] cefQuery not available, message dropped:', json);
            }
        },
        getState: function () {
            try {
                var raw = window.sessionStorage.getItem('__codag_state__');
                return raw ? JSON.parse(raw) : undefined;
            } catch (e) {
                return undefined;
            }
        },
        setState: function (state) {
            try {
                window.sessionStorage.setItem('__codag_state__', JSON.stringify(state));
            } catch (e) { /* ignore */ }
            return state;
        }
    };

    // Incoming messages from Kotlin side
    window.__codagDispatch = function (messageJson) {
        try {
            var message = typeof messageJson === 'string' ? JSON.parse(messageJson) : messageJson;
            // Dispatch as MessageEvent to match VS Code webview protocol
            var event = new MessageEvent('message', { data: message });
            window.dispatchEvent(event);
        } catch (e) {
            console.error('[codag-bridge] dispatch error:', e);
        }
    };

    // Override acquireVsCodeApi to return our bridge
    window.acquireVsCodeApi = function () {
        return bridge;
    };

    // Expose bridge globally for direct access
    window.codagBridge = bridge;
})();
