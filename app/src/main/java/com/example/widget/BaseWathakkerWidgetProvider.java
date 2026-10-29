package com.example.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.example.lockwidgets.HomeWidgets;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public abstract class BaseWathakkerWidgetProvider extends AppWidgetProvider {
    public static final int $stable = 8;

    @Override // android.appwidget.AppWidgetProvider
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int i, Bundle bundle) {
        HomeWidgets.updateAll(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onDeleted(Context context, int[] iArr) {
        SharedPreferences.Editor edit = context.getSharedPreferences("fajr_prefs", 0).edit();
        for (int i : iArr) {
            edit.remove("widget_bg_color_" + i);
            edit.remove("widget_bar_color_" + i);
            edit.remove("widget_tap_" + i);
        }
        edit.apply();
        WidgetTickScheduler.INSTANCE.schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onEnabled(Context context) {
        context.getClass();
        WidgetUpdater.INSTANCE.updateAll(context);
        WidgetTickScheduler.INSTANCE.schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        context.getClass();
        appWidgetManager.getClass();
        iArr.getClass();
        WidgetUpdater.INSTANCE.updateAll(context);
        WidgetTickScheduler.INSTANCE.schedule(context);
    }
}
