package com.termuxbackground;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class BackgroundInterface {

    private final Context context;

    public BackgroundInterface(Context ctx) {
        this.context = ctx;
    }

    @JavascriptInterface
    public void setSettings(String animation, String blur, String opacity, String base64Image) {
        try {
            byte[] imageBytes = Base64.decode(base64Image.split(",")[1], Base64.DEFAULT);
            File termuxDir = new File(Environment.getExternalStorageDirectory(), "/.termux");
            if (!termuxDir.exists()) termuxDir.mkdirs();

            File imageFile = new File(termuxDir, "background.png");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageBytes);
            }

            File propsFile = new File(termuxDir, "termux.properties");
            if (!propsFile.exists()) propsFile.createNewFile();

            String props = String.format(
                "background=background.png\nbackground.opacity=%s\nbackground.animation=%s\nbackground.blur=%s\n",
                opacity,
                animation,
                blur
            );

            try (FileOutputStream fos = new FileOutputStream(propsFile, false)) {
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                writer.write(props);
                writer.flush();
                writer.close();
            }

            Runtime.getRuntime().exec(new String[]{
                "am", "broadcast", "--user", "0",
                "-a", "com.termux.api.action.RUN_COMMAND",
                "--es", "com.termux.api.extra.COMMAND", "termux-reload-settings"
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
