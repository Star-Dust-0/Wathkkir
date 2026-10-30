package com.wathakker.wear
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
class NativeTileTest {
 @Test @Config(sdk=[34]) fun supportedHostGetsNativeArcAndBoundaryRefreshOlderHostKeepsSnapshot() {
  val service=Robolectric.buildService(PrayerTileService::class.java).create().get()
  fun tile(minor:Int): androidx.wear.tiles.TileBuilders.Tile {
   val version=androidx.wear.protolayout.expression.VersionBuilders.VersionInfo.Builder().setMajor(1).setMinor(minor).build()
   val device=androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters.Builder().setRendererSchemaVersion(version).build()
   return service.onTileRequest(androidx.wear.tiles.RequestBuilders.TileRequest.Builder().setDeviceConfiguration(device).build()).get()
  }
  val old=tile(1);val native=tile(2)
  assertEquals(60000L,old.freshnessIntervalMillis)
  assertTrue(native.freshnessIntervalMillis in 1000L..86400000L)
  fun fill(tile:androidx.wear.tiles.TileBuilders.Tile): androidx.wear.protolayout.LayoutElementBuilders.ArcLine {
   val box=tile.tileTimeline!!.timelineEntries[0].layout!!.root as androidx.wear.protolayout.LayoutElementBuilders.Box
   val arc=box.contents[1] as androidx.wear.protolayout.LayoutElementBuilders.Arc
   return arc.contents[0] as androidx.wear.protolayout.LayoutElementBuilders.ArcLine
  }
  assertNull(fill(old).length!!.dynamicValue)
  assertNotNull(fill(native).length!!.dynamicValue)
 }
 @Test @Config(sdk=[34]) fun tileArcAdvancesWithSystemClockAndClampsToOriginalLimits() {
  val service=Robolectric.buildService(PrayerTileService::class.java).create().get()
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
   service.nativeArc(PrayerState(start.toEpochMilli(),emptyList(),Slot(0,"Fajr",start.toEpochMilli()),Slot(2,"Dhuhr",start.plusSeconds(1200).toEpochMilli()),Slot(0,"Fajr",start.plusSeconds(86400).toEpochMilli()))),java.util.concurrent.Executor{it.run()},
   object:androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver<Float>{
    override fun onData(v:Float){value=v}
    override fun onInvalidated(){fail("clock expression invalidated")}
   })
  val bound=evaluator.bind(request);bound.startEvaluation()
  for((seconds,want) in listOf(-60L to 5.4f,0L to 5.4f,30L to 6.75f,600L to 135f,1200L to 270f,1500L to 270f)) {
   clock=start.plusSeconds(seconds);receiver!!.run();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
   assertEquals("seconds="+seconds,want,value,.001f)
  }
  bound.close()
 }

}
