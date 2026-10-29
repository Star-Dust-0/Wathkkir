package com.example.lockwidgets;
import android.content.Context;
import android.app.PendingIntent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34)
public class WidgetTapParityTest {
 @Test public void defaultsAndAlternativesMatchOriginalPerKind() {
  Context c=RuntimeEnvironment.getApplication();int id=50;
  for(String k:new String[]{"Hero","HeroLarge","Schedule","Clock","NextPrayer","PrayerBar","DayPrayer","Pill","Hijri","Gregorian","WideBar","OneUI"}) {
   id++;c.getSharedPreferences("fajr_prefs",0).edit().remove("widget_tap_"+id).commit();
   PendingIntent open=HomeWidgets.tapIntent(c,k,id);assertNotNull(k,open);
   assertEquals(k,k.equals("Hijri")||k.equals("Gregorian")?"android.intent.action.MAIN":"com.wathakker.locktest.OPEN_PRAYER",shadowOf(open).getSavedIntent().getAction());
   boolean refresh=java.util.Arrays.asList("Hero","NextPrayer","PrayerBar","Clock","Pill","OneUI").contains(k);
   c.getSharedPreferences("fajr_prefs",0).edit().putString("widget_tap_"+id,refresh?"refresh":"none").commit();
   PendingIntent other=HomeWidgets.tapIntent(c,k,id);
   if(refresh) {assertNotNull(k,other);assertEquals("com.wathakker.widget.REFRESH",shadowOf(other).getSavedIntent().getAction());}
   else assertNull(k,other);
  }
 }
}
