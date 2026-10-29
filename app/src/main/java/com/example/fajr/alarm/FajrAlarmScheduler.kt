package com.example.fajr.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.data.FajrPreferences
import com.example.fajr.ui.FajrAlarmActivity
import java.util.Calendar

/**
 * Schedules the Fajr alarm (main, before and after slots). Rebuilt from the original 3.4.2
 * class (R8 name m30). Request codes: 88001 main, 88002 snooze, 88003 before, 88004 after.
 */
class FajrAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = FajrPreferences(context)

    private fun alarmIntent(): Intent =
        Intent(context, FajrAlarmReceiver::class.java).setAction(ACTION_FAJR_ALARM)

    private fun cancelCode(code: Int) {
        val pi = PendingIntent.getBroadcast(context, code, alarmIntent(), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun cancelAll() {
        cancelCode(REQ_MAIN)
        cancelCode(REQ_BEFORE)
        cancelCode(REQ_AFTER)
        cancelCode(REQ_SNOOZE)
    }

    private fun fajrMillis(dayOffset: Int): Long {
        val cal = Calendar.getInstance()
        if (dayOffset != 0) cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        val method = CalculationMethod.INSTANCE.fromId(prefs.calculationMethodId)
        return PrayerAdjustments.INSTANCE.fajrTime(context, cal, prefs.latitude, prefs.longitude, method)
    }

    /** Fajr of today plus [offset]; moves to tomorrow when that time is not after [now]. */
    private fun nextTrigger(offset: Long, now: Long): Long {
        val t = fajrMillis(0) + offset
        return if (t <= now) fajrMillis(1) + offset else t
    }

    /** Re-arms all slots from the saved preferences. */
    fun rescheduleFromPrefs() {
        if (!prefs.isAlarmEnabled) {
            cancelAll()
            return
        }
        val now = System.currentTimeMillis()
        val main = nextTrigger(0L, now)
        prefs.lastCalculatedFajrTimeMillis = main
        scheduleAt(REQ_MAIN, main)
        val before = prefs.alarmBeforeMin
        if (before > 0) scheduleAt(REQ_BEFORE, nextTrigger((-before * 60000).toLong(), now)) else cancelCode(REQ_BEFORE)
        val after = prefs.alarmAfterMin
        if (after > 0) scheduleAt(REQ_AFTER, nextTrigger((after * 60000).toLong(), now)) else cancelCode(REQ_AFTER)
    }

    fun scheduleAt(requestCode: Int, triggerAt: Long) {
        val intent = Intent(context, FajrAlarmReceiver::class.java)
            .setAction(if (requestCode == REQ_SNOOZE) "ACTION_SNOOZE_TRIGGER" else ACTION_FAJR_ALARM)
            .putExtra("slot", when (requestCode) { REQ_BEFORE -> "before"; REQ_AFTER -> "after"; else -> "main" })
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val show = PendingIntent.getActivity(
            context, requestCode + 10,
            Intent(context, FajrAlarmActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    companion object {
        const val ACTION_FAJR_ALARM = "com.example.fajr.ACTION_FAJR_ALARM"
        const val REQ_MAIN = 88001
        const val REQ_SNOOZE = 88002
        const val REQ_BEFORE = 88003
        const val REQ_AFTER = 88004
    }
}
