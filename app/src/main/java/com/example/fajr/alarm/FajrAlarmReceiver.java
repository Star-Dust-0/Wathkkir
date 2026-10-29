package com.example.fajr.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import com.example.fajr.data.FajrPreferences;
import com.example.fajr.ui.FajrAlarmActivity;
import com.example.prayer.alerts.PrayerAlertStore;
import com.example.R;
import com.example.ui.i18n.L10n;

import androidx.core.app.NotificationCompat;
import com.example.fajr.alarm.FajrAlarmScheduler;
import com.example.ui.i18n.S;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class FajrAlarmReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public final void onReceive(Context context, Intent intent) {
        L10n.INSTANCE.init(context);
        intent.getClass();
        FajrPreferences fajrPreferences = new FajrPreferences(context);
        if (PrayerAlertStore.INSTANCE.isFajrAlarmActive(context)) {
            String format = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String stringExtra = intent.getStringExtra("slot");
            if (stringExtra == null) {
                stringExtra = "main";
            }
            boolean o = java.util.Objects.equals(intent.getAction(), "ACTION_SNOOZE_TRIGGER");
            if (!o && java.util.Objects.equals(fajrPreferences.getSlotDate(stringExtra), format)) {
                new FajrAlarmScheduler(context).rescheduleFromPrefs();
                return;
            }
            if (!o) {
                fajrPreferences.setSlotDate(stringExtra, format);
                fajrPreferences.setLastAlarmTriggeredDate(format);
                fajrPreferences.setSnoozeCount(0);
            }
            Intent intent2 = new Intent(context, (Class<?>) FajrAlarmActivity.class);
            intent2.setFlags(872415232);
            PendingIntent activity = PendingIntent.getActivity(context, 0, intent2, 201326592);
            Object systemService = context.getSystemService("notification");
            systemService.getClass();
            NotificationManager notificationManager = (NotificationManager) systemService;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel g = new NotificationChannel("fajr_alarm_channel", L10n.INSTANCE.t(S.FajrAlarm), 4);
                g.setDescription(L10n.INSTANCE.t(S.NotifChannelFajr));
                g.setBypassDnd(true);
                g.enableVibration(true);
                g.setShowBadge(true);
                notificationManager.createNotificationChannel(g);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "fajr_alarm_channel");
            builder.setSmallIcon(R.drawable.ic_stat_prayer);
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
            builder.setContentTitle(L10n.INSTANCE.t(S.FajrPrayer));
            builder.setContentText(L10n.INSTANCE.t(S.FajrNow));
            builder.setPriority(2);
            builder.setCategory("alarm");
            builder.setFullScreenIntent(activity, true);
            builder.setAutoCancel(true);
            Notification a = builder.build();
            a.getClass();
            notificationManager.notify(88001, a);
            try {
                context.startActivity(intent2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            new FajrAlarmScheduler(context).rescheduleFromPrefs();
        }
    }
}
