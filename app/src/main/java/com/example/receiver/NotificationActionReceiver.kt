package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.alarm.AlarmReceiver
import com.example.data.AppDatabase
import com.example.data.DhikrRepository
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        L10n.init(context)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
        val dhikrId = intent.getIntExtra("DHIKR_ID", -1)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationId != -1 && nm != null) nm.cancel(notificationId)
        when (intent.action) {
            ACTION_MARK_AS_READ -> {
                if (dhikrId == -1) return
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        DhikrRepository(db.dhikrDao(), db.tagDao(), db.historyDao()).markAsRead(dhikrId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                if (dhikrId == -1) return
                val title = intent.getStringExtra("DHIKR_TITLE") ?: L10n.t(S.NotifReminder)
                val content = intent.getStringExtra("DHIKR_CONTENT") ?: ""
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val at = System.currentTimeMillis() + 600000
                val i = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("DHIKR_ID", dhikrId)
                    putExtra("DHIKR_TITLE", title)
                    putExtra("DHIKR_CONTENT", content)
                    putExtra("IS_SNOOZE", true)
                }
                val pi = PendingIntent.getBroadcast(context, dhikrId * 100 + 99, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                try {
                    if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                        return
                    }
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } catch (e: Exception) {
                    Log.e("AlarmScheduler", "Error scheduling snooze alarm for Dhikr id=$dhikrId", e)
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_AS_READ = "com.example.ACTION_MARK_AS_READ"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
    }
}
