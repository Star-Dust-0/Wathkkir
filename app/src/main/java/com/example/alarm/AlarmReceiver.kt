package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.receiver.NotificationActionReceiver
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.utils.NotifPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires the dhikr reminder notification and re-arms the next day's alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        L10n.init(context)
        val dhikrId = intent.getIntExtra("DHIKR_ID", -1)
        val title = intent.getStringExtra("DHIKR_TITLE") ?: L10n.t(S.NotifReminder)
        val content = intent.getStringExtra("DHIKR_CONTENT") ?: ""
        val isSnooze = intent.getBooleanExtra("IS_SNOOZE", false)
        if (dhikrId == -1) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null) {
            ensureChannel(context)
            val open = Intent(context, MainActivity::class.java).apply {
                flags = 0x34000000
                putExtra("DHIKR_ID", dhikrId)
                putExtra("NOTIFICATION_ID", dhikrId)
            }
            val base = dhikrId * 10
            val openPi = PendingIntent.getActivity(context, base + 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val read = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_AS_READ
                putExtra("DHIKR_ID", dhikrId)
                putExtra("NOTIFICATION_ID", dhikrId)
            }
            val readPi = PendingIntent.getBroadcast(context, base + 2, read, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val snooze = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SNOOZE
                putExtra("DHIKR_ID", dhikrId)
                putExtra("DHIKR_TITLE", title)
                putExtra("DHIKR_CONTENT", content)
                putExtra("NOTIFICATION_ID", dhikrId)
            }
            val snoozePi = PendingIntent.getBroadcast(context, base + 3, snooze, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val channel = NotifPrefs.channelId(CHANNEL_ID, NotifPrefs.tone(context), NotifPrefs.vibration(context))
            val n = NotificationCompat.Builder(context, channel)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_stat_prayer)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .addAction(0, L10n.t(S.NotifReadDone), readPi)
                .addAction(0, L10n.t(S.NotifRemindLater), snoozePi)
                .build()
            nm.notify(dhikrId, n)
        }
        if (isSnooze) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val d = AppDatabase.getDatabase(context).dhikrDao().getDhikrById(dhikrId)
                if (d != null && d.isEnabled) DhikrAlarmScheduler(context).schedule(d)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "dhikr_channel"

        /** Creates the azkar reminder channel variant for the current sound/vibration settings. */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val template = NotificationChannel(CHANNEL_ID, "Dhikr Reminders", NotificationManager.IMPORTANCE_HIGH)
            template.description = L10n.t(S.NotifChannelAzkar)
            NotifPrefs.ensureChannel(context, nm, template, NotifPrefs.tone(context), NotifPrefs.vibration(context))
        }
    }
}
