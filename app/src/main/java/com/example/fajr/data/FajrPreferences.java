package com.example.fajr.data;

import android.content.Context;
import android.content.SharedPreferences;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class FajrPreferences {
    public static final int $stable = 8;
    private final SharedPreferences prefs;

    public FajrPreferences(Context context) {
        context.getClass();
        SharedPreferences sharedPreferences = context.getSharedPreferences("fajr_prefs", 0);
        sharedPreferences.getClass();
        this.prefs = sharedPreferences;
    }

    public final void clearCityNamesOrig() {
        this.prefs.edit().remove("city_name_ar_orig").remove("city_name_en_orig").apply();
    }

    public final int getAlarmAfterMin() {
        return this.prefs.getInt("alarm_after_min", 0);
    }

    public final int getAlarmBeforeMin() {
        return this.prefs.getInt("alarm_before_min", 0);
    }

    public final int getAlarmVolume() {
        return this.prefs.getInt("alarm_volume", 80);
    }

    public final int getCalculationMethodId() {
        return this.prefs.getInt("calculation_method", 1);
    }

    public final String getCityName() {
        String string = this.prefs.getString("city_name", "القاهرة");
        return string == null ? "القاهرة" : string;
    }

    public final String getCityNameArOrig() {
        return this.prefs.getString("city_name_ar_orig", null);
    }

    public final String getCityNameEnOrig() {
        return this.prefs.getString("city_name_en_orig", null);
    }

    public final String getCityNameLocalized(String str) {
        String cityName = getCityName();
        boolean zEquals = "ar".equals(str);
        SharedPreferences sharedPreferences = this.prefs;
        if (zEquals) {
            String string = sharedPreferences.getString("city_name_ar", cityName);
            if (string != null) {
                return string;
            }
        } else {
            String string2 = sharedPreferences.getString("city_name_en", null);
            if (string2 != null) {
                return string2;
            }
            String string3 = this.prefs.getString("city_name_ar", cityName);
            if (string3 != null) {
                return string3;
            }
        }
        return cityName;
    }

    public final String getCountryName() {
        String string = this.prefs.getString("country_name", "مصر");
        return string == null ? "مصر" : string;
    }

    public final String getLastAlarmTriggeredDate() {
        String string = this.prefs.getString("last_alarm_triggered_date", "");
        return string == null ? "" : string;
    }

    public final long getLastCalculatedFajrTimeMillis() {
        return this.prefs.getLong("last_fajr_time", 0L);
    }

    public final double getLatitude() {
        return this.prefs.getFloat("latitude", 30.0444f);
    }

    public final double getLongitude() {
        return this.prefs.getFloat("longitude", 31.2357f);
    }

    public final int getMaxDurationMinutes() {
        return this.prefs.getInt("max_duration_minutes", 5);
    }

    public final String getPlaceKey() {
        return this.prefs.getString("place_key", null);
    }

    public final String getRingtoneTitle() {
        String string = this.prefs.getString("ringtone_title", "الافتراضي");
        return string == null ? "الافتراضي" : string;
    }

    public final String getRingtoneUri() {
        String string = this.prefs.getString("ringtone_uri", "");
        return string == null ? "" : string;
    }

    public final String getSlotDate(String str) {
        String string = this.prefs.getString("alarm_slot_date_" + str, "");
        return string == null ? "" : string;
    }

    public final int getSnoozeCount() {
        return this.prefs.getInt("snooze_count", 0);
    }

    public final int getSnoozeMinutes() {
        return this.prefs.getInt("snooze_minutes", 10);
    }

    public final boolean getUseCurrentLocation() {
        return this.prefs.getBoolean("use_current_location", true);
    }

    public final String getWidgetBarColor() {
        String string = this.prefs.getString("widget_bar_color", "default");
        return string == null ? "default" : string;
    }

    public final boolean isAlarmEnabled() {
        return this.prefs.getBoolean("is_alarm_enabled", false);
    }

    public final boolean isVibrationEnabled() {
        return this.prefs.getBoolean("is_vibration_enabled", true);
    }

    public final void setAlarmAfterMin(int i) {
        this.prefs.edit().putInt("alarm_after_min", i).apply();
    }

    public final void setAlarmBeforeMin(int i) {
        this.prefs.edit().putInt("alarm_before_min", i).apply();
    }

    public final void setAlarmEnabled(boolean z) {
        this.prefs.edit().putBoolean("is_alarm_enabled", z).apply();
    }

    public final void setAlarmVolume(int i) {
        this.prefs.edit().putInt("alarm_volume", i).apply();
    }

    public final void setCalculationMethodId(int i) {
        this.prefs.edit().putInt("calculation_method", i).apply();
    }

    public final void setCityName(String str) {
        str.getClass();
        this.prefs.edit().putString("city_name", str).apply();
    }

    public final void setCityNames(String str, String str2) {
        setCityName(str);
        this.prefs.edit().putString("city_name_ar", str).putString("city_name_en", str2).apply();
    }

    public final void setCityNamesOrig(String str, String str2) {
        this.prefs.edit().putString("city_name_ar_orig", str).putString("city_name_en_orig", str2).apply();
    }

    public final void setCountryName(String str) {
        str.getClass();
        this.prefs.edit().putString("country_name", str).apply();
    }

    public final void setLastAlarmTriggeredDate(String str) {
        str.getClass();
        this.prefs.edit().putString("last_alarm_triggered_date", str).apply();
    }

    public final void setLastCalculatedFajrTimeMillis(long j) {
        this.prefs.edit().putLong("last_fajr_time", j).apply();
    }

    public final void setLatitude(double d) {
        this.prefs.edit().putFloat("latitude", (float) d).apply();
    }

    public final void setLongitude(double d) {
        this.prefs.edit().putFloat("longitude", (float) d).apply();
    }

    public final void setMaxDurationMinutes(int i) {
        this.prefs.edit().putInt("max_duration_minutes", i).apply();
    }

    public final void setPlaceKey(String str) {
        this.prefs.edit().putString("place_key", str).apply();
    }

    public final void setRingtoneTitle(String str) {
        str.getClass();
        this.prefs.edit().putString("ringtone_title", str).apply();
    }

    public final void setRingtoneUri(String str) {
        str.getClass();
        this.prefs.edit().putString("ringtone_uri", str).apply();
    }

    public final void setSlotDate(String str, String str2) {
        this.prefs.edit().putString("alarm_slot_date_" + str, str2).apply();
    }

    public final void setSnoozeCount(int i) {
        this.prefs.edit().putInt("snooze_count", i).apply();
    }

    public final void setSnoozeMinutes(int i) {
        this.prefs.edit().putInt("snooze_minutes", i).apply();
    }

    public final void setUseCurrentLocation(boolean z) {
        this.prefs.edit().putBoolean("use_current_location", z).apply();
    }

    public final void setVibrationEnabled(boolean z) {
        this.prefs.edit().putBoolean("is_vibration_enabled", z).apply();
    }

    public final void setWidgetBarColor(String str) {
        str.getClass();
        this.prefs.edit().putString("widget_bar_color", str).apply();
    }
}
