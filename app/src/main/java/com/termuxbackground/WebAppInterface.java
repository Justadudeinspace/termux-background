package com.termuxbackground;

import android.net.Uri;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;

public final class WebAppInterface {
    private final MainActivity activity;
    private final TermuxBridge transport;
    public WebAppInterface(MainActivity activity) { this.activity = activity; transport = TermuxBridge.get(activity); }
    void setLastImageUri(Uri uri) { transport.select(uri, activity::showImagePreview); }
    @JavascriptInterface public String getStatus() { return transport.status(); }
    @JavascriptInterface public void recover() { activity.runOnUiThread(activity::recoverTransport); }
    @JavascriptInterface public void authorize() { activity.runOnUiThread(activity::authorizeTermux); }
    @JavascriptInterface public void copyDiagnostics() { activity.runOnUiThread(activity::copyDiagnostics); }
    @JavascriptInterface public String run(String operation) { return transport.dispatch(operation, null, false, null); }
    @JavascriptInterface public String resetBackground() { return transport.dispatch("RESET", null, false, null); }
    @JavascriptInterface public String applyBackground(String payload) {
        try {
            if (payload == null || payload.length() > 512) throw new IllegalArgumentException();
            JSONObject values = new JSONObject(payload);
            if (values.length() != 3)
                throw new IllegalArgumentException();
            Operation.validateValues(values.get("opacity"), values.get("blur"), values.get("animation"));
            return transport.dispatch("APPLY", values.getString("opacity"), values.getBoolean("blur"), values.getString("animation"));
        } catch (Exception e) { return "{\"code\":\"VALIDATION_FAILED\",\"message\":\"Invalid apply parameters\",\"ready\":false}"; }
    }
}
