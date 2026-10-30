package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri

/**
 * 3.3.0: the single source for notification tone and vibration across the whole app
 * (prayer alerts, iqama, azkar reminders, Hijri events). Backed by app_prefs, the same
 * keys the Settings > Notifications rows write. The per-prayer sheet reads and writes
 * these same values.
 */
object NotifPrefs {
    private const val APP = "app_prefs"
    private const val K_TONE = "notification_sound"          // "default" or a uri string
    private const val K_TITLE = "notification_sound_title"
    private const val K_VIB = "vibration_enabled"
    private const val K_MIGRATED = "notif_unified_v330"

    private fun sp(c: Context) = c.applicationContext.getSharedPreferences(APP, Context.MODE_PRIVATE)

    /** Once: carry over the tone/vibration the user had set in the prayer sheet (fajr_prefs),
     *  because those were the values that actually drove prayer alerts. Nothing is lost. */
    @JvmStatic
    fun migrate(c: Context) {
        val s = sp(c)
        if (s.getBoolean(K_MIGRATED, false)) return
        val old = c.applicationContext.getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE)
        val e = s.edit()
        val oldTone = old.getString("ringtone_uri", "") ?: ""
        if (oldTone.isNotEmpty()) {
            e.putString(K_TONE, oldTone)
            e.putString(K_TITLE, old.getString("ringtone_title", "") ?: "")
        }
        if (old.contains("is_vibration_enabled")) e.putBoolean(K_VIB, old.getBoolean("is_vibration_enabled", true))
        e.putBoolean(K_MIGRATED, true).apply()
    }

    /** "" means the system default notification sound. */
    @JvmStatic
    fun tone(c: Context): String { migrate(c); val t = sp(c).getString(K_TONE, "default") ?: "default"; return if (t == "default") "" else t }

    @JvmStatic
    fun toneTitle(c: Context): String? { migrate(c); return sp(c).getString(K_TITLE, null)?.takeIf { it.isNotBlank() } }

    @JvmStatic
    fun setTone(c: Context, uri: String, title: String?) {
        migrate(c)
        sp(c).edit().putString(K_TONE, if (uri.isEmpty()) "default" else uri).putString(K_TITLE, title ?: "").apply()
        refreshChannels(c)
    }

    @JvmStatic
    fun titleFor(c: Context, uri: String): String? = try {
        android.media.RingtoneManager.getRingtone(c, Uri.parse(uri))?.getTitle(c)
    } catch (_: Throwable) { null }

    @JvmStatic
    fun vibration(c: Context): Boolean { migrate(c); return sp(c).getBoolean(K_VIB, true) }

    @JvmStatic
    fun setVibration(c: Context, on: Boolean) { migrate(c); sp(c).edit().putBoolean(K_VIB, on).apply(); refreshChannels(c) }

    /** Applies the current values to every notification channel right away. */
    @JvmStatic
    fun refreshChannels(c: Context) {
        try { com.example.alarm.AlarmReceiver.Companion.createNotificationChannel(c.applicationContext) } catch (_: Throwable) {}
        com.example.prayer.alerts.PrayerAlertReceiver.refreshChannels(c.applicationContext)
    }

    /** Android restores a deleted channel's old sound/vibration when a channel with the same id is
     *  created again, so deleting and recreating one id can never change them. The tone/vibration
     *  are therefore part of the channel id: default values keep the original id, any other
     *  combination gets "<base>_s<hash>". Changing a setting means a different id (fresh channel). */
    @JvmStatic
    fun variantId(base: String, tone: String, vib: Boolean): String =
        if (tone.isEmpty() && vib) base else base + "_s" + Integer.toHexString("$tone|$vib".hashCode())

    /** The channel id to post to, for the global tone/vibration. */
    @JvmStatic
    fun channelId(c: Context, base: String): String = variantId(base, tone(c), vibration(c))

    /** Deletes every variant of [base] (the plain id and "_s<hash>" ones) except [keep]. */
    @JvmStatic
    fun deleteVariants(nm: NotificationManager?, base: String, keep: String?) {
        if (nm == null || android.os.Build.VERSION.SDK_INT < 26) return
        val rx = Regex("^_s[0-9a-f]{1,8}$")
        for (x in nm.notificationChannels) {
            val id = x.id
            if (id == keep) continue
            if (id == base || (id.startsWith(base) && rx.matches(id.substring(base.length)))) nm.deleteNotificationChannel(id)
        }
    }

    /** Creates the channel with the global tone/vibration; returns the id to post to. */
    @JvmStatic
    fun configureChannel(c: Context, nm: NotificationManager?, ch: NotificationChannel): String =
        configureChannel(c, nm, ch, tone(c), vibration(c))

    /** Same, with explicit values (used for a prayer that has its own tone/vibration). */
    @JvmStatic
    fun configureChannel(c: Context, nm: NotificationManager?, ch: NotificationChannel, tone: String, vib: Boolean): String {
        val id = variantId(ch.id, tone, vib)
        if (nm == null || android.os.Build.VERSION.SDK_INT < 26) return id
        if (nm.getNotificationChannel(id) == null) {
            val n = NotificationChannel(id, ch.name, ch.importance)
            n.description = ch.description
            n.setShowBadge(ch.canShowBadge())
            if (tone.isNotEmpty()) {
                n.setSound(
                    Uri.parse(tone),
                    android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build()
                )
            }
            n.enableVibration(vib)
            nm.createNotificationChannel(n)
            android.util.Log.i("NotifPrefs", "created channel $id vib=$vib tone=$tone")
        }
        deleteVariants(nm, ch.id, id)
        return id
    }
}
