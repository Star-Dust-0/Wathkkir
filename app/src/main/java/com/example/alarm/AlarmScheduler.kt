package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Dhikr
import java.util.Calendar

/** Schedules the per-dhikr reminder alarms (one request code per reminder time). */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService("alarm") as? AlarmManager

    fun schedule(dhikr: Dhikr) {
        val manager = alarmManager ?: return
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
            val pendingIntent = PendingIntent.getBroadcast(
                context, dhikr.id * 100 + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }
            try {
                setAlarm(manager, calendar.timeInMillis, pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm for Dhikr id=${dhikr.id}", e)
                try {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback alarm scheduling failed", e2)
                }
            }
        }
    }

    fun scheduleSnooze(id: Int, title: String, content: String, minutes: Int = 10) {
        val manager = alarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60 * 1000
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("DHIKR_ID", id)
            putExtra("DHIKR_TITLE", title)
            putExtra("DHIKR_CONTENT", content)
            putExtra("IS_SNOOZE", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, id * 100 + 99, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            setAlarm(manager, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling snooze alarm for Dhikr id=$id", e)
        }
    }

    fun cancel(dhikr: Dhikr) {
        val manager = alarmManager ?: return
        for (index in 0 until MAX_TIMES_PER_DHIKR) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, dhikr.id * 100 + index, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                manager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    /** Exact alarm when allowed (API 31+ needs the permission), inexact-while-idle otherwise. */
    private fun setAlarm(manager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private companion object {
        const val MAX_TIMES_PER_DHIKR = 20
        const val TAG = "AlarmScheduler"
    }
}
