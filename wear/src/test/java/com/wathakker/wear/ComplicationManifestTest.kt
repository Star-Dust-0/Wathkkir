package com.wathakker.wear
import android.content.ComponentName
import android.content.pm.PackageManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class ComplicationManifestTest {
 @Test fun allSixProvidersAndBoundaryReceiverAreRegistered() {
  val c=RuntimeEnvironment.getApplication()
  for ((cls,type) in listOf(PrayerComplicationService::class.java to "RANGED_VALUE,SHORT_TEXT",DateGregComplication::class.java to "SHORT_TEXT",DateHijriComplication::class.java to "SHORT_TEXT",DateBothComplication::class.java to "LONG_TEXT",NextTimeComplication::class.java to "SHORT_TEXT",UpcomingCountdownComplication::class.java to "SHORT_TEXT")) {
   val i=c.packageManager.getServiceInfo(ComponentName(c,cls),PackageManager.GET_META_DATA)
   assertTrue(i.exported)
   assertEquals("com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER",i.permission)
   assertEquals(type,i.metaData.getString("android.support.wearable.complications.SUPPORTED_TYPES"))
  }
  assertFalse(c.packageManager.getReceiverInfo(ComponentName(c,ComplicationBoundaryReceiver::class.java),0).exported)
 }
}
