package com.example.prayer.alerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.example.fajr.calculation.CalculationMethod;
import com.example.fajr.calculation.PrayerAdjustments;
import com.example.fajr.calculation.PrayerTimesCalculator;
import com.example.fajr.data.FajrPreferences;
import com.example.lockwidgets.PrayerPhase;
import java.util.Calendar;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class PrayerAlertScheduler {
    public static final int $stable;
    public static final String EXTRA_AT = "prayer_at";
    public static final String EXTRA_KIND = "alert_kind";
    public static final String EXTRA_PRAYER = "prayer_index";
    public static final PrayerAlertScheduler INSTANCE = new PrayerAlertScheduler();
    private static final int[] IQAMA_OFFSETS;
    public static final int KIND_ADHAN = 0;
    public static final int KIND_DND_OFF = 2;
    public static final int KIND_EARLY = 3;
    public static final int KIND_IQAMA = 1;

    static {
        int[] iArr = new int[6];
        for (int i = 0; i < 6; i++) {
            iArr[i] = PrayerPhase.iqamaMinutes(i);
        }
        IQAMA_OFFSETS = iArr;
        $stable = 8;
    }

    private PrayerAlertScheduler() {
    }

    private final long nextOccurrence(Context context, int i, long[] jArr, long j, FajrPreferences fajrPreferences, CalculationMethod calculationMethod) {
        long j2 = jArr[i];
        if (j2 > j) {
            return j2;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, 1);
        PrayerTimesCalculator.DayPrayerTimes calculateAll = PrayerAdjustments.INSTANCE.calculateAll(context, calendar, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), calculationMethod);
        return new long[]{calculateAll.getFajr(), calculateAll.getSunrise(), calculateAll.getDhuhr(), calculateAll.getAsr(), calculateAll.getMaghrib(), calculateAll.getIsha()}[i];
    }

    private final PendingIntent pending(Context context, int i, int i2, long j) {
        Intent putExtra = new Intent(context, (Class<?>) PrayerAlertReceiver.class).putExtra(EXTRA_PRAYER, i).putExtra(EXTRA_KIND, i2).putExtra(EXTRA_AT, j);
        putExtra.getClass();
        PendingIntent broadcast = PendingIntent.getBroadcast(context, i2 != 0 ? i2 != 1 ? i2 != 2 ? i + 400 : i + 300 : i + 200 : i + 100, putExtra, 201326592);
        broadcast.getClass();
        return broadcast;
    }

    public static /* synthetic */ PendingIntent pending$default(PrayerAlertScheduler prayerAlertScheduler, Context context, int i, int i2, long j, int i3, Object obj) {
        if ((i3 & 8) != 0) {
            j = 0;
        }
        return prayerAlertScheduler.pending(context, i, i2, j);
    }

    private final void scheduleOne(Context context, int i, long j, long j2) {
        Object systemService = context.getSystemService("alarm");
        systemService.getClass();
        AlarmManager alarmManager = (AlarmManager) systemService;
        alarmManager.cancel(pending$default(this, context, i, 0, 0L, 8, null));
        alarmManager.cancel(pending$default(this, context, i, 1, 0L, 8, null));
        alarmManager.cancel(pending$default(this, context, i, 2, 0L, 8, null));
        alarmManager.cancel(pending$default(this, context, i, 3, 0L, 8, null));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        boolean z = i == 2 && calendar.get(7) == 6;
        PrayerAlertStore prayerAlertStore = PrayerAlertStore.INSTANCE;
        if (prayerAlertStore.isEnabled(context, i)) {
            setExact(alarmManager, j, pending$default(this, context, i, 0, 0L, 8, null));
            long offsetMinutes = prayerAlertStore.offsetMinutes(context, i) * 60000;
            if (offsetMinutes > 0) {
                long j3 = j - offsetMinutes;
                if (j3 > j2) {
                    setExact(alarmManager, j3, pending(context, i, 3, j));
                }
            }
            int[] iArr = IQAMA_OFFSETS;
            long j4 = (iArr[i] * 60000) + j;
            if (prayerAlertStore.iqamaReminder(context, i) && iArr[i] > 0 && !z && j4 > j2) {
                setExact(alarmManager, j4, pending$default(this, context, i, 1, 0L, 8, null));
            }
            if (!prayerAlertStore.autoDnd(context, i) || iArr[i] <= 0 || j4 <= j2) {
                return;
            }
            setExact(alarmManager, j4, pending$default(this, context, i, 2, 0L, 8, null));
        }
    }

    private final void setExact(AlarmManager alarmManager, long j, PendingIntent pendingIntent) {
        try {
            try {
                alarmManager.setExactAndAllowWhileIdle(0, j, pendingIntent);
            } catch (SecurityException unused) {
                alarmManager.setAndAllowWhileIdle(0, j, pendingIntent);
            }
        } catch (Exception unused2) {
        }
    }

    public final void cancelAll(Context context) {
        context.getClass();
        Object systemService = context.getSystemService("alarm");
        systemService.getClass();
        AlarmManager alarmManager = (AlarmManager) systemService;
        int i = 0;
        while (i < 6) {
            PrayerAlertScheduler prayerAlertScheduler = this;
            alarmManager.cancel(pending$default(prayerAlertScheduler, context, i, 0, 0L, 8, null));
            alarmManager.cancel(pending$default(prayerAlertScheduler, context, i, 1, 0L, 8, null));
            alarmManager.cancel(pending$default(prayerAlertScheduler, context, i, 2, 0L, 8, null));
            i++;
        }
    }

    public final void scheduleAll(Context context) {
        context.getClass();
        PrayerAlertStore.INSTANCE.migrate(context);
        FajrPreferences fajrPreferences = new FajrPreferences(context);
        CalculationMethod fromId = CalculationMethod.INSTANCE.fromId(fajrPreferences.getCalculationMethodId());
        long currentTimeMillis = System.currentTimeMillis();
        PrayerAdjustments prayerAdjustments = PrayerAdjustments.INSTANCE;
        Calendar calendar = Calendar.getInstance();
        calendar.getClass();
        double latitude = fajrPreferences.getLatitude();
        double longitude = fajrPreferences.getLongitude();
        fromId.getClass();
        PrayerTimesCalculator.DayPrayerTimes calculateAll = prayerAdjustments.calculateAll(context, calendar, latitude, longitude, fromId);
        long fajr = calculateAll.getFajr();
        long sunrise = calculateAll.getSunrise();
        long dhuhr = calculateAll.getDhuhr();
        long asr = calculateAll.getAsr();
        long maghrib = calculateAll.getMaghrib();
        long isha = calculateAll.getIsha();
        int i = 6;
        long[] jArr = {fajr, sunrise, dhuhr, asr, maghrib, isha};
        int i2 = 0;
        while (i2 < i) {
            FajrPreferences fajrPreferences2 = fajrPreferences;
            long j = currentTimeMillis;
            scheduleOne(context, i2, nextOccurrence(context, i2, jArr, j, fajrPreferences2, fromId), j);
            i2++;
            i = i;
            jArr = jArr;
            fajrPreferences = fajrPreferences2;
            fromId = fromId;
            currentTimeMillis = j;
        }
    }
}
