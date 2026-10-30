package com.example.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: WidgetTickScheduler.kt */
@Metadata(d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\bÇ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0002J\u000e\u0010\n\u001a\u00020\u000b2\u0006\u0010\b\u001a\u00020\tR\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000¨\u0006\f"}, d2 = {"Lcom/example/widget/WidgetTickScheduler;", "", "<init>", "()V", "ACTION_TICK", "", "pendingIntent", "Landroid/app/PendingIntent;", "context", "Landroid/content/Context;", "schedule", "", "app"}, k = 1, mv = {2, 2, 0}, xi = 48)
/* loaded from: classes10.dex */
public final class WidgetTickScheduler {
    public static final int $stable = 0;
    public static final String ACTION_TICK = "com.example.widget.TICK";
    public static final WidgetTickScheduler INSTANCE = new WidgetTickScheduler();

    private WidgetTickScheduler() {
    }

    private final PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, (Class<?>) WidgetTickReceiver.class).setAction(ACTION_TICK);
        Intrinsics.checkNotNullExpressionValue(intent, "setAction(...)");
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 700, intent, 201326592);
        Intrinsics.checkNotNullExpressionValue(broadcast, "getBroadcast(...)");
        return broadcast;
    }

    public final void schedule(Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        // Consolidated pulse: the single per-minute widget alarm now lives in LockWidgets.schedule,
        // which arms only while a widget exists and cancels once none remain. Cancel any alarm this
        // legacy chain armed so an install upgraded from an older version stops it here.
        Object systemService = context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.app.AlarmManager");
        AlarmManager alarmManager = (AlarmManager) systemService;
        alarmManager.cancel(pendingIntent(context));
        com.example.lockwidgets.LockWidgets.schedule(context);
    }
}
