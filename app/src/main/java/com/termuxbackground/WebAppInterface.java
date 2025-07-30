package com.termuxbackground;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;

public class WebAppInterface {
    Context mContext;

    WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void showToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void runInstallerScript() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "sh", mContext.getFilesDir().getAbsolutePath() + "/install.sh"
            });
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
