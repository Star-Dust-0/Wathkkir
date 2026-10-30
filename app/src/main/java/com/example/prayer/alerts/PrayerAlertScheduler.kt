package com.example.prayer.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            scheduleOne(context, i, nextOccurrence(context, i, times, now, prefs, method), now)
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

    private fun scheduleOne(context: Context, prayer: Int, adhanTrigger: Long, now: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Always replace both pending intents for this prayer.
        am.cancel(pending(context, prayer, KIND_ADHAN))
        am.cancel(pending(context, prayer, KIND_IQAMA))
        am.cancel(pending(context, prayer, KIND_DND_OFF))
        am.cancel(pending(context, prayer, KIND_EARLY))
        if (!PrayerAlertStore.isEnabled(context, prayer)) return
        setExact(am, adhanTrigger, pending(context, prayer, KIND_ADHAN))
        // Additional early reminder: fires IN ADDITION to the adhan-time notification.
        val leadMs = PrayerAlertStore.offsetMinutes(context, prayer) * 60000L
        if (leadMs > 0 && adhanTrigger - leadMs > now) {
            setExact(am, adhanTrigger - leadMs, pending(context, prayer, KIND_EARLY, adhanTrigger))
        }
        val iqamaAt = adhanTrigger + IQAMA_OFFSETS[prayer] * 60000L
        if (PrayerAlertStore.iqamaReminder(context, prayer) && IQAMA_OFFSETS[prayer] > 0) {
            if (iqamaAt > now) {
                setExact(am, iqamaAt, pending(context, prayer, KIND_IQAMA))
            }
        }
        // Auto-DND: receiver enables DND when the adhan fires; this alarm restores it at iqama.
        if (PrayerAlertStore.autoDnd(context, prayer) && IQAMA_OFFSETS[prayer] > 0 && iqamaAt > now) {
            setExact(am, iqamaAt, pending(context, prayer, KIND_DND_OFF))
        }
    }

    private fun setExact(am: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } catch (se: SecurityException) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi) } catch (_: Exception) {}
        } catch (_: Exception) {}
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
        }
    }
}
