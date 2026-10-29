package com.example.prayer.alerts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.example.MainActivity;
import com.example.fajr.data.FajrPreferences;
import com.example.R;
import com.example.ui.i18n.L10n;

import com.example.utils.NotifPrefs;

import androidx.core.app.NotificationCompat;
import com.example.utils.Str;

import com.example.ui.i18n.S;

import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Pattern;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class PrayerAlertReceiver extends BroadcastReceiver {
    public static void refreshChannelsStatic(Context context) {
        Companion.refreshChannels(context);
    }

    public static final String ALERT_CHANNEL = "prayer_alerts_channel";
    private static final String CHANNEL_PREFS = "bell_channels";
    public static final String IQAMA_CHANNEL = "iqama_reminders_channel";
    public static final Companion Companion = new Companion();
    public static final int $stable = 8;

    private final void enableDnd(Context context) {
        try {
            Object systemService = context.getSystemService("notification");
            systemService.getClass();
            NotificationManager notificationManager = (NotificationManager) systemService;
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.setInterruptionFilter(2);
            }
        } catch (Throwable unused) {
        }
    }

    private final void restoreDnd(Context context) {
        try {
            Object systemService = context.getSystemService("notification");
            systemService.getClass();
            NotificationManager notificationManager = (NotificationManager) systemService;
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.setInterruptionFilter(1);
            }
        } catch (Throwable unused) {
        }
    }

    public final String ensureChannel$app(Context context, NotificationManager notificationManager, String str, int i, String str2) {
        context.getClass();
        notificationManager.getClass();
        str.getClass();
        str2.getClass();
        if (Build.VERSION.SDK_INT < 26) {
            return str;
        }
        boolean startsWith = str.startsWith(IQAMA_CHANNEL);
        Pattern compile = Pattern.compile(".*_p\\d$");
        compile.getClass();
        boolean matches = compile.matcher(str).matches();
        String e = L10n.INSTANCE.t(startsWith ? S.IqamaReminder : S.AdhanReminder);
        if (matches) {
            e = Str.L(e, " - ", str2);
        }
        NotificationChannel c = new NotificationChannel(str, e, startsWith ? 3 : 4);
        c.setShowBadge(true);
        if (!matches) {
            return NotifPrefs.INSTANCE.ensureChannel(context, notificationManager, c, NotifPrefs.INSTANCE.tone(context), NotifPrefs.INSTANCE.vibration(context));
        }
        PrayerAlertStore prayerAlertStore = PrayerAlertStore.INSTANCE;
        return NotifPrefs.INSTANCE.ensureChannel(context, notificationManager, c, prayerAlertStore.effectiveTone(context, i), prayerAlertStore.effectiveVibration(context, i));
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        context.getClass();
        intent.getClass();
        L10n.INSTANCE.init(context);
        int intExtra = intent.getIntExtra(PrayerAlertScheduler.EXTRA_PRAYER, -1);
        int intExtra2 = intent.getIntExtra(PrayerAlertScheduler.EXTRA_KIND, 0);
        if (intExtra < 0 || intExtra >= 6) {
            return;
        }
        if (intExtra2 == 2) {
            restoreDnd(context);
            return;
        }
        if (intExtra == 0 && intExtra2 == 0 && new FajrPreferences(context).isAlarmEnabled()) {
            Log.i("FajrBellSuppressed", "fajr adhan bell skipped, full-screen alarm is on");
            PrayerAlertScheduler.INSTANCE.scheduleAll(context);
            return;
        }
        PrayerAlertStore prayerAlertStore = PrayerAlertStore.INSTANCE;
        if (prayerAlertStore.isEnabled(context, intExtra)) {
            String e = (intExtra == 2 && Calendar.getInstance().get(7) == 6) ? L10n.INSTANCE.t(S.PrayerJumuah) : L10n.INSTANCE.t(intExtra != 0 ? intExtra != 1 ? intExtra != 2 ? intExtra != 3 ? intExtra != 4 ? S.PrayerIsha : S.PrayerMaghrib : S.PrayerAsr : S.PrayerDhuhr : S.PrayerShuruq : S.PrayerFajr);
            Object systemService = context.getSystemService("notification");
            systemService.getClass();
            NotificationManager notificationManager = (NotificationManager) systemService;
            int i = intExtra2 == 1 ? 1 : 0;
            boolean z = intExtra2 == 3;
            String str = i != 0 ? IQAMA_CHANNEL : ALERT_CHANNEL;
            if (prayerAlertStore.hasOverride(context, intExtra)) {
                str = str + "_p" + intExtra;
            }
            String ensureChannel$app = ensureChannel$app(context, notificationManager, str, intExtra, e);
            int i2 = (intExtra2 * 10) + intExtra + 400;
            PendingIntent activity = PendingIntent.getActivity(context, i2, new Intent(context, (Class<?>) MainActivity.class).addFlags(335544320), 201326592);
            boolean z2 = intExtra == 1;
            long longExtra = intent.getLongExtra(PrayerAlertScheduler.EXTRA_AT, 0L);
            long currentTimeMillis = System.currentTimeMillis();
            String format = i != 0 ? String.format(L10n.INSTANCE.t(S.IqamaNowFmt), Arrays.copyOf(new Object[]{e}, 1)) : z ? z2 ? L10n.INSTANCE.t(S.ShuruqApproaching) : String.format(L10n.INSTANCE.t(S.PrayerApproachingFmt), Arrays.copyOf(new Object[]{e}, 1)) : z2 ? L10n.INSTANCE.t(S.ShuruqNow) : String.format(L10n.INSTANCE.t(S.PrayerNowFmt), Arrays.copyOf(new Object[]{e}, 1));
            if (!z || longExtra > currentTimeMillis) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ensureChannel$app);
                builder.setSmallIcon(R.drawable.ic_stat_prayer);
                builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
                builder.setContentTitle(e);
                builder.setContentText(format);
                builder.setPriority(i ^ 1);
                builder.setCategory("reminder");
                builder.setContentIntent(activity);
                builder.setAutoCancel(true);
                if (z) {
                    builder.setUsesChronometer(true);
                    builder.setWhen(longExtra);
                    builder.setShowWhen(true);
                    builder.getExtras().putBoolean("android.chronometerCountDown", true);
                    builder.setTimeoutAfter(longExtra - currentTimeMillis);
                } else {
                    builder.setTimeoutAfter(300000L);
                }
                Notification a = builder.build();
                a.getClass();
                if (intExtra2 == 0) {
                    notificationManager.cancel(intExtra + 430);
                }
                notificationManager.notify(i2, a);
                if (intExtra2 == 0 && prayerAlertStore.autoDnd(context, intExtra)) {
                    enableDnd(context);
                }
                PrayerAlertScheduler.INSTANCE.scheduleAll(context);
            }
        }
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class Companion {

        public final void refreshChannels(Context context) {
            context.getClass();
            if (Build.VERSION.SDK_INT < 26) {
                return;
            }
            try {
                L10n.INSTANCE.init(context);
                Object systemService = context.getSystemService("notification");
                systemService.getClass();
                NotificationManager notificationManager = (NotificationManager) systemService;
                PrayerAlertReceiver prayerAlertReceiver = new PrayerAlertReceiver();
                prayerAlertReceiver.ensureChannel$app(context, notificationManager, PrayerAlertReceiver.ALERT_CHANNEL, 0, "");
                prayerAlertReceiver.ensureChannel$app(context, notificationManager, PrayerAlertReceiver.IQAMA_CHANNEL, 0, "");
                int i = 0;
                while (i < 6) {
                    if (PrayerAlertStore.INSTANCE.hasOverride(context, i)) {
                        S sk1Var = i != 0 ? i != 1 ? i != 2 ? i != 3 ? i != 4 ? S.PrayerIsha : S.PrayerMaghrib : S.PrayerAsr : S.PrayerDhuhr : S.PrayerShuruq : S.PrayerFajr;
                        prayerAlertReceiver.ensureChannel$app(context, notificationManager, "prayer_alerts_channel_p" + i, i, L10n.INSTANCE.t(sk1Var));
                        prayerAlertReceiver.ensureChannel$app(context, notificationManager, "iqama_reminders_channel_p" + i, i, L10n.INSTANCE.t(sk1Var));
                    } else {
                        NotifPrefs.INSTANCE.deleteVariants(notificationManager, "prayer_alerts_channel_p" + i, null);
                        NotifPrefs.INSTANCE.deleteVariants(notificationManager, "iqama_reminders_channel_p" + i, null);
                    }
                    i++;
                }
            } catch (Throwable th) {
                Log.e("NotifPrefs", "prayer channels failed", th);
            }
        }

        private Companion() {
        }
    }
}
