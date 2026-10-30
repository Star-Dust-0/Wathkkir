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

// Secondary reconstruction from Oct20 historical scheduler and original APK m30.
class FajrAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService("alarm") as AlarmManager
    private val prefs = FajrPreferences(context)
    companion object {
        const val ACTION_FAJR_ALARM = "com.example.fajr.ACTION_FAJR_ALARM"
        const val ALARM_REQUEST_CODE = 88001
        const val SNOOZE_REQUEST_CODE = 88002
        const val BEFORE_REQUEST_CODE = 88003
        const val AFTER_REQUEST_CODE = 88004
    }
    private fun fajrOn(dayOffset: Int): Long {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
        return PrayerAdjustments.fajrTime(context, calendar, prefs.latitude, prefs.longitude, CalculationMethod.INSTANCE.fromId(prefs.calculationMethodId))
    }
    private fun nextWithOffset(offset: Long, now: Long): Long {
        val today = fajrOn(0) + offset
        return if (today <= now) fajrOn(1) + offset else today
    }
    fun scheduleNextFajrAlarm(): Long {
        if (!prefs.isAlarmEnabled) { cancelAllAlarms(); return 0L }
        val now = System.currentTimeMillis()
        val next = nextWithOffset(0, now)
        prefs.lastCalculatedFajrTimeMillis = next
        setExactAlarm(next, ALARM_REQUEST_CODE)
        if (prefs.alarmBeforeMin > 0) setExactAlarm(nextWithOffset(-prefs.alarmBeforeMin * 60000L, now), BEFORE_REQUEST_CODE)
        else cancelCode(BEFORE_REQUEST_CODE)
        if (prefs.alarmAfterMin > 0) setExactAlarm(nextWithOffset(prefs.alarmAfterMin * 60000L, now), AFTER_REQUEST_CODE)
        else cancelCode(AFTER_REQUEST_CODE)
        return next
    }
    fun scheduleSnooze(snoozeMinutes: Int) { setExactAlarm(System.currentTimeMillis() + snoozeMinutes * 60000L, SNOOZE_REQUEST_CODE) }
    private fun setExactAlarm(trigger: Long, code: Int) {
        val intent = Intent(context, FajrAlarmReceiver::class.java).apply {
            action = if (code == SNOOZE_REQUEST_CODE) "ACTION_SNOOZE_TRIGGER" else ACTION_FAJR_ALARM
            putExtra("slot", when (code) { BEFORE_REQUEST_CODE -> "before"; AFTER_REQUEST_CODE -> "after"; else -> "main" })
        }
        val pending = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val show = Intent(context, FajrAlarmActivity::class.java).apply { flags = 335544320 }
        val showPending = PendingIntent.getActivity(context, code + 10, show, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms())
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, showPending), pending)
        else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }
    private fun cancelCode(code: Int) {
        val intent = Intent(context, FajrAlarmReceiver::class.java).apply { action = ACTION_FAJR_ALARM }
        PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarmManager.cancel(it); it.cancel()
        }
    }
    fun cancelAllAlarms() { cancelCode(ALARM_REQUEST_CODE); cancelCode(BEFORE_REQUEST_CODE); cancelCode(AFTER_REQUEST_CODE); cancelCode(SNOOZE_REQUEST_CODE) }
}
