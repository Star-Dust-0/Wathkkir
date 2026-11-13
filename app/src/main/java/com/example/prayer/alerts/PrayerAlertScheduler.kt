package com.example.prayer.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.data.FajrPreferences
import java.util.Calendar

/**
 * Schedules per-prayer alerts (bells) as exact alarms. Rescheduled after every fire,
 * on boot/timezone/time-set (BootReceiver hook), and on any settings change.
 * Request codes: adhan alert = 100+i, iqama reminder = 200+i (i = prayer index 0..5).
 */
object PrayerAlertScheduler {
    const val EXTRA_PRAYER = "prayer_index"
    const val EXTRA_KIND = "alert_kind"
    const val EXTRA_AT = "prayer_at"
    const val KIND_ADHAN = 0
    const val KIND_IQAMA = 1
    const val KIND_DND_OFF = 2
    // Early reminder: an ADDITIONAL alarm before the adhan; the adhan-time alarm always fires.
    const val KIND_EARLY = 3

    private val IQAMA_OFFSETS = IntArray(6) { com.example.lockwidgets.PrayerPhase.iqamaMinutes(it) } // Shuruq has no iqama

    fun scheduleAll(context: Context) {
        PrayerAlertStore.migrate(context)
        val prefs = FajrPreferences(context)
        val method = CalculationMethod.INSTANCE.fromId(prefs.calculationMethodId)
        val now = System.currentTimeMillis()
        val today = PrayerAdjustments.calculateAll(
            context, Calendar.getInstance(), prefs.latitude, prefs.longitude, method
        )
        val times = longArrayOf(today.fajr, today.sunrise, today.dhuhr, today.asr, today.maghrib, today.isha)
        for (i in 0..5) {
            scheduleOne(context, i, nextOccurrence(context, i, times, now, prefs, method), times[i], now)
        }
    }

    private fun nextOccurrence(
        context: Context, prayer: Int, times: LongArray, now: Long,
        prefs: FajrPreferences, method: CalculationMethod
    ): Long {
        // The base adhan alarm always fires at the true adhan time; the early reminder is a
        // separate second alarm (see scheduleOne), never a replacement.
        var trigger = times[prayer]
        if (trigger <= now) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DATE, 1) }
            val t = PrayerAdjustments.calculateAll(context, tomorrow, prefs.latitude, prefs.longitude, method)
            val arr = longArrayOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)
            trigger = arr[prayer]
        }
        return trigger
    }

    private fun scheduleOne(context: Context, prayer: Int, adhanTrigger: Long, todayAdhan: Long, now: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Always replace both pending intents for this prayer.
        am.cancel(pending(context, prayer, KIND_ADHAN))
        am.cancel(pending(context, prayer, KIND_IQAMA))
        am.cancel(pending(context, prayer, KIND_DND_OFF))
        am.cancel(pending(context, prayer, KIND_EARLY))
        if (!PrayerAlertStore.isEnabled(context, prayer)) return
        setExact(context, am, adhanTrigger, pending(context, prayer, KIND_ADHAN))
        // Additional early reminder: fires IN ADDITION to the adhan-time notification.
        val leadMs = PrayerAlertStore.offsetMinutes(context, prayer) * 60000L
        if (leadMs > 0 && adhanTrigger - leadMs > now) {
            setExact(context, am, adhanTrigger - leadMs, pending(context, prayer, KIND_EARLY, adhanTrigger))
        }
        // Iqama and DND-off anchor to TODAY's adhan while that iqama is still ahead, so
        // rescheduling right after the adhan fires does not cancel today's iqama reminder
        // or leave DND on until tomorrow.
        val iqamaBase = if (todayAdhan + IQAMA_OFFSETS[prayer] * 60000L > now) todayAdhan else adhanTrigger
        val iqamaAt = iqamaBase + IQAMA_OFFSETS[prayer] * 60000L
        // Friday has no iqama: no iqama reminder for Jumu'a (3.3.3 behavior).
        val iqamaFriday = Calendar.getInstance().apply { timeInMillis = iqamaAt }
            .get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY && prayer == 2
        if (PrayerAlertStore.iqamaReminder(context, prayer) && IQAMA_OFFSETS[prayer] > 0 && !iqamaFriday) {
            if (iqamaAt > now) {
                setExact(context, am, iqamaAt, pending(context, prayer, KIND_IQAMA))
            }
        }
        // Auto-DND: receiver enables DND when the adhan fires; this alarm restores it at iqama.
        if (PrayerAlertStore.autoDnd(context, prayer) && IQAMA_OFFSETS[prayer] > 0 && iqamaAt > now) {
            setExact(context, am, iqamaAt, pending(context, prayer, KIND_DND_OFF))
        }
    }

    private fun setExact(context: Context, am: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } catch (se: SecurityException) {
            // Exact-alarm permission denied/revoked (mainly Android 12/12L): fall back to an
            // inexact alarm so the bell still fires, and tell the user once a day instead of
            // silently drifting.
            Log.w(TAG, "exact alarm denied; using inexact fallback", se)
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi) } catch (e: Exception) {
                Log.w(TAG, "inexact fallback failed", e)
            }
            notifyExactAlarmDenied(context)
        } catch (e: Exception) {
            Log.w(TAG, "setExact failed", e)
        }
    }

    /** One notification per day at most, pointing at the exact-alarm settings screen. */
    private fun notifyExactAlarmDenied(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val last = prefs.getLong(KEY_EXACT_DENIED_NOTIFIED, 0L)
            val now = System.currentTimeMillis()
            if (now - last < 24L * 60 * 60 * 1000) return
            if (Build.VERSION.SDK_INT < 31) return
            com.example.ui.i18n.L10n.init(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = if (Build.VERSION.SDK_INT >= 26) {
                val channel = android.app.NotificationChannel(
                    PrayerAlertReceiver.ALERT_CHANNEL,
                    com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.AdhanReminder),
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                com.example.utils.NotifPrefs.configureChannel(context, nm, channel)
            } else PrayerAlertReceiver.ALERT_CHANNEL
            val settingsIntent = PendingIntent.getActivity(
                context, 450,
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.example.R.drawable.ic_stat_prayer)
                .setContentTitle(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ExactAlarmDeniedTitle))
                .setContentText(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ExactAlarmDeniedBody))
                .setStyle(NotificationCompat.BigTextStyle().bigText(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ExactAlarmDeniedBody)))
                .setContentIntent(settingsIntent)
                .setAutoCancel(true)
                .build()
            nm.notify(450, notification)
            prefs.edit().putLong(KEY_EXACT_DENIED_NOTIFIED, now).apply()
        } catch (e: Exception) {
            Log.w(TAG, "exact-denied notification failed", e)
        }
    }

    private fun pending(context: Context, prayer: Int, kind: Int, at: Long = 0L): PendingIntent {
        val intent = Intent(context, PrayerAlertReceiver::class.java)
            .putExtra(EXTRA_PRAYER, prayer)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_AT, at)
        return PendingIntent.getBroadcast(
            context, if (kind == KIND_ADHAN) 100 + prayer else if (kind == KIND_IQAMA) 200 + prayer else if (kind == KIND_DND_OFF) 300 + prayer else 400 + prayer,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0..5) {
            am.cancel(pending(context, i, KIND_ADHAN))
            am.cancel(pending(context, i, KIND_IQAMA))
            am.cancel(pending(context, i, KIND_DND_OFF))
            am.cancel(pending(context, i, KIND_EARLY))
        }
    }

    private const val TAG = "PrayerAlertScheduler"
    private const val PREFS = "prayer_alert_scheduler"
    private const val KEY_EXACT_DENIED_NOTIFIED = "exact_denied_notified_at"
}
