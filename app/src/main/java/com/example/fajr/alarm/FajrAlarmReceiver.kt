package com.example.fajr.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.fajr.data.FajrPreferences
import com.example.fajr.ui.FajrAlarmActivity
import com.example.prayer.alerts.PrayerAlertStore
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fires the Fajr alarm (main slot, before/after offsets, or a snooze): posts the
 * full-screen alarm notification and launches [FajrAlarmActivity], then schedules the
 * next day. Kotlin replacement for the recovered decompiled Java.
 */
class FajrAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        L10n.init(context)
        val prefs = FajrPreferences(context)
        if (!PrayerAlertStore.isFajrAlarmActive(context)) return

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val slot = intent.getStringExtra("slot") ?: "main"
        val isSnooze = intent.action == FajrAlarmScheduler.ACTION_SNOOZE_TRIGGER

        // This slot already fired today (e.g. a stale duplicate alarm): just re-arm for tomorrow.
        if (!isSnooze && prefs.getSlotDate(slot) == todayDate) {
            FajrAlarmScheduler(context).scheduleNextFajrAlarm()
            return
        }
        if (!isSnooze) {
            prefs.setSlotDate(slot, todayDate)
            prefs.lastAlarmTriggeredDate = todayDate
            prefs.snoozeCount = 0
        }

        val alarmIntent = Intent(context, FajrAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                FAJR_CHANNEL, L10n.t(S.FajrAlarm), NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = L10n.t(S.NotifChannelFajr)
                setBypassDnd(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, FAJR_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(L10n.t(S.FajrPrayer))
            .setContentText(L10n.t(S.FajrNow))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(FajrAlarmScheduler.ALARM_REQUEST_CODE, notification)

        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.w(TAG, "could not launch FajrAlarmActivity", e)
        }
        FajrAlarmScheduler(context).scheduleNextFajrAlarm()
    }

    private companion object {
        const val TAG = "FajrAlarmReceiver"
        const val FAJR_CHANNEL = "fajr_alarm_channel"
    }
}
