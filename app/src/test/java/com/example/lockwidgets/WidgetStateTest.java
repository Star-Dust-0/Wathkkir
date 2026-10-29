package com.example.lockwidgets;
import android.content.Context;
import android.graphics.Bitmap;
import org.robolectric.RuntimeEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import static org.junit.Assert.*;
/** Strict replacement for the old exploratory test which swallowed every failure. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class WidgetStateTest {
 @Test public void stateAndAllHomeKindsRenderWithoutSwallowedErrors() {
  Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("app_prefs",0).edit().putString("language","en").commit();
  assertNotNull(HomeWidgets.state(c));
  for(String k:new String[]{"Hero","NextPrayer","OneUI","Schedule","Clock","PrayerBar","DayPrayer","Pill","Hijri","Gregorian","HeroLarge","WideBar"}){
   ShadowLog.clear();Bitmap b=HomeWidgets.renderKind(c,k,866,236,2);
   assertNotNull(k,b);assertTrue(k,b.getWidth()>0&&b.getHeight()>0);
   for(ShadowLog.LogItem l:ShadowLog.getLogs())if(l.type>=6&&l.tag.startsWith("Wathakker"))fail(k+": "+l.msg+" "+l.throwable);
  }
 }
}
