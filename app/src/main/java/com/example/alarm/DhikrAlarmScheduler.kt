package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Dhikr
import java.util.Calendar

/** Schedules the per-dhikr reminder alarms (up to 20 per dhikr, request code id*100+index). */
class DhikrAlarmScheduler(private val context: Context) {
    private val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun cancel(dhikr: Dhikr) {
        val am = alarmManager ?: return
        for (i in 0 until 20) {
            val pi = PendingIntent.getBroadcast(
                context, dhikr.id * 100 + i, Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun schedule(dhikr: Dhikr) {
        val am = alarmManager ?: return
        cancel(dhikr)
        dhikr.reminderTimes.forEachIndexed { index, time ->
            val parts = time.split(":")
            if (parts.size != 2) return@forEachIndexed
            val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
            val minute = parts[1].toIntOrNull() ?: return@forEachIndexed
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("DHIKR_ID", dhikr.id)
                putExtra("DHIKR_TITLE", dhikr.title)
                putExtra("DHIKR_CONTENT", dhikr.content)
            }
            val pi = PendingIntent.getBroadcast(
                context, dhikr.id * 100 + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }
            try {
                // Original fi0.w chooses the permitted branch, without attempting exact
                // after the inexact call on API31+ when access was denied.
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                }
            } catch (e: Exception) {
                Log.e("AlarmScheduler", "Error scheduling alarm for Dhikr id=${dhikr.id}", e)
                try {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                } catch (e2: Exception) {
                    Log.e("AlarmScheduler", "Fallback alarm scheduling failed", e2)
                }
            }
        }
    }
}
