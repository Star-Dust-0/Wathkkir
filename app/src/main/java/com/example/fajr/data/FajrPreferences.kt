package com.example.fajr.data

import android.content.Context
import android.content.SharedPreferences

class FajrPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fajr_prefs", 0)

    var alarmAfterMin: Int
        get() = prefs.getInt("alarm_after_min", 0)
        set(value) = prefs.edit().putInt("alarm_after_min", value).apply()

    var alarmBeforeMin: Int
        get() = prefs.getInt("alarm_before_min", 0)
        set(value) = prefs.edit().putInt("alarm_before_min", value).apply()

    var alarmVolume: Int
        get() = prefs.getInt("alarm_volume", 80)
        set(value) = prefs.edit().putInt("alarm_volume", value).apply()

    var calculationMethodId: Int
        get() = prefs.getInt("calculation_method", 1)
        set(value) = prefs.edit().putInt("calculation_method", value).apply()

    var cityName: String
        get() = prefs.getString("city_name", "القاهرة") ?: "القاهرة"
        set(value) = prefs.edit().putString("city_name", value).apply()

    var cityNameArOrig: String?
        get() = prefs.getString("city_name_ar_orig", null)
        set(value) = prefs.edit().putString("city_name_ar_orig", value).apply()

    var cityNameEnOrig: String?
        get() = prefs.getString("city_name_en_orig", null)
        set(value) = prefs.edit().putString("city_name_en_orig", value).apply()

    var countryName: String
        get() = prefs.getString("country_name", "مصر") ?: "مصر"
        set(value) = prefs.edit().putString("country_name", value).apply()

    var lastAlarmTriggeredDate: String
        get() = prefs.getString("last_alarm_triggered_date", "") ?: ""
        set(value) = prefs.edit().putString("last_alarm_triggered_date", value).apply()

    var lastCalculatedFajrTimeMillis: Long
        get() = prefs.getLong("last_fajr_time", 0L)
        set(value) = prefs.edit().putLong("last_fajr_time", value).apply()

    var latitude: Double
        get() = prefs.getFloat("latitude", 30.0444f).toDouble()
        set(value) = prefs.edit().putFloat("latitude", value.toFloat()).apply()

    var longitude: Double
        get() = prefs.getFloat("longitude", 31.2357f).toDouble()
        set(value) = prefs.edit().putFloat("longitude", value.toFloat()).apply()

    var maxDurationMinutes: Int
        get() = prefs.getInt("max_duration_minutes", 5)
        set(value) = prefs.edit().putInt("max_duration_minutes", value).apply()

    var placeKey: String?
        get() = prefs.getString("place_key", null)
        set(value) = prefs.edit().putString("place_key", value).apply()

    var ringtoneTitle: String
        get() = prefs.getString("ringtone_title", "الافتراضي") ?: "الافتراضي"
        set(value) = prefs.edit().putString("ringtone_title", value).apply()

    var ringtoneUri: String
        get() = prefs.getString("ringtone_uri", "") ?: ""
        set(value) = prefs.edit().putString("ringtone_uri", value).apply()

    var snoozeCount: Int
        get() = prefs.getInt("snooze_count", 0)
        set(value) = prefs.edit().putInt("snooze_count", value).apply()

    var snoozeMinutes: Int
        get() = prefs.getInt("snooze_minutes", 10)
        set(value) = prefs.edit().putInt("snooze_minutes", value).apply()

    var useCurrentLocation: Boolean
        get() = prefs.getBoolean("use_current_location", true)
        set(value) = prefs.edit().putBoolean("use_current_location", value).apply()

    var widgetBarColor: String
        get() = prefs.getString("widget_bar_color", "default") ?: "default"
        set(value) = prefs.edit().putString("widget_bar_color", value).apply()

    var isAlarmEnabled: Boolean
        get() = prefs.getBoolean("is_alarm_enabled", false)
        set(value) = prefs.edit().putBoolean("is_alarm_enabled", value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean("is_vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("is_vibration_enabled", value).apply()

    fun clearCityNamesOrig() {
        prefs.edit().remove("city_name_ar_orig").remove("city_name_en_orig").apply()
    }

    fun getCityNameLocalized(lang: String?): String {
        val cityName = cityName
        return if ("ar" == lang) {
            prefs.getString("city_name_ar", cityName) ?: cityName
        } else {
            prefs.getString("city_name_en", null)
                ?: prefs.getString("city_name_ar", cityName)
                ?: cityName
        }
    }

    fun getSlotDate(slot: String): String = prefs.getString("alarm_slot_date_$slot", "") ?: ""

    fun setCityNames(ar: String, en: String) {
        cityName = ar
        prefs.edit().putString("city_name_ar", ar).putString("city_name_en", en).apply()
    }

    fun setCityNamesOrig(ar: String, en: String) {
        prefs.edit().putString("city_name_ar_orig", ar).putString("city_name_en_orig", en).apply()
    }

    fun setSlotDate(slot: String, value: String) {
        prefs.edit().putString("alarm_slot_date_$slot", value).apply()
    }
}
