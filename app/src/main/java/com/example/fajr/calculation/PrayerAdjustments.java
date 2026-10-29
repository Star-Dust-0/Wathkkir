package com.example.fajr.calculation;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.fajr.calculation.PrayerTimesCalculator;

import java.util.Calendar;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class PrayerAdjustments {
    private static final String PREFS = "fajr_prefs";
    public static final PrayerAdjustments INSTANCE = new PrayerAdjustments();
    private static final String[] OFFSET_KEYS = {"adj_fajr", "adj_shuruq", "adj_dhuhr", "adj_asr", "adj_maghrib", "adj_isha"};
    public static final int $stable = 8;

    private PrayerAdjustments() {
    }

    public final boolean asrHanafi(Context context) {
        context.getClass();
        return context.getSharedPreferences(PREFS, 0).getInt("asr_juristic", 0) == 1;
    }

    public final PrayerTimesCalculator.DayPrayerTimes calculateAll(Context context, Calendar calendar, double d, double d2, CalculationMethod calculationMethod) {
        context.getClass();
        calendar.getClass();
        calculationMethod.getClass();
        PrayerTimesCalculator.DayPrayerTimes calculateAll = PrayerTimesCalculator.INSTANCE.calculateAll(calendar, d, d2, calculationMethod, asrHanafi(context) ? 2 : 1, highLatRule(context), offsetMinutes(context));
        calculateAll.getClass();
        return calculateAll;
    }

    public final long fajrTime(Context context, Calendar calendar, double d, double d2, CalculationMethod calculationMethod) {
        context.getClass();
        calendar.getClass();
        calculationMethod.getClass();
        return calculateAll(context, calendar, d, d2, calculationMethod).getFajr();
    }

    public final int highLatRule(Context context) {
        context.getClass();
        return context.getSharedPreferences(PREFS, 0).getInt("high_lat_rule", 0);
    }

    public final int hijriOffsetDays(Context context) {
        context.getClass();
        return context.getSharedPreferences(PREFS, 0).getInt("hijri_offset", 0);
    }

    public final int[] offsetMinutes(Context context) {
        context.getClass();
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, 0);
        return new int[]{sharedPreferences.getInt("adj_fajr", 0), sharedPreferences.getInt("adj_shuruq", 0), sharedPreferences.getInt("adj_dhuhr", 0), sharedPreferences.getInt("adj_asr", 0), sharedPreferences.getInt("adj_maghrib", 0), sharedPreferences.getInt("adj_isha", 0)};
    }

    public final boolean optionalRow(Context context, String str) {
        context.getClass();
        str.getClass();
        return context.getSharedPreferences(PREFS, 0).getBoolean("opt_".concat(str), false);
    }

    public final void setAsrHanafi(Context context, boolean z) {
        context.getClass();
        context.getSharedPreferences(PREFS, 0).edit().putInt("asr_juristic", z ? 1 : 0).apply();
    }

    public final void setHighLatRule(Context context, int i) {
        context.getClass();
        context.getSharedPreferences(PREFS, 0).edit().putInt("high_lat_rule", com.example.utils.Str.coerce(i, 0, 3)).apply();
    }

    public final void setHijriOffsetDays(Context context, int i) {
        context.getClass();
        context.getSharedPreferences(PREFS, 0).edit().putInt("hijri_offset", com.example.utils.Str.coerce(i, -2, 2)).apply();
    }

    public final void setOffsetMinutes(Context context, int i, int i2) {
        context.getClass();
        if (i < 0 || i >= 6) {
            return;
        }
        context.getSharedPreferences(PREFS, 0).edit().putInt(OFFSET_KEYS[i], com.example.utils.Str.coerce(i2, -30, 30)).apply();
    }

    public final void setOptionalRow(Context context, String str, boolean z) {
        context.getClass();
        str.getClass();
        context.getSharedPreferences(PREFS, 0).edit().putBoolean("opt_".concat(str), z).apply();
    }
}
