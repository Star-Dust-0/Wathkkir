package com.example.prayer.alerts

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fajr.alarm.FajrAlarmScheduler

/**
 * The user granted (or revoked) "Alarms & reminders" in system settings. Re-arm every
 * time-based alert right away so a grant takes effect without waiting for the next fire,
 * boot or app open (a revocation cancels all exact alarms system-side).
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        try { PrayerAlertScheduler.scheduleAll(context) } catch (t: Throwable) {
            Log.w(TAG, "prayer alerts reschedule failed", t)
        }
        try { FajrAlarmScheduler(context).rescheduleIfEnabled() } catch (t: Throwable) {
            Log.w(TAG, "fajr alarm reschedule failed", t)
        }
    }

    private companion object {
        const val TAG = "ExactAlarmPermRcvr"
    }
}
