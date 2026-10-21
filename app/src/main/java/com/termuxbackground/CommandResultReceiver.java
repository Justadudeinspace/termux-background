package com.termuxbackground;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Explicit one-shot PendingIntent capability; not exported to other apps. */
public final class CommandResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        TermuxBridge.get(context).receive(intent);
    }
}
