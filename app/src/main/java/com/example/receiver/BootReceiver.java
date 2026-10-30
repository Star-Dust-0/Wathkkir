package com.example.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

/* compiled from: BootReceiver.kt */
@Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0016¨\u0006\n"}, d2 = {"Lcom/example/receiver/BootReceiver;", "Landroid/content/BroadcastReceiver;", "<init>", "()V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "app"}, k = 1, mv = {2, 2, 0}, xi = 48)
/* loaded from: classes9.dex */
public final class BootReceiver extends BroadcastReceiver {
    public static final int $stable = 8;

    /* JADX WARN: Failed to find 'out' block for switch in B:4:0x0014. Please report as an issue. */
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(intent, "intent");
        try { com.example.prayer.alerts.PrayerAlertScheduler.INSTANCE.scheduleAll(context); } catch (Throwable ignored) {}
        try { new com.example.fajr.alarm.FajrAlarmScheduler(context).scheduleNextFajrAlarm(); } catch (Throwable ignored) {}
        String action = intent.getAction();
        if (action != null) {
            switch (action.hashCode()) {
                case -1787487905:
                    if (!action.equals("android.intent.action.QUICKBOOT_POWERON")) {
                        return;
                    }
                    BroadcastReceiver.PendingResult pendingResult = goAsync();
                    restoreDhikr(context, pendingResult);
                    return;
                case 502473491:
                    if (!action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                        return;
                    }
                    BroadcastReceiver.PendingResult pendingResult2 = goAsync();
                    restoreDhikr(context, pendingResult2);
                    return;
                case 505380757:
                    if (!action.equals("android.intent.action.TIME_SET")) {
                        return;
                    }
                    BroadcastReceiver.PendingResult pendingResult22 = goAsync();
                    restoreDhikr(context, pendingResult22);
                    return;
                case 798292259:
                    if (!action.equals("android.intent.action.BOOT_COMPLETED")) {
                        return;
                    }
                    BroadcastReceiver.PendingResult pendingResult222 = goAsync();
                    restoreDhikr(context, pendingResult222);
                    return;
                case 1737074039:
                    if (!action.equals("android.intent.action.MY_PACKAGE_REPLACED")) {
                        return;
                    }
                    BroadcastReceiver.PendingResult pendingResult2222 = goAsync();
                    restoreDhikr(context, pendingResult2222);
                    return;
                default:
                    return;
            }
        }
    }
    /** Reconstructed continuation of recovered BootReceiver coroutine from APK. */
    private static void restoreDhikr(Context context, BroadcastReceiver.PendingResult pending) {
      com.example.receiver.BootRecovery.restore(context,pending);
    }

}
