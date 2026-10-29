package com.example.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.example.lockwidgets.HomeWidgets;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class WidgetRefreshReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.wathakker.widget.REFRESH";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String str;
        if (intent == null || !ACTION.equals(intent.getAction())) {
            return;
        }
        int intExtra = intent.getIntExtra("appWidgetId", -1);
        try {
            String providerName = AppWidgetManager.getInstance(context).getAppWidgetInfo(intExtra).provider.getClassName();
            str = providerName.substring(19, providerName.length() - 14);
        } catch (Exception e) {
            Log.w("WathakkerWidgetRefresh", "provider lookup failed for id " + intExtra, e);
            str = null;
        }
        String refreshAllAndText = HomeWidgets.refreshAllAndText(context, str);
        Log.i("WathakkerWidgetRefresh", "tap id=" + intExtra + " kind=" + str + " text=" + refreshAllAndText);
        Toast.makeText(context.getApplicationContext(), refreshAllAndText, 1).show();
    }
}
