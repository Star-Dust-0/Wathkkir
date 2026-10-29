package com.example.prayer;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.R;
import com.example.utils.NotifPrefs;
import androidx.core.app.NotificationCompat;
import android.app.NotificationChannel;



/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class EventReminderReceiver extends BroadcastReceiver {
    public static final int $stable = 8;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        context.getClass();
        intent.getClass();
        String stringExtra = intent.getStringExtra("event_name");
        if (stringExtra == null) {
            return;
        }
        Object systemService = context.getSystemService("notification");
        systemService.getClass();
        NotificationManager notificationManager = (NotificationManager) systemService;
        String a = NotifPrefs.INSTANCE.ensureChannel(context, notificationManager, new NotificationChannel("event_reminders_channel", "مناسبات هجرية", 3), NotifPrefs.INSTANCE.tone(context), NotifPrefs.INSTANCE.vibration(context));
        int currentTimeMillis = (int) (System.currentTimeMillis() % 100000);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, a);
        builder.setSmallIcon(R.drawable.ic_stat_prayer);
        builder.setContentTitle(context.getString(context.getApplicationInfo().labelRes));
        builder.setContentText(stringExtra);
        builder.setAutoCancel(true);
        notificationManager.notify(currentTimeMillis, builder.build());
        com.example.prayer.HijriEvents.INSTANCE.scheduleAll(context);
    }
}
