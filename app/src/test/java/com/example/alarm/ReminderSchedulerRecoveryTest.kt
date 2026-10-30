package com.example.alarm
import android.app.AlarmManager
import android.content.Context
import com.example.data.Dhikr
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class ReminderSchedulerRecoveryTest {
 @Test fun rescheduleReplacesOldTimesAndCancelRemovesAllAlarms() {
  val c=RuntimeEnvironment.getApplication();val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val scheduler=DhikrAlarmScheduler(c)
  val d=Dhikr(id=77,title="Title",content="Text",reminderTimes=listOf("09:00","14:30","bad"))
  scheduler.schedule(d);assertEquals(2,shadowOf(am).scheduledAlarms.size)
  for(a in shadowOf(am).scheduledAlarms) {
   val intent=shadowOf(a.operation).savedIntent
   assertEquals(77,intent.getIntExtra("DHIKR_ID",-1));assertEquals("Text",intent.getStringExtra("DHIKR_CONTENT"))
   assertEquals(AlarmManager.RTC_WAKEUP,a.type)
   assertTrue(a.triggerAtTime>=System.currentTimeMillis())
  }
  scheduler.schedule(d.copy(reminderTimes=listOf("23:59")));assertEquals(1,shadowOf(am).scheduledAlarms.size)
  scheduler.cancel(d);assertTrue(shadowOf(am).scheduledAlarms.isEmpty())
 }
}
