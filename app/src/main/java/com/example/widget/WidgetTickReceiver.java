package com.example.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class WidgetTickReceiver extends BroadcastReceiver {
    public static final int $stable = 8;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        context.getClass();
        intent.getClass();
        if (java.util.Objects.equals(intent.getAction(), WidgetTickScheduler.ACTION_TICK)) {
            WidgetUpdater.INSTANCE.updateAll(context);
            WidgetTickScheduler.INSTANCE.schedule(context);
        }
    }
}
