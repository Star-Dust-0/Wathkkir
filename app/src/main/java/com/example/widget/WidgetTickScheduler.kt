package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.lockwidgets.LockWidgets

object WidgetTickScheduler {
    const val ACTION_TICK = "com.example.widget.TICK"

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetTickReceiver::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(context, 700, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(context: Context) {
        // Consolidated pulse: the single per-minute widget alarm now lives in LockWidgets.schedule,
        // which arms only while a widget exists and cancels once none remain. Cancel any alarm this
        // legacy chain armed so an install upgraded from an older version stops it here.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
        LockWidgets.schedule(context)
    }
}
