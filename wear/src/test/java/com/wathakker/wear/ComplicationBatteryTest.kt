package com.wathakker.wear
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class ComplicationBatteryTest {
 @Test fun legacyMinuteWakeIsCanceledWhileBoundaryIsPreserved() {
  val s=Robolectric.buildService(PrayerComplicationService::class.java).create().get()
  val am=s.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val pi=PendingIntent.getBroadcast(s,8001,Intent(s,ComplicationTickReceiver::class.java).setAction("tick_"+s.selfName).putExtra("cls",s.selfName),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,pi)
  assertEquals(1,shadowOf(am).scheduledAlarms.size)
  s.cancelLegacyMinutePulse()
  assertEquals(0,shadowOf(am).scheduledAlarms.size)
  s.scheduleBoundary(System.currentTimeMillis()+3600000)
  assertEquals(1,shadowOf(am).scheduledAlarms.size)
  assertEquals(AlarmManager.RTC_WAKEUP,shadowOf(am).scheduledAlarms[0].type)
  s.cancelLegacyMinutePulse()
  assertEquals(1,shadowOf(am).scheduledAlarms.size)
 }
}
