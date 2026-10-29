package com.example.prayer.alerts;
import android.content.Context;
import com.example.utils.NotifPrefs;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class PrayerOverrideRecoveryTest {
 @Test public void prayerOverridesStayLocalAndCancelRestoresSnapshot() {
  Context c=RuntimeEnvironment.getApplication();
  PrayerAlertStore s=PrayerAlertStore.INSTANCE;
  NotifPrefs.INSTANCE.setTone(c,"content://shared","Shared");
  NotifPrefs.INSTANCE.setVibration(c,true);
  Map<String,Object> before=s.snapshot(c,2);
  s.setToneOverride(c,2,"content://custom","Custom");
  s.setVibrationOverride(c,2,false);
  assertEquals("content://custom",s.effectiveTone(c,2));
  assertFalse(s.effectiveVibration(c,2));
  assertEquals("content://shared",s.effectiveTone(c,3));
  assertTrue(s.effectiveVibration(c,3));
  s.restore(c,2,before);
  assertFalse(s.hasOverride(c,2));
  assertEquals("content://shared",s.effectiveTone(c,2));
  s.setToneOverride(c,2,"content://custom","Custom");
  s.clearOverride(c,2);
  assertFalse(s.hasOverride(c,2));
  assertEquals("content://shared",NotifPrefs.INSTANCE.tone(c));
 }
}
