package com.example.receiver
import android.app.NotificationManager
import android.content.*
import com.example.alarm.AlarmScheduler
import com.example.data.*
import com.example.ui.i18n.L10n
import kotlinx.coroutines.*
/** Secondary reconstruction of original APK receiver action routing. */
class NotificationActionReceiver : BroadcastReceiver() {
 override fun onReceive(context: Context, intent: Intent) {
  L10n.init(context)
  val notificationId = intent.getIntExtra("NOTIFICATION_ID",-1)
  val id = intent.getIntExtra("DHIKR_ID",-1)
  if(notificationId != -1) context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
  if(id == -1) return
  when(intent.action) {
   ACTION_MARK_AS_READ -> { val pending=goAsync(); CoroutineScope(Dispatchers.IO).launch {
    try { val db=AppDatabase.getDatabase(context); DhikrRepository(db.dhikrDao(),db.tagDao(),db.historyDao()).markAsRead(id) }
    finally {pending.finish()}
   } }
   ACTION_SNOOZE -> AlarmScheduler(context).scheduleSnooze(id,intent.getStringExtra("DHIKR_TITLE") ?: "ذكر",intent.getStringExtra("DHIKR_CONTENT") ?: "",10)
  }
 }
 companion object {
  const val ACTION_MARK_AS_READ="com.example.ACTION_MARK_AS_READ"
  const val ACTION_SNOOZE="com.example.ACTION_SNOOZE"
 }
}
