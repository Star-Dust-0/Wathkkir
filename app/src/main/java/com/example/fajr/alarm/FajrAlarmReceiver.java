package com.example.fajr.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.R;
import com.example.fajr.data.FajrPreferences;
import com.example.fajr.ui.FajrAlarmActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import com.example.ui.i18n.L10n;
import com.example.ui.i18n.S;

/* compiled from: FajrAlarmReceiver.kt */
@Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0016¨\u0006\n"}, d2 = {"Lcom/example/fajr/alarm/FajrAlarmReceiver;", "Landroid/content/BroadcastReceiver;", "<init>", "()V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "app"}, k = 1, mv = {2, 2, 0}, xi = 48)
/* loaded from: classes5.dex */
public final class FajrAlarmReceiver extends BroadcastReceiver {
    public static final int $stable = 8;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        L10n.INSTANCE.init(context);
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(intent, "intent");
        FajrPreferences prefs = new FajrPreferences(context);
        if (com.example.prayer.alerts.PrayerAlertStore.INSTANCE.isFajrAlarmActive(context)) {
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String slot = intent.getStringExtra("slot");
            if (slot == null) slot = "main";
            boolean isSnooze = Intrinsics.areEqual(intent.getAction(), "ACTION_SNOOZE_TRIGGER");
            if (!isSnooze && Intrinsics.areEqual(prefs.getSlotDate(slot), todayDate)) {
                new FajrAlarmScheduler(context).scheduleNextFajrAlarm();
                return;
            }
            if (!isSnooze) {
                prefs.setSlotDate(slot, todayDate);
                prefs.setLastAlarmTriggeredDate(todayDate);
                prefs.setSnoozeCount(0);
            }
            Intent alarmIntent = new Intent(context, (Class<?>) FajrAlarmActivity.class);
            alarmIntent.setFlags(872415232);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0, alarmIntent, 201326592);
            Object systemService = context.getSystemService("notification");
            Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.app.NotificationManager");
            NotificationManager notificationManager = (NotificationManager) systemService;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel("fajr_alarm_channel", L10n.INSTANCE.t(S.FajrAlarm), 4);
                channel.setDescription(L10n.INSTANCE.t(S.NotifChannelFajr));
                channel.setBypassDnd(true);
                channel.enableVibration(true);
                channel.setShowBadge(true);
                notificationManager.createNotificationChannel(channel);
            }
            Notification notification = new NotificationCompat.Builder(context, "fajr_alarm_channel").setSmallIcon(R.drawable.ic_stat_prayer).setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)).setContentTitle(L10n.INSTANCE.t(S.FajrPrayer)).setContentText(L10n.INSTANCE.t(S.FajrNow)).setPriority(2).setCategory(NotificationCompat.CATEGORY_ALARM).setFullScreenIntent(fullScreenPendingIntent, true).setAutoCancel(true).build();
            Intrinsics.checkNotNullExpressionValue(notification, "build(...)");
            notificationManager.notify(FajrAlarmScheduler.ALARM_REQUEST_CODE, notification);
            try {
                context.startActivity(alarmIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            new FajrAlarmScheduler(context).scheduleNextFajrAlarm();
        }
    }
}
