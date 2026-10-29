package com.example.lockwidgets;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class PhoneBatteryParityTest {
 @Test public void singleNonWakingMinutePulseAndWakingBoundaryAreCanceledWithoutWidgets() throws Exception {
  Context c=RuntimeEnvironment.getApplication();
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  LockWidgets.schedule(c);assertEquals(0,shadowOf(am).getScheduledAlarms().size());
  AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,com.example.widget.HeroWidgetProvider.class);
  shadowOf(AppWidgetManager.getInstance(c)).addBoundWidget(81,info);
  LockWidgets.schedule(c);
  int minute=0,boundary=0;
  for(org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm a:shadowOf(am).getScheduledAlarms()) {
   String action=shadowOf(a.operation).getSavedIntent().getAction();
   if(action!=null&&action.endsWith(".TICK")){minute++;assertEquals(AlarmManager.RTC,a.type);}
   if(action!=null&&action.endsWith(".BOUNDARY")){boundary++;assertEquals(AlarmManager.RTC_WAKEUP,a.type);}
  }
  assertEquals(1,minute);assertEquals(1,boundary);
  com.example.widget.WidgetTickScheduler.INSTANCE.schedule(c);
  assertEquals(2,shadowOf(am).getScheduledAlarms().size());
  Object shadow=shadowOf(AppWidgetManager.getInstance(c));
  java.lang.reflect.Field infos=shadow.getClass().getDeclaredField("widgetInfos");
  infos.setAccessible(true);((java.util.Map<?,?>)infos.get(shadow)).clear();
  LockWidgets.schedule(c);assertEquals(0,shadowOf(am).getScheduledAlarms().size());
 }
}
