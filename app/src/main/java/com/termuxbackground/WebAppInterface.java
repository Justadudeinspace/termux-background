package com.termuxbackground;

import android.content.Context;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import java.io.*;

public class WebAppInterface {
    Context context;

    public WebAppInterface(Context c) {
        context = c;
    }

    @JavascriptInterface
    public void applyImage(String base64Image, String opacity, String scale, boolean blur) {
        try {
            File termuxDir = new File("/data/data/com.termux/files/home/.termux");
            if (!termuxDir.exists()) termuxDir.mkdirs();

            byte[] decodedBytes = Base64.decode(base64Image.split(",")[1], Base64.DEFAULT);
            FileOutputStream imageOut = new FileOutputStream(new File(termuxDir, "background.png"));
            imageOut.write(decodedBytes);
            imageOut.close();

            File propFile = new File(termuxDir, "termux.properties");
            FileWriter writer = new FileWriter(propFile, false);
            writer.write("background-opacity=" + (Integer.parseInt(opacity) / 100.0) + "\n");
            writer.write("background-style=" + scale + "\n");
            if (blur)
                writer.write("background-blur=true\n");
            writer.close();

            Runtime.getRuntime().exec(new String[]{"am", "broadcast", "--user", "0",
                    "-a", "com.termux.api.action.RUN_COMMAND",
                    "--es", "com.termux.api.extra.COMMAND", "termux-reload-settings"});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
