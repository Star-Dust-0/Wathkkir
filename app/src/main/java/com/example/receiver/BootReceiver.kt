package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.DhikrAlarmScheduler
import com.example.data.AppDatabase
import com.example.fajr.alarm.FajrAlarmScheduler
import com.example.prayer.alerts.PrayerAlertScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Re-arms prayer alerts, the Fajr alarm and every enabled dhikr reminder after boot, update or clock change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try { PrayerAlertScheduler.INSTANCE.scheduleAll(context) } catch (_: Throwable) {}
        try { FajrAlarmScheduler(context).rescheduleFromPrefs() } catch (_: Throwable) {}
        when (intent.action) {
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED" -> {
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        val scheduler = DhikrAlarmScheduler(context)
                        AppDatabase.getDatabase(context).dhikrDao().getEnabledDhikrSync().forEach { scheduler.schedule(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
