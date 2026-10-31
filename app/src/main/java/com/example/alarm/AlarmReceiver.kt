package com.example.alarm

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.receiver.NotificationActionReceiver
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.utils.NotifPrefs
import kotlinx.coroutines.*

/** Secondary reconstruction of original APK AlarmReceiver, using recovered data/channel APIs. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        L10n.init(context)
        val id = intent.getIntExtra("DHIKR_ID", -1)
        if (id == -1) return
        val title = intent.getStringExtra("DHIKR_TITLE") ?: "ذكر"
        val content = intent.getStringExtra("DHIKR_CONTENT") ?: ""
        val nm = context.getSystemService(NotificationManager::class.java)
        createNotificationChannel(context)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val launch = Intent(context, MainActivity::class.java).setFlags(872415232)
            .putExtra("DHIKR_ID",id).putExtra("NOTIFICATION_ID",id)
        fun action(name: String, code: Int) = PendingIntent.getBroadcast(context,code,
            Intent(context,NotificationActionReceiver::class.java).setAction(name).putExtra("DHIKR_ID",id)
                .putExtra("DHIKR_TITLE",title).putExtra("DHIKR_CONTENT",content).putExtra("NOTIFICATION_ID",id),flags)
        val notification = NotificationCompat.Builder(context,NotifPrefs.channelId(context,"dhikr_channel"))
            .setContentTitle(title).setContentText(content).setSmallIcon(R.drawable.ic_stat_prayer)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.mipmap.ic_launcher))
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentIntent(PendingIntent.getActivity(context,id*10+1,launch,flags))
            .addAction(0,L10n.t(S.NotifReadDone),action(NotificationActionReceiver.ACTION_MARK_AS_READ,id*10+2))
            .addAction(0,L10n.t(S.NotifRemindLater),action(NotificationActionReceiver.ACTION_SNOOZE,id*10+3)).build()
        nm?.notify(id,notification)
        if (!intent.getBooleanExtra("IS_SNOOZE",false)) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { AppDatabase.getDatabase(context).dhikrDao().getDhikrById(id)?.let { if(it.isEnabled) AlarmScheduler(context).schedule(it) } }
                finally { pending.finish() }
            }
        }
    }
    companion object {
        fun createNotificationChannel(context: Context) {
            if(android.os.Build.VERSION.SDK_INT >= 26) {
                val nm = context.getSystemService(NotificationManager::class.java)
                NotifPrefs.configureChannel(context,nm,NotificationChannel("dhikr_channel",L10n.t(S.NotifChannelAzkar),NotificationManager.IMPORTANCE_HIGH))
            }
        }
    }
}
