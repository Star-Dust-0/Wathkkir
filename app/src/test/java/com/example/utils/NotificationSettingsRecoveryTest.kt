package com.example.utils
import android.app.Application
import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class NotificationSettingsRecoveryTest {
 @Test fun migrationRunsBeforeViewModelReadsAndVibrationRefreshesChannels() {
  val app=RuntimeEnvironment.getApplication()
  app.getSharedPreferences("app_prefs",0).edit().clear().commit()
  app.getSharedPreferences("fajr_prefs",0).edit().putBoolean("is_vibration_enabled",false).putString("ringtone_uri","content://tones/7").putString("ringtone_title","Original tone").commit()
  val vm=MainViewModel(app)
  assertFalse(vm.vibrationEnabled.value);assertEquals("content://tones/7",vm.notificationSound.value)
  assertEquals("Original tone",NotifPrefs.toneTitle(app))
  vm.updateVibrationEnabled(true);assertTrue(NotifPrefs.vibration(app))
  vm.updateNotificationSound("default");assertEquals("",NotifPrefs.tone(app))
  assertTrue(app.getSharedPreferences("app_prefs",0).getBoolean("notif_unified_v330",false))
 }
}
