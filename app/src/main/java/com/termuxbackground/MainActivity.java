package com.termuxbackground;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TermuxBG";
    private static final int REQUEST_FLAG = Intent.FLAG_GRANT_READ_URI_PERMISSION
        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;

    private WebView webView;
    @Nullable
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private WebAppInterface bridge;
    private boolean webViewReached = false;
    private String lastError = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "BOOT: onCreate start");
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "BOOT: content view set");

            webView = findViewById(R.id.webview);
            Log.d(TAG, "BOOT: webview init start");

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowContentAccess(true);
            Log.d(TAG, "BOOT: webview settings applied");

            bridge = new WebAppInterface(this, webView);
            webView.addJavascriptInterface(bridge, "Android");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    Log.d(TAG, "BOOT: webview onPageStarted: " + url);
                    webViewReached = true;
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "BOOT: webview onPageFinished: " + url);
                    super.onPageFinished(view, url);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "BOOT: webview onReceivedError (deprecated): " + description);
                    lastError = "Error " + errorCode + ": " + description + " at " + failingUrl;
                    super.onReceivedError(view, errorCode, description, failingUrl);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Log.e(TAG, "BOOT: webview onReceivedError: " + error.getDescription());
                        if (request.isForMainFrame()) {
                            lastError = "Error " + error.getErrorCode() + ": " + error.getDescription();
                        }
                    }
                    super.onReceivedError(view, request, error);
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                    if (MainActivity.this.filePathCallback != null) {
                        MainActivity.this.filePathCallback.onReceiveValue(null);
                    }
                    MainActivity.this.filePathCallback = filePathCallback;
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    intent.addFlags(REQUEST_FLAG);
                    fileChooserLauncher.launch(intent);
                    return true;
                }
            });

            fileChooserLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleFileChooserResult);

            webView.loadUrl("file:///android_asset/termux-background-ui.html");
            Log.d(TAG, "BOOT: webview loadUrl issued");

        } catch (Exception e) {
            Log.e(TAG, "BOOT: Fatal error during onCreate", e);
            lastError = e.getMessage();
            showFallbackUI(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Diagnostics");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            showDiagnostics();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFallbackUI(Exception e) {
        try {
            setContentView(R.layout.activity_fallback);
            Log.d(TAG, "BOOT: fallback UI set");

            TextView diagnostics = findViewById(R.id.fallback_diagnostics);
            String diagText = buildDiagnostics(e);
            diagnostics.setText(diagText);

            Button copyBtn = findViewById(R.id.btn_copy_diagnostics);
            copyBtn.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Diagnostics", diagText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show();
            });

            Button termuxBtn = findViewById(R.id.btn_open_termux);
            termuxBtn.setOnClickListener(v -> launchPackage("com.termux"));

            Button termuxApiBtn = findViewById(R.id.btn_open_termux_api);
            termuxApiBtn.setOnClickListener(v -> launchPackage("com.termux.api"));

        } catch (Exception fallbackError) {
            Log.e(TAG, "BOOT: Failed to show fallback UI", fallbackError);
        }
    }

    private void showDiagnostics() {
        showFallbackUI(null);
    }

    private String buildDiagnostics(@Nullable Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TERMUX BACKGROUND DIAGNOSTICS ===\n\n");

        // Version info
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            sb.append("App Version: ").append(versionName).append(" (").append(versionCode).append(")\n");
        } catch (PackageManager.NameNotFoundException ex) {
            sb.append("App Version: Unknown\n");
        }

        // Android version
        sb.append("Android SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("Android Release: ").append(Build.VERSION.RELEASE).append("\n\n");

        // Dependency detection
        sb.append("Termux installed: ").append(isPackageInstalled("com.termux") ? "YES" : "NO").append("\n");
        sb.append("Termux:API installed: ").append(isPackageInstalled("com.termux.api") ? "YES" : "NO").append("\n\n");

        // WebView status
        sb.append("WebView reached: ").append(webViewReached ? "YES" : "NO").append("\n\n");

        // Error info
        if (e != null) {
            sb.append("Exception: ").append(e.getClass().getName()).append("\n");
            sb.append("Message: ").append(e.getMessage()).append("\n\n");
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            sb.append("Stack Trace:\n").append(sw.toString());
        } else if (lastError != null) {
            sb.append("Last Error: ").append(lastError).append("\n");
        } else {
            sb.append("No errors recorded.\n");
        }

        return sb.toString();
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void launchPackage(String packageName) {
        if (isPackageInstalled(packageName)) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cannot launch " + packageName, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, packageName + " is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFileChooserResult(ActivityResult result) {
        Uri[] uris = null;
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Best effort; SAF permission might already be granted.
                }
                uris = new Uri[]{uri};
                bridge.setLastImageUri(uri);
                final String js = "window.onAndroidFileSelected && window.onAndroidFileSelected(" + JSONObject.quote(uri.toString()) + ");";
                webView.post(() -> webView.evaluateJavascript(js, null));
            }
        }
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(uris);
            filePathCallback = null;
        }
    }
}
