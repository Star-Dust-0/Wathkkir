package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fajr.alarm.FajrAlarmScheduler
import com.example.prayer.alerts.PrayerAlertScheduler

/**
 * Re-arms everything time-based after boot, time/timezone changes and app updates:
 * prayer bells, the Fajr alarm, then the dhikr reminders (async via [BootRecovery]).
 * Kotlin replacement for the recovered decompiled Java.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try { PrayerAlertScheduler.scheduleAll(context) } catch (t: Throwable) {
            Log.w(TAG, "prayer alerts reschedule failed", t)
        }
        try { FajrAlarmScheduler(context).scheduleNextFajrAlarm() } catch (t: Throwable) {
            Log.w(TAG, "fajr alarm reschedule failed", t)
        }
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val pending = goAsync()
                BootRecovery.restore(context, pending)
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
