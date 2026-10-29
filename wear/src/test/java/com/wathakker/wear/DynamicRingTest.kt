package com.wathakker.wear
import android.os.Build
import androidx.wear.watchface.complications.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
class DynamicRingTest {
 private fun refreshPeriod(): Int {
  val c=org.robolectric.RuntimeEnvironment.getApplication()
  return c.packageManager.getServiceInfo(android.content.ComponentName(c,PrayerComplicationService::class.java),android.content.pm.PackageManager.GET_META_DATA).metaData.getInt("android.support.wearable.complications.UPDATE_PERIOD_SECONDS")
 }
 private fun data(): RangedValueComplicationData {
  val s=Robolectric.buildService(PrayerComplicationService::class.java).create().get()
  val now=1793300000000L
  val a=Slot(0,"Fajr",now-600000);val b=Slot(2,"Dhuhr",now+600000)
  val state=PrayerState(now,listOf(a,b),a,b,b)
  return s.build(ComplicationType.RANGED_VALUE,state) as RangedValueComplicationData
 }
 @Test @Config(sdk=[34]) fun supportedPlatformGetsSystemClockExpressionAndStaticFallback() {
  val d=data();assertEquals(0, refreshPeriod());assertNotNull(d.dynamicValue);assertEquals(10f,d.value,0.01f)
  assertTrue(d.text is TimeDifferenceComplicationText)
  assertTrue(d.dynamicValue!!.toDynamicFloatByteArray().size > 30)
  assertEquals(20f,d.max,0.01f)
 }
 @Test @Config(sdk=[30]) fun olderPlatformGetsSafeStaticRingAndNativeCountdownText() {
  val d=data();assertEquals(300, refreshPeriod());assertNull(d.dynamicValue);assertEquals(10f,d.value,0.01f)
  assertTrue(d.text is TimeDifferenceComplicationText)
 }
 @Test @Config(sdk=[34]) fun platformEvaluatorAdvancesAndClampsRingWithoutProviderUpdates() {
  val service=Robolectric.buildService(PrayerComplicationService::class.java).create().get()
  val start=java.time.Instant.parse("2026-10-30T10:00:00Z")
  var clock=start
  var receiver: Runnable?=null
  val notifier=object: androidx.wear.protolayout.expression.pipeline.PlatformTimeUpdateNotifier {
   override fun setReceiver(e: java.util.concurrent.Executor,r: Runnable) { receiver=Runnable{e.execute(r)} }
   override fun clearReceiver() {receiver=null}
  }
  val config=androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator.Config.Builder()
   .setClock {clock}.setPlatformTimeUpdateNotifier(notifier).build()
  val evaluator=androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator(config)
  var value=-1f
  val request=androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest.forDynamicFloat(
   service.nativeRing(start.toEpochMilli(),start.plusSeconds(1200).toEpochMilli()),java.util.concurrent.Executor{it.run()},
   object:androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver<Float>{
    override fun onData(v:Float){value=v}
    override fun onInvalidated(){fail("clock expression invalidated")}
   })
  val bound=evaluator.bind(request);bound.startEvaluation()
  for((seconds,want) in listOf(-60L to 0f,0L to 0f,30L to .5f,600L to 10f,1200L to 20f,1500L to 20f)) {
   clock=start.plusSeconds(seconds);receiver!!.run();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
   assertEquals("seconds="+seconds,want,value,.001f)
  }
  bound.close()
 }

}
