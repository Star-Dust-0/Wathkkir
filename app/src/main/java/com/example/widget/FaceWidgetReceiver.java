package com.example.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class FaceWidgetReceiver extends BroadcastReceiver {
    public static final int $stable = 8;
    public static final String ACTION_REQUEST = "com.samsung.android.intent.action.REQUEST_SERVICEBOX_REMOTEVIEWS";
    public static final String ACTION_RESPONSE = "com.samsung.android.intent.action.RESPONSE_SERVICEBOX_REMOTEVIEWS";
    public static final String PAGE_ID_PRAYER = "wathakker_prayer";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String stringExtra;
        context.getClass();
        intent.getClass();
        if (!java.util.Objects.equals(intent.getAction(), ACTION_REQUEST) || (stringExtra = intent.getStringExtra("pageId")) == null) {
            return;
        }
        WidgetUpdater.INSTANCE.pushFaceWidget(context, stringExtra);
    }
}
