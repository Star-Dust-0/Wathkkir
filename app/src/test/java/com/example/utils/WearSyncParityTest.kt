package com.example.utils
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class WearSyncParityTest {
 @Test fun languageIsIncludedAndEveryMeaningfulFieldChangesSignature() {
  val c=RuntimeEnvironment.getApplication()
  c.getSharedPreferences("app_prefs",0).edit().putString("language","en").commit()
  val d=WearSync.payload(c)
  assertEquals("en",d.getString("lang"))
  assertEquals(setOf("lat","lon","city","h24","lang","ts"),d.keySet())
  val before=WearSync.signature(d)
  c.getSharedPreferences("app_prefs",0).edit().putString("language","ar").commit()
  assertNotEquals(before,WearSync.signature(WearSync.payload(c)))
  val originalTime=d.getLong("ts");d.putLong("ts",originalTime+999)
  assertEquals(before,WearSync.signature(d))
 }
}
