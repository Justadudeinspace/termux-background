package com.termuxbackground;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.Executors;

final class TermuxBridge {
    static final String PERMISSION = "com.termux.permission.RUN_COMMAND";
    private static TermuxBridge instance;
    static synchronized TermuxBridge get(Context c) {
        if (instance == null) instance = new TermuxBridge(c.getApplicationContext());
        return instance;
    }
    private final android.app.Application context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    private String token, operation, code = "NOT_CHECKED", message = "Run diagnostics to check the CLI", configuration = "Not checked";
    private PendingIntent callback;
    private boolean ready, busy, uncertain;
    private long started;
    private String preparedImage;
    private TermuxBridge(Context c) {
        context = (android.app.Application) c.getApplicationContext();
        uncertain = c.getSharedPreferences("transport", 0).getBoolean("inFlight", false);
        if (uncertain) { code = "COMPLETION_UNKNOWN"; message = "Previous operation was interrupted. Stop Termux before using Recover."; }
    }
    synchronized void recover() {
        if (busy) return;
        uncertain = false; ready = false;
        context.getSharedPreferences("transport", 0).edit().putBoolean("inFlight", false).apply();
        code = "NOT_CHECKED"; message = "Run diagnostics again.";
    }
    synchronized void select(Uri uri, java.util.function.Consumer<String> preview) {
        if (busy || uncertain) return;
        preparedImage = null; busy = true; operation = "SELECT";
        token = UUID.randomUUID().toString(); started = SystemClock.elapsedRealtime();
        final String id = token;
        code = "PREPARING_IMAGE"; message = "Validating and preparing image…";
        handler.postDelayed(() -> timeout(id), Operation.TIMEOUT_MS);
        worker.execute(() -> {
            try {
                String encoded = encodeImage(uri);
                synchronized (TermuxBridge.this) {
                    if (!id.equals(token) || !busy) return;
                    preparedImage = encoded;
                    finish("COMPLETED", "Image validated and ready. Preview shows the transferred JPEG.");
                }
                handler.post(() -> preview.accept(encoded));
            } catch (Exception | OutOfMemoryError e) {
                synchronized (TermuxBridge.this) {
                    if (id.equals(token) && busy) finish("IMAGE_TRANSFER_FAILED", "Image unreadable, corrupt, unsupported or too large. Select a PNG/JPEG again.");
                }
            }
        });
    }
    private boolean installed() {
        try { return context.getPackageManager().getApplicationInfo("com.termux", 0).enabled; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }
    private boolean serviceAvailable() {
        try { return context.getPackageManager().getServiceInfo(new ComponentName("com.termux", "com.termux.app.RunCommandService"), 0).exported; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }
    synchronized String status() {
        try {
            return new JSONObject().put("termuxInstalled", installed()).put("commandInterface", serviceAvailable())
                .put("permission", context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED)
                .put("ready", ready && !busy && !uncertain && installed() && serviceAvailable()
                    && context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED).put("busy", busy).put("code", code)
                .put("selfTestPassed", ready).put("message", message).put("configuration", configuration).put("imageSelected", preparedImage != null).toString();
        } catch (org.json.JSONException e) { return "{}"; }
    }
    synchronized String dispatch(String op, String opacity, boolean blur, String animation) {
        if (busy) return status();
        if (uncertain) { message = "Previous command timed out. Stop Termux, then use Recover; completion is unknown."; return status(); }
        try {
            String[] args = Operation.monetArguments(op, opacity, blur, animation);
            if (!installed()) { finish("TERMUX_UNAVAILABLE", "Install and open Termux first."); return status(); }
            if (!serviceAvailable()) { finish("INTERFACE_UNAVAILABLE", "Use Termux 0.109 or newer with RUN_COMMAND support."); return status(); }
            if (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                finish("PERMISSION_MISSING", "Grant Run commands in Termux permission using Authorize."); return status();
            }
            if ((op.equals("APPLY") || op.equals("RESET") || op.equals("RELOAD")) && !ready)
                throw new IllegalArgumentException("Run diagnostics successfully first");
            if (op.equals("APPLY") && preparedImage == null) throw new IllegalArgumentException("Select an image first");
            if (!context.getSharedPreferences("transport", 0).edit().putBoolean("inFlight", true).commit())
                throw new IllegalArgumentException("Cannot persist operation state; check app storage");
            busy = true; operation = op; token = UUID.randomUUID().toString(); started = SystemClock.elapsedRealtime();
            code = "PREPARING"; message = "Preparing " + op.toLowerCase(java.util.Locale.ROOT);
            final String id = token; final String selected = preparedImage;
            handler.postDelayed(() -> timeout(id), Operation.TIMEOUT_MS);
            worker.execute(() -> {
                try {
                    String stdin = op.equals("APPLY") ? selected : op.equals("PROBE") ? "VEJHX1RSQU5TRkVSPTE=" : null;
                    handler.post(() -> send(id, args, stdin));
                } catch (Exception e) {
                    synchronized (TermuxBridge.this) { if (id.equals(token)) finish("IMAGE_TRANSFER_FAILED", "Image unavailable, corrupt, too large, or storage read failed. Select a PNG/JPEG again."); }
                }
            });
        } catch (IllegalArgumentException e) { code = "VALIDATION_FAILED"; message = e.getMessage(); }
        return status();
    }
    private String encodeImage(Uri uri) throws Exception {
        byte[] source;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalArgumentException();
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (bytes.size() + n > 20 * 1024 * 1024) throw new IllegalArgumentException();
                bytes.write(buffer, 0, n);
            }
            source = bytes.toByteArray();
        }
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(source, 0, source.length, options);
        Operation.validateImage(options.outWidth, options.outHeight, options.outMimeType);
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(source, 0, source.length, options);
        if (bitmap == null) throw new IllegalArgumentException();
        try {
            for (int quality = 85; quality >= 25; quality -= 10) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) throw new IllegalArgumentException();
                if (out.size() <= 122880) return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            }
            throw new IllegalArgumentException();
        } finally { bitmap.recycle(); }
    }
    private synchronized void send(String id, String[] args, String stdin) {
        if (!id.equals(token) || !busy) return;
        Intent result = new Intent(context, CommandResultReceiver.class).setData(Uri.parse("tbg-result:" + id));
        callback = PendingIntent.getBroadcast(context, 0, result, PendingIntent.FLAG_ONE_SHOT |
                (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));
        Intent command = new Intent("com.termux.RUN_COMMAND").setClassName("com.termux", "com.termux.app.RunCommandService")
            .putExtra("com.termux.RUN_COMMAND_PATH", Operation.EXECUTABLE)
            .putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args).putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            .putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", callback);
        if (stdin != null) command.putExtra("com.termux.RUN_COMMAND_STDIN", stdin);
        try {
            if (context.startService(command) == null) { finish("INTERFACE_UNAVAILABLE", "Termux command service unavailable"); return; }
            code = "DISPATCHED"; message = "Command dispatched; waiting for completion…";
        } catch (SecurityException e) { finish("PERMISSION_MISSING", "Grant command permission and enable allow-external-apps=true in Termux."); }
        catch (RuntimeException e) { finish("DISPATCH_FAILED", "Open Termux, return here, and retry diagnostics."); }
    }
    synchronized void receive(Intent intent) {
        if (!busy || intent.getData() == null || !intent.getData().toString().equals("tbg-result:" + token)) return;
        Bundle result = intent.getBundleExtra("result");
        if (result == null || !result.containsKey("err")) {
            uncertain = true; finish("INVALID_RESULT", "Malformed Termux result; completion unknown."); return;
        }
        String mapped = Operation.resultCode(result.getInt("err"), result.getInt("exitCode", -999));
        if (operation.equals("PROBE") && mapped.equals("CLI_VALIDATION_FAILED")) {
            finish("CLI_INCOMPATIBLE", "Install the CLI from this release; Monet backend and protocol 1 are required."); return;
        }
        String stdout = result.getString("stdout", "");
        if (operation.equals("PROBE") && mapped.equals("COMPLETED")) {
            if (!Operation.protocolSupported(stdout)) { finish("CLI_INCOMPATIBLE", "Install the CLI from this release (protocol 1 required)."); return; }
            if (!stdout.contains("BACKEND=monet\n") || !stdout.contains("IMAGE_TRANSFER=VERIFIED\n") || !stdout.contains("RELOAD=COMPLETED\n")) {
                finish("CLI_INCOMPATIBLE", "CLI diagnostics incomplete; install CLI from this release."); return;
            }
            ready = true;
            configuration = stdout.contains("BACKGROUND=SET\n") ? "Managed background configured" : "No managed background";
        }
        if (operation.equals("STATUS") && mapped.equals("COMPLETED"))
            configuration = stdout.contains("No managed background configured") ? "No managed background" : "Managed background configured";
        if (operation.equals("APPLY") && mapped.equals("COMPLETED")) configuration = "Managed background configured";
        if (operation.equals("RESET") && mapped.equals("COMPLETED")) configuration = "No managed background";
        finish(mapped, mapped.equals("COMPLETED") ? "CLI completed successfully. Enable Background Image in Termux Monet settings to render." :
            mapped.equals("EXECUTION_ERROR") ? "Termux rejected execution. Check allow-external-apps=true, CLI installation, and Termux notifications." :
            mapped.equals("CLI_MISSING") ? "Install the release DEB inside Termux, then run diagnostics." :
            mapped.equals("DEPENDENCY_MISSING") ? "Install file, coreutils and termux-tools inside Termux, then rerun diagnostics." :
            mapped.equals("RELOAD_UNAVAILABLE") ? "Reload unavailable; configuration may have changed. Install termux-tools and rerun diagnostics." :
            mapped.equals("CONFIG_WRITE_FAILED") ? "Configuration write failed; image may already be installed. Check Termux storage and permissions." :
            mapped.equals("CLEANUP_FAILED") ? "Operation finished but private temporary-file cleanup failed. Inspect Termux storage before retrying." :
            mapped.equals("RELOAD_FAILED") ? "Configuration changed, but reload failed. Open Termux and run termux-background reload." : mapped.replace('_', ' '));
    }
    private synchronized void timeout(String id) {
        if (busy && id.equals(token) && Operation.timedOut(SystemClock.elapsedRealtime() - started)) {
            if ("SELECT".equals(operation)) { finish("IMAGE_TRANSFER_FAILED", "Image provider timed out. Select an image again."); return; }
            uncertain = true; finish("TIMEOUT", "No completion after 60 seconds. Command may still run. Stop Termux, then use Recover.");
        }
    }
    private void finish(String c, String m) {
        if (callback != null) { callback.cancel(); callback = null; }
        if (!uncertain) context.getSharedPreferences("transport", 0).edit().putBoolean("inFlight", false).apply();
        busy = false; code = c; message = m;
        if (!c.equals("COMPLETED")) ready = false;
    }
}
