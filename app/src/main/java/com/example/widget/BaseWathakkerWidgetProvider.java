package com.example.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import com.example.lockwidgets.HomeWidgets;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: WidgetProviders.kt */
@Metadata(d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0015\n\u0002\b\u0002\b'\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J \u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bH\u0016J\u0010\u0010\f\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u0016¨\u0006\r"}, d2 = {"Lcom/example/widget/BaseWathakkerWidgetProvider;", "Landroid/appwidget/AppWidgetProvider;", "<init>", "()V", "onUpdate", "", "context", "Landroid/content/Context;", "appWidgetManager", "Landroid/appwidget/AppWidgetManager;", "appWidgetIds", "", "onEnabled", "app"}, k = 1, mv = {2, 2, 0}, xi = 48)
/* loaded from: classes10.dex */
public abstract class BaseWathakkerWidgetProvider extends AppWidgetProvider {
    public static final int $stable = 8;

    @Override // android.appwidget.AppWidgetProvider
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int i, Bundle bundle) {
        HomeWidgets.updateAll(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(appWidgetManager, "appWidgetManager");
        Intrinsics.checkNotNullParameter(appWidgetIds, "appWidgetIds");
        WidgetUpdater.INSTANCE.updateAll(context);
        WidgetTickScheduler.INSTANCE.schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onDeleted(Context context, int[] appWidgetIds) {
        android.content.SharedPreferences.Editor e = context.getSharedPreferences("fajr_prefs", 0).edit();
        for (int id : appWidgetIds) {
            e.remove("widget_bg_color_" + id);
            e.remove("widget_bar_color_" + id);
            e.remove("widget_tap_" + id);
        }
        e.apply();
        // Re-evaluate the shared pulse: LockWidgets.schedule cancels it only when no widget of
        // ANY family remains, so deleting the last com.example.widget.* instance stops the pulse
        // immediately instead of at the next (possibly doze-deferred) tick delivery.
        WidgetTickScheduler.INSTANCE.schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onEnabled(Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        WidgetUpdater.INSTANCE.updateAll(context);
        WidgetTickScheduler.INSTANCE.schedule(context);
    }
}
