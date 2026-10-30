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
 @Test public void arabicHeroTextIsDrawnOnceWithMirroredAlignment() {
  Context c=RuntimeEnvironment.getApplication();
  android.graphics.Typeface face=HomeWidgets.face(c,false,true);
  android.graphics.Bitmap actual=android.graphics.Bitmap.createBitmap(400,100,android.graphics.Bitmap.Config.ARGB_8888);
  android.graphics.Bitmap expected=android.graphics.Bitmap.createBitmap(400,100,android.graphics.Bitmap.Config.ARGB_8888);
  HomeWidgets.ovBegin();HomeWidgets.ovToken.remove();
  HomeWidgets.heroText(new android.graphics.Canvas(actual),"القاهرة",10,50,360,20,face,0xff000000,android.text.Layout.Alignment.ALIGN_NORMAL,1);
  android.text.TextPaint paint=new android.text.TextPaint(1);paint.setTypeface(face);paint.setTextSize(20);paint.setColor(0xff000000);
  android.text.StaticLayout layout=android.text.StaticLayout.Builder.obtain("القاهرة",0,7,paint,360)
   .setAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE).setTextDirection(android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR)
   .setIncludePad(false).setMaxLines(1).build();
  android.graphics.Canvas canvas=new android.graphics.Canvas(expected);canvas.translate(10,50-layout.getHeight()/2f);layout.draw(canvas);
  assertTrue("RTL text must not leave a second copy on the opposite side",actual.sameAs(expected));
  HomeWidgets.ovWanted.remove();
 }
 @Test public void lockNativeOverlayUsesSameFitCenterAsArtwork() {
  HomeWidgets.ovGeo.set(new float[]{123f,81f,100f,18f,0xff000000,0f});
  LockWidgets.fitNativeOverlay(246,108,214,76);
  float scale=76f/108f;float[] g=HomeWidgets.ovGeo.get();
  assertEquals(107f,g[0],.01f);assertEquals(81f*scale,g[1],.01f);
  assertEquals(100f*scale,g[2],.01f);assertEquals(18f*scale,g[3],.01f);
  HomeWidgets.ovGeo.remove();
 }
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
