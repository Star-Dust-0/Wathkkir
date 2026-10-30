package com.example.receiver
import android.content.*
import com.example.data.AppDatabase
import com.example.alarm.AlarmScheduler
import kotlinx.coroutines.*
object BootRecovery {
 @JvmStatic fun restore(context: Context,pending: BroadcastReceiver.PendingResult) {
  CoroutineScope(Dispatchers.IO).launch {
   try { val scheduler=AlarmScheduler(context);AppDatabase.getDatabase(context).dhikrDao().getEnabledDhikrSync().forEach {scheduler.schedule(it)} }
   finally {pending.finish()}
  }
 }
}
