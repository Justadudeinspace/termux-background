package com.termuxbackground;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WebAppInterface {

    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_API_PACKAGE = "com.termux.api";
    private static final String TERMUX_API_ACTION = "com.termux.api.action.RUN_COMMAND";
    private static final String TERMUX_HOME = "/data/data/com.termux/files/home";
    private static final String TERMUX_CONFIG_DIR = TERMUX_HOME + "/.termux";
    private static final String BACKGROUND_NAME = "background.png";

    private final Context context;
    private final ContentResolver contentResolver;
    private final PackageManager packageManager;
    private final WebView webView;

    private Uri lastImageUri;

    public WebAppInterface(Context context, WebView webView) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
        this.packageManager = context.getPackageManager();
        this.webView = webView;
    }

    public void setLastImageUri(Uri uri) {
        this.lastImageUri = uri;
    }

    @JavascriptInterface
    public String getStatus() {
        try {
            JSONObject status = buildStatus();
            return status.toString();
        } catch (Exception e) {
            return buildError("Failed to build status: " + e.getMessage()).toString();
        }
    }

    @JavascriptInterface
    public String applyBackground(String payloadJson) {
        JSONObject result = new JSONObject();
        try {
            JSONObject payload = new JSONObject(payloadJson == null ? "{}" : payloadJson);
            Uri imageUri = resolveImageUri(payload.optString("imageUri", null));
            String opacityStr = payload.optString("opacity", "");
            String animation = payload.optString("animation", "none");
            boolean blur = payload.optBoolean("blur", false);

            Status status = parseStatus();
            if (!status.canRunCommands) {
                return buildBlocked("Termux or Termux:API missing. Install Termux and Termux:API to continue.").toString();
            }

            if (imageUri == null) {
                return buildError("Select an image before applying.").toString();
            }

            double opacity = parseOpacity(opacityStr);
            if (Double.isNaN(opacity)) {
                return buildError("Invalid opacity value.").toString();
            }

            if (!validateAnimation(animation)) {
                return buildError("Invalid animation option.").toString();
            }

            String mimeType = contentResolver.getType(imageUri);
            if (!isSupportedMime(mimeType)) {
                return buildError("Unsupported image type. Use PNG or JPEG.").toString();
            }

            File termuxDir = new File(TERMUX_CONFIG_DIR);
            if (!termuxDir.exists() && !termuxDir.mkdirs()) {
                return buildError("Unable to create Termux config directory.").toString();
            }

            File backgroundFile = new File(termuxDir, BACKGROUND_NAME);
            copyUriToFile(imageUri, backgroundFile);

            File propsFile = new File(termuxDir, "termux.properties");
            mergeProperties(propsFile, opacity, blur, animation);

            JSONObject reloadResult = runTermuxApiCommand("termux-reload-settings", new JSONArray(), TERMUX_HOME, true);
            if (!reloadResult.optBoolean("ok", false)) {
                return reloadResult.toString();
            }

            result.put("ok", true);
            result.put("blocked", false);
            result.put("message", "Background applied and Termux settings reloaded.");
            return result.toString();
        } catch (JSONException e) {
            return buildError("Invalid payload: " + e.getMessage()).toString();
        } catch (IOException e) {
            return buildError("Failed to write files: " + e.getMessage()).toString();
        }
    }

    @JavascriptInterface
    public String resetBackground() {
        try {
            Status status = parseStatus();
            if (!status.canRunCommands) {
                return buildBlocked("Termux or Termux:API missing. Install Termux and Termux:API to continue.").toString();
            }

            File propsFile = new File(TERMUX_CONFIG_DIR, "termux.properties");
            clearBackgroundKeys(propsFile);

            File backgroundFile = new File(TERMUX_CONFIG_DIR, BACKGROUND_NAME);
            if (backgroundFile.exists() && !backgroundFile.delete()) {
                return buildError("Unable to delete existing background image.").toString();
            }

            JSONObject reloadResult = runTermuxApiCommand("termux-reload-settings", new JSONArray(), TERMUX_HOME, true);
            if (!reloadResult.optBoolean("ok", false)) {
                return reloadResult.toString();
            }

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("blocked", false);
            result.put("message", "Background settings reset and Termux reloaded.");
            return result.toString();
        } catch (Exception e) {
            return buildError("Failed to reset: " + e.getMessage()).toString();
        }
    }

    @JavascriptInterface
    public void openTermuxApiInstallHelp() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Termux:API"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private Status parseStatus() {
        Status status = new Status();
        status.termuxInstalled = isPackageInstalled(TERMUX_PACKAGE);
        status.termuxApiInstalled = isPackageInstalled(TERMUX_API_PACKAGE);
        status.canRunCommands = status.termuxInstalled && status.termuxApiInstalled && canResolveApiBroadcast();
        if (!status.termuxInstalled) {
            status.lastError = "Termux not installed";
        } else if (!status.termuxApiInstalled) {
            status.lastError = "Termux:API not installed";
        } else if (!status.canRunCommands) {
            status.lastError = "Termux:API broadcast unavailable";
        }
        return status;
    }

    private JSONObject buildStatus() throws JSONException {
        Status status = parseStatus();
        JSONObject statusJson = new JSONObject();
        statusJson.put("termuxInstalled", status.termuxInstalled);
        statusJson.put("termuxApiInstalled", status.termuxApiInstalled);
        statusJson.put("canRunCommands", status.canRunCommands);
        statusJson.put("lastError", status.lastError);
        statusJson.put("appVersion", BuildConfig.VERSION_NAME);
        return statusJson;
    }

    private boolean validateAnimation(String animation) {
        return TextUtils.equals(animation, "none") || TextUtils.equals(animation, "scroll");
    }

    private boolean isSupportedMime(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.equals("image/png") || mimeType.equals("image/jpeg");
    }

    private double parseOpacity(String opacityStr) {
        try {
            double value = Double.parseDouble(opacityStr);
            if (value >= 0.0 && value <= 1.0) {
                return value;
            }
            return Double.NaN;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private Uri resolveImageUri(String uriFromPayload) {
        if (!TextUtils.isEmpty(uriFromPayload)) {
            return Uri.parse(uriFromPayload);
        }
        return lastImageUri;
    }

    private boolean isPackageInstalled(String pkg) {
        try {
            packageManager.getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean canResolveApiBroadcast() {
        Intent intent = new Intent(TERMUX_API_ACTION);
        intent.setPackage(TERMUX_API_PACKAGE);
        ResolveInfo info = packageManager.resolveBroadcast(intent, 0);
        return info != null;
    }

    private void copyUriToFile(Uri uri, File destination) throws IOException {
        try (InputStream in = contentResolver.openInputStream(uri); OutputStream out = new FileOutputStream(destination)) {
            if (in == null) {
                throw new IOException("Unable to open selected file.");
            }
            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    private void mergeProperties(File propsFile, double opacity, boolean blur, String animation) throws IOException {
        Map<String, String> desired = new HashMap<>();
        desired.put("background", BACKGROUND_NAME);
        desired.put("background.opacity", String.valueOf(opacity));
        desired.put("background.blur", String.valueOf(blur));
        desired.put("background.animation", animation);

        List<String> lines = new ArrayList<>();
        if (propsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propsFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        }

        Set<String> handledKeys = new HashSet<>();
        List<String> updated = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            boolean replaced = false;
            for (String key : desired.keySet()) {
                if (trimmed.startsWith(key + "=")) {
                    updated.add(key + "=" + desired.get(key));
                    handledKeys.add(key);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                updated.add(line);
            }
        }

        List<String> missingLines = new ArrayList<>();
        for (Map.Entry<String, String> entry : desired.entrySet()) {
            if (!handledKeys.contains(entry.getKey())) {
                missingLines.add(entry.getKey() + "=" + entry.getValue());
            }
        }

        if (!missingLines.isEmpty()) {
            if (!updated.isEmpty()) {
                updated.add("");
            }
            updated.add("# Termux Background");
            updated.addAll(missingLines);
        }

        writeLines(propsFile, updated);
    }

    private void clearBackgroundKeys(File propsFile) throws IOException {
        if (!propsFile.exists()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propsFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        List<String> filtered = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("background=") ||
                trimmed.startsWith("background.opacity=") ||
                trimmed.startsWith("background.blur=") ||
                trimmed.startsWith("background.animation=")) {
                continue;
            }
            filtered.add(line);
        }

        writeLines(propsFile, filtered);
    }

    private void writeLines(File file, List<String> lines) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                fos.write(line.getBytes(StandardCharsets.UTF_8));
                if (i < lines.size() - 1) {
                    fos.write('\n');
                }
            }
        }
    }

    private JSONObject runTermuxApiCommand(String command, JSONArray args, String cwd, boolean background) {
        Status status = parseStatus();
        if (!status.termuxInstalled || !status.termuxApiInstalled) {
            return buildBlocked("Install Termux and Termux:API to continue.");
        }

        Intent intent = new Intent(TERMUX_API_ACTION);
        intent.setPackage(TERMUX_API_PACKAGE);
        intent.putExtra("com.termux.api.extra.COMMAND", command);
        intent.putExtra("com.termux.api.extra.ARGUMENTS", toStringArray(args));
        intent.putExtra("com.termux.api.extra.WORKDIR", cwd);
        intent.putExtra("com.termux.api.extra.BACKGROUND", background);

        ResolveInfo resolveInfo = packageManager.resolveBroadcast(intent, 0);
        if (resolveInfo == null) {
            return buildError("Termux:API invocation failed: unable to resolve broadcast receiver.");
        }

        try {
            context.sendBroadcast(intent);
            JSONObject response = new JSONObject();
            response.put("ok", true);
            response.put("blocked", false);
            response.put("message", "Reload triggered");
            return response;
        } catch (Exception e) {
            return buildError("Termux:API invocation failed: " + e.getMessage());
        }
    }

    private String[] toStringArray(JSONArray array) {
        String[] out = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            out[i] = array.optString(i, "");
        }
        return out;
    }

    private JSONObject buildError(String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ok", false);
            obj.put("blocked", false);
            obj.put("message", message);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    private JSONObject buildBlocked(String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ok", false);
            obj.put("blocked", true);
            obj.put("message", message);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    private static class Status {
        boolean termuxInstalled;
        boolean termuxApiInstalled;
        boolean canRunCommands;
        String lastError;
    }
}
