package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.alarm.AlarmReceiver
import com.example.prayer.alerts.PrayerAlertReceiver

/**
 * Unified notification sound / vibration preferences and per-setting channel ids.
 * Rebuilt from the original 3.4.2 helper (R8 name c21).
 */
object NotifPrefs {
    private const val APP = "app_prefs"
    private val VARIANT = Regex("^_s[0-9a-f]{1,8}$")

    private fun sp(c: Context) = c.applicationContext.getSharedPreferences(APP, Context.MODE_PRIVATE)

    /** One-time migration of the old fajr_prefs sound/vibration into app_prefs. */
    fun migrate(c: Context) {
        val s = sp(c)
        if (s.getBoolean("notif_unified_v330", false)) return
        val old = c.applicationContext.getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE)
        val e = s.edit()
        val oldTone = old.getString("ringtone_uri", "") ?: ""
        if (oldTone.isNotEmpty()) {
            e.putString("notification_sound", oldTone)
            e.putString("notification_sound_title", old.getString("ringtone_title", "") ?: "")
        }
        if (old.contains("is_vibration_enabled")) e.putBoolean("vibration_enabled", old.getBoolean("is_vibration_enabled", true))
        e.putBoolean("notif_unified_v330", true).apply()
    }

    /** Sound uri, or "" for the system default. */
    fun tone(c: Context): String {
        migrate(c)
        val t = sp(c).getString("notification_sound", "default") ?: "default"
        return if (t == "default") "" else t
    }

    fun toneTitle(c: Context): String? {
        migrate(c)
        val t = sp(c).getString("notification_sound_title", null)
        return if (t == null || t.isBlank()) null else t
    }

    fun vibration(c: Context): Boolean {
        migrate(c)
        return sp(c).getBoolean("vibration_enabled", true)
    }

    fun setVibration(c: Context, on: Boolean) {
        migrate(c)
        sp(c).edit().putBoolean("vibration_enabled", on).apply()
        refreshChannels(c)
    }

    fun setTone(c: Context, uri: String, title: String?) {
        migrate(c)
        sp(c).edit()
            .putString("notification_sound", if (uri.isEmpty()) "default" else uri)
            .putString("notification_sound_title", title ?: "")
            .apply()
        refreshChannels(c)
    }

    /** Channel id for a base id and the current tone/vibration: the base id itself for default+vibrate. */
    fun channelId(base: String, tone: String, vibrate: Boolean): String =
        if (tone.isEmpty() && vibrate) base else base + "_s" + Integer.toHexString("$tone|$vibrate".hashCode())

    /** Creates (if missing) the variant channel for [tone]/[vibrate] and drops stale variants of [template]. */
    fun ensureChannel(c: Context, nm: NotificationManager?, template: NotificationChannel, tone: String, vibrate: Boolean): String {
        val id = channelId(template.id, tone, vibrate)
        if (nm != null && Build.VERSION.SDK_INT >= 26) {
            if (nm.getNotificationChannel(id) == null) {
                val ch = NotificationChannel(id, template.name, template.importance)
                ch.description = template.description
                ch.setShowBadge(template.canShowBadge())
                if (tone.isNotEmpty()) {
                    ch.setSound(Uri.parse(tone), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                }
                ch.enableVibration(vibrate)
                nm.createNotificationChannel(ch)
                Log.i("NotifPrefs", "created channel $id vib=$vibrate tone=$tone")
            }
            deleteVariants(nm, template.id, id)
        }
        return id
    }

    /** Deletes every channel that is a stale "_s<hex>" variant of [base] (or [base] itself) except [keep]. */
    fun deleteVariants(nm: NotificationManager?, base: String, keep: String?) {
        if (nm == null || Build.VERSION.SDK_INT < 26) return
        for (ch in nm.notificationChannels) {
            val id = ch.id
            if (id == keep) continue
            if (id != base && !(id.startsWith(base) && VARIANT.matches(id.substring(base.length)))) continue
            nm.deleteNotificationChannel(id)
        }
    }

    fun refreshChannels(c: Context) {
        Log.i("NotifPrefs", "refreshChannels tone=${tone(c)} vib=${vibration(c)}")
        try {
            AlarmReceiver.ensureChannel(c.applicationContext)
        } catch (t: Throwable) {
            Log.e("NotifPrefs", "azkar channel failed", t)
        }
        try {
            com.example.prayer.alerts.PrayerAlertReceiver.refreshChannelsStatic(c.applicationContext)
        } catch (t: Throwable) {
            Log.e("NotifPrefs", "prayer alert channels failed", t)
        }
    }
}
