package com.example.prayer.alerts

import android.content.Context
import com.example.fajr.data.FajrPreferences

/**
 * Per-prayer alert (bell) settings for 3.1.0. Per-prayer: enable, timing (minutes
 * before adhan, 0 = at adhan), iqama reminder. Bells are always ordinary notifications
 * (no alarm mode). Sound/vibration stay SHARED in FajrPreferences (user decision).
 * Prayer index: 0 Fajr, 1 Shuruq, 2 Dhuhr, 3 Asr, 4 Maghrib, 5 Isha.
 */
object PrayerAlertStore {
    private const val PREFS = "prayer_alerts"

    private fun p(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** All stored settings of one prayer (keys end with _<prayer>); used to discard staged edits on Cancel. */
    fun snapshot(context: Context, prayer: Int): Map<String, Any?> =
        p(context).all.filterKeys { it.endsWith("_$prayer") }

    fun restore(context: Context, prayer: Int, snap: Map<String, Any?>) {
        val e = p(context).edit()
        p(context).all.keys.filter { it.endsWith("_$prayer") && it !in snap }.forEach { e.remove(it) }
        snap.forEach { (k, v) ->
            when (v) {
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Float -> e.putFloat(k, v)
                is String -> e.putString(k, v)
            }
        }
        e.apply()
    }

    fun isEnabled(context: Context, prayer: Int): Boolean {
        migrate(context)
        return p(context).getBoolean("enabled_$prayer", false)
    }

    fun setEnabled(context: Context, prayer: Int, enabled: Boolean) {
        migrate(context)
        p(context).edit().putBoolean("enabled_$prayer", enabled).apply()
    }

    /** Minutes before adhan; 0 = exactly at adhan. */
    fun offsetMinutes(context: Context, prayer: Int): Int {
        migrate(context)
        return p(context).getInt("offset_$prayer", 0)
    }

    fun setOffsetMinutes(context: Context, prayer: Int, minutes: Int) {
        migrate(context)
        p(context).edit().putInt("offset_$prayer", minutes.coerceIn(0, 60)).apply()
    }

    /** Auto-DND during adhan -> iqama window (gap item 3). */
    fun autoDnd(context: Context, prayer: Int): Boolean {
        migrate(context)
        return p(context).getBoolean("dnd_" + prayer, false)
    }

    fun setAutoDnd(context: Context, prayer: Int, enabled: Boolean) {
        migrate(context)
        p(context).edit().putBoolean("dnd_" + prayer, enabled).apply()
    }

    /** Alert style at adhan time: false = notification (default), true = full-screen. */
    fun fullScreen(context: Context, prayer: Int): Boolean {
        migrate(context)
        return p(context).getBoolean("fullscreen_$prayer", false)
    }

    fun setFullScreen(context: Context, prayer: Int, full: Boolean) {
        migrate(context)
        p(context).edit().putBoolean("fullscreen_$prayer", full).apply()
    }

    fun iqamaReminder(context: Context, prayer: Int): Boolean {
        migrate(context)
        return p(context).getBoolean("iqama_$prayer", false)
    }

    fun setIqamaReminder(context: Context, prayer: Int, enabled: Boolean) {
        migrate(context)
        p(context).edit().putBoolean("iqama_$prayer", enabled).apply()
    }

    // 3.3.0: optional per-prayer tone/vibration. Absent = follow the app-level Settings values.
    /** null = inherit; "default" = system default; otherwise a uri string. */
    fun toneOverride(context: Context, prayer: Int): String? = p(context).getString("tone_ov_$prayer", null)
    fun toneOverrideTitle(context: Context, prayer: Int): String? = p(context).getString("tone_ov_title_$prayer", null)?.takeIf { it.isNotBlank() }
    fun setToneOverride(context: Context, prayer: Int, uri: String, title: String?) {
        p(context).edit().putString("tone_ov_$prayer", uri.ifEmpty { "default" }).putString("tone_ov_title_$prayer", title ?: "").apply()
        PrayerAlertReceiver.refreshChannels(context)
    }
    /** null = inherit. */
    fun vibrationOverride(context: Context, prayer: Int): Boolean? =
        if (p(context).contains("vib_ov_$prayer")) p(context).getBoolean("vib_ov_$prayer", true) else null
    fun setVibrationOverride(context: Context, prayer: Int, on: Boolean) {
        p(context).edit().putBoolean("vib_ov_$prayer", on).apply()
        PrayerAlertReceiver.refreshChannels(context)
    }
    fun hasOverride(context: Context, prayer: Int) = toneOverride(context, prayer) != null || vibrationOverride(context, prayer) != null
    fun clearOverride(context: Context, prayer: Int) {
        p(context).edit().remove("tone_ov_$prayer").remove("tone_ov_title_$prayer").remove("vib_ov_$prayer").apply()
        PrayerAlertReceiver.refreshChannels(context)
    }
    /** Effective values for this prayer: its own choice if set, otherwise the app-level setting. */
    fun effectiveTone(context: Context, prayer: Int): String {
        val o = toneOverride(context, prayer) ?: return com.example.utils.NotifPrefs.tone(context)
        return if (o == "default") "" else o
    }
    fun effectiveVibration(context: Context, prayer: Int): Boolean =
        vibrationOverride(context, prayer) ?: com.example.utils.NotifPrefs.vibration(context)

    /** Legacy alarm path gate: always false after migration silenced the legacy flag. */
    fun isFajrAlarmActive(context: Context): Boolean {
        migrate(context)
        return FajrPreferences(context).isAlarmEnabled
    }

    /** One-time migration: an enabled legacy Fajr alarm becomes the Fajr bell in alarm mode. */
    @Synchronized
    fun migrate(context: Context) {
        val prefs = p(context)
        if (prefs.getBoolean("migrated_v1", false)) return
        val legacy = FajrPreferences(context)
        if (legacy.isAlarmEnabled) {
            prefs.edit()
                .putBoolean("enabled_0", true)
                .apply()
            // Silence the legacy scheduler path so only the new bell fires.
            legacy.isAlarmEnabled = false
        }
        prefs.edit().putBoolean("migrated_v1", true).apply()
    }
}
