package com.example.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.example.lockwidgets.HomeWidgets;

/**
 * 3.4.2: tap on a Home countdown/progress widget whose tap option is "تحديث العرض". Refreshes the
 * widgets on demand (they can lag with the non-wakeup minute pulse) and shows the just-rendered
 * remaining time in a standard native Toast - the exact same text the widget shows.
 */
public final class WidgetRefreshReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.wathakker.widget.REFRESH";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        int id = intent.getIntExtra("appWidgetId", -1);
        String kind = null;
        try {
            ComponentName cn = AppWidgetManager.getInstance(context).getAppWidgetInfo(id).provider;
            String cls = cn.getClassName();
            kind = cls.substring("com.example.widget.".length(), cls.length() - "WidgetProvider".length());
        } catch (Exception e) {
            Log.w("WathakkerWidgetRefresh", "provider lookup failed for id " + id, e);
        }
        String text = HomeWidgets.refreshAllAndText(context, kind);
        Log.i("WathakkerWidgetRefresh", "tap id=" + id + " kind=" + kind + " text=" + text);
        Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
}
