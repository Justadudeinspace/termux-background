// MainActivity.java package com.termuxbackground;

import android.Manifest; import android.app.Activity; import android.content.Intent; import android.net.Uri; import android.os.Bundle; import android.webkit.WebChromeClient; import android.webkit.WebSettings; import android.webkit.WebView; import android.webkit.WebViewClient; import android.webkit.ValueCallback; import android.webkit.WebView; import android.webkit.WebChromeClient; import android.webkit.WebSettings; import android.widget.Toast;

import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity; import androidx.core.app.ActivityCompat;

import java.io.File; import java.io.InputStream; import java.io.OutputStream; import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

private WebView webView;
private ValueCallback<Uri[]> filePathCallback;
private final static int FILE_CHOOSER_REQUEST_CODE = 1001;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    }, 1);

    webView = new WebView(this);
    setContentView(webView);

        // Copy install.sh from assets to files directory
        try {
            InputStream script = getAssets().open("install.sh");
            File outFile = new File(getFilesDir(), "install.sh");
            OutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = script.read(buf)) > 0) out.write(buf, 0, len);
            script.close();
            out.close();
            outFile.setExecutable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }


    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setAllowFileAccess(true);
    webView.setWebChromeClient(new WebChromeClient() {
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            MainActivity.this.filePathCallback = filePathCallback;
            Intent intent = fileChooserParams.createIntent();
            startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
            return true;
        }
    });

    webView.setWebViewClient(new WebViewClient());
    webView.addJavascriptInterface(new WebAppInterface(this), "Android");
    webView.loadUrl("file:///android_asset/termux-background-ui.html");
}

@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == FILE_CHOOSER_REQUEST_CODE && filePathCallback != null) {
        Uri[] results = (resultCode == Activity.RESULT_OK && data != null) ?
                new Uri[]{data.getData()} : null;
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;

        if (results != null && results.length > 0) {
            applyBackgroundImage(results[0]);
        }
    }
}

private void applyBackgroundImage(Uri uri) {
    try {
        File termuxDir = new File("/data/data/com.termux/files/home/.termux");
        if (!termuxDir.exists()) termuxDir.mkdirs();

        File backgroundFile = new File(termuxDir, "background.png");
        InputStream in = getContentResolver().openInputStream(uri);
        OutputStream out = new FileOutputStream(backgroundFile);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);

        in.close();
        out.close();

        // Run termux-reload-settings via Termux:API
        Runtime.getRuntime().exec(new String[]{"am", "broadcast", "--user", "0",
                "-a", "com.termux.api.action.RUN_COMMAND",
                "--es", "com.termux.api.extra.COMMAND", "termux-reload-settings"});

        Toast.makeText(this, "Background applied!", Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Failed to apply background.", Toast.LENGTH_SHORT).show();
    }
}

}

