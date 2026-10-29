package com.example.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.example.lockwidgets.LockWidgets;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class WidgetTickScheduler {
    public static final int $stable = 0;
    public static final String ACTION_TICK = "com.example.widget.TICK";
    public static final WidgetTickScheduler INSTANCE = new WidgetTickScheduler();

    private WidgetTickScheduler() {
    }

    private final PendingIntent pendingIntent(Context context) {
        Intent action = new Intent(context, (Class<?>) WidgetTickReceiver.class).setAction(ACTION_TICK);
        action.getClass();
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 700, action, 201326592);
        broadcast.getClass();
        return broadcast;
    }

    public final void schedule(Context context) {
        context.getClass();
        Object systemService = context.getSystemService("alarm");
        systemService.getClass();
        ((AlarmManager) systemService).cancel(pendingIntent(context));
        LockWidgets.schedule(context);
    }
}
