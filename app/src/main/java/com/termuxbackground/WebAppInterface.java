package com.termuxbackground;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class WebAppInterface {
    private Context mContext;
    private String animation = "none";
    private String blur = "false";
    private String opacity = "1.0";

    public WebAppInterface(Context context) {
        mContext = context;
    }

    @JavascriptInterface
    public void setSettings(String anim, String blurVal, String opac) {
        this.animation = anim;
        this.blur = blurVal;
        this.opacity = opac;
    }

    public String getAnimation() {
        return animation;
    }

    public String getBlur() {
        return blur;
    }

    public String getOpacity() {
        return opacity;
    }
}
