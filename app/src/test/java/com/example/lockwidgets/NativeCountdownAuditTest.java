package com.example.lockwidgets;
import android.content.Context;
import android.widget.RemoteViews;
import android.view.View;
import android.widget.Chronometer;
import com.example.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeCountdownAuditTest {
 @Test public void auditEachPublishedHomeKindBeforePrayerAndDuringIqama() throws Exception {
  Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("app_prefs",0).edit().putString("language","en").commit();
  StringBuilder out=new StringBuilder("kind,iqama,native_chronometer\n");
  for(boolean iq:new boolean[]{false,true}) {
   HomeWidgets.State s=HomeWidgets.state(c);s.iqama=iq;s.at=s.now+600000;
   HomeWidgets.batchState.set(s);
   try {
    for(String k:new String[]{"Hero","NextPrayer","PrayerBar","Schedule","DayPrayer","Pill","Clock","Hijri","Gregorian","OneUI"}) {
     RemoteViews rv=HomeWidgets.view(c,k,1,330,110,false);View v=rv.apply(c,null);boolean nativeTimer=false;
     for(int id:new int[]{R.id.ov_chrono_med,R.id.ov_chrono_semi,R.id.ov_f_med,R.id.ov_f_semi}) {
      Chronometer ch=v.findViewById(id);if(ch!=null&&ch.getVisibility()==View.VISIBLE){nativeTimer=true;assertTrue(ch.isCountDown());}
     }
     boolean expected=(k.equals("Hero")||k.equals("NextPrayer")||k.equals("Pill")||k.equals("Clock")||k.equals("OneUI"));
     assertEquals(k+" iqama="+iq,expected,nativeTimer);
     out.append(k+","+iq+","+nativeTimer+"\n");
    }
   } finally {HomeWidgets.batchState.remove();}
  }
  try(java.io.FileWriter f=new java.io.FileWriter("/tmp/native-countdown-home.csv")){f.write(out.toString());}
 }
 @Test public void lockCountdownKindsUseNativeBeforePrayerAndDuringIqama() throws Exception {
  Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("app_prefs",0).edit().putString("language","en").commit();
  java.util.Calendar cal=java.util.Calendar.getInstance();cal.set(2026,9,29,10,0,0);
  long prayer=LockWidgets.timesCached(c,cal)[2];
  StringBuilder out=new StringBuilder("kind,iqama,native_chronometer\n");
  for(boolean iq:new boolean[]{false,true}) {
   android.os.SystemClock.setCurrentTimeMillis(prayer+(iq?180000:-600000));
   for(Class<?> k:new Class<?>[]{LockWidgets.Next.class,LockWidgets.Bar.class,LockWidgets.Countdown.class,LockWidgets.CountdownClean.class}){
    HomeWidgets.ovBegin();HomeWidgets.langForNative.set("en");
    android.graphics.Bitmap b=LockWidgets.renderSize(c,k,1,false);
    RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.lock_widget_image);
    HomeWidgets.ovApply(c,rv,b.getWidth(),b.getHeight());View v=rv.apply(c,null);boolean nativeTimer=false;
    for(int id:new int[]{R.id.ov_chrono_med,R.id.ov_chrono_semi,R.id.ov_f_med,R.id.ov_f_semi}) {
     Chronometer ch=v.findViewById(id);if(ch!=null&&ch.getVisibility()==View.VISIBLE){nativeTimer=true;assertTrue(ch.isCountDown());}
    }
    assertEquals(k.getSimpleName(),k!=LockWidgets.Bar.class,nativeTimer);
    out.append(k.getSimpleName()+","+iq+","+nativeTimer+"\n");
   }
  }
  try(java.io.FileWriter f=new java.io.FileWriter("/tmp/native-countdown-lock.csv")){f.write(out.toString());}
 }

}
