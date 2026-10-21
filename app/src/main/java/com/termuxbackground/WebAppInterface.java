package com.termuxbackground;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

/** Narrow bridge for the bundled, local UI. The app cannot access Termux's private files. */
public final class WebAppInterface {
    private final Context context;

    public WebAppInterface(Context context) {
        this.context = context.getApplicationContext();
    }

    @JavascriptInterface
    public String getStatus() {
        JSONObject status = new JSONObject();
        try {
            boolean termux = installed("com.termux");
            boolean api = installed("com.termux.api");
            status.put("termuxInstalled", termux);
            status.put("termuxApiInstalled", api);
            status.put("canRunCommands", false);
            status.put("configurationAccessible", false);
            status.put("appVersion", BuildConfig.VERSION_NAME);
            status.put("androidSdk", Build.VERSION.SDK_INT);
            status.put("lastError", termux
                    ? "Android isolates Termux's private configuration. Use the Termux CLI for changes."
                    : "Termux is not installed.");
        } catch (JSONException ignored) {
            return "{}";
        }
        return status.toString();
    }

    @JavascriptInterface
    public String applyBackground(String ignoredPayload) {
        return unavailable("Use 'termux-background set --image PATH' inside Termux.");
    }

    @JavascriptInterface
    public String resetBackground() {
        return unavailable("Use 'termux-background reset' inside Termux.");
    }

    private String unavailable(String message) {
        JSONObject result = new JSONObject();
        try {
            result.put("ok", false);
            result.put("blocked", true);
            result.put("code", "CONFIG_ACCESS_UNAVAILABLE");
            result.put("message", message);
        } catch (JSONException ignored) {
            return "{\"ok\":false,\"blocked\":true}";
        }
        return result.toString();
    }

    private boolean installed(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
